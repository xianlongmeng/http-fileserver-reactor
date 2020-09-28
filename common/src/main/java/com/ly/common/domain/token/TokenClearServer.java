package com.ly.common.domain.token;

import java.time.Instant;

public class TokenClearServer {
    public static final int TC_TYPE_FILE_DELETE=0;
    public static final int TC_TYPE_TOKEN_CLEAR=1;
    private int type=0;
    private TokenInfo tokenInfo;
    private long serverId;
    private long timestamp;

    public TokenClearServer(int type,TokenInfo tokenInfo, long serverId){
        this.type=type;
        this.tokenInfo=tokenInfo;
        this.serverId=serverId;
        timestamp= Instant.now().toEpochMilli();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public TokenInfo getTokenInfo() {
        return tokenInfo;
    }

    public void setTokenInfo(TokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
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
