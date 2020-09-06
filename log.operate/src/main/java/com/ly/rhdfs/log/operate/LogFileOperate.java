package com.ly.rhdfs.log.operate;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.ly.common.util.ConvertUtil;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.log.OperationLog;
import com.ly.common.util.DateFormatUtils;
import com.ly.common.util.DfsFileUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class LogFileOperate {

    private final String logPath;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private String currentFileName;
    private List<String> logFileNames;
    private int curLogIndex=-1;
    private File currentFile;
    private BufferedReader currentFileReader;
    private FileWriter currentFileWriter;
    private LocalDate currentFileDate;
    private DfsFileUtils dfsFileUtils;

    public void setDfsFileUtils(DfsFileUtils dfsFileUtils){
        this.dfsFileUtils=dfsFileUtils;
    }
    public LogFileOperate(String logPath) {
        this.logPath = logPath;
    }

    public BufferedReader openOperateFile(LocalDate logDate) {
        if (logDate == null)
            return null;
        currentFileDate = logDate;
        try {
            if (currentFileReader != null)
                currentFileReader.close();
            currentFileName = String.format("%s/rhdfs_op.%s.log", logPath,
                    DateFormatUtils.buildSimpleDateFormatter(currentFileDate));
            currentFile = new File(currentFileName);
            if (currentFile.exists()) {
                return new BufferedReader(new FileReader(currentFile));
            } else {
                return null;
            }
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    private int findStartPos(List<String> fileNames,String writeDatetime){
        for (int i=0;i<fileNames.size();i++){
            String fileName=fileNames.get(i);
            if (writeDatetime.compareTo(fileName)<=0)
                return i;
        }
        return -1;
    }
    public OperationLog openOperateFile(long writeLastTime) {
        logFileNames = dfsFileUtils.findFilePath(logPath,
                new AndFileFilter(new PrefixFileFilter("rhdfs_op"), new SuffixFileFilter(".log")), true);
        logFileNames.sort(String::compareTo);
        LocalDateTime writeDateTime=ConvertUtil.toLocalDateTime(writeLastTime);
        String wdtStr=DateFormatUtils.buildSimpleDateFormatter(writeDateTime);
        curLogIndex=findStartPos(logFileNames,wdtStr);
        if (curLogIndex==-1)
            return null;
        while (curLogIndex<logFileNames.size()) {
            try {
                if (currentFileReader != null) {
                    currentFileReader.close();
                    currentFileReader=null;
                }
                currentFileName = logFileNames.get(curLogIndex);
                currentFile = new File(currentFileName);
                if (currentFile.exists()) {
                    currentFileReader=new BufferedReader(new FileReader(currentFile));
                    String line;
                    while ((line = currentFileReader.readLine())!=null){
                        long dt=LogOperateUtils.parseOperationLogWriteTime(line);
                        if (dt>=writeLastTime) {
                            return LogOperateUtils.parseOperationLog(line);
                        }
                    }
                } else {
                    curLogIndex++;
                }
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(), e);
                if (currentFileReader!=null) {
                    try {
                        currentFileReader.close();
                        currentFileReader=null;
                    } catch (IOException ignored) {
                        //do nothing
                    }
                }
                curLogIndex++;
            }
        }
        return null;
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

    public OperationLog readNextOperationLog() {
        if (currentFileReader == null)
            return null;
        try {
            String logStr=currentFileReader.readLine();
            if (logStr==null){
                currentFileReader.close();
                if (logFileNames!=null && curLogIndex>=0 && curLogIndex<logFileNames.size()){
                    while (curLogIndex<logFileNames.size()) {
                        currentFileName = logFileNames.get(curLogIndex);
                        currentFile = new File(currentFileName);
                        if (currentFile.exists()) {
                            currentFileReader = new BufferedReader(new FileReader(currentFile));
                            break;
                        }
                        curLogIndex++;
                    }
                }else{
                    return null;
                }
            }
            return LogOperateUtils.parseOperationLog(currentFileReader.readLine());
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            return null;
        }
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

    public void close() {
        if (currentFileWriter != null) {
            try {
                currentFileWriter.close();
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }
        if (currentFileReader != null) {
            try {
                currentFileReader.close();
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }
    }
}
