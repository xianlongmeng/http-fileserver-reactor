package com.ly.rhdfs.file.server.dfs.handler;

import com.ly.common.domain.ResultInfo;
import com.ly.common.util.ConvertUtil;
import com.ly.rhdfs.authentication.AuthenticationVerify;
import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.master.manager.MasterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class CommandDfsHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private MasterManager masterManager;
    private ServerConfig serverConfig;
    private AuthenticationVerify authenticationVerify;
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
    private void setHandlerUtil(HandlerUtil handlerUtil) {
        this.handlerUtil = handlerUtil;
    }

    @NonNull
    public Mono<ServerResponse> backupServer(ServerRequest request) {
        return authenticationVerify.verifyAuthentication(request).flatMap(resultInfo -> {
            if (resultInfo.getResult() != ResultInfo.S_OK) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }
            // param：filename,path,size,--reserved:user,token
            long serverId = ConvertUtil.parseLong(request.pathVariable("serverId"), -1);
            if (serverId == -1) {
                return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
            }
            if (masterManager.addBackupServerId(serverId))
                return ServerResponse.ok().build();
            else
                return ServerResponse.badRequest().build();
        });
    }

    @NonNull
    public Mono<ServerResponse> recoverServer(ServerRequest request) {
        return authenticationVerify.verifyAuthentication(request).flatMap(resultInfo -> {
            if (resultInfo.getResult() != ResultInfo.S_OK) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }
            // param：filename,path,size,--reserved:user,token
            long oldServerId = ConvertUtil.parseLong(request.pathVariable("oldServerId"), -1);
            long newServerId = ConvertUtil.parseLong(request.pathVariable("newServerId"), -1);
            if (oldServerId == -1 || newServerId == -1) {
                return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Parameter error!");
            }
            if (masterManager.addRecoverServerId(oldServerId, newServerId))
                return ServerResponse.ok().build();
            else
                return ServerResponse.badRequest().build();
        });
    }

}
