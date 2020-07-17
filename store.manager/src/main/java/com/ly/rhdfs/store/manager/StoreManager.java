package com.ly.rhdfs.store.manager;

import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.manager.connect.ConnectServerTask;
import com.ly.rhdfs.manager.connect.ServerStateHeartBeatTask;
import com.ly.rhdfs.manager.handler.CommandEventHandler;
import com.ly.rhdfs.manager.server.ServerManager;
import com.ly.rhdfs.store.manager.task.ComputerVoteTask;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class StoreManager extends ServerManager {
    private final int scheduledThreadCount = 5;
    private final int initThreadDelay = 10;
    private final int initThreadSecondDelay = 20;
    private final int initThreadThirdDelay = 30;
    public void initial(){
        // 初始化ServerManager
        loadMasterServer(serverConfig.getConfigPath());
        // 通过近期日志，加载write的last time
        initLastTime();
        // 加载缓存
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(scheduledThreadCount);
        // 初始化监听
        connectManager.startSocketListen(serverConfig.getPort(), new CommandEventHandler());
        // 定时连接线程，连接Master
        scheduledThreadPoolExecutor.schedule(new ConnectServerTask(this), initThreadDelay, TimeUnit.SECONDS);
        // 定时发送心跳
        scheduledThreadPoolExecutor.schedule(new ServerStateHeartBeatTask(this), initThreadDelay, TimeUnit.SECONDS);
        // 投票，发送到
        scheduledThreadPoolExecutor.schedule(new ComputerVoteTask(this),initThreadThirdDelay,TimeUnit.SECONDS);
    }
    public void computerVoteMaster(){
        if (localServerState.getType()!= ServerState.SIT_STORE || masterServerConfig==null)
            return;
        if (masterServerId!=-1){
            ServerState masterServerState=serverInfoMap.get(masterServerId);
            if (masterServerState.isOnline()){
                return;
            }
            if (Instant.now().toEpochMilli() - masterServerState.getLastTime()>serverConfig.getStoreServerDisconnectedMasterVote()){
                masterServerId=-1;
            }
        }
        Integer sid=Integer.MAX_VALUE;
        if (masterServerId==-1){
            for (Integer i:masterServerConfig.getMasterServerMap().keySet()){
                if (i>=sid)
                    continue;
                ServerState serverState=serverInfoMap.get(i);
                if (serverState!=null && serverState.isOnline()){
                    sid=i;
                }
            }
        }
        localServerState.setVotedServerId(sid);
    }
}