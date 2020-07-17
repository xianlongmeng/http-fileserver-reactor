package com.ly.rhdfs.master.manager.task;

import com.ly.common.domain.server.ServerInfoConfiguration;
import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.master.manager.MasterManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class UpdateServerAddressTask implements Runnable{
    private int waitTime=30;
    private MasterManager masterManager;
    public UpdateServerAddressTask(MasterManager masterManager){
        this.masterManager=masterManager;
    }
    @Override
    public void run() {
        if (masterManager.getMasterServerId()==masterManager.getLocalServerId()){
            for (ServerState serverState:masterManager.getServerInfoMap().values()){
                List<ServerInfoConfiguration> serverInfoConfigurationList=masterManager.collectChangeAddressServer(serverState);
                if (!serverInfoConfigurationList.isEmpty()){
                    //send package
                    masterManager.sendServerAddressUpdate(serverState,serverInfoConfigurationList);
                }
            }
        }
        masterManager.getScheduledThreadPoolExecutor().schedule(this,waitTime, TimeUnit.SECONDS);
    }
}
