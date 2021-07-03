package com.ly.rhdfs.client;

import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.server.ServerAddressInfo;
import com.ly.common.domain.token.TokenInfo;

public class FileChunkPieceTransfer {
    private ServerAddressInfo serverAddressInfo;
    private int chunk;
    private int chunkPiece;
    private int size;
    private int chunkCount;
    private TokenInfo tokenInfo;
    private FileInfo fileInfo;
    private int times;

    public FileChunkPieceTransfer(ServerAddressInfo serverAddressInfo, int chunk, int chunkPiece, int size,int chunkCount, TokenInfo tokenInfo, FileInfo fileInfo){
        this.serverAddressInfo=serverAddressInfo;
        this.chunk=chunk;
        this.chunkPiece=chunkPiece;
        this.size=size;
        this.tokenInfo=tokenInfo;
        this.fileInfo=fileInfo;
        this.chunkCount=chunkCount;
    }

    public ServerAddressInfo getServerAddressInfo() {
        return serverAddressInfo;
    }

    public void setServerAddressInfo(ServerAddressInfo serverAddressInfo) {
        this.serverAddressInfo = serverAddressInfo;
    }

    public int getChunk() {
        return chunk;
    }

    public void setChunk(int chunk) {
        this.chunk = chunk;
    }

    public int getChunkPiece() {
        return chunkPiece;
    }

    public void setChunkPiece(int chunkPiece) {
        this.chunkPiece = chunkPiece;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
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

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }
}
