package com.ly.common.domain.log;

import com.ly.common.domain.file.FileInfo;

import java.time.LocalDateTime;

public class OperationLog {
    public static final int OP_TYPE_ADD=1;
    public static final int OP_TYPE_UPDATE=2;
    public static final int OP_TYPE_DELETE=4;
    public static final int OP_TYPE_CHUNK_MOVE=1;
    private LocalDateTime dateTime;
    private int opType;
    private FileInfo fileInfo;

}
