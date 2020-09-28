package com.ly.rhdfs.file.server.dfs.handler;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.token.TokenInfo;
import com.ly.etag.ETagComputer;
import com.ly.rhdfs.authentication.AuthenticationVerify;
import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.master.manager.MasterManager;
import com.ly.rhdfs.token.TokenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
@ComponentScan({"com.ly.rhdfs.config"})
public class DownloadDfsHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ETagComputer eTagComputer;
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
    public void setETagComputer(ETagComputer eTagComputer) {
        this.eTagComputer = eTagComputer;
    }

    @Autowired
    private void setHandlerUtil(HandlerUtil handlerUtil) {
        this.handlerUtil = handlerUtil;
    }

    @NonNull
    public Mono<ServerResponse> downloadFileMasterRequest(ServerRequest request) {
        return authenticationVerify.verifyAuthentication(request).flatMap(resultInfo -> {
            if (resultInfo.getResult() != ResultInfo.S_OK) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }
            // param：filename,path,size,--reserved:user,token
            String path = request.pathVariable("path");
            String fileName = request.queryParam(serverConfig.getFileNameParamName()).orElse("");
            if (StringUtils.isEmpty(fileName)) {
                return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
            }
            AtomicReference<TokenInfo> tokenInfoAtomicReference = new AtomicReference<>();
            return tokenFactory.createUploadToken(path, fileName).flatMap(tokenInfo -> {
                tokenInfoAtomicReference.set(tokenInfo);
                return masterManager.questFileServer(tokenInfo);
            }).flatMap(fileInfo -> {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("token", tokenInfoAtomicReference.get());
                resultMap.put("fileInfo", fileInfo);
                return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resultMap);
            }).onErrorResume(t -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        });
    }

    @NonNull
    public Mono<ServerResponse> downloadFileChunkMasterRequest(ServerRequest request) {
        return ServerResponse.badRequest().build();
    }

    @NonNull
    public Mono<ServerResponse> downloadFileFinish(ServerRequest request) {
        TokenInfo tokenInfo = handlerUtil.queryRequestTokenParam(request);
        if (tokenInfo == null) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
        }
        masterManager.getFileServerRunManager().downloadFileFinish(tokenInfo);
        masterManager.sendBackupClearToken(tokenInfo);
        return ServerResponse.ok().build();
    }
    @NonNull
    public Mono<ServerResponse> listDirectInfo(ServerRequest request){
        String path = request.pathVariable("path");
        return masterManager.findDirectInfoAsync(path)
                .onErrorResume(t->Mono.empty())
                .flatMap(directInfo->ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(directInfo))
                .switchIfEmpty(ServerResponse.notFound().build());
    }
    @NonNull
    public Mono<ServerResponse> listFileInfo(ServerRequest request){
        String path = request.pathVariable("path");
        String fileName = request.queryParam(serverConfig.getFileNameParamName()).orElse("");
        if (StringUtils.isEmpty(fileName)) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
        }
        return masterManager.findFileInfoAsync(path,fileName)
                .onErrorResume(t->Mono.empty())
                .flatMap(fileInfo->ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(fileInfo))
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}
