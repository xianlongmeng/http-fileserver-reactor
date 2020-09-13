package com.ly.common.domain.file;

public class FileChunkCopy {

    private String fileName;
    private String path;
    private int chunk;
    private int chunkSize;
    private int chunkCount;
    private long fileSize;
    private long serverId;

    public FileChunkCopy(String path,String fileName,int chunk,int chunkSize,int chunkCount,long fileSize,long serverId){
        this.path=path;
        this.fileName=fileName;
        this.chunk=chunk;
        this.chunkSize=chunkSize;
        this.chunkCount=chunkCount;
        this.fileSize=fileSize;
        this.serverId=serverId;
    }
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getChunk() {
        return chunk;
    }

    public void setChunk(int chunk) {
        this.chunk = chunk;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

}
