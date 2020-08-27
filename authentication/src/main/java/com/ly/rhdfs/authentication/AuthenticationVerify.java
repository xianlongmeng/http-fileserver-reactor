package com.ly.rhdfs.authentication;

import com.ly.common.domain.ResultInfo;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

public interface AuthenticationVerify {
    Mono<ResultInfo> verifyAuthentication(ServerRequest request);
}
