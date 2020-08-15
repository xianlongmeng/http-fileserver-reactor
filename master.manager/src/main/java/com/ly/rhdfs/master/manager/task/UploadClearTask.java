package com.ly.rhdfs.master.manager.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ly.common.domain.token.TokenInfo;
import com.ly.rhdfs.master.manager.MasterManager;

public class UploadClearTask implements Runnable{
    private int waitTime=1800;
    private MasterManager masterManager;
    public UploadClearTask(MasterManager masterManager){
        this.masterManager=masterManager;
    }
    @Override
    public void run() {
        List<TokenInfo> tokenInfoList=new ArrayList<>();
        for (TokenInfo tokenInfo:masterManager.getFileServerRunManager().getUploadRunningTask().keySet()){
            if (tokenInfo!=null && tokenInfo.getLastTime()+tokenInfo.getExpirationMills()> Instant.now().toEpochMilli()){
                tokenInfoList.add(tokenInfo);
            }
        }
        for(TokenInfo tokenInfo:tokenInfoList){
            masterManager.getFileServerRunManager().clearUploadFile(tokenInfo,false);
        }
        masterManager.getFileServerRunManager().resetAvailableOrderlyServerRunStates();
        masterManager.getScheduledThreadPoolExecutor().schedule(this,waitTime, TimeUnit.SECONDS);
    }
}
