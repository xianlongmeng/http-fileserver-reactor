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

    public TransferBackupTask(StoreManager storeManager, DFSBackupStoreFileChunkInfo dfsBackupStoreFileChunkInfo) {
        this.storeManager = storeManager;
        this.dfsBackupStoreFileChunkInfo = dfsBackupStoreFileChunkInfo;
    }

    @Override
    public void run() {
        if (dfsBackupStoreFileChunkInfo == null)
            return;
        if (dfsBackupStoreFileChunkInfo.getTimes() > dfsBackupStoreFileChunkInfo.getMaxTimes()
                || (dfsBackupStoreFileChunkInfo.getExpire() > 0 && dfsBackupStoreFileChunkInfo.getCreateTime()
                        + dfsBackupStoreFileChunkInfo.getExpire() < Instant.now().toEpochMilli())) {
            // todo:
            // send backup failed message to master
            return;
        }
        Flux<DataBuffer> dataBufferFlux = FileChunkReader.readFile2Buffer(
                dfsBackupStoreFileChunkInfo.getDfsPartChunk().getFileFullName(),
                dfsBackupStoreFileChunkInfo.getDfsPartChunk().getChunk()
                        * dfsBackupStoreFileChunkInfo.getDfsPartChunk().getChunkSize(),
                dfsBackupStoreFileChunkInfo.getDfsPartChunk().getContentLength());
        storeManager.sendBackupStoreFile(dataBufferFlux, dfsBackupStoreFileChunkInfo);
    }
}
