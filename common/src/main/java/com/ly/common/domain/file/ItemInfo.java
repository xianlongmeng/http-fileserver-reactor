package com.ly.common.domain.file;

public class ItemInfo {

    private String name;
    private boolean directory;

    public ItemInfo(String name,boolean directory){
        this.name=name;
        this.directory=directory;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }
}
