package com.ly.rhdfs.communicate.command;

import java.util.UUID;

public class DFSCommandReply extends DFSCommand {
    public static final byte REPLY_STATE_FALSE = 0x00;
    public static final byte REPLY_STATE_TRUE = 0x01;
    private UUID replyUUID;
    //0-99:exception;100-199:param,read;200-299:write;300-399:;400-499:transfer;500-599:verify
    private int errorCode;
    private byte reply = REPLY_STATE_FALSE;

    public DFSCommandReply() {
        commandType = DFSCommand.CT_REPLY;
        fixLength = 57;
    }

    public UUID getReplyUUID() {
        return replyUUID;
    }

    public void setReplyUUID(UUID replyUUID) {
        this.replyUUID = replyUUID;
    }

    public long getReplyMostSigBits() {
        return replyUUID.getMostSignificantBits();
    }

    public long getReplyLeastSigBits() {
        return replyUUID.getLeastSignificantBits();
    }

    public byte getReply() {
        return reply;
    }

    public void setReply(byte reply) {
        this.reply = reply;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}
