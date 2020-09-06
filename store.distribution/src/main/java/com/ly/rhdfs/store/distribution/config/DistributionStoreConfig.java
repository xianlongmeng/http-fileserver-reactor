package com.ly.rhdfs.store.distribution.config;

import com.ly.common.util.DfsFileUtils;
import com.ly.etag.ETagAccess;
import com.ly.etag.impl.access.ETagAccessDFS;
import com.ly.rhdfs.communicate.socket.parse.DFSCommandParse;
import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.file.config.FileInfoManager;
import com.ly.rhdfs.store.StoreFile;
import com.ly.rhdfs.store.distribution.DistributionFileStore;
import com.ly.rhdfs.store.manager.StoreManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "store",name="model",havingValue = "distribution")
public class DistributionStoreConfig {
    private ServerConfig serverConfig;
    private FileInfoManager fileInfoManager;
    private DfsFileUtils dfsFileUtils;
    private DFSCommandParse dfsCommandParse;
    private StoreManager storeManager;

    @Autowired
    private void setServerConfig(ServerConfig serverConfig){
        this.serverConfig=serverConfig;
    }
    @Autowired
    private void setFileInfoManager(FileInfoManager fileInfoManager){
        this.fileInfoManager=fileInfoManager;
    }
    @Autowired
    private void setDfsFileUtils(DfsFileUtils dfsFileUtils){
        this.dfsFileUtils=dfsFileUtils;
    }
    @Autowired
    private void setDfsCommandParse(DFSCommandParse dfsCommandParse){
        this.dfsCommandParse=dfsCommandParse;
    }
    @Autowired
    private void setStoreManager(StoreManager storeManager){
        this.storeManager=storeManager;
    }
    @Bean
    public DistributionFileStore storeFile(){
        DistributionFileStore storeFile=new DistributionFileStore();
        storeFile.setConfig(this);
        storeFile.setServerConfig(serverConfig);
        storeFile.setFileInfoManager(fileInfoManager);
        storeFile.setDfsFileUtils(dfsFileUtils);
        storeFile.setDfsCommandParse(dfsCommandParse);
        storeFile.setStoreManager(storeManager);
        return storeFile;
    }
    @Bean
    public ETagAccess DistributionEtagAccess(){
        return new ETagAccessDFS();
    }
}
