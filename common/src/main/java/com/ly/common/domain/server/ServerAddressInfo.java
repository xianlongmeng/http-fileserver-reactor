package com.ly.common.domain.server;

import org.springframework.lang.NonNull;

public class ServerAddressInfo implements Comparable{

    private long serverId;
    private String address;
    private int port;

    private String hostUri;

    public ServerAddressInfo(long serverId) {
        setServerId(serverId);
    }
    public ServerAddressInfo(ServerState serverState) {
        setAddress(serverState.getAddress());
        setPort(serverState.getPort());
        setHostUri(serverState.getHostUrl());
        setServerId(serverState.getServerId());
    }

    public void setServerState(ServerState serverState) {
        setAddress(serverState.getAddress());
        setPort(serverState.getPort());
        setHostUri(serverState.getHostUrl());
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

    public String getHostUri() {
        return hostUri;
    }

    public void setHostUri(String hostUri) {
        this.hostUri = hostUri;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        if (o instanceof ServerAddressInfo oSai)
            return Long.compare(serverId,oSai.serverId);
        else if (o instanceof Long ol)
            return Long.compare(serverId,ol);
        else
            return 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServerAddressInfo sai)
            return serverId==sai.serverId;
        if (obj instanceof Long l)
            return serverId==l;
        return false;
    }
}
