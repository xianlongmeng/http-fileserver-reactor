package com.ly.rhdfs.manager.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.manager.connect.ConnectServerTask;
import com.ly.rhdfs.manager.connect.ServerStateHeartBeatTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ly.common.domain.server.MasterServerConfig;
import com.ly.common.domain.server.ServerInfoConfiguration;
import com.ly.common.domain.server.ServerState;
import com.ly.common.util.DateFormatUtils;
import com.ly.rhdfs.communicate.command.DFSCommandState;
import com.ly.rhdfs.log.operate.LogOperateUtils;
import com.ly.rhdfs.manager.connect.ConnectManager;
import com.ly.rhdfs.manager.handler.CommandEventHandler;

public abstract class ServerManager {
    protected int scheduledThreadCount = 5;
    protected final int initThreadDelay = 10;
    protected final int initThreadSecondDelay = 20;
    protected final int initThreadThirdDelay = 30;

    protected final Map<Long, ServerState> serverInfoMap = new ConcurrentHashMap<>();
    protected final ServerState localServerState = new ServerState();
    private final DFSCommandState localDFSCommandState = new DFSCommandState(localServerState);
    private final LogOperateUtils logOperateUtils;
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected ConnectManager connectManager;
    protected ServerConfig serverConfig;
    protected ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    protected long masterServerId = -1;
    protected MasterServerConfig masterServerConfig;
    //Master:disconnect 3 store last time;Store:disconnect master last time
    private long masterDisconnectedLastTime;

    public ServerManager() {
        logOperateUtils = new LogOperateUtils(serverConfig.getLogPath());
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    @Autowired
    protected void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Autowired
    protected void setConnectManager(ConnectManager connectManager) {
        this.connectManager = connectManager;
    }

    public ServerState getLocalServerState() {
        return localServerState;
    }

    public Map<Long, ServerState> getServerInfoMap() {
        return serverInfoMap;
    }

    public long getServerAddressUpdateLastTime() {
        return localServerState.getUpdateAddressLastTime();
    }

    public void setServerAddressUpdateLastTime(long serverAddressUpdateLastTime) {
        localServerState.setUpdateAddressLastTime(serverAddressUpdateLastTime);
    }

    public void initial(){
        // 初始化ServerManager
        loadMasterServer(serverConfig.getConfigPath());
        // 通过近期日志，加载write的last time
        initLastTime();
        // 加载缓存
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(scheduledThreadCount);
        // 定时连接线程，连接Master
        scheduledThreadPoolExecutor.schedule(new ConnectServerTask(this), initThreadDelay, TimeUnit.SECONDS);
        // 定时发送心跳
        scheduledThreadPoolExecutor.schedule(new ServerStateHeartBeatTask(this), initThreadDelay, TimeUnit.SECONDS);
    }
    /**
     * 收到心跳后，放入新的ServerState，处理相关Config信息。
     * @param serverState
     */
    public void putServerState(ServerState serverState) {
        if (serverState == null || serverState.getType() == ServerState.SIT_UNKNOWN || masterServerConfig == null)
            return;
        if (!serverInfoMap.containsKey(serverState.getServerId())) {
            serverInfoMap.put(serverState.getServerId(), serverState);
            masterServerConfig.putServerInfoConfiguration(new ServerInfoConfiguration(serverState));
        } else {
            ServerInfoConfiguration serverInfoConfiguration = masterServerConfig
                    .getServerInfoConfiguration(serverState.getServerId());
            if (serverInfoConfiguration != null
                    && ((serverState.getAddress() != null
                            && !serverState.getAddress().equals(serverInfoConfiguration.getAddress()))
                    || serverState.getPort() != serverInfoConfiguration.getPort())) {
                serverState.setUpdateAddressLastTime(Instant.now().toEpochMilli());
                serverInfoConfiguration.setServerState(serverState);
            }
            ServerState curServerState = serverInfoMap.get(serverState.getServerId());
            curServerState.copyFrom(serverState);
        }
    }

    //只负责删除ServerState内容，不负责数据转存
    public void removeServerState(ServerState serverState){
        if (serverState==null)
            return;
        serverInfoMap.remove(serverState.getServerId());
        if (masterServerConfig!=null){
            if (serverState.getType()==ServerState.SIT_STORE){
                masterServerConfig.removeMasterServer(serverState.getServerId());
            }else{
                masterServerConfig.removeStoreServer(serverState.getServerId());
            }
        }
    }
    public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
        return scheduledThreadPoolExecutor;
    }

