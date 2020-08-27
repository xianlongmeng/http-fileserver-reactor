package com.ly.rhdfs.master.manager.handler;

import java.time.Instant;

import com.ly.rhdfs.manager.handler.ServerStateCommandEventHandler;
import com.ly.rhdfs.master.manager.MasterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandState;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;

public class ServerStateCommandMasterEventHandler implements EventHandler {
    private final Logger logger= LoggerFactory.getLogger(getClass());
    private MasterManager masterManager;
    public ServerStateCommandMasterEventHandler(MasterManager masterManager){
        this.masterManager=masterManager;
    }
    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandState)){
            logger.error("Illegal command,not a server state command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandState dfsCommandState=(DFSCommandState)dfsCommand;
        dfsCommandState.getServerState().setLastTime(Instant.now().toEpochMilli());
        // 更新状态
        masterManager.putServerState(dfsCommandState.getServerState());
        // 是否更新FileInfo
        synchronized (masterManager) {
            if (masterManager.getMasterServerId() == masterManager.getLocalServerId()
                    && masterManager.getLocalServerState().getType() == ServerState.SIT_MASTER
                    && dfsCommandState.getServerState().getType() == ServerState.SIT_MASTER_BACKUP
                    && !masterManager.isContainUpdateFileInfo(dfsCommandState.getServerState())
                    && !dfsCommandState.getServerState().isReady()
                    && dfsCommandState.getServerState().getWriteLastTime() < masterManager.getLocalServerState().getWriteLastTime()) {
                // start update file info thread
                masterManager.startMasterUpdateFileInfo(dfsCommandState.getServerState());
            }
        }
        return ResultInfo.S_OK;
    }
}
