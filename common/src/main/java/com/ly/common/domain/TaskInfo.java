package com.ly.common.domain;

import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.token.TokenInfo;

public class TaskInfo {
    private TokenInfo tokenInfo;
    private FileInfo fileInfo;

    public TaskInfo(TokenInfo tokenInfo, FileInfo fileInfo) {
        this.tokenInfo = tokenInfo;
        this.fileInfo = fileInfo;
    }

    public TokenInfo getTokenInfo() {
        return tokenInfo;
    }

    public void setTokenInfo(TokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }
}
