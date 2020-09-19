package com.ly.common.domain.file;

public class FileChunkInfo {
    private String path;
    private String fileName;
    private int chunk;
    private String chunkEtag;

    public FileChunkInfo(String path,String fileName,int chunk,String chunkEtag){
        this.path=path;
        this.fileName=fileName;
        this.chunk=chunk;
        this.chunkEtag=chunkEtag;
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

    public int getChunk() {
        return chunk;
    }

    public void setChunk(int chunk) {
        this.chunk = chunk;
    }

    public String getChunkEtag() {
        return chunkEtag;
    }

    public void setChunkEtag(String chunkEtag) {
        this.chunkEtag = chunkEtag;
    }
}
