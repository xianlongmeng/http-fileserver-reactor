package com.ly.rhdfs.master.manager.task;

import com.ly.common.domain.file.FileDelete;
import com.ly.common.domain.log.OperationLog;
import com.ly.common.domain.server.ServerState;
import com.ly.common.domain.token.TokenInfo;
import com.ly.common.util.DfsFileUtils;
import com.ly.rhdfs.log.operate.LogFileOperate;
import com.ly.rhdfs.log.operate.LogOperateUtils;
import com.ly.rhdfs.master.manager.MasterManager;

public class UpdateMasterFileInfoTask implements Runnable {

    private MasterManager masterManager;
    private ServerState serverState;

    public UpdateMasterFileInfoTask(MasterManager masterManager, ServerState serverState) {
        this.masterManager = masterManager;
        this.serverState = serverState;
    }

    @Override
    public void run() {
        //
        LogFileOperate logFileOperate = new LogFileOperate(LogOperateUtils.LOG_PATH);
        OperationLog operationLog = logFileOperate.openOperateFile(serverState.getWriteLastTime());
        while (operationLog != null) {
            if (OperationLog.OP_TYPE_ADD_FILE_FINISH.equals(operationLog.getOpType())
                    || OperationLog.OP_TYPE_UPDATE_FILE.equals(operationLog.getOpType())) {
                if (!masterManager.sendFileInfoSync(serverState,
                        DfsFileUtils.readFileInfo(operationLog.getPath(), operationLog.getFileName())))
                    return;
            } else if (OperationLog.OP_TYPE_DELETE_FILE.equals(operationLog.getOpType())) {
                if (!masterManager.sendFileDeleteSync(serverState,
                        new TokenInfo(operationLog.getPath(), operationLog.getFileName())))
                    return;
            }
            operationLog = logFileOperate.readNextOperationLog();
        }
        serverState.setReady(true);
        operationLog = new OperationLog(OperationLog.OP_TYPE_UPDATE_FINISH, "", "");
        masterManager.sendFileOperationLog(serverState, operationLog);
        masterManager.getMasterUpdateFileInfo().remove(serverState);
    }
}
