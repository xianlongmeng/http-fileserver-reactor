package com.ly.common.domain.server;

public class ServerInfoConfiguration {
    private int serverId;
    private String address;
    private int port;
    // 是否为master，还是store
    private boolean master=true;

    public ServerInfoConfiguration(ServerState serverState){
        setAddress(serverState.getAddress());
        setPort(serverState.getPort());
        setServerId(serverState.getServerId());
        setMaster(serverState.getType()!=ServerState.SIT_STORE);
    }
    public void setServerState(ServerState serverState){
        setAddress(serverState.getAddress());
        setPort(serverState.getPort());
    }
    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
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

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }
}
