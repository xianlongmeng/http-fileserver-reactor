package com.ly.rhdfs.manager.connect;

import java.util.concurrent.TimeUnit;

import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.manager.server.ServerManager;

public class ServerStateHeartBeatTask implements Runnable {

    private final int waitTime = 30;
    private final ServerManager serverManager;

    public ServerStateHeartBeatTask(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public void run() {
        // 循环处理MasterServer连接
        for (ServerState serverState : serverManager.getServerInfoMap().values()) {
            if (serverState == null)
                continue;
            serverManager.sendHeart(serverState);
        }
        // 启动下次任务
        serverManager.getScheduledThreadPoolExecutor().schedule(this, waitTime, TimeUnit.SECONDS);
    }
}
