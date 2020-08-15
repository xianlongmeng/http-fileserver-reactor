package com.ly.rhdfs.master.manager.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ly.common.domain.server.ServerInfoConfiguration;
import com.ly.common.domain.server.ServerState;
import com.ly.common.domain.token.TokenInfo;
import com.ly.rhdfs.master.manager.MasterManager;

public class DownloadClearTask implements Runnable{
    private int waitTime=300;
    private MasterManager masterManager;
    public DownloadClearTask(MasterManager masterManager){
        this.masterManager=masterManager;
    }
    @Override
    public void run() {
        List<TokenInfo> tokenInfoList=new ArrayList<>();
        for (TokenInfo tokenInfo:masterManager.getFileServerRunManager().getDownloadRunningTask().keySet()){
            if (tokenInfo!=null && tokenInfo.getLastTime()+tokenInfo.getExpirationMills()> Instant.now().toEpochMilli()){
                tokenInfoList.add(tokenInfo);
            }
        }
        for(TokenInfo tokenInfo:tokenInfoList){
            masterManager.getFileServerRunManager().clearDownloadFile(tokenInfo,false);
        }
        masterManager.getFileServerRunManager().resetAvailableOrderlyServerRunStates();
        masterManager.getScheduledThreadPoolExecutor().schedule(this,waitTime, TimeUnit.SECONDS);
    }
}
