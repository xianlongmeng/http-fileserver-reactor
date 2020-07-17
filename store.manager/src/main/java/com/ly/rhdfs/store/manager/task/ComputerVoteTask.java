package com.ly.rhdfs.store.manager.task;

import java.util.concurrent.TimeUnit;

import com.ly.rhdfs.store.manager.StoreManager;

public class ComputerVoteTask implements Runnable {

    private int longWaitTime = 300;
    private int shortWaitTime = 30;
    private StoreManager storeManager;

    public ComputerVoteTask(StoreManager storeManager) {
        this.storeManager = storeManager;
    }

    @Override
    public void run() {
        storeManager.computerVoteMaster();
        if (storeManager.getMasterServerId() == -1) {
            storeManager.getScheduledThreadPoolExecutor().schedule(this, shortWaitTime, TimeUnit.SECONDS);
        } else {
            storeManager.getScheduledThreadPoolExecutor().schedule(this, longWaitTime, TimeUnit.SECONDS);
        }
    }
}
