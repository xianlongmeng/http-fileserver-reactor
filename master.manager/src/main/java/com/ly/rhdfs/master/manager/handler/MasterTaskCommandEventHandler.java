package com.ly.rhdfs.master.manager.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.ResultInfo;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandTask;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.master.manager.MasterManager;

public class MasterTaskCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final MasterManager masterManager;

    public MasterTaskCommandEventHandler(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandTask)) {
            logger.error("Illegal command,not a server address command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandTask dfsCommandTask = (DFSCommandTask) dfsCommand;
        masterManager.getFileServerRunManager().updateTaskInfo(dfsCommandTask.getTaskInfo());
        return ResultInfo.S_OK;
    }
}
