package com.ly.rhdfs.store.manager.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.ResultInfo;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandToken;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.store.manager.StoreManager;

public class TokenCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final StoreManager storeManager;

    public TokenCommandEventHandler(StoreManager storeManager) {
        this.storeManager = storeManager;
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandToken)) {
            logger.error("Illegal command,not a server address command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandToken dfsCommandToken = (DFSCommandToken) dfsCommand;
        storeManager.putTokenInfo(dfsCommandToken.getTokenInfo());
        return ResultInfo.S_OK;
    }
}
