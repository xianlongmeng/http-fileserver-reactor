package com.ly.common.domain.token;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * invalid after 20 minutes not visit
 */
public class TokenInfo {

    public static final int TOKEN_READ = 0;
    public static final int TOKEN_WRITE = 1;
    public static final int TOKEN_UPDATE = 2;
    protected int tokenType;
    private long lastTime;
    private long expirationMills;
    private String token;
    private String path;
    private String fileName;

    public int getTokenType() {
        return tokenType;
    }

    public void setTokenType(int tokenType) {
        this.tokenType = tokenType;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public long getExpirationMills() {
        return expirationMills;
    }

    public void setExpirationMills(long expirationMills) {
        this.expirationMills = expirationMills;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TokenInfo) {
            TokenInfo tokenInfo = (TokenInfo) obj;
            if (tokenType != tokenInfo.getTokenType())
                return false;
            if (tokenType == TOKEN_READ) {
                return ObjectUtils.nullSafeEquals(token, tokenInfo.getToken());
            } else {
                return ObjectUtils.nullSafeEquals(path, tokenInfo.getPath())
                        && StringUtils.pathEquals(fileName, tokenInfo.getFileName());
            }
        } else
            return false;
    }
}
