package com.ly.rhdfs.token;

import java.util.Map;

import com.ly.common.domain.token.TokenInfo;
import reactor.core.publisher.Mono;

public interface TokenFactory {

    default Mono<TokenInfo> createUploadToken(String path, String fileName) {
        return createToken(path, fileName, TokenInfo.TOKEN_WRITE);
    }

    default Mono<TokenInfo> createDownloadToken(String path, String fileName) {
        return createToken(path, fileName, TokenInfo.TOKEN_READ);
    }

    default Mono<TokenInfo> createUploadToken(String path, String fileName, Map<String, String> paramMap) {
        return createToken(path, fileName, TokenInfo.TOKEN_WRITE, paramMap);
    }

    default Mono<TokenInfo> createDownloadToken(String path, String fileName, Map<String, String> paramMap) {
        return createToken(path, fileName, TokenInfo.TOKEN_READ, paramMap);
    }

    default Mono<TokenInfo> createToken(String path, String fileName, int type) {
        return createToken(path, fileName, type, null);
    }

    Mono<TokenInfo> createToken(String path, String fileName, int type, Map<String, String> paramMap);

    long computerTokenExpiration(TokenInfo tokenInfo);

    void setReadTimeout(long readTimeout);

    void setWriteTimeout(long writeTimeout);

    void setDefaultTimeout(long defaultTimeout);
}
