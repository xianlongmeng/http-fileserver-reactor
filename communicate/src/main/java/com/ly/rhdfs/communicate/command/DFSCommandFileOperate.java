package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.log.OperationLog;

public class DFSCommandFileOperate extends DFSCommand {

    private OperationLog operationLog;

    public DFSCommandFileOperate() {
        commandType = DFSCommand.CT_FILE_OPERATE;
    }

    public OperationLog getOperationLog() {
        return operationLog;
    }

    public void setOperationLog(OperationLog operationLog) {
        this.operationLog = operationLog;
    }
}
