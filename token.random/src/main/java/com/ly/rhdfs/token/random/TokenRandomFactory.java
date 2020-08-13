package com.ly.rhdfs.token.random;

import com.fasterxml.uuid.Generators;
import com.ly.common.domain.token.TokenInfo;
import com.ly.rhdfs.token.TokenFactory;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;

public class TokenRandomFactory implements TokenFactory {
    @Override
    public TokenInfo createToken(String path, String fileName, int type, Map<String, String> paramMap) {
        if (StringUtils.isEmpty(path) || StringUtils.isEmpty(fileName))
            return null;
        TokenInfo tokenInfo=new TokenInfo();
        tokenInfo.setPath(path);
        tokenInfo.setFileName(fileName);
        tokenInfo.setTokenType(type);
        tokenInfo.setToken(Generators.timeBasedGenerator().generate().toString());
        tokenInfo.setLastTime(Instant.now().toEpochMilli());
        tokenInfo.setExpirationMills(computerTokenExpiration(tokenInfo));
        return tokenInfo;
    }

    @Override
    public long computerTokenExpiration(TokenInfo tokenInfo) {
        //缺省30分钟，需要配置
        return 1800000;
    }
}
