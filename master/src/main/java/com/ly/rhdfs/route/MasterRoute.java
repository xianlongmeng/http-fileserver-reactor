package com.ly.rhdfs.route;

import com.ly.rhdfs.handle.MasterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@ComponentScan({"com.ly.rhdfs"})
public class MasterRoute {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    public RouterFunction<ServerResponse> routeMasterFile(MasterHandler masterHandler) {
        return RouterFunctions
                .route()
                .path("/upload/master", builder -> builder
                        .route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST), masterHandler::uploadFile))
                .path("/download/master", builder -> builder
                        .route(RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST), masterHandler::downloadFile))
                .build();
    }
}
