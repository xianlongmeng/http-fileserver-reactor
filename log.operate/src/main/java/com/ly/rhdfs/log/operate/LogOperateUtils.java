package com.ly.rhdfs.log.operate;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import com.ly.common.util.ConvertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ly.common.domain.log.OperationLog;
import com.ly.common.util.DateFormatUtils;
import com.ly.common.util.DfsFileUtils;

@Component
public class LogOperateUtils {

    private static final Logger logger = LoggerFactory.getLogger(LogOperateUtils.class);

    private String logPath;
    private DfsFileUtils dfsFileUtils;
    @Autowired
    public void setDfsFileUtils(DfsFileUtils dfsFileUtils){
        this.dfsFileUtils=dfsFileUtils;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public OperationLog parseOperationLog(String line) {
        if (line == null)
            return null;
        String[] s = line.split(",");
        if (s[0].length() != 21 || s.length < 4)
            return null;
        long timestamp = ConvertUtil.parseLong(s[0],0);
        String opType = s[1];
        String path = s[2];
        String fileName = s[3];
        return new OperationLog(timestamp, opType, path, fileName);
    }

    public long parseOperationLogWriteTime(String line) {
        if (line == null)
            return 0;
        int index=line.indexOf(',');
        if (index==-1)
            index=line.length();
        return ConvertUtil.parseLong(line.substring(0,index),0);
    }

    public String findLastTimeFileName() {
        List<String> fileNameList = dfsFileUtils.findFilePath(logPath, "", true).stream().sorted()
                .collect(Collectors.toList());
        if (fileNameList.isEmpty())
            return null;
        else
            return fileNameList.get(fileNameList.size() - 1);
    }

    public String readLastLine(String fileName) {
        if (StringUtils.isEmpty(fileName))
            return null;
        RandomAccessFile rf = null;
        try {
            rf = new RandomAccessFile(fileName, "r");
            long len = rf.length();
            long start = rf.getFilePointer();
            long nextend = start + len - 1;
            String line;
            rf.seek(nextend);
            int c = -1;
            while (nextend > start) {
                c = rf.read();
                if (c == '\n' || c == '\r') {
                    line = rf.readLine();
                    return line;
                }
                nextend--;
                rf.seek(nextend);
                if (nextend == 0) {// 当文件指针退至文件开始处，输出第一行
                    return rf.readLine();
                }
            }
            return null;
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            return null;
        } finally {
            try {
                if (rf != null)
                    rf.close();
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }
    }

    public OperationLog readLastTimeOperationLog() {
        String operationLogLine = readLastLine(findLastTimeFileName());
        if (StringUtils.isEmpty(operationLogLine))
            return null;
        return parseOperationLog(operationLogLine);
    }

    public long readLastTime() {
        OperationLog operationLog = readLastTimeOperationLog();
        if (operationLog == null)
            return Instant.now().toEpochMilli();
        else
            return operationLog.getTimestamp();
    }
}
