package com.ly.common.domain.log;

import java.time.Instant;

import org.springframework.util.ObjectUtils;

import com.ly.common.util.ConvertUtil;

public class ServerFileChunkLog {

    public static final String SERVER_FILE_ADD = "add";
    public static final String SERVER_FILE_DEL = "del";

    private String opType;
    private long timestamp;
    private String path;
    private String fileName;
    private int chunk;
    private long position;

    public ServerFileChunkLog(String opType, String path, String fileName, int chunk) {
        this(Instant.now().toEpochMilli(), opType, path, fileName, chunk);
    }

    public ServerFileChunkLog(long timestamp, String opType, String path, String fileName, int chunk) {
        this.opType = opType;
        this.path = path;
        this.fileName = fileName;
        this.timestamp = timestamp;
        this.chunk = chunk;
    }

    // 细化log
    public static ServerFileChunkLog parseString(String line, long position) {
        ServerFileChunkLog serverFileChunkLog = parseString(line);
        serverFileChunkLog.setPosition(position);
        return serverFileChunkLog;
    }

    public static ServerFileChunkLog parseString(String line) {
        if (line == null)
            return null;
        String[] s = line.split(",");
        long timestamp = ConvertUtil.parseLong(s[0], 0);
        String opType = s.length >= 2 ? s[1] : "";
        String path = s.length >= 3 ? s[2] : "";
        String fileName = s.length >= 4 ? s[3] : "";
        int chunk = ConvertUtil.parseInt(s.length >= 5 ? s[4] : "0", 0);
        return new ServerFileChunkLog(timestamp, opType, path, fileName, chunk);
    }

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

    public int getChunk() {
        return chunk;
    }

    public void setChunk(int chunk) {
        this.chunk = chunk;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return timestamp + "," + opType + "," + path + "," + fileName + "," + chunk + "\n";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ServerFileChunkLog))
            return false;
        ServerFileChunkLog serverFileChunkLog = (ServerFileChunkLog) obj;
        return ObjectUtils.nullSafeEquals(opType, serverFileChunkLog.opType)
                && ObjectUtils.nullSafeEquals(path, serverFileChunkLog.path)
                && ObjectUtils.nullSafeEquals(fileName, serverFileChunkLog.fileName)
                && chunk == serverFileChunkLog.chunk && timestamp == serverFileChunkLog.timestamp;
    }

    public boolean equalsIgnoreTime(ServerFileChunkLog serverFileChunkLog) {
        if (serverFileChunkLog == null)
            return false;
        return ObjectUtils.nullSafeEquals(opType, serverFileChunkLog.opType)
                && ObjectUtils.nullSafeEquals(path, serverFileChunkLog.path)
                && ObjectUtils.nullSafeEquals(fileName, serverFileChunkLog.fileName)
                && chunk == serverFileChunkLog.chunk;
    }
}
