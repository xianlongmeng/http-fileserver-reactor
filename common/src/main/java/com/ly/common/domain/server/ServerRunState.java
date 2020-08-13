package com.ly.common.domain.server;

import java.util.concurrent.atomic.AtomicInteger;

public class ServerRunState {

    private final ServerState serverState;
    private final AtomicInteger uploadCount = new AtomicInteger(0);
    private final AtomicInteger downloadCount = new AtomicInteger(0);
    private long serverId;

    public ServerRunState(ServerState serverState) {
        this.serverState = serverState;
        if (serverState != null)
            serverId = serverState.getServerId();
    }

    public boolean isOnline() {
        if (serverState == null)
            return false;
        return serverState.isOnline();
    }

    public long getSpaceSize() {
        if (serverState == null)
            return 0;
        return serverState.getSpaceSize();
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public ServerState getServerState() {
        return serverState;
    }

    public int getUploadCount() {
        return uploadCount.get();
    }

    public void setUploadCount(int uploadCount) {
        this.uploadCount.set(uploadCount);
    }

    public int uploadSub() {
        return uploadSub(1);
    }

    public int uploadSub(int count) {
        return uploadPlus(-count);
    }

    public int uploadPlus() {
        return uploadPlus(1);
    }

    public int uploadPlus(int count) {
        return uploadCount.addAndGet(count);
    }

    public int getDownloadCount() {
        return downloadCount.get();
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount.set(downloadCount);
    }

    public int downloadSub() {
        return downloadSub(1);
    }

    public int downloadSub(int count) {
        return downloadPlus(-count);
    }

    public int downloadPlus() {
        return downloadPlus(1);
    }

    public int downloadPlus(int count) {
        return downloadCount.addAndGet(count);
    }
}
