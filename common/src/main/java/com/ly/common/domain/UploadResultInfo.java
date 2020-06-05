package com.ly.common.domain;

public class UploadResultInfo extends ResultInfo {

    private String name;
    private String fileName;

    public UploadResultInfo(int result) {
        super(result);
    }

    public UploadResultInfo(int result, String name, String fileName) {
        super(result);
        this.name = name;
        this.fileName = fileName;
    }

    public UploadResultInfo(int result, String errorCode, String errorDesc, String name, String fileName) {
        super(result, errorCode, errorDesc);
        this.name = name;
        this.fileName = fileName;
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
}
