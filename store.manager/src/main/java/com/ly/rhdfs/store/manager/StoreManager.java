package com.ly.rhdfs.store.manager;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.ly.common.constant.ParamConstants;
import com.ly.common.util.DfsFileUtils;
import com.ly.rhdfs.manager.handler.CommandEventHandler;
import com.ly.rhdfs.store.manager.task.ComputerStateTask;
import org.springframework.stereotype.Component;

import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.manager.server.ServerManager;
import com.ly.rhdfs.store.manager.task.ComputerVoteTask;

@Component
public class StoreManager extends ServerManager {
    public StoreManager() {
        scheduledThreadCount = 4;
    }

    public void initial() {
        super.initial();
        if (!ParamConstants.ST_STORE.equals(serverConfig.getServerType()))
            return;
        // 初始化监听
        connectManager.startSocketListen(serverConfig.getPort(), new CommandEventHandler(this));
        // 投票，发送到
        scheduledThreadPoolExecutor.schedule(new ComputerVoteTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
        // 定时计算配置
        scheduledThreadPoolExecutor.schedule(new ComputerStateTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
    }

    public void computerVoteMaster() {
        if (localServerState.getType() != ServerState.SIT_STORE || masterServerConfig == null)
            return;
        if (masterServerId != -1) {
            ServerState masterServerState = serverInfoMap.get(masterServerId);
            if (masterServerState.isOnline()) {
                return;
            }
            if (Instant.now().toEpochMilli() - masterServerState.getLastTime() > serverConfig
                    .getStoreServerDisconnectedMasterVote()) {
                masterServerId = -1;
            }
        }
        Long sid = Long.MAX_VALUE;
        if (masterServerId == -1) {
            for (Long i : masterServerConfig.getMasterServerMap().keySet()) {
                if (i >= sid)
                    continue;
                ServerState serverState = serverInfoMap.get(i);
                if (serverState != null && serverState.isOnline()) {
                    sid = i;
                }
            }
        }
        localServerState.setVotedServerId(sid);
    }
    public void computerFreeSpaceSize(){
        localServerState.setSpaceSize(DfsFileUtils.diskFreeSpace(serverConfig.getFileRootPath()));
    }
}