package com.ly.rhdfs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.ly.common.constant.ParamConstants;

@Configuration
@PropertySource(value = "classpath:server.properties")
public class ServerConfig {

    @Value("${server.id}")
    private long currentServerId;
    @Value("${manager.server.port}")
    private int port;
    @Value("${master.server.config.path}")
    private String configPath;
    @Value("${read.timeout:60}")
    private int readTimeout;
    @Value("${manager.server.type}")
    private String serverType = ParamConstants.ST_STORE;
    @Value("${log.path:log}")
    private String logPath;
    @Value("${log.path.server.file:server_log}")
    private String serverFileLogPath;
    @Value("${file.copies:3}")
    private int fileCopies;
    // 失去太多StoreServer的Master多长时间失去Master资格
    @Value("${store.server.dtm.cancel.master:3600000}")
    private long storeServerDTMCancelMaster;
    @Value("${backup.master.server.update.timeout:300000}")
    private long backupMasterServerUpdateTimeout;
    // 失去太多StoreServer的Master多长时间失去Master资格
    @Value("${store.server.disconnected.master.vote:300000}")
    private long storeServerDisconnectedMasterVote;
    @Value("${store.file.root.path}")
    private String fileRootPath;
    @Value("${token.read.timeout}")
    private long tokenReadTimeout;
    @Value("${token.write.timeout}")
    private long tokenWriteTimeout;
    @Value("${token.default.timeout}")
    private long tokenDefaultTimeout;
    @Value("${file.chunk.suffix:chk}")
    private String fileChunkSuffix;

    @Value("${param.name.path:path}")
    private String pathParamName;
    @Value("${param.name.file:file_name}")
    private String fileNameParamName;
    @Value("${param.name.file.size:file_size}")
    private String fileSizeParamName;
    @Value("${param.name.token}")
    private String tokenParamName = ParamConstants.PARAM_TOKEN_NAME;

    @Value("${param.name.chunk}")
    private String chunkParamName = ParamConstants.PARAM_CHUNK;
    @Value("${store.file.rewrite:false}")
    private boolean rewrite;
    @Value("${store.file.temp.suffix}")
    private String tmpFileSuffix;

    @Value("${file.config.suffix:dfc}")
    private String fileConfigSuffix;
    @Value("${file.temp.config.suffix:dft}")
    private String fileTmpConfigSuffix;

    @Value("${store.backup.mode:sync}")
    private String storeBackupMode;
//    @Value("${store.backup.log.path:store_server_log}")
//    private String storeBackupLogPath;

    @Value("${store.chunk.size:0x4000000}")
    private int chunkSize;
    @Value("${store.chunk.piece.size:0x100000}")
    private int chunkPieceSize;
    @Value("${datagram.package.max.size:0x101000}")
    private int frameDatagramMaxSize;
    public String getTmpFileSuffix() {
        return tmpFileSuffix;
    }

    public void setTmpFileSuffix(String tmpFileSuffix) {
        this.tmpFileSuffix = tmpFileSuffix;
    }
    public long getCurrentServerId() {
        return currentServerId;
    }

    public void setCurrentServerId(long currentServerId) {
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

    public long getBackupMasterServerUpdateTimeout() {
        return backupMasterServerUpdateTimeout;
    }

    public void setBackupMasterServerUpdateTimeout(long backupMasterServerUpdateTimeout) {
        this.backupMasterServerUpdateTimeout = backupMasterServerUpdateTimeout;
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

    public long getTokenReadTimeout() {
        return tokenReadTimeout;
    }

    public void setTokenReadTimeout(long tokenReadTimeout) {
        this.tokenReadTimeout = tokenReadTimeout;
    }

    public long getTokenWriteTimeout() {
        return tokenWriteTimeout;
    }

    public void setTokenWriteTimeout(long tokenWriteTimeout) {
        this.tokenWriteTimeout = tokenWriteTimeout;
    }

    public long getTokenDefaultTimeout() {
        return tokenDefaultTimeout;
    }

    public void setTokenDefaultTimeout(long tokenDefaultTimeout) {
        this.tokenDefaultTimeout = tokenDefaultTimeout;
    }

    public String getPathParamName() {
        return pathParamName;
    }

    public void setPathParamName(String pathParamName) {
        this.pathParamName = pathParamName;
    }

    public String getFileNameParamName() {
        return fileNameParamName;
    }

    public void setFileNameParamName(String fileNameParamName) {
        this.fileNameParamName = fileNameParamName;
    }

    public String getChunkParamName() {
        return chunkParamName;
    }

    public void setChunkParamName(String chunkParamName) {
        this.chunkParamName = chunkParamName;
    }

    public String getFileSizeParamName() {
        return fileSizeParamName;
    }

    public void setFileSizeParamName(String fileSizeParamName) {
        this.fileSizeParamName = fileSizeParamName;
    }

    public String getTokenParamName() {
        return tokenParamName;
    }

    public void setTokenParamName(String tokenParamName) {
        this.tokenParamName = tokenParamName;
    }

    public String getFileChunkSuffix() {
        return fileChunkSuffix;
    }

    public void setFileChunkSuffix(String fileChunkSuffix) {
        this.fileChunkSuffix = fileChunkSuffix;
    }

    public boolean isRewrite() {
        return rewrite;
    }

    public void setRewrite(boolean rewrite) {
        this.rewrite = rewrite;
    }

    public String getFileConfigSuffix() {
        return fileConfigSuffix;
    }

    public void setFileConfigSuffix(String fileConfigSuffix) {
        this.fileConfigSuffix = fileConfigSuffix;
    }

    public String getFileTmpConfigSuffix() {
        return fileTmpConfigSuffix;
    }

    public void setFileTmpConfigSuffix(String fileTmpConfigSuffix) {
        this.fileTmpConfigSuffix = fileTmpConfigSuffix;
    }

    public String getStoreBackupMode() {
        return storeBackupMode;
    }

    public void setStoreBackupMode(String storeBackupMode) {
        this.storeBackupMode = storeBackupMode;
    }

    public String getServerFileLogPath() {
        return serverFileLogPath;
    }

    public void setServerFileLogPath(String serverFileLogPath) {
        this.serverFileLogPath = serverFileLogPath;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkPieceSize() {
        return chunkPieceSize;
    }

    public void setChunkPieceSize(int chunkPieceSize) {
        this.chunkPieceSize = chunkPieceSize;
    }

    public int getFrameDatagramMaxSize() {
        return frameDatagramMaxSize;
    }

    public void setFrameDatagramMaxSize(int frameDatagramMaxSize) {
        this.frameDatagramMaxSize = frameDatagramMaxSize;
    }
}
