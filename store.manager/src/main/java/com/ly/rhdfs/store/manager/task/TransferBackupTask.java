package com.ly.rhdfs.store.manager.task;

import java.time.Instant;

import org.springframework.core.io.buffer.DataBuffer;

import com.ly.common.domain.file.DFSBackupStoreFileChunkInfo;
import com.ly.common.service.FileChunkReader;
import com.ly.rhdfs.store.manager.StoreManager;

import reactor.core.publisher.Flux;

public class TransferBackupTask implements Runnable {

    private final StoreManager storeManager;
    private final DFSBackupStoreFileChunkInfo dfsBackupStoreFileChunkInfo;
    private final long serverId;

    public TransferBackupTask(StoreManager storeManager, DFSBackupStoreFileChunkInfo dfsBackupStoreFileChunkInfo,
            long serverId) {
        this.storeManager = storeManager;
        this.dfsBackupStoreFileChunkInfo = dfsBackupStoreFileChunkInfo;
        this.serverId = serverId;
    }

    @Override
    public void run() {
        if (dfsBackupStoreFileChunkInfo == null)
            return;
        if (storeManager.findServerState(serverId) == null
                || (dfsBackupStoreFileChunkInfo.getTimes() > dfsBackupStoreFileChunkInfo.getMaxTimes()
                        && dfsBackupStoreFileChunkInfo.getMaxTimes() > 0)
                || (dfsBackupStoreFileChunkInfo.getExpire() > 0 && dfsBackupStoreFileChunkInfo.getCreateTime()
                        + dfsBackupStoreFileChunkInfo.getExpire() < Instant.now().toEpochMilli())) {
            // send backup failed,wait master move server store file
            return;
        }
        storeManager.sendBackupStoreFile(serverId, dfsBackupStoreFileChunkInfo);
    }
}
