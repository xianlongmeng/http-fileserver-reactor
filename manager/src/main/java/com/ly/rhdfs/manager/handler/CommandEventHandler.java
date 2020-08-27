package com.ly.rhdfs.manager.handler;

import com.ly.common.domain.ResultInfo;
import com.ly.rhdfs.communicate.command.*;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.internal.shaded.reactor.pool.PoolAcquireTimeoutException;

public class CommandEventHandler implements EventHandler {
    private final Logger logger= LoggerFactory.getLogger(getClass());
    private EventHandler serverStateCommandEventHandler;
    private EventHandler serverAddressCommandEventHandler;
    private EventHandler operationLogCommandEventHandler;
    private EventHandler fileDeleteCommandEventHandler;
    private EventHandler fileInfoCommandEventHandler;
    private EventHandler fileTransferCommandEventHandler;
    private EventHandler fileTransferStateCommandEventHandler;
    private EventHandler tokenCommandEventHandler;

    private final ServerManager serverManager;

    public CommandEventHandler(ServerManager serverManager){
        this.serverManager=serverManager;
    }

    public EventHandler getServerStateCommandEventHandler() {
        return serverStateCommandEventHandler;
    }

    public void setServerStateCommandEventHandler(EventHandler serverStateCommandEventHandler) {
        this.serverStateCommandEventHandler = serverStateCommandEventHandler;
    }

    public EventHandler getServerAddressCommandEventHandler() {
        return serverAddressCommandEventHandler;
    }

    public void setServerAddressCommandEventHandler(EventHandler serverAddressCommandEventHandler) {
        this.serverAddressCommandEventHandler = serverAddressCommandEventHandler;
    }

    public EventHandler getOperationLogCommandEventHandler() {
        return operationLogCommandEventHandler;
    }

    public void setOperationLogCommandEventHandler(EventHandler operationLogCommandEventHandler) {
        this.operationLogCommandEventHandler = operationLogCommandEventHandler;
    }

    public EventHandler getFileDeleteCommandEventHandler() {
        return fileDeleteCommandEventHandler;
    }

    public void setFileDeleteCommandEventHandler(EventHandler fileDeleteCommandEventHandler) {
        this.fileDeleteCommandEventHandler = fileDeleteCommandEventHandler;
    }

    public EventHandler getFileInfoCommandEventHandler() {
        return fileInfoCommandEventHandler;
    }

    public void setFileInfoCommandEventHandler(EventHandler fileInfoCommandEventHandler) {
        this.fileInfoCommandEventHandler = fileInfoCommandEventHandler;
    }

    public EventHandler getFileTransferCommandEventHandler() {
        return fileTransferCommandEventHandler;
    }

    public void setFileTransferCommandEventHandler(EventHandler fileTransferCommandEventHandler) {
        this.fileTransferCommandEventHandler = fileTransferCommandEventHandler;
    }

    public EventHandler getFileTransferStateCommandEventHandler() {
        return fileTransferStateCommandEventHandler;
    }

    public void setFileTransferStateCommandEventHandler(EventHandler fileTransferStateCommandEventHandler) {
        this.fileTransferStateCommandEventHandler = fileTransferStateCommandEventHandler;
    }

    public EventHandler getTokenCommandEventHandler() {
        return tokenCommandEventHandler;
    }

    public void setTokenCommandEventHandler(EventHandler tokenCommandEventHandler) {
        this.tokenCommandEventHandler = tokenCommandEventHandler;
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (dfsCommand==null) {
            logger.error("null command.");
            return ResultInfo.S_ERROR;
        }
        if (dfsCommand instanceof DFSCommandState && serverStateCommandEventHandler!=null){
            return serverStateCommandEventHandler.actorCommand(dfsCommand);
        }else if (dfsCommand instanceof DFSCommandServerAddress && serverAddressCommandEventHandler!=null){
            return serverAddressCommandEventHandler.actorCommand(dfsCommand);
        }else if (dfsCommand instanceof DFSCommandFileOperate && operationLogCommandEventHandler!=null){
            return operationLogCommandEventHandler.actorCommand(dfsCommand);
        }else if (dfsCommand instanceof DFSCommandFileDelete && fileDeleteCommandEventHandler!=null){
            return fileDeleteCommandEventHandler.actorCommand(dfsCommand);
        }else if (dfsCommand instanceof DFSCommandFileInfo && fileInfoCommandEventHandler!=null){
            return fileInfoCommandEventHandler.actorCommand(dfsCommand);
        }else if (dfsCommand instanceof DFSCommandFileTransfer && fileTransferCommandEventHandler!=null){
            return fileTransferCommandEventHandler.actorCommand(dfsCommand);
        }else if (dfsCommand instanceof DFSCommandFileTransferState && fileTransferStateCommandEventHandler!=null){
            return fileTransferStateCommandEventHandler.actorCommand(dfsCommand);
        }else if (dfsCommand instanceof DFSCommandToken && tokenCommandEventHandler!=null){
            return tokenCommandEventHandler.actorCommand(dfsCommand);
        }else {
            logger.error("Illegal command, the resolution handler was not found.");
            return ResultInfo.S_ERROR;
        }
    }
}
