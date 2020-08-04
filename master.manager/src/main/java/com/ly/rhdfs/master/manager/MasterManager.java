package com.ly.rhdfs.master.manager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ly.common.domain.server.ServerRunState;
import com.ly.rhdfs.master.manager.runstate.FileServerRunManager;
import com.ly.rhdfs.master.manager.task.AvailableOrderlyServerRunStateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ly.common.constant.ParamConstants;
import com.ly.common.domain.server.ServerInfoConfiguration;
import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.manager.handler.CommandEventHandler;
import com.ly.rhdfs.manager.server.ServerManager;
import com.ly.rhdfs.master.manager.task.ComputerVoteTask;
import com.ly.rhdfs.master.manager.task.MasterQualificationVerify;
import com.ly.rhdfs.master.manager.task.UpdateServerAddressTask;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

@Component
public class MasterManager extends ServerManager {

    private FileServerRunManager fileServerRunManager;
    @Autowired
    private void setFileServerRunManager(FileServerRunManager fileServerRunManager){
        this.fileServerRunManager=fileServerRunManager;
    }
    public FileServerRunManager getFileServerRunManager(){
        return fileServerRunManager;
    }
    public MasterManager() {
        scheduledThreadCount = 5;
    }

    public void initial() {

        super.initial();
        if (!ParamConstants.ST_MASTER.equals(serverConfig.getServerType()))
            return;
        // 初始化监听
        connectManager.startSocketListen(serverConfig.getPort(), new CommandEventHandler(this));
        // 定时配置更新
        scheduledThreadPoolExecutor.schedule(new MasterQualificationVerify(this), initThreadSecondDelay,
                TimeUnit.SECONDS);
        // 定时统计投票计数，主从状态
        scheduledThreadPoolExecutor.schedule(new ComputerVoteTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
        // 定时发送MasterServer和StoreServer的地址和端口信息
        scheduledThreadPoolExecutor.schedule(new UpdateServerAddressTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
        // 定时处理下载超时任务
        // todo:
        // 定时处理有效服务负载排序状态
        scheduledThreadPoolExecutor.schedule(new AvailableOrderlyServerRunStateTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
    }

    @Override
    protected void addServerInfo(ServerInfoConfiguration serverInfoConfiguration) {
        if (serverInfoConfiguration!=null) {
            ServerState serverState=newServerInfo(serverInfoConfiguration);
            serverInfoMap.put(serverInfoConfiguration.getServerId(), serverState);
            ServerRunState serverRunState=new ServerRunState(serverState);
            fileServerRunManager.putServerRunState(serverRunState);
        }
    }

    public void resetServerState() {
        verifyMasterQualification();
    }

    public void verifyMasterQualification() {
        // if (localServerState.getType()==ServerState.SIT_MASTER){
        // localServerState.setReady(false);
        // return;
        // }
        // verify count
        int count = 0;
        for (ServerState serverState : serverInfoMap.values()) {
            if (!serverState.isOnline())
                count++;
            // verify writeLastTime
            if (serverState.getWriteLastTime() > localServerState.getWriteLastTime()) {
                localServerState.setReady(false);
                return;
            }
        }
        if (count >= serverConfig.getFileCopies()) {
            if (localServerState.isReady()) {
                localServerState.setReady(false);
                localServerState.setLastTime(Instant.now().toEpochMilli());
            } else if (localServerState.getType() == ServerState.SIT_MASTER && localServerState.getLastTime() != -1
                    && Instant.now().toEpochMilli() - localServerState.getLastTime() > serverConfig
                            .getStoreServerDTMCancelMaster()) {
                // clear master state,too many store server disconnected too long
                localServerState.setType(ServerState.SIT_MASTER_BACKUP);
                localServerState.setState(ServerState.SIS_MASTER_FAULT);
                masterServerId = -1;
            }
            return;
        } else {
            localServerState.setLastTime(-1);
        }
        localServerState.setReady(true);
    }

    public List<ServerInfoConfiguration> collectChangeAddressServer(ServerState serverState) {
        if (serverState == null) {
            return new ArrayList<>();
        }
        return collectChangeAddressServer(serverState.getUpdateAddressLastTime());
    }

    public List<ServerInfoConfiguration> collectChangeAddressServer(long lastTime) {
        List<ServerInfoConfiguration> serverInfoConfigurationList = new ArrayList<>();
        for (ServerState serverState : serverInfoMap.values()) {
            if (serverState == null)
                continue;
            if (serverState.getUpdateAddressLastTime() > lastTime) {
                serverInfoConfigurationList
                        .add(masterServerConfig.getServerInfoConfiguration(serverState.getServerId()));
            }
        }
        return serverInfoConfigurationList;
    }

    public void computerVoteCount() {
        if (masterServerConfig == null)
            return;
        int count = 0;
        for (ServerState serverState : serverInfoMap.values()) {
            if (serverState.getType() != ServerState.SIT_STORE)
                continue;
            if (serverState.getVotedServerId() == getLocalServerId())
                count++;
        }
        if (count > masterServerConfig.getStoreServerMap().size() * 2 / 3) {
            localServerState.setType(ServerState.SIT_MASTER);
            masterServerId = getLocalServerId();
        }
    }

    public void sendServerAddressUpdate(ServerState serverState,
            List<ServerInfoConfiguration> serverInfoConfigurations) {
        if (serverState == null)
            return;
        connectManager.sendCommunicationObject(serverState, serverInfoConfigurations);
    }
}
