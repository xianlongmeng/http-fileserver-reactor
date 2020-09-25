package com.ly.common.domain.server;

public class ServerAddressInfo implements Comparable{

    private long serverId;
    private String address;
    private int port;

    public ServerAddressInfo(long serverId) {
        setServerId(serverId);
    }
    public ServerAddressInfo(ServerState serverState) {
        setAddress(serverState.getAddress());
        setPort(serverState.getPort());
        setServerId(serverState.getServerId());
    }

    public void setServerState(ServerState serverState) {
        setAddress(serverState.getAddress());
        setPort(serverState.getPort());
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

    @Override
    public int compareTo(Object o) {
        if (o instanceof ServerAddressInfo)
            return Long.compare(serverId,((ServerAddressInfo) o).serverId);
        else
            return 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServerAddressInfo)
            return serverId==((ServerAddressInfo) obj).serverId;
        return false;
    }
}
