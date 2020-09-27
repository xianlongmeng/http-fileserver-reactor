package com.ly.rhdfs.master.manager.runstate;

import com.ly.common.domain.TaskInfo;
import com.ly.common.domain.file.ChunkInfo;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.log.OperationLog;
import com.ly.common.domain.log.ServerFileChunkLog;
import com.ly.common.domain.log.UpdateChunkServer;
import com.ly.common.domain.server.ServerAddressInfo;
import com.ly.common.domain.server.ServerRunState;
import com.ly.common.domain.token.TokenClearServer;
import com.ly.common.domain.token.TokenInfo;
import com.ly.rhdfs.file.util.DfsFileUtils;
import com.ly.rhdfs.file.config.FileInfoManager;
import com.ly.rhdfs.master.manager.MasterManager;
import org.apache.logging.log4j.core.jmx.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Component
public class FileServerRunManager {

    // config file initial
    // 磁盘预留最小空间64G
    private static final long MIN_SPACE_SIZE = 0x1000000000L;
    private static final int MAX_TASK_COUNT = 500;
    private static final int FILE_COPIES = 3;
    // 分块大小，缺省64M

    private final Map<Long, ServerRunState> serverRunStateMap = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lockObj = new ReentrantReadWriteLock();
    private final Map<TokenInfo, TaskInfo> uploadRunningTask = new ConcurrentHashMap<>();
    private final Map<TokenInfo, FileInfo> downloadRunningTask = new ConcurrentHashMap<>();
    // 准备删除的file信息，系统定时扫描此队列，并循环处理，如果server不存在，则丢弃
    private final Queue<TokenClearServer> clearTokenQueue = new ConcurrentLinkedDeque<>();
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private MasterManager masterManager;
    private List<ServerRunState> availableOrderlyServerRunStates;
    private FileInfoManager fileInfoManager;
    private DfsFileUtils dfsFileUtils;

