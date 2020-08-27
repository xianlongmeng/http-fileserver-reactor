package com.ly.rhdfs.file.server.dfs.route;

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

import com.ly.rhdfs.file.server.dfs.handler.DownloadDfsHandler;
import com.ly.rhdfs.file.server.dfs.handler.UploadDfsHandler;

@Configuration
@ComponentScan({ "com.ly.rhdfs" })
public class FileDfsTransferRoute {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    public RouterFunction<ServerResponse> routeDownloadFile(DownloadDfsHandler downloadDfsHandler) {
        return RouterFunctions.route()
                .path("/dfs/download-request",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                downloadDfsHandler::downloadFileMasterRequest))
                .path("/dfs/download-chunk-request",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                downloadDfsHandler::downloadFileChunkMasterRequest))
                .path("/dfs/download-finish",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                downloadDfsHandler::downloadFileFinish))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> routeUploadFile(UploadDfsHandler uploadDfsHandler) {
        return RouterFunctions.route()
                .path("/dfs/upload-request",
                        builder -> builder.route(
                                RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                uploadDfsHandler::uploadFileMasterRequest))
                .path("/dfs/upload-chunk-request",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                uploadDfsHandler::uploadFileServerChunkMasterRequest))
                .path("/dfs/upload-finish",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                uploadDfsHandler::uploadFileServerFinish))
                .build();
    }
}
