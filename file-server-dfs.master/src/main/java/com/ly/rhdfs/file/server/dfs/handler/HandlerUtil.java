package com.ly.rhdfs.file.server.dfs.handler;

import com.ly.common.domain.token.TokenInfo;
import com.ly.rhdfs.config.ServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;

@Component
public class HandlerUtil {
    private ServerConfig serverConfig;
    @Autowired
    private void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }
    public TokenInfo queryRequestTokenParam(ServerRequest request){
        String token = request.queryParam(serverConfig.getTokenParamName()).orElse("");
        String path = request.queryParam(serverConfig.getPathParamName()).orElse("");
        String fileName = request.queryParam(serverConfig.getFileNameParamName()).orElse("");
        if (StringUtils.isEmpty(path) || StringUtils.isEmpty(fileName) || StringUtils.isEmpty(token)){
            return null;
        }
        TokenInfo tokenInfo=new TokenInfo();
        tokenInfo.setToken(token);
        tokenInfo.setPath(path);
        tokenInfo.setFileName(fileName);
        return tokenInfo;
    }
}
