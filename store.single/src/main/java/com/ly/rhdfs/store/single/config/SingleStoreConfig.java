package com.ly.rhdfs.store.single.config;

import com.ly.etag.ETagAccess;
import com.ly.etag.impl.access.ETagAccessDFS;
import com.ly.etag.impl.access.ETagAccessSingle;
import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.store.StoreFile;
import com.ly.rhdfs.store.single.SingleFileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConditionalOnProperty(prefix = "store",name="model",havingValue = "single")
public class SingleStoreConfig {

    private ServerConfig serverConfig;
    @Autowired
    private void setServerConfig(ServerConfig serverConfig){
        this.serverConfig=serverConfig;
    }
    @Bean
    public SingleFileStore storeFile(){
        SingleFileStore storeFile=new SingleFileStore();
        storeFile.setConfig(this);
        storeFile.setServerConfig(serverConfig);
        return storeFile;
    }
    @Bean
    public ETagAccess DistributionEtagAccess(){
        return new ETagAccessSingle();
    }
}
