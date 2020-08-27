package com.ly.rhdfs.manager.handler;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandServerAddress;
import com.ly.rhdfs.communicate.command.DFSCommandState;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class ServerStateCommandEventHandler implements EventHandler {
    private final Logger logger= LoggerFactory.getLogger(getClass());
    protected final ServerManager serverManager;
    public ServerStateCommandEventHandler(ServerManager serverManager){
        this.serverManager=serverManager;
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
        serverManager.putServerState(dfsCommandState.getServerState());
        return 0;
    }
}
