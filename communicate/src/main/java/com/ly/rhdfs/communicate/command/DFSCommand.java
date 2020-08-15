package com.ly.rhdfs.communicate.command;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.UUIDGenerator;

import java.rmi.server.UID;
import java.util.UUID;

public class DFSCommand {

    public static final int CT_HEART_BEAT = 0;
    public static final int CT_STATE = 1;
    public static final int CT_TOKEN = 2;
    public static final int CT_FILE_CHUNK = 4;
    public static final int CT_FILE_INFO = 8;
    public static final int CT_FILE_DELETE = 9;
    public static final int CT_DIRECT_FILE_ITEM = 0x10;
    public static final int CT_CLEAR = 0x20;
    public static final int CT_REQUEST_EXPAND = 0x40;
    public static final int CT_SERVER_ADDRESS = 0x80;
    public static final int CT_FILE_TRANSFER = 0x100;
    public static final int CT_FILE_TRANSFER_STATE = 0x101;
    public static final int CT_FILE_OPERATE = 0x200;
    public static final byte REPLY_STATE_FALSE = 0x00;
    public static final byte REPLY_STATE_TRUE = 0x01;
    protected int commandType;
    protected int length;
    protected long serverId;
    protected long timestamp;
    protected byte reply=0;
    protected UUID uuid;
    protected int fixLength = 37;

    public DFSCommand(){
        uuid= Generators.timeBasedGenerator().generate();
    }

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

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
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

    public byte getReply() {
        return reply;
    }

    public void setReply(byte reply) {
        this.reply = reply;
    }

    public long getMostSigBits() {
        return uuid.getMostSignificantBits();
    }

    public long getLeastSigBits() {
        return uuid.getLeastSignificantBits();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setUuid(long mostSigBits,long leastSigBits){
        uuid=new UUID(mostSigBits,leastSigBits);
    }
}
