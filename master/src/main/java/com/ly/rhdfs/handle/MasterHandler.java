package com.ly.rhdfs.handle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class MasterHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    public Mono<ServerResponse> uploadFile(ServerRequest request) {
        return null;
    }
    public Mono<ServerResponse> downloadFile(ServerRequest request) {
        return null;
    }
}
