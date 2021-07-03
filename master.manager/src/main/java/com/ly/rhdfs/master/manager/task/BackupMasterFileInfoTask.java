package com.ly.rhdfs.master.manager.task;

import com.ly.common.domain.file.BackupMasterFileInfo;
import com.ly.common.domain.token.TokenInfo;
import com.ly.rhdfs.master.manager.MasterManager;

import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

public class BackupMasterFileInfoTask implements Runnable {

    private int waitTime = 100;
    private long maxDeleteTime = 3600000;
    private MasterManager masterManager;

    public BackupMasterFileInfoTask(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Override
    public void run() {
        for (Map.Entry<Long, BlockingDeque<BackupMasterFileInfo>> entry : masterManager.getBackupMasterFileInfoBlockingQueue().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null)
                continue;
            while (!entry.getValue().isEmpty()) {
                BackupMasterFileInfo backupMasterFileInfo = entry.getValue().pollFirst();
                if (backupMasterFileInfo == null)
                    continue;
                if (backupMasterFileInfo.getType() == BackupMasterFileInfo.TYPE_ADD) {
                    if (!masterManager.sendFileInfoSync(entry.getKey(), backupMasterFileInfo.getFileInfo())) {
                        entry.getValue().addFirst(backupMasterFileInfo);
                        break;
                    }
                } else if (backupMasterFileInfo.getType() == BackupMasterFileInfo.TYPE_DELETE) {
                    if (!masterManager.sendFileDeleteSync(entry.getKey(), new TokenInfo(backupMasterFileInfo.getFileInfo().getPath(), backupMasterFileInfo.getFileInfo().getFileName()))) {
                        entry.getValue().addFirst(backupMasterFileInfo);
                        break;
                    }
                }
            }
        }
        masterManager.getScheduledThreadPoolExecutor().schedule(this, waitTime, TimeUnit.SECONDS);
    }
}
