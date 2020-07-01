package com.ly.rhdfs.communicate.command;

import com.alibaba.fastjson.annotation.JSONField;

import java.beans.Transient;

public class DFSCommand {
    public static final int CT_HEART_BEAT=0;
    public static final int CT_STATE=1;
    public static final int CT_TOKEN=2;
    public static final int CT_FILE_CHUNK=4;
    public static final int CT_FILE_INFO=8;
    public static final int CT_DIRECT_FILE_ITEM=0x10;
    public static final int CT_CLEAR=0x20;
    public static final int CT_REQUEST_EXPAND=0x40;
    public static final int CT_FILE_TRANSFER=0x100;
    protected int commandType;
    protected int length;
    protected int serverId;
    protected long timestamp;
    protected int fixLength=16;

    public int getCommandType() {
        return commandType;
    }

    public void setCommandType(int commandType) {
        this.commandType = commandType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getFixLength() {
        return fixLength;
    }
}
