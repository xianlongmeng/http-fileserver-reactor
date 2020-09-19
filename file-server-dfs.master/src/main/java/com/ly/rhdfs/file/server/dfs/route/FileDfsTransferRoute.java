package com.ly.rhdfs.file.server.dfs.route;

import com.ly.rhdfs.file.server.dfs.handler.CommandDfsHandler;
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
                .path("/dfs/download-request/{*path}",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                downloadDfsHandler::downloadFileMasterRequest))
                .path("/dfs/download-chunk-request/{*path}",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                downloadDfsHandler::downloadFileChunkMasterRequest))
                .path("/dfs/download-finish/{*path}",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                downloadDfsHandler::downloadFileFinish))
                .path("/dfs/find-direct/{*path}",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                downloadDfsHandler::listDirectInfo))
                .path("/dfs/find-file/{*path}",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                downloadDfsHandler::listFileInfo))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> routeUploadFile(UploadDfsHandler uploadDfsHandler) {
        return RouterFunctions.route()
                .path("/dfs/upload-request/{*path}",
                        builder -> builder.route(
                                RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                uploadDfsHandler::uploadFileMasterRequest))
                .path("/dfs/upload-chunk-request/{*path}",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                uploadDfsHandler::uploadFileServerChunkMasterRequest))
                .path("/dfs/upload-finish/{*path}",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                uploadDfsHandler::uploadFileServerFinish))
                .path("/dfs/delete-file/{*path}",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                uploadDfsHandler::deleteFileMasterRequest))
                .build();
    }
    @Bean
    public RouterFunction<ServerResponse> routeCommand(CommandDfsHandler commandDfsHandler) {
        return RouterFunctions.route()
                .path("/dfs/backup-server/{serverId}",
                        builder -> builder.route(
                                RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                commandDfsHandler::backupServer))
                .path("/dfs/recover-server/{oldServerId}/{newServerId}",
                        builder -> builder.route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST)
                                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                commandDfsHandler::recoverServer))
                .build();
    }
}
