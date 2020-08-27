package com.ly.rhdfs.authentication.impl;

import com.ly.common.domain.ResultInfo;
import com.ly.rhdfs.authentication.AuthenticationVerify;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

public class DefaultAuthenticationImpl implements AuthenticationVerify {
    @Override
    public Mono<ResultInfo> verifyAuthentication(ServerRequest request) {
        return Mono.just(new ResultInfo(ResultInfo.S_OK));
    }
}
