package com.ly.rhdfs.log.server.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.log.ServerFileChunkLog;

public class ServerFileChunkUtil {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    private String bootPath;

    public void setBootPath(String bootPath) {
        this.bootPath = bootPath;
    }

    public void writeAddServerFileChunk(long serverId, String path, String fileName, int chunk) {
        ReentrantLock lock = lockMap.computeIfAbsent(serverId, key -> new ReentrantLock());
        lock.lock();
        String fileLogName = String.format("%s/server_%d_file_chunk.sfc", bootPath, serverId);
        try (FileWriter fileWriter = new FileWriter(fileLogName, true)) {
            fileWriter.write(
                    new ServerFileChunkLog(ServerFileChunkLog.SERVER_FILE_ADD, path, fileName, chunk).toString());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public void writeDeleteServerFileChunk(long serverId, String path, String fileName, int chunk) {
        ServerFileChunkLog serverFileChunkLog = new ServerFileChunkLog(ServerFileChunkLog.SERVER_FILE_ADD, path,
                fileName, chunk);
        String fileLogName = String.format("%s/server_%d_file_chunk.sfc", bootPath, serverId);
        try (RandomAccessFile fileAccess = new RandomAccessFile(fileLogName, "rw")) {
            while (true) {
                long pos = fileAccess.getFilePointer();
                String line = fileAccess.readLine();
                if (line == null)
                    break;
                ServerFileChunkLog sfcLog = ServerFileChunkLog.parseString(line);
                if (sfcLog != null && sfcLog.equalsIgnoreTime(serverFileChunkLog)) {
                    fileAccess.seek(pos);
                    fileAccess.writeBytes(ServerFileChunkLog.SERVER_FILE_DEL);
                    break;
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void writeDeleteServerFileChunk(long serverId, long position) {
        String fileLogName = String.format("%s/server_%d_file_chunk.sfc", bootPath, serverId);
        try (RandomAccessFile fileAccess = new RandomAccessFile(fileLogName, "rw")) {
            fileAccess.seek(position);
            fileAccess.writeBytes(ServerFileChunkLog.SERVER_FILE_DEL);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
    public void deleteServerFileChunkLog(long serverId){
        String fileLogName = String.format("%s/server_%d_file_chunk.sfc", bootPath, serverId);
        try {
            FileUtils.forceDelete(new File(fileLogName));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
