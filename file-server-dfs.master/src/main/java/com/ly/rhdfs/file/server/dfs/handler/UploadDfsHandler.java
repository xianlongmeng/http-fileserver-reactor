package com.ly.rhdfs.file.server.dfs.handler;

import com.ly.common.constant.ParamConstants;
import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.TaskInfo;
import com.ly.common.domain.token.TokenInfo;
import com.ly.common.util.ConvertUtil;
import com.ly.rhdfs.authentication.AuthenticationVerify;
import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.master.manager.MasterManager;
import com.ly.rhdfs.token.TokenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class UploadDfsHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private MasterManager masterManager;
    private ServerConfig serverConfig;
    private AuthenticationVerify authenticationVerify;
    private TokenFactory tokenFactory;
    private HandlerUtil handlerUtil;

    @Autowired
    private void setMasterManager(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Autowired
    private void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Autowired
    private void setAuthenticationVerify(AuthenticationVerify authenticationVerify) {
        this.authenticationVerify = authenticationVerify;
    }

    @Autowired
    private void setTokenFactory(TokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    @Autowired
    private void setHandlerUtil(HandlerUtil handlerUtil) {
        this.handlerUtil = handlerUtil;
    }
    @NonNull
    public Mono<ServerResponse> uploadFileMasterRequest(ServerRequest request) {
        return authenticationVerify.verifyAuthentication(request).flatMap(resultInfo -> {
            if (resultInfo.getResult() != ResultInfo.S_OK) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }
            // param：filename,path,size,--reserved:user,token
            String path = request.pathVariable("path");
            String fileName = request.queryParam(serverConfig.getFileNameParamName()).orElse("");
            long fileSize = ConvertUtil.parseLong(request.queryParam(serverConfig.getFileSizeParamName()).orElse("0"),
                    0);
            if (StringUtils.isEmpty(fileName) || fileSize <= 0) {
                return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
            }
            AtomicReference<TokenInfo> tokenInfoAtomicReference = new AtomicReference<>();
            return tokenFactory.createUploadToken(path, fileName).flatMap(tokenInfo -> {
                tokenInfoAtomicReference.set(tokenInfo);
                return masterManager.apportionFileServer(tokenInfo, fileSize);
            }).flatMap(fileInfo -> {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("token", tokenInfoAtomicReference.get());
                resultMap.put("fileInfo", fileInfo);
                return ServerResponse.ok().bodyValue(resultMap);
            }).onErrorResume(t -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        });

    }
    @NonNull
    public Mono<ServerResponse> uploadFileServerChunkMasterRequest(ServerRequest request) {
        // 主Server错误，切换分配Server，验证备Server，设置一个为主Server，重新选择一个备Server
        // 返回主StoreServer
        TokenInfo tokenInfo = handlerUtil.queryRequestTokenParam(request);
        if (tokenInfo == null) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
        }
        TaskInfo taskInfo = masterManager.getFileServerRunManager().getUploadRunningTask().get(tokenInfo);
        if (taskInfo == null || taskInfo.getTokenInfo() == null || taskInfo.getFileInfo() == null)
            return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
        TokenInfo finalTokenInfo = taskInfo.getTokenInfo();
        finalTokenInfo.setLastTime(Instant.now().toEpochMilli());
        return authenticationVerify.verifyAuthentication(request)
                .flatMap(resultInfo -> {
                    if (resultInfo.getResult() != ResultInfo.S_OK) {
                        return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
                    }
                    // param：filename,path,size,--reserved:user,token
                    int chunk = ConvertUtil.parseInt(request.queryParam(serverConfig.getChunkParamName()).orElse("-1"), -1);
                    if (chunk < 0) {
                        return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
                    }
                    AtomicReference<TokenInfo> tokenInfoAtomicReference = new AtomicReference<>();
                    return masterManager.apportionFileServerChunk(finalTokenInfo, chunk)
                            .flatMap(fileInfo -> {
                                Map<String, Object> resultMap = new HashMap<>();
                                resultMap.put("token", tokenInfoAtomicReference.get());
                                resultMap.put("fileInfo", fileInfo);
                                return ServerResponse.ok().bodyValue(resultMap);
                            })
                            .onErrorResume(t -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
    @NonNull
    public Mono<ServerResponse> uploadFileServerFinish(ServerRequest request) {
        TokenInfo tokenInfo = handlerUtil.queryRequestTokenParam(request);
        if (tokenInfo == null) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
        }
        String etag = request.queryParam(ParamConstants.PARAM_ETAG).orElse("");
        masterManager.getFileServerRunManager().uploadFileFinish(tokenInfo, etag);
        return ServerResponse.ok().build();
    }

    public Mono<ServerResponse> uploadFileServerFailed(ServerRequest request) {
        // parameter:path/file/size/etag/
        TokenInfo tokenInfo = handlerUtil.queryRequestTokenParam(request);
        if (tokenInfo == null) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
        }
        masterManager.getFileServerRunManager().clearUploadFile(tokenInfo, true);
        return ServerResponse.ok().build();

    }

    @NonNull
    public Mono<ServerResponse> deleteFileMasterRequest(@NotNull ServerRequest request) {
        TokenInfo tokenInfo = handlerUtil.queryRequestTokenParam(request);
        if (tokenInfo == null) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
        }
        return authenticationVerify
                .verifyAuthentication(request)
                .flatMap(resultInfo -> {
                    if (resultInfo.getResult() != ResultInfo.S_OK) {
                        return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
                    }
                    // param：filename,path,size,--reserved:user,token
                    if (tokenInfo == null || StringUtils.isEmpty(tokenInfo.getFileName())) {
                        return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
                    }
                    return Mono
                            .just(tokenInfo)
                            .flatMap(ti -> {
                                if (masterManager.getFileServerRunManager().deleteFile(tokenInfo)) {
                                    return ServerResponse.ok().bodyValue("delete submit!");
                                } else {
                                    return ServerResponse.status(HttpStatus.CONFLICT).bodyValue("delete file failed!");
                                }
                            })
                            .onErrorResume(t -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });

    }

}
