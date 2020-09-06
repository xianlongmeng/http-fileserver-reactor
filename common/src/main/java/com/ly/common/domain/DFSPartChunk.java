package com.ly.common.domain;

import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.token.TokenInfo;

public class DFSPartChunk extends PartChunk{

    private boolean chunked;
    private int index=0;
    private int chunk = 0;
    private int chunkSize = 0;
    private int chunkCount = 0;
    private int contentLength=0;
    private TokenInfo tokenInfo;
    private FileInfo fileInfo;
    private String fileFullName;

    public DFSPartChunk(boolean chunked,TokenInfo tokenInfo) {
        super(chunked);
        this.tokenInfo=tokenInfo;
    }
    public DFSPartChunk(boolean chunked, int index, int chunk, int chunkSize, int chunkCount, TokenInfo tokenInfo) {
        super(chunked,chunk,chunkSize,chunkCount);
        this.index=index;
        this.tokenInfo=tokenInfo;
    }
    public DFSPartChunk(boolean chunked, int index, int chunk, int chunkSize, int chunkCount, TokenInfo tokenInfo,FileInfo fileInfo) {
        this(chunked,index,chunk,chunkSize,chunkCount,tokenInfo);
        this.fileInfo=fileInfo;
    }
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
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

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public String getFileFullName() {
        return fileFullName;
    }

    public void setFileFullName(String fileFullName) {
        this.fileFullName = fileFullName;
    }
}
