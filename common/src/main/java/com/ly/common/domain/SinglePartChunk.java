package com.ly.common.domain;

public class SinglePartChunk extends PartChunk{
    private String fileName;
    private String path;
    public SinglePartChunk(boolean chunked) {
        super(chunked);
    }
    
    public SinglePartChunk(boolean chunked, int chunk, int chunkSize, int chunkCount) {
        super(chunked, chunk, chunkSize, chunkCount);
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
}
