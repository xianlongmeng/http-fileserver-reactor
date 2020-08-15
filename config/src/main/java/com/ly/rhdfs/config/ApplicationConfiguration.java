package com.ly.rhdfs.config;

import com.ly.etag.ETagComputer;
import com.ly.rhdfs.token.TokenFactory;
import com.ly.rhdfs.token.random.TokenRandomFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.ly.etag.impl.ETagComputer4MD5;
import com.ly.etag.impl.ETagComputer4UUIDTimestamp;

@Configuration
public class ApplicationConfiguration {

    private ServerConfig serverConfig;
    @Autowired
    private void setServerConfig(ServerConfig serverConfig){
        this.serverConfig=serverConfig;
    }
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "etag", name = "type", havingValue = "MD5", matchIfMissing = true)
    public ETagComputer eTagComputer4MD5() {
        return new ETagComputer4MD5();
    }

    @Bean
    @ConditionalOnProperty(prefix = "etag", name = "type", havingValue = "UUIDTimestamp")
    public ETagComputer eTagComputer4UUIDTimestamp() {
        return new ETagComputer4UUIDTimestamp();
    }

    @Bean
    public TokenFactory tokenRandomFactory(){
        TokenFactory tokenFactory= new TokenRandomFactory();
        tokenFactory.setDefaultTimeout(serverConfig.getTokenDefaultTimeout());
        tokenFactory.setReadTimeout(serverConfig.getReadTimeout());
        tokenFactory.setWriteTimeout(serverConfig.getTokenWriteTimeout());
        return tokenFactory;
    }
}
