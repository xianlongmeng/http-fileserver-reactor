package com.ly.rhdfs.manager.handler;

import com.ly.rhdfs.communicate.command.DFSCommandTokenClear;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.ResultInfo;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandFileDelete;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;

public class ClearTokenCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerManager serverManager;

    public ClearTokenCommandEventHandler(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandTokenClear)) {
            logger.error("Illegal command,not a server address command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandTokenClear dfsCommandTokenClear = (DFSCommandTokenClear) dfsCommand;
        serverManager.clearToken(dfsCommandTokenClear.getTokenInfo());
        return ResultInfo.S_OK;
    }
}
