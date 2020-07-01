package com.ly.common.domain.token;

public class TokenInfo{
    public static final int TOKEN_READ=0;
    public static final int TOKEN_WRITE=1;
    public static final int TOKEN_UPDATE=2;
    private long lastTime;
    private String token;
    private String path;
    private String fileName;

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
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
}
