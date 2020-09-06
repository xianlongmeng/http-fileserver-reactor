package com.ly.rhdfs.file.server.dfs.store.handler;

import com.ly.common.constant.ParamConstants;
import com.ly.common.domain.DFSPartChunk;
import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.ResultValueInfo;
import com.ly.common.domain.UploadResultInfo;
import com.ly.common.domain.token.TokenInfo;
import com.ly.common.util.ConvertUtil;
import com.ly.etag.ETagComputer;
import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.store.StoreFile;
import com.ly.rhdfs.store.manager.StoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class UploadDfsStoreHandler {

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
    public void setStoreFile(StoreFile storeFile) {
        this.storeFile = storeFile;
    }

    public Mono<ServerResponse> uploadStoreFile(ServerRequest request) {
        // 1/If-Match(412) && If-Unmodified-Since(412)
        String token = request.queryParam(ParamConstants.PARAM_TOKEN_NAME).orElse("");
        if (StringUtils.isEmpty(token))
            return ServerResponse.status(HttpStatus.NON_AUTHORITATIVE_INFORMATION).build();
        TokenInfo tokenInfo = storeManager.findTokenInfo(token);
        if (tokenInfo == null
                || tokenInfo.getLastTime() + tokenInfo.getExpirationMills() < Instant.now().toEpochMilli())
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();

        DFSPartChunk partChunk;
        boolean chunked = Boolean.parseBoolean(request.queryParam(ParamConstants.PARAM_CHUNKED).orElse("false"));
        if (chunked) {
            int chunkIndex = ConvertUtil.parseInt(request.queryParam(ParamConstants.PARAM_CHUNK_INDEX).orElse("0"), 0);
            int chunk = ConvertUtil.parseInt(request.queryParam(ParamConstants.PARAM_CHUNK).orElse("0"), 0);
            int chunkSize = ConvertUtil.parseInt(request.queryParam(ParamConstants.PARAM_CHUNK_SIZE).orElse("0"), 0);
            int chunkCount = ConvertUtil.parseInt(request.queryParam(ParamConstants.PARAM_CHUNK_COUNT).orElse("0"), 1);
            partChunk = new DFSPartChunk(true, chunkIndex, chunk, chunkSize, chunkCount, tokenInfo);
        } else {
            partChunk = new DFSPartChunk(false, tokenInfo);
        }
        String path = request.queryParam(serverConfig.getPathParamName()).orElse(ParamConstants.PARAM_PATH_NAME);
        return Mono
                .fromFuture(CompletableFuture.supplyAsync(() -> Optional.ofNullable(storeManager.getFileInfoManager()
                        .findFileInfo(storeFile.takeFilePathString(tokenInfo.getFileName(), tokenInfo.getPath())))))
                .flatMap(optionalFileInfo -> {
                    if (optionalFileInfo.isEmpty())
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue("file information not found!");
                    //验证Chunk信息是否正确
                    else
                        return request.body(BodyExtractors.toParts())
                                .single()
                                .onErrorResume(t -> Mono.empty())
                                .flatMap(part -> {
                                    if (part instanceof FilePart) {
                                        partChunk.setFileInfo(optionalFileInfo.get());
                                        partChunk.setContentLength((int)part.headers().getContentLength());
                                        return storeFile.storeFile((FilePart) part, path, partChunk);
                                    } else {
                                        return Mono.just(new ResultValueInfo<>(ResultInfo.S_ERROR, "part.100", "not file part!", part));
                                    }
                                })
                                .map(resultValueInfo -> new UploadResultInfo(resultValueInfo.getResult(),
                                        resultValueInfo.getErrorCode(),
                                        resultValueInfo.getErrorDesc(),
                                        resultValueInfo.getSource().name(),
                                        (resultValueInfo.getSource() instanceof FilePart) ? ((FilePart) resultValueInfo.getSource()).filename() : "",
                                        partChunk))
                                .flatMap(uploadResultInfo -> ServerResponse.accepted()
                                        .contentType(MediaType.APPLICATION_JSON).bodyValue(uploadResultInfo))
                                .switchIfEmpty(ServerResponse.badRequest().bodyValue("no part data!"));
                });
    }

}
