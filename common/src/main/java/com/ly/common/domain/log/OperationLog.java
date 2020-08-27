package com.ly.common.domain.log;

import java.time.Instant;

public class OperationLog {

    public static final String OP_TYPE_ADD_FILE_FINISH = "add-file-finish";
    public static final String OP_TYPE_ADD_FILE_INIT = "add-file-init";
    public static final String OP_TYPE_ADD_FILE_INIT_UPDATE = "add-file-init-update";
    public static final String OP_TYPE_ADD_FILE_FAIL = "add-file-fail";
    public static final String OP_TYPE_UPDATE_FILE = "update-file";
    public static final String OP_TYPE_DELETE_FILE = "delete-file";
    public static final String OP_TYPE_UPDATE_FINISH = "update-finish";

    private long timestamp;
    private String opType;
    private String path;
    private String fileName;

    public OperationLog(String opType, String path, String fileName) {
        this(Instant.now().toEpochMilli(), opType, path, fileName);
    }

    public OperationLog(long timestamp, String opType, String path, String fileName) {
        this.opType = opType;
        this.path = path;
        this.fileName = fileName;
        this.timestamp = timestamp;
    }
    // 细化log

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    @Override
    public String toString() {
        return timestamp + "," + opType + "," + path + "," + fileName + "\n";
    }
}
