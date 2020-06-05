package com.ly.common.domain;

import java.util.List;

import org.springframework.core.io.support.ResourceRegion;
import org.springframework.util.StringUtils;

public class PartRange {

    private String name;
    private long startPos;
    private long endPos;
    private List<ResourceRegion> resourceRegionList;

    public PartRange(long startPos, long endPos){
        this("",startPos,endPos);
    }
    public PartRange(String name,long startPos, long endPos) {
        this.name=name;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public String getName() {
        if (StringUtils.isEmpty(name)){
            return String.format("%d-%d",startPos,endPos);
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStartPos() {
        return startPos;
    }

    public void setStartPos(long startPos) {
        this.startPos = startPos;
    }

    public long getEndPos() {
        return endPos;
    }

    public void setEndPos(long endPos) {
        this.endPos = endPos;
    }

    public List<ResourceRegion> getResourceRegionList() {
        return resourceRegionList;
    }

    public void setResourceRegionList(List<ResourceRegion> resourceRegionList) {
        this.resourceRegionList = resourceRegionList;
    }
}
