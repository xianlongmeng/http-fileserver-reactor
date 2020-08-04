package com.ly.rhdfs.master.manager.runstate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.ly.common.util.MyFileUtils;
import org.springframework.stereotype.Component;

import com.ly.common.domain.file.ChunkInfo;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.server.ServerRunState;

@Component
public class FileServerRunManager {

    // config file initial
    // 磁盘预留最小空间64G
    private static final long MIN_SPACE_SIZE = 0x1000000000L;
    private static final int MAX_TASK_COUNT = 500;
    private static final int FILE_COPIES = 3;
    // 分块大小，缺省64M
    private static final int CHUNK_SIZE = 0x4000000;
    private final Map<Long, ServerRunState> serverRunStateMap = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lockObj = new ReentrantReadWriteLock();
    private List<ServerRunState> availableOrderlyServerRunStates;

    public void putServerRunState(ServerRunState serverRunState) {
        if (serverRunState != null && serverRunState.getServerState() != null) {
            serverRunStateMap.put(serverRunState.getServerId(), serverRunState);
        }
    }

    private List<ServerRunState> sortAvailableServerRunState() {
        // 剩余空间>50G:运行负载、已有文件数、序号
        return serverRunStateMap.values().stream()
                .filter(serverRunState -> serverRunState != null && serverRunState.isOnline()
                        && serverRunState.getSpaceSize() > MIN_SPACE_SIZE
                        && serverRunState.getUploadCount() * 3 + serverRunState.getDownloadCount() > MAX_TASK_COUNT)
                .sorted(this::compareValidServerPrior).collect(Collectors.toList());
    }

    public void resetForceAvailableOrderlyServerRunStates() {
        lockObj.writeLock().lock();
        try {
            availableOrderlyServerRunStates = sortAvailableServerRunState();
        } finally {
            lockObj.writeLock().unlock();
        }
    }

    public boolean resetAvailableOrderlyServerRunStates() {
        boolean res = lockObj.writeLock().tryLock();
        if (res) {
            try {
                availableOrderlyServerRunStates = sortAvailableServerRunState();
            } finally {
                lockObj.writeLock().unlock();
            }
        }
        return res;
    }

