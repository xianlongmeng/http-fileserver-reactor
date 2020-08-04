package com.ly.rhdfs.store.manager.task;

import java.util.concurrent.TimeUnit;

import com.ly.rhdfs.store.manager.StoreManager;

public class ComputerStateTask implements Runnable {

    private int waitTime = 300;
    private StoreManager storeManager;

    public ComputerStateTask(StoreManager storeManager) {
        this.storeManager = storeManager;
    }

    @Override
    public void run() {
        storeManager.computerFreeSpaceSize();
        storeManager.getScheduledThreadPoolExecutor().schedule(this, waitTime, TimeUnit.SECONDS);
    }
}
