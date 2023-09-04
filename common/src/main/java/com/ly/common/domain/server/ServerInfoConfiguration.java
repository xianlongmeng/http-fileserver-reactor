package com.ly.common.domain.server;

public class ServerInfoConfiguration {

    private long serverId;
    private String address;
    private int port;
    private String hostUrl;
    private long updateLastTime;
    // 是否为master，还是store
    private boolean master = true;

    public ServerInfoConfiguration(){

    }
    public ServerInfoConfiguration(ServerState serverState) {
        setAddress(serverState.getAddress());
        setPort(serverState.getPort());
        setHostUrl(serverState.getHostUrl());
        setServerId(serverState.getServerId());
        setMaster(serverState.getType() != ServerState.SIT_STORE);
        setUpdateLastTime(serverState.getUpdateAddressLastTime());
    }

    public void setServerState(ServerState serverState) {
        setAddress(serverState.getAddress());
        setPort(serverState.getPort());
        setHostUrl(serverState.getHostUrl());
        setUpdateLastTime(serverState.getUpdateAddressLastTime());
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getUpdateLastTime() {
        return updateLastTime;
    }

    public void setUpdateLastTime(long updateLastTime) {
        this.updateLastTime = updateLastTime;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }
}
