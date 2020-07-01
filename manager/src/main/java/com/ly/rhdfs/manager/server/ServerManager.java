package com.ly.rhdfs.manager.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ly.common.domain.server.MasterServerConfig;
import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.manager.connect.ConnectManager;
import com.ly.rhdfs.manager.handler.CommandEventHandler;

@Component
public class ServerManager {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Map<Integer, MasterServerConfig> masterServerConfigs = new ConcurrentHashMap<>();
    private final Map<Integer, ServerState> serverInfoMap = new ConcurrentHashMap<>();
    private int localServerId;
    private int port;
    private ConnectManager connectManager;

    private int masterServerId;

    @Autowired
    private void setConnectManager(ConnectManager connectManager) {
        this.connectManager = connectManager;
    }

    public void loadMasterServer(String configPath) {
        if (StringUtils.isEmpty(configPath)) {
            return;
        }
        try {
            File configFile = ResourceUtils.getFile(configPath);
            String configContent = Files.readString(configFile.toPath());
            Map<Integer, MasterServerConfig> mscMap = JSON.parseObject(configContent,
                    new TypeReference<Map<Integer, MasterServerConfig>>() {
                    });
            masterServerConfigs.putAll(mscMap);
            for (MasterServerConfig masterServerConfig : mscMap.values()) {
                if (masterServerConfig != null) {
                    serverInfoMap.put(masterServerConfig.getServerId(), newServerInfo(masterServerConfig));
                }
            }
        } catch (FileNotFoundException e) {
            logger.error(String.format("%s file is not found!", configPath), e);
        } catch (IOException e) {
            logger.error(String.format("%s file is read error!", configPath), e);
        }
    }

    private ServerState newServerInfo(MasterServerConfig masterServerConfig) {
        if (masterServerConfig == null) {
            return null;
        }
        ServerState serverState = new ServerState();
        serverState.setServerId(masterServerConfig.getServerId());
        serverState.setAddress(masterServerConfig.getAddress());
        serverState.setPort(masterServerConfig.getPort());
        return serverState;
    }

    public synchronized void saveMasterServerConfig(String configPath) {
        if (StringUtils.isEmpty(configPath)) {
            return;
        }
        try {
            File configFile = ResourceUtils.getFile(configPath);
            Files.writeString(configFile.toPath(), JSON.toJSONString(serverInfoMap));
        } catch (FileNotFoundException e) {
            logger.error(String.format("%s file is not found!", configPath), e);
        } catch (IOException e) {
            logger.error(String.format("%s file is write error!", configPath), e);
        }
    }

    public int getLocalServerId() {
        return localServerId;
    }

    public void setLocalServerId(int localServerId) {
        this.localServerId = localServerId;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ServerState findServerState4ServerId(int serverId) {
        return serverInfoMap.get(serverId);
    }

    public void reconnectServer(ServerState serverState) {
        connectManager.closeServer(serverState);
        connectManager.startConnectServer(serverState, new CommandEventHandler());
    }
    public void connectServer(ServerState serverState) {
        connectManager.startConnectServer(serverState, new CommandEventHandler());
    }

    public void closeConnect(ServerState serverState) {
        connectManager.closeServer(serverState);
    }
    public void sendHeart(ServerState serverState){
        //TODO:
    }

}