    @Autowired
    public void setMasterManager(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Autowired
    private void setFileInfoManager(FileInfoManager fileInfoManager) {
        this.fileInfoManager = fileInfoManager;
    }

    @Autowired
    private void setDfsFileUtils(DfsFileUtils dfsFileUtils) {
        this.dfsFileUtils = dfsFileUtils;
    }

    public Map<TokenInfo, TaskInfo> getUploadRunningTask() {
        return uploadRunningTask;
    }

    public Map<TokenInfo, FileInfo> getDownloadRunningTask() {
        return downloadRunningTask;
    }

    public Queue<TokenClearServer> getClearTokenQueue() {
        return clearTokenQueue;
    }

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

    public int compareServerPrior(ServerAddressInfo sa1, ServerAddressInfo sa2) {
        // 剩余空间>50G:运行负载、已有文件数、序号
        return compareServerPrior(sa1.getServerId(), sa2.getServerId());
    }

    private int getChunkSize() {
        return masterManager.getServerConfig().getChunkSize();
    }

    /**
     * 分配上传文件的分块服务器信息
     *
     * @param tokenInfo
     * @param fileSize
     * @return
     */
    public FileInfo assignUploadFileServer(TokenInfo tokenInfo, long fileSize) {
        if (tokenInfo == null || tokenInfo.getTokenType() == TokenInfo.TOKEN_READ
                || StringUtils.isEmpty(tokenInfo.getPath()) || StringUtils.isEmpty(tokenInfo.getFileName())
                || fileSize <= 0)
            return null;
        if (uploadRunningTask.containsKey(tokenInfo)) {
            logger.error("this file is running upload!");
            return null;
        }
        lockObj.readLock().lock();
        if (availableOrderlyServerRunStates.size() < FILE_COPIES) {
            logger.warn("Available server not enough!");
            return null;
        }
        if (dfsFileUtils.fileExist(tokenInfo.getPath(), tokenInfo.getFileName())) {
            logger.warn("File existed!path:{},file name:{}", tokenInfo.getPath(), tokenInfo.getFileName());
            return null;
        }
        FileInfo fileInfo = new FileInfo();
        fileInfo.setPath(tokenInfo.getPath());
        fileInfo.setFileName(tokenInfo.getFileName());
        fileInfo.setCreateTime(Instant.now().toEpochMilli());
        fileInfo.setLastModifyTime(fileInfo.getCreateTime());
        fileInfo.setChunk(true);
        fileInfo.setSize(fileSize);
        try {
            int chunkCount = (int) ((fileSize + getChunkSize() - 1) / getChunkSize());
            fileInfo.setChunkCount(chunkCount);
            fileInfo.setChunkSize(getChunkSize());
            for (int i = 0; i < chunkCount; i++) {
                ChunkInfo chunkInfo = new ChunkInfo();
                chunkInfo.setIndex(i);
                for (int j = 0; j < FILE_COPIES; j++) {
                    ServerRunState serverRunState = availableOrderlyServerRunStates
                            .get((chunkCount * j + i) % availableOrderlyServerRunStates.size());
                    chunkInfo.getChunkServerIdList().add(new ServerAddressInfo(serverRunState.getServerState()));
                    serverRunState.uploadPlus();
                }
                fileInfo.getFileChunkList().add(chunkInfo);
                dfsFileUtils.JSONWriteFile(
                        dfsFileUtils.joinFileTempConfigName(tokenInfo.getPath(), tokenInfo.getFileName()), fileInfo);
            }
        } finally {
            lockObj.readLock().unlock();
        }
        uploadRunningTask.put(tokenInfo, new TaskInfo(tokenInfo, fileInfo));
        masterManager.getLogFileOperate().writeOperateLog(new OperationLog(Instant.now().toEpochMilli(),
                OperationLog.OP_TYPE_ADD_FILE_INIT, tokenInfo.getPath(), tokenInfo.getFileName()));
        // 上传分配后，重置ServerRunState的排序
        resetAvailableOrderlyServerRunStates();
        return fileInfo;
    }

    /**
     * 分配上传服务器的某个分块信息
     *
     * @param tokenInfo
     * @param chunk
     * @return
     */
    public FileInfo assignUploadFileServerChunk(TokenInfo tokenInfo, int chunk) {
        if (tokenInfo == null || tokenInfo.getTokenType() == TokenInfo.TOKEN_READ
                || StringUtils.isEmpty(tokenInfo.getPath()) || StringUtils.isEmpty(tokenInfo.getFileName()))
            return null;
        lockObj.readLock().lock();
        if (availableOrderlyServerRunStates.size() < FILE_COPIES)
            return null;
        TaskInfo taskInfo = uploadRunningTask.get(tokenInfo);
        if (taskInfo == null) {
            logger.error("Do not find upload file information.");
            return null;
        }
        FileInfo fileInfo = taskInfo.getFileInfo();
        if (fileInfo == null || fileInfo.getFileChunkList() == null || fileInfo.getFileChunkList().size() <= chunk) {
            logger.error("Do not find upload file information.");
            return null;
        }
        try {
            ChunkInfo chunkInfo = new ChunkInfo();
            chunkInfo.setIndex(chunk);
            ChunkInfo oldChunkInfo = fileInfo.getFileChunkList().get(chunk);
            if (availableOrderlyServerRunStates.size() >= FILE_COPIES * 2) {
                int index = 0;
                for (int j = 0; j < FILE_COPIES; j++) {
                    ServerRunState serverRunState = availableOrderlyServerRunStates.get(index);
                    while (oldChunkInfo.getChunkServerIdList().contains(serverRunState.getServerId())) {
                        index++;
                        serverRunState = availableOrderlyServerRunStates.get(index);
                    }
                    chunkInfo.getChunkServerIdList().add(new ServerAddressInfo(serverRunState.getServerState()));
                    serverRunState.uploadPlus();
                }
            } else {
                for (int j = 0; j < FILE_COPIES; j++) {
                    ServerRunState serverRunState = availableOrderlyServerRunStates.get(j);
                    chunkInfo.getChunkServerIdList().add(new ServerAddressInfo(serverRunState.getServerState()));
                    serverRunState.uploadPlus();
                }
            }
            if (oldChunkInfo != null && oldChunkInfo.getChunkServerIdList() != null) {
                // 清除之前分配Server的负载
                for (ServerAddressInfo serverAddressInfo : oldChunkInfo.getChunkServerIdList()) {
                    ServerRunState oldServerRunState = serverRunStateMap.get(serverAddressInfo.getServerId());
                    if (oldServerRunState != null) {
                        oldServerRunState.uploadSub();
                    }
                }
            }
            fileInfo.getFileChunkList().set(chunk, chunkInfo);
        } finally {
            lockObj.readLock().unlock();
        }
        dfsFileUtils.JSONWriteFile(dfsFileUtils.joinFileTempConfigName(tokenInfo.getPath(), tokenInfo.getFileName()),
                fileInfo);
        masterManager.getLogFileOperate().writeOperateLog(new OperationLog(Instant.now().toEpochMilli(),
                OperationLog.OP_TYPE_ADD_FILE_INIT_UPDATE, tokenInfo.getPath(), tokenInfo.getFileName()));
        return fileInfo;
    }

    public UpdateChunkServer assignUpdateFileChunkServer(long serverId, ServerFileChunkLog serverFileChunkLog) {
        if (serverFileChunkLog == null || StringUtils.isEmpty(serverFileChunkLog.getFileName()))
            return null;
        lockObj.readLock().lock();
        if (availableOrderlyServerRunStates.size() < FILE_COPIES)
            return null;
        FileInfo fileInfo = fileInfoManager.findFileInfo(serverFileChunkLog.getPath(), serverFileChunkLog.getFileName());
        if (fileInfo == null || fileInfo.getFileChunkList() == null
                || fileInfo.getFileChunkList().size() <= serverFileChunkLog.getChunk()) {
            logger.error("Do not find upload file information.");
            return null;
        }
        ServerRunState serverRunState;
        try {
            ChunkInfo chunkInfo = fileInfo.getFileChunkList().get(serverFileChunkLog.getChunk());
            int index = 0;
            serverRunState = availableOrderlyServerRunStates.get(index);
            while (serverId == serverRunState.getServerId()
                    || chunkInfo.getChunkServerIdList().contains(new ServerAddressInfo(serverRunState.getServerId()))
                    || availableOrderlyServerRunStates.size() <= index) {
                index++;
                serverRunState = availableOrderlyServerRunStates.get(index);
            }
            chunkInfo.getChunkServerIdList().add(new ServerAddressInfo(serverRunState.getServerState()));
            serverRunState.uploadPlus();
        } finally {
            lockObj.readLock().unlock();
        }
        return new UpdateChunkServer(fileInfo, serverFileChunkLog.getChunk(), serverId, serverRunState.getServerId());
    }

    public UpdateChunkServer takeUpdateFileChunkServer(long serverId,long newServerId, ServerFileChunkLog serverFileChunkLog) {
        if (serverFileChunkLog == null || StringUtils.isEmpty(serverFileChunkLog.getFileName()))
            return null;
        FileInfo fileInfo = fileInfoManager.findFileInfo(serverFileChunkLog.getPath(), serverFileChunkLog.getFileName());
        if (fileInfo == null || fileInfo.getFileChunkList() == null
                || fileInfo.getFileChunkList().size() <= serverFileChunkLog.getChunk()
                || fileInfo.getFileChunkList().get(serverFileChunkLog.getChunk()).getChunkServerIdList().size() < 2) {
            logger.error("Do not find upload file information.");
            return null;
        }
        ServerRunState serverRunState =  serverRunStateMap.get(newServerId);
        if (serverRunState == null || !serverRunState.isOnline()) {
            logger.error("No backup.");
            return null;
        }
        serverRunState.uploadPlus();
        return new UpdateChunkServer(fileInfo, serverFileChunkLog.getChunk(), serverId, newServerId);
    }

    public void clearUpdateChunkServer(UpdateChunkServer updateChunkServer) {
        if (updateChunkServer == null)
            return;
        ServerRunState serverRunState = serverRunStateMap.get(updateChunkServer.getNewServerId());
        if (serverRunState != null)
            serverRunState.uploadSub();
        ServerRunState sourceServerRunState = serverRunStateMap.get(updateChunkServer.getSourceServerId());
        if (sourceServerRunState != null)
            sourceServerRunState.uploadSub();
    }

    public void resetUpdateChunkServerSource(UpdateChunkServer updateChunkServer) {
        if (updateChunkServer == null || updateChunkServer.getFileInfo() == null
                || updateChunkServer.getFileInfo().getFileChunkList().size() <= updateChunkServer.getChunk()
                || updateChunkServer.getFileInfo().getFileChunkList().get(updateChunkServer.getChunk())
                .getChunkServerIdList() == null)
            return;
        int sourceIndex = updateChunkServer.getSourceIndex();
        if (sourceIndex < 0)
            sourceIndex = 0;
        List<ServerAddressInfo> serverList = updateChunkServer.getFileInfo().getFileChunkList().get(updateChunkServer.getChunk())
                .getChunkServerIdList();
        for (int i = 0; i < serverList.size(); i++) {
            int index = (sourceIndex + i) % serverList.size();
            long serverId = serverList.get(index).getServerId();
            if (serverId != updateChunkServer.getNewServerId() && serverId != updateChunkServer.getSourceServerId()) {
                ServerRunState oldServerRunState = serverRunStateMap.get(updateChunkServer.getSourceServerId());
                if (oldServerRunState != null)
                    oldServerRunState.uploadSub();
                updateChunkServer.setSourceIndex(index);
                updateChunkServer.setSourceServerId(serverId);
                ServerRunState newServerRunState = serverRunStateMap.get(updateChunkServer.getSourceServerId());
                if (newServerRunState != null)
                    newServerRunState.uploadPlus();
                return;
            }
        }
    }

    /**
     * 上传任务完成
     *
     * @param tokenInfo
     */
    public void uploadFileFinish(TokenInfo tokenInfo,String etag) {
        if (tokenInfo == null || tokenInfo.getTokenType() == TokenInfo.TOKEN_READ
                || StringUtils.isEmpty(tokenInfo.getPath()) || StringUtils.isEmpty(tokenInfo.getFileName()))
            return;

        TaskInfo taskInfo = uploadRunningTask.get(tokenInfo);
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            logger.warn("this file is not running upload!");
            return;
        }
        FileInfo fileInfo = taskInfo.getFileInfo();
        if (fileInfo==null)
            return;
        fileInfo.setEtag(etag);
        uploadRunningTask.remove(tokenInfo);
        for (int i = 0; i < fileInfo.getFileChunkList().size(); i++) {
            ChunkInfo chunkInfo = fileInfo.getFileChunkList().get(i);
            for (ServerAddressInfo serverAddressInfo : chunkInfo.getChunkServerIdList()) {
                ServerRunState serverRunState = serverRunStateMap.get(serverAddressInfo.getServerId());
                if (serverRunState != null) {
                    serverRunState.uploadSub();
                }
                //记录当前服务器chunk增加日志
                masterManager.getServerFileChunkUtil().writeAddServerFileChunk(serverAddressInfo.getServerId(),tokenInfo.getPath(),tokenInfo.getFileName(),chunkInfo.getIndex());
            }
        }
        for (ServerAddressInfo serverAddressInfo : tidyServerId(fileInfo)) {
            clearTokenQueue.add(new TokenClearServer(TokenClearServer.TC_TYPE_TOKEN_CLEAR, tokenInfo, serverAddressInfo.getServerId()));
        }
        fileInfoManager.submitFileInfo(fileInfo,
                dfsFileUtils.joinFileTempConfigName(tokenInfo.getPath(), tokenInfo.getFileName()));
        long writeLogDateTime = Instant.now().toEpochMilli();
        masterManager.getLogFileOperate().writeOperateLog(new OperationLog(writeLogDateTime,
                OperationLog.OP_TYPE_ADD_FILE_FINISH, tokenInfo.getPath(), tokenInfo.getFileName()));
        masterManager.getLocalServerState().setWriteLastTime(writeLogDateTime);
        // 上传完成后，重置ServerRunState的排序
        resetAvailableOrderlyServerRunStates();
    }