    public boolean resetAvailableOrderlyServerRunStates(int lockTime) {
        boolean res;
        try {
            res = lockObj.writeLock().tryLock(lockTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
        if (res) {
            try {
                availableOrderlyServerRunStates = sortAvailableServerRunState();
            } finally {
                lockObj.writeLock().unlock();
            }
        }
        return res;
    }

    /**
     * 比较有效的服务器
     *
     * @param ss1
     * @param ss2
     * @return
     */
    public int compareValidServerPrior(ServerRunState ss1, ServerRunState ss2) {
        return ss1.getUploadCount() * 3 + ss1.getDownloadCount() - ss2.getUploadCount() * 3 - ss2.getDownloadCount();
    }

    /**
     * 比较所有的服务器
     *
     * @param ss1
     * @param ss2
     * @return
     */
    public int compareServerPrior(ServerRunState ss1, ServerRunState ss2) {
        // 剩余空间>50G:运行负载、已有文件数、序号
        if (ss2 == null && ss1 != null)
            return 1;
        else if (ss1 == null && ss2 != null)
            return -1;
        else if (ss1 == null)
            return 0;
        if (ss1.isOnline() && !ss2.isOnline())
            return 1;
        else if (ss2.isOnline() && !ss1.isOnline())
            return -1;
        if (ss1.getSpaceSize() < MIN_SPACE_SIZE && ss2.getSpaceSize() > MIN_SPACE_SIZE) {
            return 1;
        } else if (ss1.getSpaceSize() > MIN_SPACE_SIZE && ss2.getSpaceSize() < MIN_SPACE_SIZE) {
            return -1;
        }
        int res = ss1.getUploadCount() * 3 + ss1.getDownloadCount() - ss2.getUploadCount() * 3 - ss2.getDownloadCount();
        if (res != 0)
            return res;
        if (ss2.getServerId() > ss1.getServerId())
            return 1;
        else
            return -1;
    }

    public int compareServerPrior(long sid1, long sid2) {
        // 剩余空间>50G:运行负载、已有文件数、序号
        ServerRunState ss1 = serverRunStateMap.get(sid1);
        ServerRunState ss2 = serverRunStateMap.get(sid2);
        return compareServerPrior(ss1, ss2);
    }

    /**
     * 分配上传文件的分块服务器信息
     * 
     * @param path
     * @param fileName
     * @param fileSize
     * @return
     */
    public FileInfo assignUploadFileServer(String path, String fileName, long fileSize) {
        lockObj.readLock().lock();
        if (availableOrderlyServerRunStates.size() < FILE_COPIES)
            return null;
        FileInfo fileInfo = new FileInfo();
        fileInfo.setCreateTime(Instant.now().toEpochMilli());
        fileInfo.setChunk(true);
        fileInfo.setSize(fileSize);
        try {
            int chunkCount = (int) ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE);
            fileInfo.setChunkCount(chunkCount);
            fileInfo.setChunkSize(CHUNK_SIZE);
            for (int i = 0; i < chunkCount; i++) {
                ChunkInfo chunkInfo = new ChunkInfo();
                chunkInfo.setIndex(i);
                for (int j = 0; j < FILE_COPIES; j++) {
                    chunkInfo.getChunkServerIdList().add(availableOrderlyServerRunStates
                            .get((chunkCount * j + i) % availableOrderlyServerRunStates.size()).getServerId());
                }
                fileInfo.getFileChunkList().add(chunkInfo);
            }
        } finally {
            lockObj.readLock().unlock();
        }
        return fileInfo;
    }

    /**
     * 分配上传服务器的某个分块信息
     * 
     * @param path
     * @param fileName
     * @param fileSize
     * @param chunk
     * @return
     */
    public FileInfo assignUploadFileServer(String path, String fileName, long fileSize, int chunk) {
        lockObj.readLock().lock();
        if (availableOrderlyServerRunStates.size() < FILE_COPIES)
            return null;
        FileInfo fileInfo = null;
        try {
            int chunkCount = (int) ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE);
            if (chunkCount <= chunk)
                return null;
            fileInfo = new FileInfo();
            fileInfo.setCreateTime(Instant.now().toEpochMilli());
            fileInfo.setChunk(true);
            fileInfo.setSize(fileSize);
            fileInfo.setChunkCount(chunkCount);
            fileInfo.setChunkSize(CHUNK_SIZE);
            ChunkInfo chunkInfo = new ChunkInfo();
            chunkInfo.setIndex(chunk);
            for (int j = 0; j < FILE_COPIES; j++) {
                chunkInfo.getChunkServerIdList().add(availableOrderlyServerRunStates.get(j).getServerId());
            }
            fileInfo.getFileChunkList().add(chunkInfo);
        } finally {
            lockObj.readLock().unlock();
        }
        return fileInfo;
    }

    /**
     * 分配下载文件的服务器信息
     * 
     * @param path
     * @param fileName
     * @return
     */
    public FileInfo assignDownloadFileServer(String path, String fileName) {
        lockObj.readLock().lock();
        if (availableOrderlyServerRunStates.size() < FILE_COPIES)
            return null;
        FileInfo fileInfo = null;
        try {
            // load file save information
            fileInfo= MyFileUtils.JSONReadFileInfo(MyFileUtils.joinFileName(path,fileName));
            // 根据负载排序
            for (ChunkInfo chunkInfo:fileInfo.getFileChunkList()){
                if (chunkInfo==null)
                    continue;
                chunkInfo.getChunkServerIdList().sort(this::compareServerPrior);
            }
            
        } finally {
            lockObj.readLock().unlock();
        }
        return fileInfo;
    }

    /**
     * 分配下载服务器的某个分块的下载信息
     * 
     * @param path
     * @param fileName
     * @param chunk
     * @return
     */
    public FileInfo assignDownloadFileServer(String path, String fileName, int chunk) {
        if (chunk<0)
            return null;
        lockObj.readLock().lock();
        if (availableOrderlyServerRunStates.size() < FILE_COPIES)
            return null;
        FileInfo fileInfo;
        try {
            // load file save information
            fileInfo= MyFileUtils.JSONReadFileInfo(MyFileUtils.joinFileName(path,fileName));
            if (fileInfo==null || fileInfo.getFileChunkList().size()<=chunk)
                return null;
            // 根据负载排序
            if (fileInfo.getFileChunkList().size() > chunk + 1) {
                fileInfo.getFileChunkList().subList(chunk + 1, fileInfo.getFileChunkList().size()).clear();
            }
            ChunkInfo chunkInfo=fileInfo.getFileChunkList().get(chunk);
            if (chunkInfo!=null){
                chunkInfo.getChunkServerIdList().sort(this::compareServerPrior);
            }
            fileInfo.getFileChunkList().subList(0, chunk).clear();
        } finally {
            lockObj.readLock().unlock();
        }
        return fileInfo;
    }
}