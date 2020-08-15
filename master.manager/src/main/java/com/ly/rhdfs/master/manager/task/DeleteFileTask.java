package com.ly.rhdfs.master.manager.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ly.common.domain.file.FileDeleteServer;
import com.ly.rhdfs.master.manager.MasterManager;

import io.netty.channel.ChannelFuture;

public class DeleteFileTask implements Runnable {

    private int waitTime = 100;
    private long maxDeleteTime = 3600000;
    private MasterManager masterManager;

    public DeleteFileTask(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Override
    public void run() {
        List<FileDeleteServer> tokenInfoList = new ArrayList<>();
        int size = masterManager.getFileServerRunManager().getDeleteFileQueue().size();
        for (int i = 0; i < size; i++) {
            FileDeleteServer fileDeleteServer = masterManager.getFileServerRunManager().getDeleteFileQueue().poll();
            if (fileDeleteServer == null
                    || fileDeleteServer.getTimestamp() + maxDeleteTime > Instant.now().toEpochMilli())
                continue;
            ChannelFuture channelFuture = masterManager.sendFileDeleteAsync(fileDeleteServer.getServerId(),
                    fileDeleteServer.getFileDelete());
            channelFuture.addListener(future -> {
                if (!future.isSuccess()) {
                    masterManager.getFileServerRunManager().getDeleteFileQueue().add(fileDeleteServer);
                }
            });
        }
        masterManager.getScheduledThreadPoolExecutor().schedule(this, waitTime, TimeUnit.SECONDS);
    }
}
