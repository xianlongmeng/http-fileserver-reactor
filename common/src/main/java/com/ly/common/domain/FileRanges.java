package com.ly.common.domain;

import java.util.List;

import org.springframework.core.io.Resource;

public class FileRanges {

    private List<PartRange> partRangeList;
    private long totalLength;
    private Resource resource;

    public FileRanges(long totalLength) {
        this(totalLength, null);
    }

    public FileRanges(Resource resource) {
        this(0, resource);
    }

    public FileRanges(long totalLength, Resource resource) {
        this.totalLength = totalLength;
        this.resource = resource;
    }

    public long getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(long totalLength) {
        this.totalLength = totalLength;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public List<PartRange> getPartRangeList() {
        return partRangeList;
    }

    public void setPartRangeList(List<PartRange> partRangeList) {
        this.partRangeList = partRangeList;
    }

    public String toContentRange(int index) {
        if (partRangeList == null || partRangeList.size() <= index) {
            return "";
        }
        PartRange partRange = partRangeList.get(index);
        return toContentRange(partRange);
    }

    public String toContentRange(PartRange partRange) {
        return String.format("%d-%d/%d", partRange.getStartPos(), partRange.getEndPos(), totalLength);
    }
}
