package com.ly.common.domain;

import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.token.TokenInfo;

public class DFSPartChunk extends PartChunk {

    private boolean chunked;
    private int index = 0;
    private int contentLength = 0;
    private TokenInfo tokenInfo;
    private FileInfo fileInfo;
    private String fileFullName;
    private String etag;

    public DFSPartChunk(boolean chunked, TokenInfo tokenInfo,String etag) {
        super(chunked);
        this.tokenInfo = tokenInfo;
        this.etag = etag;
    }

    public DFSPartChunk(boolean chunked, int index, int chunk, int chunkSize, int chunkCount, TokenInfo tokenInfo,
            String etag) {
        super(chunked, chunk, chunkSize, chunkCount);
        this.index = index;
        this.tokenInfo = tokenInfo;
        this.etag = etag;
    }

    public DFSPartChunk(boolean chunked, int index, int chunk, int chunkSize, int chunkCount, TokenInfo tokenInfo,
            String etag, FileInfo fileInfo) {
        this(chunked, index, chunk, chunkSize, chunkCount, tokenInfo, etag);
        this.fileInfo = fileInfo;
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

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }
}
