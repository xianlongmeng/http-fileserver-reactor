package com.ly.rhdfs.route;

import com.ly.rhdfs.handler.DownloadHandler;
import com.ly.rhdfs.handler.UploadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class DownloadFileRoute {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    public RouterFunction<ServerResponse> routeDownloadFile(DownloadHandler downloadHandler) {
        return RouterFunctions
                .route()
                .path("/download", builder -> builder
                        .route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST), downloadHandler::downloadFile))
                .build();
    }
}
