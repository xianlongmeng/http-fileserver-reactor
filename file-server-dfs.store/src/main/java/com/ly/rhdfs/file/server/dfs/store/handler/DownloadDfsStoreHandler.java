package com.ly.rhdfs.file.server.dfs.store.handler;

import com.ly.common.constant.ParamConstants;
import com.ly.common.domain.PartRange;
import com.ly.common.service.FileChunkReader;
import com.ly.common.util.ConvertUtil;
import com.ly.common.util.DateFormatUtils;
import com.ly.etag.ETagComputer;
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

import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.store.manager.StoreManager;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DownloadDfsStoreHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private StoreManager storeManager;
    private ServerConfig serverConfig;
    private StoreFile storeFile;
    private ETagComputer eTagComputer;


    @Autowired
    private void setStoreManager(StoreManager storeManager) {
        this.storeManager = storeManager;
    }

    @Autowired
    private void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Autowired
    public void setETagComputer(ETagComputer eTagComputer) {
        this.eTagComputer = eTagComputer;
    }

    @Autowired
    public void setStoreFile(StoreFile storeFile){
        this.storeFile=storeFile;
    }

    public Mono<ServerResponse> downloadStoreFile(ServerRequest request) {
        // 1/If-Match(416) && If-Unmodified-Since(412)
        // 2/If-Modified-Since(304)
        // 3/If-None-Match(304)

        // If-Range
        // Range
        String filePath = request.queryParam(ParamConstants.PARAM_PATH_NAME).orElse("");
        String fn = request.queryParam(ParamConstants.PARAM_FILE_NAME).orElse("");
        int chunk= ConvertUtil.parseInt(request.queryParam(ParamConstants.PARAM_CHUNK).orElse("0"),0);
        if (StringUtils.isEmpty(filePath) || StringUtils.isEmpty(fn)) {
            //文件资源不存在
            return ServerResponse.notFound().build();
        }
        String fileName=String.format("%s.%d.%s",fn,chunk,serverConfig.getFileChunkSuffix());
        String ifRange = request.headers().firstHeader(HttpHeaders.IF_RANGE);
        List<HttpRange> rangeList=request.headers().range();
        AtomicBoolean ifRangeResult = new AtomicBoolean(true);
        boolean etagFlag = false;
        int ifRangeType = 0;
        if (rangeList.isEmpty()) {
            ifRangeResult.set(false);
        }else if (!StringUtils.isEmpty(ifRange)) {
            if (ifRange.contains("\"")) {
                if (ifRange.startsWith("W/")) {
                    //弱比较
                    ifRangeType = 2;
                } else {
                    ifRangeType = 1;
                }
                //需要比较etag
                etagFlag = true;
            }else{
                //比较时间
                ifRangeType=3;
            }
        }
        //构建返回下载文件资源的Mono
        Mono<ServerResponse> responseMono = Mono.defer(() -> Mono.just(ifRangeResult)).flatMap(ifRangeResultValue -> {
            if (ifRangeResultValue.get()) {
                //ifRange结果为真，返回部分range文件
                return storeFile.loadFile(fileName, filePath, request.headers().range()).flatMap(fileRanges -> {
                    if (fileRanges.getPartRangeList() == null && fileRanges.getResource() == null) {
                        //未找到资源
                        return ServerResponse.notFound().build();
                    } else if (fileRanges.getResource() != null) {
                        //直接返回资源，不分组
                        return ServerResponse.ok().body((p, c) -> {
                            ResourceHttpMessageWriter resp = new ResourceHttpMessageWriter();
                            return resp.write(Flux.just(fileRanges.getResource()), null,
                                    ResolvableType.forInstance(fileRanges.getResource()), MediaType.MULTIPART_FORM_DATA,
                                    request.exchange().getRequest(), p, Hints.none());
                        });
                    } else if (fileRanges.getPartRangeList().size() == 1) {
                        //只有一个part，拼接后，直接返回
                        PartRange partRange = fileRanges.getPartRangeList().get(0);
                        return ServerResponse.status(HttpStatus.PARTIAL_CONTENT)
                                .header(HttpHeaders.CONTENT_RANGE, fileRanges.toContentRange(partRange))
                                .body((p, a) -> p.writeAndFlushWith(Flux.fromIterable(partRange.getResourceRegionList())
                                        .flatMap(resourceRegion -> Flux
                                                .just(FileChunkReader
                                                        .readFile2Buffer(resourceRegion.getResource(),
                                                                resourceRegion.getPosition(), resourceRegion.getCount())))));
                    } else {
                        //多个part，需要分别获取，并采用MultiPart的方法返回
                        return ServerResponse
                                .status(HttpStatus.PARTIAL_CONTENT)
                                .body((p, c) -> {
                                    MultipartHttpMessageWriter multiWriter = new MultipartHttpMessageWriter(
                                            Arrays.asList(new EncoderHttpMessageWriter<>(new DataBufferEncoder()),
                                                    new ResourceHttpMessageWriter()));
                                    return multiWriter
                                            .write(Flux.fromIterable(fileRanges.getPartRangeList())
                                                            .collect(MultipartBodyBuilder::new,
                                                                    (bodyBuild, partRange) -> bodyBuild
                                                                            .asyncPart(partRange.getName(),
                                                                                    Flux.fromIterable(partRange.getResourceRegionList())
                                                                                            .flatMap(resourceRegion -> FileChunkReader
                                                                                                    .readFile2Buffer(resourceRegion.getResource(),
                                                                                                            resourceRegion.getPosition(),
                                                                                                            resourceRegion.getCount())),
                                                                                    DataBuffer.class))
                                                            .map(MultipartBodyBuilder::build),
                                                    ResolvableType.forClass(DataBuffer.class),
                                                    MediaType.MULTIPART_FORM_DATA,
                                                    p,
                                                    Hints.none());
                                });

                    }
                });
            } else {
                //直接返回整个文件，使用zeroCopy
                return ServerResponse.ok().body((p, a) -> {
                    var resp = (ZeroCopyHttpOutputMessage) p;
                    return resp.writeWith(storeFile.takeFilePath(fileName, filePath), 0,
                            storeFile.takeFileSize(fileName, filePath));
                });
            }
        });
        //验证
        List<String> ifMatchList = request.headers().asHttpHeaders().getIfMatch();
        List<String> ifNoneMatchList = request.headers().asHttpHeaders().getIfNoneMatch();
        if (!ifMatchList.isEmpty() || !ifNoneMatchList.isEmpty()) {
            //需要比较etag
            etagFlag = true;
        }
        if (etagFlag) {
            int finalIfRangeType = ifRangeType;
            //验证etag模式
            return eTagComputer.etagFile(storeFile.takeFilePath(fn, filePath)).flatMap(etag -> {
                if (finalIfRangeType == 2) {
                    //ifRange为etag的弱比较模式
                    ifRangeResult.set(etag.contains(ifRange));
                } else if (finalIfRangeType == 1) {
                    //ifRange为etag的比较模式
                    ifRangeResult.set(etag.equals(ifRange));
                } else if (finalIfRangeType == 3){
                    //时间比较模式
                    ifRangeResult.set(storeFile.takeFileUpdateTime(fn, filePath)
                            .equals(DateFormatUtils.parseLocalDateTime4GMT(ifRange)));
                }
                //验证checkModify
                return request.checkNotModified(storeFile.takeFileInstant(fn, filePath), etag);
            }).switchIfEmpty(responseMono);
        } else {
            //验证ifRange的时间模式
            if (ifRangeType == 3) {
                ifRangeResult.set(storeFile.takeFileUpdateTime(fileName, filePath)
                        .equals(DateFormatUtils.parseLocalDateTime4GMT(ifRange)));
            }
            return responseMono;
        }

    }
}
