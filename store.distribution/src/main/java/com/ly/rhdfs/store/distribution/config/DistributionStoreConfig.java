package com.ly.rhdfs.store.distribution.config;

import com.ly.etag.ETagAccess;
import com.ly.etag.ETagComputer;
import com.ly.etag.impl.access.ETagAccessDFS;
import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.file.config.FileInfoManager;
import com.ly.rhdfs.file.util.DfsFileUtils;
import com.ly.rhdfs.store.distribution.DistributionFileStore;
import com.ly.rhdfs.store.manager.StoreManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "store", name = "model", havingValue = "distribution")
public class DistributionStoreConfig {
    private ServerConfig serverConfig;
    private FileInfoManager fileInfoManager;
    private DfsFileUtils dfsFileUtils;
    private StoreManager storeManager;
    private ETagComputer eTagComputer;

    @Autowired
    private void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Autowired
    private void setFileInfoManager(FileInfoManager fileInfoManager) {
        this.fileInfoManager = fileInfoManager;
    }

    @Autowired
    private void setDfsFileUtils(DfsFileUtils dfsFileUtils) {
        this.dfsFileUtils = dfsFileUtils;
    }

    @Autowired
    private void setStoreManager(StoreManager storeManager) {
        this.storeManager = storeManager;
    }

    @Autowired
    private void setETagComputer(ETagComputer eTagComputer) {
        this.eTagComputer = eTagComputer;
    }

    @Bean
    public DistributionFileStore storeFile() {
        DistributionFileStore storeFile = new DistributionFileStore();
        storeFile.setServerConfig(serverConfig);
        storeFile.setFileInfoManager(fileInfoManager);
        storeFile.setDfsFileUtils(dfsFileUtils);
        storeFile.setETagComputer(eTagComputer);
        storeFile.setStoreManager(storeManager);
        return storeFile;
    }

    @Bean
    public ETagAccess DistributionEtagAccess() {
        return new ETagAccessDFS();
    }
}
