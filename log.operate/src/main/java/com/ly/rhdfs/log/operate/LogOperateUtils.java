package com.ly.rhdfs.log.operate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.ly.common.domain.log.OperationLog;
import com.ly.common.util.DateFormatUtils;
import com.ly.common.util.MyFileUtils;

public class LogOperateUtils {

    private Logger logger = LoggerFactory.getLogger(LogOperateUtils.class);
    private String currentFileName;
    private File currentFile;
    private FileWriter currentFileWriter;
    private LocalDate currentFileDate;
    private String logPath;

    public LogOperateUtils(String logPath) {
        this.logPath = logPath;
    }

    private FileWriter openOperateFile() {
        try {
            if (!LocalDate.now().equals(currentFileDate)) {
                currentFileDate = LocalDate.now();
                if (currentFileWriter != null)
                    currentFileWriter.close();
            } else if (currentFileWriter != null) {
                return currentFileWriter;
            }
            currentFileName = String.format("%s/rhdfs_op.%s.log", logPath,
                    DateFormatUtils.buildSimpleDateFormatter(currentFileDate));
            currentFile = new File(currentFileName);

            if (currentFile.exists()) {
                currentFileWriter = new FileWriter(currentFileName, true);
            } else if (currentFileWriter == null) {
                currentFileWriter = new FileWriter(currentFileName);
            }
            return currentFileWriter;
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public OperationLog parseOperationLog(String line) {
        if (line == null)
            return null;
        String[] s = line.split(",");
        if (s[0].length() != 21 || s.length < 4)
            return null;
        LocalDateTime dateTime = DateFormatUtils.parseLocalDateTime4SimpleMillSecond(s[0]);
        String opType = s[1];
        String path = s[2];
        String fileName = s[3];
        int index = -1;
        if (s.length >= 5) {
            index = Integer.parseInt(s[4]);
        }
        OperationLog operationLog = new OperationLog();
        operationLog.setDateTime(dateTime);
        operationLog.setOpType(opType);
        operationLog.setPath(path);
        operationLog.setFileName(fileName);
        operationLog.setIndex(index);
        return operationLog;
    }

    public boolean writeOperateLog(OperationLog operationLog) {
        if (operationLog == null)
            return false;
        try {
            FileWriter fileWriter = openOperateFile();
            if (fileWriter == null) {
                logger.error("Operation log open file failed!");
                return false;
            }
            fileWriter.append(operationLog.toString());
            fileWriter.flush();
            return true;
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            return false;
        }
    }

    private String readLastLine(String fileName) {
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

    private String findLastTimeFileName() {
        List<String> fileNameList = MyFileUtils.findFile(logPath, "").stream().sorted().collect(Collectors.toList());
        if (fileNameList.isEmpty())
            return null;
        else
            return fileNameList.get(fileNameList.size() - 1);
    }

    public OperationLog readLastTimeOperationLog() {
        String operationLogLine = readLastLine(findLastTimeFileName());
        if (StringUtils.isEmpty(operationLogLine))
            return null;
        return parseOperationLog(operationLogLine);
    }

    public LocalDateTime readLastTime() {
        OperationLog operationLog = readLastTimeOperationLog();
        if (operationLog == null)
            return null;
        else
            return operationLog.getDateTime();
    }

    public void close() {
        if (currentFileWriter != null) {
            try {
                currentFileWriter.close();
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }
    }
}
