package com.ly.common.domain;

public class UploadResultInfo extends ResultInfo {

    private String name;
    private String fileName;
    private PartChunk partChunk;

    public UploadResultInfo(int result) {
        super(result);
    }

    public UploadResultInfo(int result, String name, String fileName) {
        super(result);
        this.name = name;
        this.fileName = fileName;
    }

    public UploadResultInfo(int result, String errorCode, String errorDesc, String name, String fileName,PartChunk partChunk) {
        super(result, errorCode, errorDesc);
        this.name = name;
        this.fileName = fileName;
        this.partChunk=partChunk;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public PartChunk getPartChunk() {
        return partChunk;
    }

    public void setPartChunk(PartChunk partChunk) {
        this.partChunk = partChunk;
    }
}
