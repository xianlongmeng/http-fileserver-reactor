package com.ly.rhdfs.token.random;

import java.time.Instant;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.fasterxml.uuid.Generators;
import com.ly.common.domain.token.TokenInfo;
import com.ly.rhdfs.token.TokenFactory;
import reactor.core.publisher.Mono;

public class TokenRandomFactory implements TokenFactory {

    private long readTimeout = 1800000;
    private long writeTimeout = 28800000;
    private long defaultTimeout = 1200000;

    @Override
    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public void setWriteTimeout(long writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    @Override
    public void setDefaultTimeout(long defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    @Override
    public Mono<TokenInfo> createToken(String path, String fileName, int type, Map<String, String> paramMap) {
        if (StringUtils.isEmpty(path) || StringUtils.isEmpty(fileName))
            return null;
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setPath(path);
        tokenInfo.setFileName(fileName);
        tokenInfo.setTokenType(type);
        tokenInfo.setToken(Generators.timeBasedGenerator().generate().toString());
        tokenInfo.setLastTime(Instant.now().toEpochMilli());
        tokenInfo.setExpirationMills(computerTokenExpiration(tokenInfo));
        return Mono.just(tokenInfo);
    }

    @Override
    public long computerTokenExpiration(TokenInfo tokenInfo) {
        // 缺省30分钟，需要配置
        if (tokenInfo.getTokenType() == TokenInfo.TOKEN_READ)
            return readTimeout;
        else if (tokenInfo.getTokenType() == TokenInfo.TOKEN_WRITE)
            return writeTimeout;
        else
            return defaultTimeout;
    }
}
