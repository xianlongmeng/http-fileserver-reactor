package com.ly.common.domain.file;

public class AbstractFileTransfer {

    private short pathLength;
    private String path;
    private short fileNameLength;
    private String fileName;
    private short etagLength;
    private String etag;
    private int chunkIndex;
    private int chunkSize;
    private int chunkCount;
    private int chunkPieceIndex;
    private int chunkPieceSize;
    private int chunkPieceCount;
    private int startPos;
    private int packageLength;

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

    public short getEtagLength() {
        return etagLength;
    }

    public void setEtagLength(short etagLength) {
        this.etagLength = etagLength;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
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

    public int getChunkPieceIndex() {
        return chunkPieceIndex;
    }

    public void setChunkPieceIndex(int chunkPieceIndex) {
        this.chunkPieceIndex = chunkPieceIndex;
    }

    public int getChunkPieceSize() {
        return chunkPieceSize;
    }

    public void setChunkPieceSize(int chunkPieceSize) {
        this.chunkPieceSize = chunkPieceSize;
    }

    public int getChunkPieceCount() {
        return chunkPieceCount;
    }

    public void setChunkPieceCount(int chunkPieceCount) {
        this.chunkPieceCount = chunkPieceCount;
    }

    public int getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public int getPackageLength() {
        return packageLength;
    }

    public void setPackageLength(int packageLength) {
        this.packageLength = packageLength;
    }
}
