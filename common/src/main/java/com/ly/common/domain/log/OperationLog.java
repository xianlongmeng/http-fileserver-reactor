package com.ly.common.domain.log;

import java.time.LocalDateTime;

import com.ly.common.util.DateFormatUtils;

public class OperationLog {

    public static final String OP_TYPE_ADD_FILE = "add-file";
    public static final String OP_TYPE_UPDATE_FILE = "update-file";
    public static final String OP_TYPE_DELETE_FILE = "delete-file";
    public static final String OP_TYPE_ADD_CHUNK = "add-chunk";
    public static final String OP_TYPE_UPDATE_CHUNK = "update-chunk";
    public static final String OP_TYPE_DELETE_CHUNK = "delete-chunk";
    public static final String OP_TYPE_MOVE_CHUNK = "move-chunk";
    private LocalDateTime dateTime;
    private String opType;
    private String path;
    private String fileName;
    private int index;
    // 细化log

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getOpType() {
        return opType;
    }

    public void setOpType(String opType) {
        this.opType = opType;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return DateFormatUtils.buildSimpleDateTimeMillSecondFormatter(dateTime == null ? LocalDateTime.now() : dateTime)
                + "," + opType + "," + path + "," + fileName + "," + index + "\n";
    }
}
