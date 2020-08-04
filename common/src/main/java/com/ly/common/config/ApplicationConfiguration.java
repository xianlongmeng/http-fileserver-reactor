package com.ly.common.config;

import com.ly.etag.impl.ETagComputer4MD5;
import com.ly.etag.impl.ETagComputer4UUIDTimestamp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ApplicationConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "etag",name = "type",havingValue = "MD5",matchIfMissing = true)
    public ETagComputer4MD5 eTagComputer4MD5(){
        return new ETagComputer4MD5();
    }

    @Bean
    @ConditionalOnProperty(prefix = "etag",name = "type",havingValue = "UUIDTimestamp")
    public ETagComputer4UUIDTimestamp eTagComputer4UUIDTimestamp(){
        return new ETagComputer4UUIDTimestamp();
    }
}
