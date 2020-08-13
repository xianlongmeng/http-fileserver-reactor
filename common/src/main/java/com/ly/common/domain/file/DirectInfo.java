package com.ly.common.domain.file;

import java.util.List;

public class DirectInfo extends ItemInfo {

    private List<ItemInfo> itemInfos;

    public List<ItemInfo> getItemInfos() {
        return itemInfos;
    }

    public void setItemInfos(List<ItemInfo> itemInfos) {
        this.itemInfos = itemInfos;
    }
}
