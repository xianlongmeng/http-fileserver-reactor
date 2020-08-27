package com.ly.common.domain.token;

import com.ly.common.domain.token.TokenInfo;

import java.time.Instant;

public class FileDeleteServer {
    private TokenInfo fileDeleteTokenInfo;
    private long serverId;
    private long timestamp;

    public FileDeleteServer(TokenInfo fileDeleteTokenInfo,long serverId){
        this.fileDeleteTokenInfo=fileDeleteTokenInfo;
        this.serverId=serverId;
        timestamp= Instant.now().toEpochMilli();
    }

    public TokenInfo getFileDeleteTokenInfo() {
        return fileDeleteTokenInfo;
    }

    public void setFileDeleteTokenInfo(TokenInfo fileDeleteTokenInfo) {
        this.fileDeleteTokenInfo = fileDeleteTokenInfo;
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
}