    public ServerState findServerState(long serverId) {
        return serverInfoMap.get(serverId);
    }

    protected void addServerInfo(ServerInfoConfiguration serverInfoConfiguration){
        if (serverInfoConfiguration!=null)
            serverInfoMap.put(serverInfoConfiguration.getServerId(), newServerInfo(serverInfoConfiguration));
    }

    public void loadMasterServer(String configPath) {
        if (StringUtils.isEmpty(configPath)) {
            return;
        }
        try {
            File configFile = ResourceUtils.getFile(configPath);
            String configContent = Files.readString(configFile.toPath());
            masterServerConfig = JSON.parseObject(configContent, new TypeReference<>() {
            });
            for (ServerInfoConfiguration serverInfoConfiguration : masterServerConfig.getMasterServerMap().values()) {
                addServerInfo(serverInfoConfiguration);
            }
            for (ServerInfoConfiguration serverInfoConfiguration : masterServerConfig.getStoreServerMap().values()) {
                addServerInfo(serverInfoConfiguration);
            }
        } catch (FileNotFoundException e) {
            logger.error(String.format("%s file is not found!", configPath), e);
        } catch (IOException e) {
            logger.error(String.format("%s file is read error!", configPath), e);
        }
    }

    public ServerState newServerInfo(ServerInfoConfiguration serverInfoConfiguration) {
        if (serverInfoConfiguration == null) {
            return null;
        }
        ServerState serverState = new ServerState();
        serverState.setServerId(serverInfoConfiguration.getServerId());
        serverState.setAddress(serverInfoConfiguration.getAddress());
        serverState.setPort(serverInfoConfiguration.getPort());
        serverState.setUpdateAddressLastTime(Instant.now().toEpochMilli());
        return serverState;
    }

    public void saveMasterServerConfig(){
        saveMasterServerConfig(serverConfig.getConfigPath());
    }
    public synchronized void saveMasterServerConfig(String configPath) {
        if (StringUtils.isEmpty(configPath)) {
            return;
        }
        try {
            File configFile = ResourceUtils.getFile(configPath);
            Files.writeString(configFile.toPath(), JSON.toJSONString(masterServerConfig));
        } catch (FileNotFoundException e) {
            logger.error(String.format("%s file is not found!", configPath), e);
        } catch (IOException e) {
            logger.error(String.format("%s file is write error!", configPath), e);
        }
    }

    public long getLocalServerId() {
        return localServerState.getServerId();
    }

    public void setLocalServerId(int localServerId) {
        localServerState.setServerId(localServerId);
    }

    public int getPort() {
        return localServerState.getPort();
    }

    public void setPort(int port) {
        localServerState.setPort(port);
    }

    public ServerState findServerState4ServerId(long serverId) {
        return serverInfoMap.get(serverId);
    }

    public void reconnectServer(ServerState serverState) {
        connectManager.closeServer(serverState);
        connectManager.startConnectServer(serverState, new CommandEventHandler(this));
    }

    public void connectServer(ServerState serverState) {
        connectManager.startConnectServer(serverState, new CommandEventHandler(this));
    }

    public void closeConnect(ServerState serverState) {
        connectManager.closeServer(serverState);
    }

    public void sendHeart(ServerState serverState) {
        if (serverState == null)
            return;
        connectManager.sendCommunicationObject(serverState, localServerState);
    }

    public void initLastTime() {
        localServerState.setWriteLastTime(DateFormatUtils.getTimeStampByDateTime(logOperateUtils.readLastTime()));
    }

    public MasterServerConfig getMasterServerConfig(){
        return masterServerConfig;
    }
    public int getStoreServerCount() {
        if (masterServerConfig == null)
            return 3;
        else
            return masterServerConfig.getStoreServerInitCount();
    }

    public long getMasterServerId() {
        return masterServerId;
    }

    public void setMasterServerId(long masterServerId) {
        this.masterServerId = masterServerId;
    }

    public long getMasterDisconnectedLastTime() {
        return masterDisconnectedLastTime;
    }

    public void setMasterDisconnectedLastTime(long masterDisconnectedLastTime) {
        this.masterDisconnectedLastTime = masterDisconnectedLastTime;
    }
}