    /**
     * 清除上传任务，超过token使用时间的上传任务，仍未上传完成，则放弃
     *
     * @param tokenInfo
     * @param resetOrder
     */
    public void clearUploadFile(TokenInfo tokenInfo, boolean resetOrder) {
        if (tokenInfo == null || tokenInfo.getTokenType() == TokenInfo.TOKEN_READ
                || StringUtils.isEmpty(tokenInfo.getPath()) || StringUtils.isEmpty(tokenInfo.getFileName()))
            return;
        Set<Long> serverIdSet = new TreeSet<>();
        TaskInfo taskInfo = uploadRunningTask.get(tokenInfo);
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            logger.warn("this file is not running upload!");
            return;
        }
        FileInfo fileInfo = taskInfo.getFileInfo();
        for (int i = 0; i < fileInfo.getFileChunkList().size(); i++) {
            ChunkInfo chunkInfo = fileInfo.getFileChunkList().get(i);
            for (ServerAddressInfo serverAddressInfo : chunkInfo.getChunkServerIdList()) {
                ServerRunState serverRunState = serverRunStateMap.get(serverAddressInfo.getServerId());
                if (serverRunState != null) {
                    serverRunState.uploadSub();
                }
                // send file delete command
                serverIdSet.add(serverAddressInfo.getServerId());
            }
        }
        for (long serverId : serverIdSet) {
            clearTokenQueue.add(new TokenClearServer(TokenClearServer.TC_TYPE_FILE_DELETE, tokenInfo, serverId));
        }
        // 删除当前配置文件
        dfsFileUtils.fileDelete(tokenInfo.getPath(), tokenInfo.getFileName());
        logger.warn("delete upload file,path[{}] file name[{}]", tokenInfo.getPath(), tokenInfo.getFileName());
        uploadRunningTask.remove(tokenInfo);
        // 上传完成后，重置ServerRunState的排序
        if (resetOrder)
            resetAvailableOrderlyServerRunStates();
        masterManager.getLogFileOperate().writeOperateLog(new OperationLog(Instant.now().toEpochMilli(),
                OperationLog.OP_TYPE_ADD_FILE_FAIL, tokenInfo.getPath(), tokenInfo.getFileName()));
    }
    public boolean deleteFile(TokenInfo tokenInfo) {
        if (tokenInfo==null || StringUtils.isEmpty(tokenInfo.getFileName()))
            return false;
        FileInfo fileInfo = fileInfoManager.findFileInfo(tokenInfo.getPath(),tokenInfo.getFileName());
        if (fileInfo==null)
            return false;
        Set<Long> serverIdSet = new TreeSet<>();
        for (int i = 0; i < fileInfo.getFileChunkList().size(); i++) {
            ChunkInfo chunkInfo = fileInfo.getFileChunkList().get(i);
            for (ServerAddressInfo serverAddressInfo : chunkInfo.getChunkServerIdList()) {
                masterManager.getServerFileChunkUtil().writeDeleteServerFileChunk(serverAddressInfo.getServerId(),tokenInfo.getPath(),tokenInfo.getFileName(),chunkInfo.getIndex());
                serverIdSet.add(serverAddressInfo.getServerId());
            }
        }
        for (long serverId : serverIdSet) {
            clearTokenQueue.add(new TokenClearServer(TokenClearServer.TC_TYPE_FILE_DELETE, tokenInfo, serverId));
        }
        // 删除当前配置文件
        boolean res=fileInfoManager.deleteFile(tokenInfo.getPath(),tokenInfo.getFileName());
        logger.warn("delete upload file,path[{}] file name[{}]", tokenInfo.getPath(),tokenInfo.getFileName());
        // 上传完成后，重置ServerRunState的排序
        masterManager.getLogFileOperate().writeOperateLog(new OperationLog(Instant.now().toEpochMilli(),
                OperationLog.OP_TYPE_DELETE_FILE, tokenInfo.getPath(),tokenInfo.getFileName()));
        return res;
    }
    /**
     * 分配下载文件的服务器信息
     *
     * @param tokenInfo
     * @return
     */
    public FileInfo assignDownloadFileServer(TokenInfo tokenInfo) {
        if (tokenInfo == null || tokenInfo.getTokenType() != TokenInfo.TOKEN_READ
                || StringUtils.isEmpty(tokenInfo.getPath()) || StringUtils.isEmpty(tokenInfo.getFileName()))
            return null;
        lockObj.readLock().lock();
        if (availableOrderlyServerRunStates.size() < FILE_COPIES)
            return null;
        FileInfo fileInfo;
        try {
            // load file save information
            fileInfo = fileInfoManager
                    .findFileInfo(tokenInfo.getPath(), tokenInfo.getFileName());
            if (fileInfo == null || fileInfo.getFileChunkList() == null || fileInfo.getFileChunkList().isEmpty()) {
                logger.error("File config information is error.");
                return null;
            }
            // 根据负载排序
            for (ChunkInfo chunkInfo : fileInfo.getFileChunkList()) {
                if (chunkInfo == null || chunkInfo.getChunkServerIdList() == null
                        || chunkInfo.getChunkServerIdList().isEmpty())
                    continue;
                chunkInfo.getChunkServerIdList().sort(this::compareServerPrior);
                ServerRunState serverRunState = serverRunStateMap.get(chunkInfo.getChunkServerIdList().get(0).getServerId());
                if (serverRunState != null) {
                    serverRunState.downloadPlus();
                }
            }
            downloadRunningTask.put(tokenInfo, fileInfo);
        } finally {
            lockObj.readLock().unlock();
        }
        // 下载分配后，重置ServerRunState的排序
        resetAvailableOrderlyServerRunStates();
        return fileInfo;
    }

    /**
     * 分配下载服务器的某个分块的下载信息
     *
     * @param tokenInfo
     * @param chunk
     * @return
     */
    public FileInfo assignDownloadFileServer(TokenInfo tokenInfo, int chunk) {
        if (tokenInfo == null || tokenInfo.getTokenType() != TokenInfo.TOKEN_READ
                || StringUtils.isEmpty(tokenInfo.getPath()) || StringUtils.isEmpty(tokenInfo.getFileName()))
            return null;
        if (chunk < 0)
            return null;
        lockObj.readLock().lock();
        if (availableOrderlyServerRunStates.size() < FILE_COPIES)
            return null;
        FileInfo fileInfo;
        try {
            // load file save information
            fileInfo = downloadRunningTask.get(tokenInfo);
            if (fileInfo == null || fileInfo.getFileChunkList().size() <= chunk)
                return null;
            // 根据负载排序
            if (fileInfo.getFileChunkList().size() > chunk + 1) {
                fileInfo.getFileChunkList().subList(chunk + 1, fileInfo.getFileChunkList().size()).clear();
            }
            ChunkInfo chunkInfo = fileInfo.getFileChunkList().get(chunk);
            if (chunkInfo != null && chunkInfo.getChunkServerIdList() != null
                    && !chunkInfo.getChunkServerIdList().isEmpty()) {
                chunkInfo.getChunkServerIdList().sort(this::compareServerPrior);
                // ServerRunState serverRunState=serverRunStateMap.get(chunkInfo.getChunkServerIdList().get(0));
                // if (serverRunState!=null){
                // serverRunState.downloadPlus();
                // }
            }
            fileInfo.getFileChunkList().subList(0, chunk).clear();
        } finally {
            lockObj.readLock().unlock();
        }
        return fileInfo;
    }

    /**
     * 下载任务完成
     *
     * @param tokenInfo
     */
    public void downloadFileFinish(TokenInfo tokenInfo) {
        if (tokenInfo == null || tokenInfo.getTokenType() != TokenInfo.TOKEN_READ
                || StringUtils.isEmpty(tokenInfo.getToken()))
            return;
        FileInfo fileInfo = downloadRunningTask.get(tokenInfo);
        if (fileInfo == null || fileInfo.getFileChunkList() == null) {
            logger.warn("this file is not find running download!");
            return;
        }
        for (ChunkInfo chunkInfo : fileInfo.getFileChunkList()) {
            if (chunkInfo == null || chunkInfo.getChunkServerIdList() == null
                    || chunkInfo.getChunkServerIdList().isEmpty())
                continue;
            ServerRunState serverRunState = serverRunStateMap.get(chunkInfo.getChunkServerIdList().get(0).getServerId());
            if (serverRunState != null) {
                serverRunState.downloadSub();
            }
        }
        for (ServerAddressInfo serverAddressInfo : tidyServerId(fileInfo)) {
            clearTokenQueue.add(new TokenClearServer(TokenClearServer.TC_TYPE_TOKEN_CLEAR, tokenInfo, serverAddressInfo.getServerId()));
        }
        downloadRunningTask.remove(tokenInfo);
        // 上传完成后，重置ServerRunState的排序
        resetAvailableOrderlyServerRunStates();
    }

    /**
     * 清除下载任务，超过token使用时间的上传任务，仍未下载完成，则删除任务
     *
     * @param tokenInfo
     * @param resetOrder
     */
    public void clearDownloadFile(TokenInfo tokenInfo, boolean resetOrder) {
        if (tokenInfo == null || tokenInfo.getTokenType() == TokenInfo.TOKEN_READ
                || StringUtils.isEmpty(tokenInfo.getPath()) || StringUtils.isEmpty(tokenInfo.getFileName()))
            return;
        FileInfo fileInfo = downloadRunningTask.get(tokenInfo);
        if (fileInfo == null) {
            logger.warn("this file is not running download!");
            return;
        }
        for (int i = 0; i < fileInfo.getFileChunkList().size(); i++) {
            ChunkInfo chunkInfo = fileInfo.getFileChunkList().get(i);
            for (ServerAddressInfo serverAddressInfo : chunkInfo.getChunkServerIdList()) {
                ServerRunState serverRunState = serverRunStateMap.get(serverAddressInfo.getServerId());
                if (serverRunState != null) {
                    serverRunState.downloadSub();
                }
            }
        }
        for (ServerAddressInfo serverAddressInfo : tidyServerId(fileInfo)) {
            clearTokenQueue.add(new TokenClearServer(TokenClearServer.TC_TYPE_TOKEN_CLEAR, tokenInfo, serverAddressInfo.getServerId()));
        }
        logger.warn("delete download task,path[{}] file name[{}]", tokenInfo.getPath(), tokenInfo.getFileName());
        uploadRunningTask.remove(tokenInfo);
        // 上传完成后，重置ServerRunState的排序
        if (resetOrder)
            resetAvailableOrderlyServerRunStates();
    }

    public Set<ServerAddressInfo> tidyServerId(FileInfo fileInfo) {
        Set<ServerAddressInfo> res = new TreeSet<>();
        if (fileInfo == null)
            return res;
        for (ChunkInfo chunkInfo : fileInfo.getFileChunkList()) {
            if (chunkInfo == null)
                continue;
            for (ServerAddressInfo serverAddressInfo : chunkInfo.getChunkServerIdList()) {
                if (!res.contains(serverAddressInfo))
                    res.add(serverAddressInfo);
            }
        }
        return res;
    }

    public boolean resetInvalidServerId(FileInfo fileInfo, Set<ServerAddressInfo> validServerIds) {
        if (fileInfo == null || validServerIds == null || validServerIds.size() < FILE_COPIES)
            return false;
        int validIndex = 0;
        ServerAddressInfo[] vsIds = (ServerAddressInfo[]) validServerIds.toArray();
        for (ChunkInfo chunkInfo : fileInfo.getFileChunkList()) {
            if (chunkInfo == null)
                continue;
            for (int i = 0; i < chunkInfo.getChunkServerIdList().size(); i++) {
                ServerAddressInfo serverAddressInfo = chunkInfo.getChunkServerIdList().get(i);
                if (!validServerIds.contains(serverAddressInfo)) {
                    chunkInfo.getChunkServerIdList().set(i, vsIds[validIndex]);
                    validIndex = (++validIndex) % vsIds.length;
                }
            }
        }
        dfsFileUtils.JSONWriteFile(dfsFileUtils.joinFileTempConfigName(fileInfo.getPath(), fileInfo.getFileName()),
                fileInfo);
        masterManager.getLogFileOperate().writeOperateLog(new OperationLog(Instant.now().toEpochMilli(),
                OperationLog.OP_TYPE_ADD_FILE_INIT_UPDATE, fileInfo.getPath(), fileInfo.getFileName()));
        return true;
    }
}