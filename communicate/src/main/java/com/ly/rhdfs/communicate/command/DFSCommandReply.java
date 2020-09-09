package com.ly.rhdfs.communicate.command;

import java.util.UUID;

public class DFSCommandReply extends DFSCommand{
    public static final byte REPLY_STATE_FALSE = 0x00;
    public static final byte REPLY_STATE_TRUE = 0x01;
    private UUID replyUUID;
    private byte reply=REPLY_STATE_FALSE;

    public DFSCommandReply(){
        commandType=DFSCommand.CT_REPLY;
        fixLength=54;
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
}
