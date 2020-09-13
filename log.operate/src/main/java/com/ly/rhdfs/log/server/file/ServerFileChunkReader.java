package com.ly.rhdfs.log.server.file;

import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.log.ServerFileChunkLog;

public class ServerFileChunkReader {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String bootPath;
    private final long serverId;
    private RandomAccessFile fileReader;

    public ServerFileChunkReader(String bootPath, long serverId) {
        this.bootPath = bootPath;
        this.serverId = serverId;
    }

    public long getServerId(){
        return serverId;
    }
    public boolean openFile() {
        String fileLogName = String.format("%s/server_%d_file_chunk.sfc", bootPath, serverId);
        try {
            fileReader = new RandomAccessFile(fileLogName,"r");
            return true;
        } catch (FileNotFoundException e) {
            logger.error(String.format("File open failed,fileName:%s", fileLogName), e);
            return false;
        }
    }

    public synchronized ServerFileChunkLog readNext() {
        if (fileReader == null)
            return null;
        try {
            long pos=fileReader.getFilePointer();
            String line = fileReader.readLine();
            return ServerFileChunkLog.parseString(line,pos);
        } catch (IOException e) {
            logger.error(String.format("read file failed,serverId:%d", serverId), e);
            return null;
        }
    }

    public void close() {
        try {
            fileReader.close();
        } catch (IOException e) {
            logger.error(String.format("close file failed,serverId:%d", serverId), e);
        }
    }
}
