package com.ly.common.domain.file;

public class FileTransferInfo {
    private short pathLength;
    private String path;
    private short fileNameLength;
    private String fileName;
    private int chunk;
    private int chunkSize;
    private int chunkCount;
    private int size;

    public short getPathLength() {
        return pathLength;
    }

    public void setPathLength(short pathLength) {
        this.pathLength = pathLength;
    }

    public short getFileNameLength() {
        return fileNameLength;
    }

    public void setFileNameLength(short fileNameLength) {
        this.fileNameLength = fileNameLength;
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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
