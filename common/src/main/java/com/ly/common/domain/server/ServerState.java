package com.ly.common.domain.server;

public class ServerState {

    public static final int SIT_UNKNOWN = 0;
    public static final int SIT_MASTER = 1;
    public static final int SIT_MASTER_BACKUP = 2;
    public static final int SIT_STORE = 0x100;
    public static final int SIS_MASTER_START = 1;
    public static final int SIS_MASTER_INIT = 2;
    public static final int SIS_MASTER_UPDATE_CONFIG = 4;
    public static final int SIS_MASTER_VOTE = 8;
    public static final int SIS_MASTER_PRIME = 0x10;
    public static final int SIS_MASTER_SECOND = 0x20;
    public static final int SIS_MASTER_FAULT = 0x40;
    public static final int SIS_STORE_START = 0x10000;
    public static final int SIS_STORE_INIT = 0x20000;
    public static final int SIS_STORE_VOTE = 0x40000;
    public static final int SIS_STORE_READY = 0x80000;
    public static final int SIS_STORE_CONFIG = 0x100000;
    public static final int SIS_STORE_CLEAR = 0x200000;
    public static final int SIS_STORE_FAULT = 0x400000;
    public static final int SIS_UNKNOWN = 0;
    private long serverId;
    private String address;
    private int port;
    private int type = SIT_UNKNOWN;
    private long spaceSize;
    // new、running、reconnect
    private int state = SIS_UNKNOWN;
    private boolean online = false;
    // 执行文件写操作完成时设置
    private long writeLastTime;
    // address & port updated last time,可以取所有server中的最大值
    private long updateAddressLastTime;
    // master是否准备就绪，true：可以成为master，等待投票
    private boolean ready;
    // 是否投票给我
    private long votedServerId;
    // 最后收到心跳时间，对于备用Master为准备就绪可以成为Master的时间
    private long lastTime;

    public void copyFrom(ServerState serverState) {
        address = serverState.address;
        port = serverState.port;
        type = serverState.type;
        spaceSize = serverState.spaceSize;
        state = serverState.state;
        online = serverState.online;
        writeLastTime = serverState.writeLastTime;
        updateAddressLastTime = serverState.updateAddressLastTime;
        ready= serverState.ready;
        votedServerId=serverState.votedServerId;
        lastTime=serverState.lastTime;
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getSpaceSize() {
        return spaceSize;
    }

    public void setSpaceSize(long spaceSize) {
        this.spaceSize = spaceSize;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getWriteLastTime() {
        return writeLastTime;
    }

    public void setWriteLastTime(long writeLastTime) {
        this.writeLastTime = writeLastTime;
    }

    public long getUpdateAddressLastTime() {
        return updateAddressLastTime;
    }

    public void setUpdateAddressLastTime(long updateAddressLastTime) {
        this.updateAddressLastTime = updateAddressLastTime;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public long getVotedServerId() {
        return votedServerId;
    }

    public void setVotedServerId(long votedServerId) {
        this.votedServerId = votedServerId;
    }
}
