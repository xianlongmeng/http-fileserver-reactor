package com.ly.rhdfs.master.manager.handler;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.log.OperationLog;
import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandFileOperate;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.master.manager.MasterManager;

public class OperateLogCommandMasterEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private MasterManager masterManager;

    public OperateLogCommandMasterEventHandler(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileOperate)) {
            logger.error("Illegal command,not a server state command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandFileOperate dfsCommandFileOperate = (DFSCommandFileOperate) dfsCommand;
        OperationLog operationLog = dfsCommandFileOperate.getOperationLog();

        if (dfsCommandFileOperate.getServerId() == masterManager.getMasterServerId()
                && OperationLog.OP_TYPE_UPDATE_FINISH.equals(operationLog.getOpType())) {
            // log update finish
            // set ready
            masterManager.getLocalServerState().setReady(true);
            masterManager.getLocalServerState().setLastTime(Instant.now().toEpochMilli());
            masterManager.getLocalServerState()
                    .setState(masterManager.getLocalServerState().getState() & ~ServerState.SIS_MASTER_LOST_CONTACT);
        }
        return ResultInfo.S_OK;
    }
}
