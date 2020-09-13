package com.ly.rhdfs.file.server.dfs.handler;

import com.ly.common.domain.ResultInfo;
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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

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

    public Mono<ServerResponse> uploadFileMasterRequest(ServerRequest request) {
        return authenticationVerify.verifyAuthentication(request).flatMap(resultInfo -> {
            if (resultInfo.getResult() != ResultInfo.S_OK) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }
            // param：filename,path,size,--reserved:user,token
            String path = request.queryParam(serverConfig.getPathParamName()).orElse("");
            String fileName = request.queryParam(serverConfig.getFileNameParamName()).orElse("");
            long fileSize = ConvertUtil.parseLong(request.queryParam(serverConfig.getFileSizeParamName()).orElse("0"),
                    0);
            if (StringUtils.isEmpty(path) || StringUtils.isEmpty(fileName) || fileSize <= 0) {
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

    public Mono<ServerResponse> uploadFileServerChunkMasterRequest(ServerRequest request) {
        // 主Server错误，切换分配Server，验证备Server，设置一个为主Server，重新选择一个备Server
        // 返回主StoreServer
        TokenInfo tokenInfo = handlerUtil.queryRequestTokenParam(request);
        if (tokenInfo == null) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
        }
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
                    return masterManager.apportionFileServerChunk(tokenInfo, chunk)
                            .flatMap(fileInfo -> {
                                Map<String, Object> resultMap = new HashMap<>();
                                resultMap.put("token", tokenInfoAtomicReference.get());
                                resultMap.put("fileInfo", fileInfo);
                                return ServerResponse.ok().bodyValue(resultMap);
                            })
                            .onErrorResume(t -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    public Mono<ServerResponse> uploadFileServerFinish(ServerRequest request) {
        TokenInfo tokenInfo = handlerUtil.queryRequestTokenParam(request);
        if (tokenInfo == null) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
        }
        masterManager.getFileServerRunManager().uploadFileFinish(tokenInfo);
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

}
