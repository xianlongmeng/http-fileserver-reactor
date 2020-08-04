package com.ly.rhdfs.handler;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;

import com.ly.rhdfs.store.single.SingleFileStore;
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

import com.ly.common.domain.PartChunk;
import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.ResultValueInfo;
import com.ly.common.domain.UploadResultInfo;
import com.ly.common.util.ConvertUtil;
import com.ly.common.util.ToolUtils;
import com.ly.rhdfs.config.StoreConfiguration;
import com.ly.rhdfs.store.StoreFile;
import com.ly.common.constant.ParamConstants;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class UploadHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private StoreFile storeFile=new SingleFileStore();;
    private StoreConfiguration storeConfiguration;

    @Autowired
    public void setStoreConfiguration(StoreConfiguration storeConfiguration) {
        this.storeConfiguration = storeConfiguration;
    }

    public Mono<ServerResponse> uploadFileSelf(ServerRequest request) {

        String path = request.queryParam(storeConfiguration.getPathParamName()).orElse("path");
        return request.body(BodyExtractors.toParts()).flatMap(part -> {
            if (part instanceof FilePart) {
                FilePart filePart = (FilePart) part;

                logger.info("name:{};fileName:{}", filePart.name(), filePart.filename());
                if (!storeConfiguration.isRewrite() && storeFile.existed(filePart.filename(), path)) {
                    logger.warn("file is existed and can not rewrite.fileName:{}", filePart.filename());
                    return Flux.just(false);
                }
                return Flux.create(sink -> {
                    try {
                        AsynchronousFileChannel asynchronousFileChannel = AsynchronousFileChannel.open(
                                storeFile.takeFilePath(filePart.filename(), path), StandardOpenOption.CREATE,
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
        String path = request.queryParam(storeConfiguration.getPathParamName()).orElse(ParamConstants.PARAM_PATH_NAME);
        return request.body(BodyExtractors.toParts()).flatMap(part -> {
            if (part instanceof FilePart) {
                FilePart filePart = (FilePart) part;
                if (!storeConfiguration.isRewrite() && storeFile.existed(filePart.filename(), path)) {
                    logger.warn("file is existed and can not rewrite.fileName:{}", filePart.filename());
                    return Flux.just(
                            new ResultValueInfo<>(ResultInfo.S_ERROR, "file.exist.100", "file is exist", filePart));
                } else {
                    return storeFile.storeFile(filePart, path, partChunk);
                }
            } else {
                return Flux.just(new ResultValueInfo<>(ResultInfo.S_ERROR, "part.100", "not file part!", part));
            }
        }).map(resultValueInfo -> new UploadResultInfo(ResultInfo.S_ERROR, "part.100", "not file part!",
                resultValueInfo.getSource().name(),
                (resultValueInfo.getSource() instanceof FilePart) ? ((FilePart) resultValueInfo.getSource()).filename()
                        : ""))
                .collectList().flatMap(resultInfos -> ServerResponse.accepted().contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resultInfos));
    }
}
