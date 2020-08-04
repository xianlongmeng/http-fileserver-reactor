package com.ly.rhdfs.master.manager.task;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.ly.rhdfs.master.manager.MasterManager;

public class AvailableOrderlyServerRunStateTask implements Runnable {

    private final int longWaitTime = 10;
    private final int shortWaitTime = 2;
    private final int timeFire=180000;
    private final int foreFire=600000;
    private long updateTimestamp;
    private final MasterManager masterManager;

    public AvailableOrderlyServerRunStateTask(MasterManager masterManager) {
        this.masterManager = masterManager;
        updateTimestamp = Instant.now().toEpochMilli();
    }

    @Override
    public void run() {
        long ins=Instant.now().toEpochMilli()-updateTimestamp;
        boolean res=true;
        if (ins<timeFire){
            res=masterManager.getFileServerRunManager().resetAvailableOrderlyServerRunStates();
        }else if(ins>foreFire){
            res=masterManager.getFileServerRunManager().resetAvailableOrderlyServerRunStates(50);
        }else{
            masterManager.getFileServerRunManager().resetForceAvailableOrderlyServerRunStates();
        }
        if (res) {
            updateTimestamp=Instant.now().toEpochMilli();
            masterManager.getScheduledThreadPoolExecutor().schedule(this, longWaitTime, TimeUnit.SECONDS);
        } else {
            masterManager.getScheduledThreadPoolExecutor().schedule(this, shortWaitTime, TimeUnit.SECONDS);
        }
    }
}
