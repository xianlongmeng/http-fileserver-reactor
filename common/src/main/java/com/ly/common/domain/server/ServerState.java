package com.ly.common.domain.server;

public class ServerState {
    public static final int SIT_UNKNOWN=0;
    public static final int SIT_MASTER=1;
    public static final int SIT_MASTER_BACKUP=2;
    public static final int SIT_STORE=0x100;
    public static final int SIS_MASTER_START=1;
    public static final int SIS_MASTER_INIT=2;
    public static final int SIS_MASTER_UPDATE_CONFIG=4;
    public static final int SIS_MASTER_VOTE=8;
    public static final int SIS_MASTER_PRIME=0x10;
    public static final int SIS_MASTER_SECOND=0x20;
    public static final int SIS_MASTER_FAULT=0x40;
    public static final int SIS_STORE_START=0x10000;
    public static final int SIS_STORE_INIT=0x20000;
    public static final int SIS_STORE_VOTE=0x40000;
    public static final int SIS_STORE_READY=0x80000;
    public static final int SIS_STORE_CONFIG=0x100000;
    public static final int SIS_STORE_CLEAR=0x200000;
    public static final int SIS_STORE_FAULT=0x400000;
    public static final int SIS_UNKNOWN=0;
    private int serverId;
    private String address;
    private int port;
    private int type=SIT_UNKNOWN;
    private long spaceSize;
    // new、running、reconnect
    private int state=SIS_UNKNOWN;
    private boolean online=false;
    private long writeLastTime;
    private int runningCount;
    private int writingCount;
    private int masterCount;
    private int storeCount;

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

    public int getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(int runningCount) {
        this.runningCount = runningCount;
    }

    public int getWritingCount() {
        return writingCount;
    }

    public void setWritingCount(int writingCount) {
        this.writingCount = writingCount;
    }

    public int getMasterCount() {
        return masterCount;
    }

    public void setMasterCount(int masterCount) {
        this.masterCount = masterCount;
    }

    public int getStoreCount() {
        return storeCount;
    }

    public void setStoreCount(int storeCount) {
        this.storeCount = storeCount;
    }
}
