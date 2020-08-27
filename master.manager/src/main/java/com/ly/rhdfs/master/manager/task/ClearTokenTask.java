package com.ly.rhdfs.master.manager.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ly.common.domain.token.FileDeleteServer;
import com.ly.common.domain.token.TokenClearServer;
import com.ly.rhdfs.master.manager.MasterManager;

import io.netty.channel.ChannelFuture;

public class ClearTokenTask implements Runnable {

    private int waitTime = 100;
    private long maxDeleteTime = 3600000;
    private MasterManager masterManager;

    public ClearTokenTask(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Override
    public void run() {
        List<TokenClearServer> tokenInfoList = new ArrayList<>();
        int size = masterManager.getFileServerRunManager().getClearTokenQueue().size();
        for (int i = 0; i < size; i++) {
            TokenClearServer tokenClearServer = masterManager.getFileServerRunManager().getClearTokenQueue().poll();
            if (tokenClearServer == null
                    || tokenClearServer.getTimestamp() + maxDeleteTime < Instant.now().toEpochMilli())
                continue;
            ChannelFuture channelFuture = null;
            if (tokenClearServer.getType()==TokenClearServer.TC_TYPE_FILE_DELETE) {
                channelFuture=masterManager.sendFileDeleteAsync(tokenClearServer.getServerId(),
                        tokenClearServer.getTokenInfo());
            }else if (tokenClearServer.getType()==TokenClearServer.TC_TYPE_TOKEN_CLEAR){
                channelFuture=masterManager.sendClearTokenAsync(tokenClearServer.getServerId(),
                        tokenClearServer.getTokenInfo());
            }
            if (channelFuture!=null) {
                channelFuture.addListener(future -> {
                    if (!future.isSuccess()) {
                        masterManager.getFileServerRunManager().getClearTokenQueue().add(tokenClearServer);
                    }
                });
            }
        }
        masterManager.getScheduledThreadPoolExecutor().schedule(this, waitTime, TimeUnit.SECONDS);
    }
}
