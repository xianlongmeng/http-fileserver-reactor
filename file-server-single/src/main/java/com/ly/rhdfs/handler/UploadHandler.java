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

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;

@Component
public class UploadHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private StoreFile storeFile;

    private ServerConfig serverConfig;
    @Autowired
    public void setServerConfig(ServerConfig serverConfig){
        this.serverConfig=serverConfig;
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
    public Mono<ServerResponse> uploadFileSelf(ServerRequest request) {

        String path = request.pathVariable("path");
        if(StringUtils.isEmpty(path))
            path = request.queryParam(serverConfig.getPathParamName()).orElse("path");
        String finalPath = path;
        return request.body(BodyExtractors.toParts()).flatMap(part -> {
            if (part instanceof FilePart) {
                FilePart filePart = (FilePart) part;

                logger.info("name:{};fileName:{}", filePart.name(), filePart.filename());
                if (!serverConfig.isRewrite() && storeFile.existed(filePart.filename(), finalPath)) {
                    logger.warn("file is existed and can not rewrite.fileName:{}", filePart.filename());
                    return Flux.just(false);
                }
                return Flux.create(sink -> {
                    try {
                        AsynchronousFileChannel asynchronousFileChannel = AsynchronousFileChannel.open(
                                storeFile.takeFilePath(filePart.filename(), finalPath), StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE);
                        sink.onDispose(() -> ToolUtils.closeChannel(asynchronousFileChannel));
                        DataBufferUtils.write(filePart.content(), asynchronousFileChannel, 0)
                                .subscribe(DataBufferUtils::release, sink::error, sink::complete);
                    } catch (IOException ex) {
                        sink.error(ex);
                    }
                }).materialize().map(signal -> {
                    logger.info("map isOnError:{}", signal.isOnError());
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
                return ServerResponse.accepted().bodyValue("aa");
            else
                return ServerResponse.badRequest().bodyValue("bb");
        });
    }

    /**
     * 上传文件，分片，检查
     *
     * @param request
     * @return
     */
    public Mono<ServerResponse> uploadFile(ServerRequest request) {
        // 1/If-Match(412) && If-Unmodified-Since(412)

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
        if(StringUtils.isEmpty(path))
            path = request.queryParam(serverConfig.getPathParamName()).orElse(ParamConstants.PARAM_PATH_NAME);
        String finalPath = path;
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
                            return storeFile.storeFile(filePart, finalPath, partChunk);
                        }
                    } else {
                        return Mono.just(new ResultValueInfo<>(ResultInfo.S_ERROR, "part.100", "not file part!", part));
                    }
                }).map(resultValueInfo -> new UploadResultInfo(resultValueInfo.getResult(),
                        resultValueInfo.getErrorCode(),
                        resultValueInfo.getErrorDesc(),
                        resultValueInfo.getSource().name(),
                        (resultValueInfo.getSource() instanceof FilePart) ? ((FilePart) resultValueInfo.getSource()).filename()
                                : "",
                        partChunk))
                .flatMap(uploadResultInfo -> ServerResponse.accepted().contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(uploadResultInfo))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("no part data!"));
    }
}
