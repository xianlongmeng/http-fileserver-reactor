package com.ly.rhdfs.master.manager.task;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.file.FileChunkCopy;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.log.ServerFileChunkLog;
import com.ly.common.domain.log.UpdateChunkServer;
import com.ly.common.domain.server.ServerAddressInfo;
import com.ly.rhdfs.log.server.file.ServerFileChunkReader;
import com.ly.rhdfs.master.manager.MasterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.util.function.Tuple2;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class RecoverStoreServerTask implements Runnable {
    private final BlockingQueue<Long> waitBackupServerId = new LinkedBlockingDeque<>();
    private final BlockingQueue<Tuple2<Long, Long>> waitRecoverServerId = new LinkedBlockingDeque<>();
    private final BlockingQueue<ServerFileChunkLog> failedChunkQueue = new LinkedBlockingDeque<>();
    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    private final MasterManager masterManager;
    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(8, 8, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>());
    private Logger logger = LoggerFactory.getLogger(getClass());
    private int longWaitTime = 300;
    private int shortWaitTime = 30;
    private long curServerId = -1;

    public RecoverStoreServerTask(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    private synchronized void lock(String path) {
        ReentrantLock reentrantLock = lockMap.get(path);
        if (reentrantLock == null) {
            reentrantLock = new ReentrantLock();
        }
        reentrantLock.lock();
        lockMap.put(path, reentrantLock);
    }

    private synchronized void unlock(String path) {
        ReentrantLock reentrantLock = lockMap.get(path);
        if (reentrantLock == null) {
            return;
        }
        lockMap.remove(path);
        reentrantLock.unlock();
    }

    public void addBackupServerId(long serverId) {
        waitBackupServerId.add(serverId);
    }

    public void addRecoverServerId(Tuple2<Long,Long> serverIds) {
        waitRecoverServerId.add(serverIds);
    }

    public void removeBackupServerId(long serverId) {
        waitBackupServerId.remove(serverId);
    }

    public void removeRecoverServerId(Tuple2<Long,Long> serverIds) {
        waitRecoverServerId.remove(serverIds);
    }

    @Override
    public void run() {
        if (curServerId != -1 && executorService.getActiveCount() == 0) {
            // clear ServerID,通知backup master
            masterManager.getServerFileChunkUtil().deleteServerFileChunkLog(curServerId);
            masterManager.removeServerState(curServerId);
            masterManager.saveMasterServerConfig();
            curServerId = -1;
        }
        if (waitBackupServerId.isEmpty() && waitRecoverServerId.isEmpty()) {
            masterManager.getScheduledThreadPoolExecutor().schedule(this, longWaitTime, TimeUnit.SECONDS);
        } else {
            if (executorService.getActiveCount() == 0) {
                Long sid = waitBackupServerId.poll();
                if (sid != null) {
                    curServerId = sid;
                    ServerFileChunkReader serverFileChunkReader = new ServerFileChunkReader(
                            masterManager.getServerConfig().getServerFileLogPath(), curServerId);
                    if (serverFileChunkReader.openFile()) {
                        for (int i = 0; i < executorService.getCorePoolSize(); i++) {
                            executorService.submit(new RecoverStoreServerFileTask(serverFileChunkReader, RecoverStoreServerFileTask.RSSF_TYPE_BACKUP));
                        }
                    }
                } else {
                    Tuple2<Long, Long> recoverServerId = waitRecoverServerId.poll();
                    if (recoverServerId != null) {
                        curServerId = recoverServerId.getT1();
                        ServerFileChunkReader serverFileChunkReader = new ServerFileChunkReader(
                                masterManager.getServerConfig().getServerFileLogPath(), curServerId);
                        if (serverFileChunkReader.openFile()) {
                            for (int i = 0; i < executorService.getCorePoolSize(); i++) {
                                executorService.submit(new RecoverStoreServerFileTask(serverFileChunkReader, RecoverStoreServerFileTask.RSSF_TYPE_RECOVER));
                            }
                        }
                    }
                }
            }
            masterManager.getScheduledThreadPoolExecutor().schedule(this, shortWaitTime, TimeUnit.SECONDS);
        }
    }

    private class RecoverStoreServerFileTask implements Runnable {

        public static final int RSSF_TYPE_BACKUP = 1;
        public static final int RSSF_TYPE_RECOVER = 2;
        private ServerFileChunkReader serverFileChunkReader;
        private int type;
        private long recoverServerId;

        public RecoverStoreServerFileTask(ServerFileChunkReader serverFileChunkReader) {
            this(serverFileChunkReader, RSSF_TYPE_BACKUP);
        }

        public RecoverStoreServerFileTask(ServerFileChunkReader serverFileChunkReader, int type) {
            this(serverFileChunkReader, type, -1);
        }

        public RecoverStoreServerFileTask(ServerFileChunkReader serverFileChunkReader, int type, long recoverServerId) {
            this.serverFileChunkReader = serverFileChunkReader;
            this.type = type;
            this.recoverServerId = recoverServerId;
        }


        @Override
        public void run() {
            boolean readFlag = true;
            while (true) {
                ServerFileChunkLog serverFileChunkLog = null;
                if (readFlag)
                    serverFileChunkLog = serverFileChunkReader.readNext();
                if (serverFileChunkLog == null) {
                    readFlag = false;
                    serverFileChunkLog = failedChunkQueue.poll();
                }
                if (serverFileChunkLog == null)
                    break;
                UpdateChunkServer updateChunkServer;
                if (type == RSSF_TYPE_BACKUP) {
                    updateChunkServer = masterManager.getFileServerRunManager()
                            .assignUpdateFileChunkServer(serverFileChunkReader.getServerId(), serverFileChunkLog);
                } else if (type == RSSF_TYPE_RECOVER && recoverServerId != -1) {
                    updateChunkServer = masterManager.getFileServerRunManager().takeUpdateFileChunkServer(serverFileChunkReader.getServerId(), recoverServerId, serverFileChunkLog);
                } else {
                    break;
                }
                if (updateChunkServer == null) {
                    continue;
                }
                FileChunkCopy fileChunkCopy = new FileChunkCopy(serverFileChunkLog.getPath(),
                        serverFileChunkLog.getFileName(), serverFileChunkLog.getChunk(),
                        updateChunkServer.getFileInfo().getChunkSize(), updateChunkServer.getFileInfo().getChunkCount(),
                        updateChunkServer.getFileInfo().getSize(), updateChunkServer.getNewServerId());
                masterManager.getFileServerRunManager().resetUpdateChunkServerSource(updateChunkServer);
                CompletableFuture<Integer> completableFuture = masterManager.sendFileChunkCopyAsyncReply(
                        updateChunkServer.getSourceServerId(), fileChunkCopy, 300, TimeUnit.SECONDS);
                ServerFileChunkLog finalServerFileChunkLog = serverFileChunkLog;
                completableFuture.exceptionally(t -> ResultInfo.S_ERROR).whenCompleteAsync((result, t) -> {
                    if (result == ResultInfo.S_OK) {
                        // success
                        String fileName = masterManager.getDfsFileUtils()
                                .joinFileName(finalServerFileChunkLog.getPath(), finalServerFileChunkLog.getFileName());
                        lock(fileName);
                        FileInfo fileInfo = masterManager.getFileInfoManager().findFileInfo(fileName);
                        if (fileInfo != null) {
                            fileInfo.getFileChunkList().get(finalServerFileChunkLog.getChunk()).getChunkServerIdList()
                                    .remove(new ServerAddressInfo(updateChunkServer.getOldServerId()));
                            fileInfo.getFileChunkList().get(finalServerFileChunkLog.getChunk()).getChunkServerIdList()
                                    .add(new ServerAddressInfo(updateChunkServer.getNewServerId()));
                            masterManager.getFileInfoManager().submitFileInfo(fileInfo);
                        }
                        unlock(fileName);
                    } else {
                        // failed
                        failedChunkQueue.add(finalServerFileChunkLog);
                    }
                    finalServerFileChunkLog.notify();
                });
                while (!completableFuture.isDone()) {
                    try {
                        serverFileChunkLog.wait(100);
                    } catch (InterruptedException e) {
                        logger.error(e.getLocalizedMessage(),e);
                    }
                }
                masterManager.getFileServerRunManager().clearUpdateChunkServer(updateChunkServer);
            }
        }
    }
}
