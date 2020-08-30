package com.ly.rhdfs.file.server.dfs.store.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.ly.rhdfs.file.server.dfs.store.handler.DownloadDfsStoreHandler;
import com.ly.rhdfs.file.server.dfs.store.handler.UploadDfsStoreHandler;

@Configuration
@ComponentScan({ "com.ly.rhdfs" })
public class FileDfsStoreTransferRoute {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    public RouterFunction<ServerResponse> routeDownloadFile(DownloadDfsStoreHandler downloadDfsStoreHandler) {
        return RouterFunctions.route()
                .path("/dfs/store/download-file",
                        builder -> builder.route(
                                RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                downloadDfsStoreHandler::downloadStoreFile))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> routeUploadFile(UploadDfsStoreHandler uploadDfsStoreHandler) {
        return RouterFunctions.route()
                .path("/dfs/store/upload-file",
                        builder -> builder.route(
                                RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                uploadDfsStoreHandler::uploadStoreFile))
                .build();
    }
}
