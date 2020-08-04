package com.ly.rhdfs.manager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.ly.common.constant.ParamConstants;

@Configuration
@PropertySource(value = "classpath:server.properties")
public class ServerConfig {

    @Value("${server.id}")
    private int currentServerId;
    @Value("${server.port}")
    private int port;
    @Value("${master.server.config.path}")
    private String configPath;
    @Value("${read.timeout:60}")
    private int readTimeout;
    @Value("${server.type}")
    private String serverType = ParamConstants.ST_STORE;
    @Value("${log.path:..}")
    private String logPath;
    @Value("${file.copies:3}")
    private int fileCopies;
    //失去太多StoreServer的Master多长时间失去Master资格
    @Value("${store.server.dtm.cancel.master:3600000}")
    private long storeServerDTMCancelMaster;
    //失去太多StoreServer的Master多长时间失去Master资格
    @Value("${store.server.disconnected.master.vote:300000}")
    private long storeServerDisconnectedMasterVote;
    @Value("${store.file.root.path}")
    private String fileRootPath;

    public int getCurrentServerId() {
        return currentServerId;
    }

    public void setCurrentServerId(int currentServerId) {
        this.currentServerId = currentServerId;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public int getFileCopies() {
        return fileCopies;
    }

    public void setFileCopies(int fileCopies) {
        this.fileCopies = fileCopies;
    }

    public long getStoreServerDTMCancelMaster() {
        return storeServerDTMCancelMaster;
    }

    public void setStoreServerDTMCancelMaster(long storeServerDTMCancelMaster) {
        this.storeServerDTMCancelMaster = storeServerDTMCancelMaster;
    }

    public long getStoreServerDisconnectedMasterVote() {
        return storeServerDisconnectedMasterVote;
    }

    public void setStoreServerDisconnectedMasterVote(long storeServerDisconnectedMasterVote) {
        this.storeServerDisconnectedMasterVote = storeServerDisconnectedMasterVote;
    }

    public String getFileRootPath() {
        return fileRootPath;
    }

    public void setFileRootPath(String fileRootPath) {
        this.fileRootPath = fileRootPath;
    }
}
