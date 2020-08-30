package com.ly.rhdfs.file.server.dfs.store.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.ly.rhdfs.store.manager.StoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.token.TokenInfo;
import com.ly.common.util.ConvertUtil;
import com.ly.rhdfs.authentication.AuthenticationVerify;
import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.master.manager.MasterManager;
import com.ly.rhdfs.token.TokenFactory;

import reactor.core.publisher.Mono;

@Component
public class UploadDfsStoreHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private StoreManager storeManager;
    private ServerConfig serverConfig;

    @Autowired
    private void setStoreManager(StoreManager storeManager) {
        this.storeManager = storeManager;
    }

    @Autowired
    private void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }


    public Mono<ServerResponse> uploadStoreFile(ServerRequest request) {

    }

}
