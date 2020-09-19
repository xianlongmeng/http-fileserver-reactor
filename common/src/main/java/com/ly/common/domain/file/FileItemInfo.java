package com.ly.common.domain.file;

public class FileItemInfo extends ItemInfo{

    private long size;
    private long lastModifyTime;
    private String etag;
    public FileItemInfo(String name) {
        super(name, false);
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }
}
