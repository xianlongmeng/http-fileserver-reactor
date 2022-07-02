package com.ly.rhdfs.handler;

import com.ly.common.constant.ParamConstants;
import com.ly.common.domain.PartChunk;
import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.ResultValueInfo;
import com.ly.common.domain.UploadResultInfo;
import com.ly.common.util.ConvertUtil;
import com.ly.common.util.ToolUtils;
import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.store.StoreFile;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.NonNullApi;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
public class UploadHandler {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private StoreFile storeFile;
    
    private ServerConfig serverConfig;
    
    @Autowired
    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }
    
    @Autowired
    void setStoreFile(StoreFile storeFile) {
        this.storeFile = storeFile;
    }
    
    /**
     * 上传文件不分片
     *
     * @param request
     * @return
     */
    @NonNull
    public Mono<ServerResponse> uploadFileSelf(ServerRequest request) {
        
        String path = request.pathVariable("path");
        if (StringUtils.isEmpty(path))
            path = request.queryParam(serverConfig.getPathParamName()).orElse("path");
        int index = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
        String fileName = null;
        if (index == -1) {
            fileName = path;
            path = "";
        } else if (index == path.length() - 1) {
            path = path.substring(0, index);
        } else {
            fileName = path.substring(index + 1);
            path = path.substring(0, index);
        }
        String finalPath = path;
        String finalFileName = fileName;
        return request.body(BodyExtractors.toParts()).flatMap(part -> {
            if (part instanceof FilePart) {
                FilePart filePart = (FilePart) part;
                String fname = finalFileName;
                if (StringUtils.isEmpty(finalFileName)) {
                    fname = filePart.filename();
                }
                logger.info("name:{};fileName:{}", filePart.name(), fname);
                if (!serverConfig.isRewrite() && storeFile.existed(fname, finalPath)) {
                    logger.warn("file is existed and can not rewrite.fileName:{}", filePart.filename());
                    return Flux.just(false);
                }
                String finalFname = fname;
                return Flux.create(sink -> {
                    Path filePath = storeFile.takeFilePath(finalFname, finalPath);
                    try {
                        File file = filePath.toFile();
                        if (!file.getParentFile().exists()) {
                            file.getParentFile().mkdirs();
                        }
                        AsynchronousFileChannel asynchronousFileChannel = AsynchronousFileChannel.open(
                                filePath, StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE);
                        sink.onDispose(() -> ToolUtils.closeChannel(asynchronousFileChannel));
                        DataBufferUtils.write(filePart.content(), asynchronousFileChannel, 0)
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe(DataBufferUtils::release, sink::error, sink::complete);
                    } catch (IOException ex) {
                        logger.error("write file {} is error", filePath, ex);
                        sink.error(ex);
                    }
                }).materialize().map(signal -> {
                    if (signal.isOnError())
                        logger.error("map isOnError:{}", signal.isOnError(), signal.getThrowable());
                    return !signal.isOnError();
                });
            } else {
                logger.info("name:{}", part.name());
            }
            return Flux.just(true);
        }).filter(value -> {
            logger.info("filter:{}", value);
            return !value;
        }).count().flatMap(c -> {
            logger.info("count:{}", c);
            if (c <= 0)
                return ServerResponse.accepted().bodyValue("success");
            else
                return ServerResponse.badRequest().bodyValue(c + " files upload failed");
        });
    }
    
    /**
     * 上传文件，分片，检查
     *
     * @param request
     * @return
     */
    @NonNull
    public Mono<ServerResponse> uploadFile(ServerRequest request) {
        // 1/If-Match(412) && If-Unmodified-Since(412)
        //???是否需要FileSize
        PartChunk partChunk;
        boolean chunked = Boolean.parseBoolean(request.queryParam(ParamConstants.PARAM_CHUNKED).orElse("false"));
        if (chunked) {
            int chunk = ConvertUtil.parseInt(request.queryParam(ParamConstants.PARAM_CHUNK).orElse("0"), 0);
            int chunkSize = ConvertUtil.parseInt(request.queryParam(ParamConstants.PARAM_CHUNK_SIZE).orElse("0"), 0);
            int chunkCount = ConvertUtil.parseInt(request.queryParam(ParamConstants.PARAM_CHUNK_COUNT).orElse("0"), 1);
            partChunk = new PartChunk(true, chunk, chunkSize, chunkCount);
        } else {
            partChunk = new PartChunk(false);
        }
        String path = request.pathVariable("path");
        if (StringUtils.isEmpty(path))
            path = request.queryParam(serverConfig.getPathParamName()).orElse(ParamConstants.PARAM_PATH_NAME);
        int index = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
        String fileName = null;
        if (index == -1) {
            fileName = path;
            path = "";
        } else if (index == path.length() - 1) {
            path = path.substring(0, index);
        } else {
            fileName = path.substring(index + 1);
            path = path.substring(0, index);
        }
        String finalPath = path;
        String finalFileName = fileName;
        return request.body(BodyExtractors.toParts())
                .single().onErrorResume(t -> Mono.empty())
                .flatMap(part -> {
                    if (part instanceof FilePart) {
                        FilePart filePart = (FilePart) part;
                        if (!serverConfig.isRewrite() && storeFile.existed(filePart.filename(), finalPath)) {
                            logger.warn("file is existed and can not rewrite.fileName:{}", filePart.filename());
                            return Mono.just(
                                    new ResultValueInfo<>(ResultInfo.S_ERROR, "file.exist.100", "file is exist", filePart));
                        } else {
                            return storeFile.storeFile(filePart, finalPath, finalFileName, partChunk);
                        }
                    } else {
                        return Mono.just(new ResultValueInfo<>(ResultInfo.S_ERROR, "part.100", "not file part!", part));
                    }
                }).map(resultValueInfo -> new UploadResultInfo(resultValueInfo.getResult(),
                        resultValueInfo.getErrorCode(),
                        resultValueInfo.getErrorDesc(),
                        resultValueInfo.getSource().name(),
                        finalFileName,
                        partChunk))
                .flatMap(uploadResultInfo -> ServerResponse.accepted().contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(uploadResultInfo))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("no part data!"));
    }
}
