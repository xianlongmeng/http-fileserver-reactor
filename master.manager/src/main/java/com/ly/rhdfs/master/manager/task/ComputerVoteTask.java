package com.ly.rhdfs.master.manager.task;

import java.util.concurrent.TimeUnit;

import com.ly.rhdfs.master.manager.MasterManager;

public class ComputerVoteTask implements Runnable {

    private int longWaitTime = 300;
    private int shortWaitTime = 30;
    private MasterManager masterManager;

    public ComputerVoteTask(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Override
    public void run() {
        if (masterManager.getMasterServerId() != masterManager.getLocalServerId()) {
            masterManager.computerVoteCount();
        }
        if (masterManager.getMasterServerId() == masterManager.getLocalServerId()) {
            masterManager.getScheduledThreadPoolExecutor().schedule(this, longWaitTime, TimeUnit.SECONDS);
        } else {
            masterManager.getScheduledThreadPoolExecutor().schedule(this, shortWaitTime, TimeUnit.SECONDS);
        }
    }
}
