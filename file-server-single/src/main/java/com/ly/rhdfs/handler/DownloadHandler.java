package com.ly.rhdfs.handler;

import com.ly.common.constant.ParamConstants;
import com.ly.common.domain.PartRange;
import com.ly.common.service.FileChunkReader;
import com.ly.common.util.DateFormatUtils;
import com.ly.etag.ETagComputer;
import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.store.StoreFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DownloadHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private StoreFile storeFile;
    private ETagComputer eTagComputer;
    private ServerConfig serverConfig;

    @Autowired
    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Autowired
    public void setETagComputer(ETagComputer eTagComputer) {
        this.eTagComputer = eTagComputer;
    }

    @Autowired
    public void setStoreFile(StoreFile storeFile) {
        this.storeFile = storeFile;
    }
    
    @NonNull
    public Mono<ServerResponse> downloadFile(ServerRequest request) {
        String path = request.pathVariable("path");
        if (org.apache.commons.lang3.StringUtils.isEmpty(path))
            path = request.queryParam(serverConfig.getPathParamName()).orElse(ParamConstants.PARAM_PATH_NAME);
        int index = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
        String fName = null;
        if (index == -1) {
            fName = path;
            path = "";
        } else if (index == path.length() - 1) {
            path = path.substring(0, index);
            request.queryParam(serverConfig.getFileNameParamName()).orElse(ParamConstants.PARAM_FILE_NAME);
        } else {
            fName = path.substring(index + 1);
            path = path.substring(0, index);
        }
        // 1/If-Match(416) && If-Unmodified-Since(412)
        // 2/If-Modified-Since(304)
        // 3/If-None-Match(304)

        // If-Range
        // Range
        String filePath = path;
        String fileName = fName;
        if (StringUtils.isEmpty(fileName)) {
            // 文件资源不存在
            return ServerResponse.notFound().build();
        }
        String ifRange = request.headers().firstHeader(HttpHeaders.IF_RANGE);
        List<HttpRange> rangeList = request.headers().range();
        AtomicBoolean ifRangeResult = new AtomicBoolean(true);
        boolean etagFlag = false;
        int ifRangeType = 0;
        if (rangeList.isEmpty()) {
            ifRangeResult.set(false);
        } else if (!StringUtils.isEmpty(ifRange)) {
            if (ifRange.contains("\"")) {
                if (ifRange.startsWith("W/")) {
                    // 弱比较
                    ifRangeType = 2;
                } else {
                    ifRangeType = 1;
                }
                // 需要比较etag
                etagFlag = true;
            } else {
                // 比较时间
                ifRangeType = 3;
            }
        }
        // 构建返回下载文件资源的Mono
        Mono<ServerResponse> responseMono = Mono.defer(() -> Mono.just(ifRangeResult)).flatMap(ifRangeResultValue -> {
            if (ifRangeResultValue.get()) {
                // ifRange结果为真，返回部分range文件
                return storeFile.loadFile(fileName, filePath, request.headers().range())
                        .flatMap(fileRanges -> {
                            if (fileRanges.getResource() != null) {
                                // 直接返回资源，不分组
                               return ServerResponse.ok().body((p, c) -> {
                                        ResourceHttpMessageWriter resp = new ResourceHttpMessageWriter();
                                        return resp.write(Flux.just(fileRanges.getResource()), null,
                                                ResolvableType.forInstance(fileRanges.getResource()), MediaType.MULTIPART_FORM_DATA,
                                                request.exchange().getRequest(), p, Hints.none());
                                    });
                            }else{
                                return ServerResponse.notFound().build();
                            }
                        });
            } else {
                // 直接返回整个文件，使用zeroCopy
                return ServerResponse.ok().body((p, a) -> {
                    var resp = (ZeroCopyHttpOutputMessage) p;
                    return resp.writeWith(storeFile.takeFilePath(fileName, filePath), 0,
                            storeFile.takeFileSize(fileName, filePath));
                });
            }
        });
        // 验证
        List<String> ifMatchList = request.headers().asHttpHeaders().getIfMatch();
        List<String> ifNoneMatchList = request.headers().asHttpHeaders().getIfNoneMatch();
        if (!ifMatchList.isEmpty() || !ifNoneMatchList.isEmpty()) {
            // 需要比较etag
            etagFlag = true;
        }
        if (etagFlag) {
            int finalIfRangeType = ifRangeType;
            // 验证etag模式
            return eTagComputer.etagFile(storeFile.takeFilePath(fileName, filePath)).flatMap(etag -> {
                if (finalIfRangeType == 2) {
                    // ifRange为etag的弱比较模式
                    ifRangeResult.set(etag.contains(ifRange));
                } else if (finalIfRangeType == 1) {
                    // ifRange为etag的比较模式
                    ifRangeResult.set(etag.equals(ifRange));
                } else if (finalIfRangeType == 3) {
                    // 时间比较模式
                    ifRangeResult.set(storeFile.takeFileUpdateTime(fileName, filePath)
                            .equals(DateFormatUtils.parseLocalDateTime4GMT(ifRange)));
                }
                // 验证checkModify
                return request.checkNotModified(storeFile.takeFileInstant(fileName, filePath), etag);
            }).switchIfEmpty(responseMono);
        } else {
            // 验证ifRange的时间模式
            if (ifRangeType == 3) {
                ifRangeResult.set(storeFile.takeFileUpdateTime(fileName, filePath).withNano(0)
                        .equals(DateFormatUtils.parseLocalDateTime4GMT(ifRange)));
            }
            return responseMono;
        }

    }

}
