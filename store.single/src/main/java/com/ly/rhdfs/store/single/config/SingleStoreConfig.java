package com.ly.rhdfs.store.single.config;

import com.ly.etag.ETagAccess;
import com.ly.etag.impl.access.ETagAccessSingle;
import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.file.util.DfsFileUtils;
import com.ly.rhdfs.store.single.SingleFileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "store", name = "model", havingValue = "single")
@ComponentScan("com.ly.rhdfs")
public class SingleStoreConfig {

    private ServerConfig serverConfig;
    private DfsFileUtils dfsFileUtils;

    @Autowired
    private void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Autowired
    private void setDfsFileUtils(DfsFileUtils dfsFileUtils) {
        this.dfsFileUtils = dfsFileUtils;
    }

    @Bean
    public SingleFileStore storeFile() {
        SingleFileStore storeFile = new SingleFileStore();
        storeFile.setConfig(this);
        storeFile.setServerConfig(serverConfig);
        storeFile.setDfsFileUtils(dfsFileUtils);
        return storeFile;
    }

    @Bean
    public ETagAccess DistributionEtagAccess() {
        return new ETagAccessSingle();
    }
}
