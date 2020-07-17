package com.ly.rhdfs.master.manager.task;

import java.util.concurrent.TimeUnit;

import com.ly.rhdfs.master.manager.MasterManager;

public class MasterQualificationVerify implements Runnable {

    private int longWaitTime = 300;
    private int shortWaitTime = 30;
    private MasterManager masterManager;

    public MasterQualificationVerify(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Override
    public void run() {
        masterManager.verifyMasterQualification();
        if (masterManager.getMasterServerId() == -1) {
            masterManager.getScheduledThreadPoolExecutor().schedule(this, longWaitTime, TimeUnit.SECONDS);
        } else {
            masterManager.getScheduledThreadPoolExecutor().schedule(this, shortWaitTime, TimeUnit.SECONDS);
        }
    }
}
