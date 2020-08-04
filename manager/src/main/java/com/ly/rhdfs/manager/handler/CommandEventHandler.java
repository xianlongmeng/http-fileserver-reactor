package com.ly.rhdfs.manager.handler;

import com.ly.common.domain.ResultInfo;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandServerAddress;
import com.ly.rhdfs.communicate.command.DFSCommandState;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandEventHandler implements EventHandler {
    private final Logger logger= LoggerFactory.getLogger(getClass());
    private final ServerStateCommandEventHandler serverStateCommandEventHandler;
    private final ServerAddressCommandEventHandler serverAddressCommandEventHandler;
    private final ServerManager serverManager;

    public CommandEventHandler(ServerManager serverManager){
        this.serverManager=serverManager;
        serverStateCommandEventHandler=new ServerStateCommandEventHandler(serverManager);
        serverAddressCommandEventHandler=new ServerAddressCommandEventHandler(serverManager);
    }
    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (dfsCommand==null) {
            logger.error("null command.");
            return ResultInfo.S_ERROR;
        }
        if (dfsCommand instanceof DFSCommandState){
            return serverStateCommandEventHandler.actorCommand(dfsCommand);
        }else if (dfsCommand instanceof DFSCommandServerAddress){
            return serverAddressCommandEventHandler.actorCommand(dfsCommand);
            // TODO
        }else {
            logger.error("Illegal command, the resolution handler was not found.");
            return ResultInfo.S_ERROR;
        }
    }
}
