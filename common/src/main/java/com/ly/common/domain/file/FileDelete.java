package com.ly.common.domain.file;

public class FileDelete {
    private String path;
    private String fileName;

    public FileDelete(String path,String fileName){
        this.path=path;
        this.fileName=fileName;
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
}
