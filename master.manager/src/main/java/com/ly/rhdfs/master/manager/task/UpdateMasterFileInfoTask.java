package com.ly.rhdfs.master.manager.task;

import com.ly.common.domain.log.OperationLog;
import com.ly.common.domain.server.ServerState;
import com.ly.common.domain.token.TokenInfo;
import com.ly.rhdfs.file.util.DfsFileUtils;
import com.ly.common.util.SpringContextUtil;
import com.ly.rhdfs.log.operate.LogFileOperate;
import com.ly.rhdfs.log.operate.LogOperateUtils;
import com.ly.rhdfs.master.manager.MasterManager;

public class UpdateMasterFileInfoTask implements Runnable {

    private final MasterManager masterManager;
    private final ServerState serverState;
    private final DfsFileUtils dfsFileUtils;

    public UpdateMasterFileInfoTask(MasterManager masterManager, DfsFileUtils dfsFileUtils, ServerState serverState) {
        this.masterManager = masterManager;
        this.dfsFileUtils = dfsFileUtils;
        this.serverState = serverState;
    }

    @Override
    public void run() {
        //
        LogFileOperate logFileOperate = new LogFileOperate(masterManager.getServerConfig().getLogPath());
        logFileOperate.setDfsFileUtils(SpringContextUtil.getBean(DfsFileUtils.class));
        logFileOperate.setLogOperateUtils(SpringContextUtil.getBean(LogOperateUtils.class));
        OperationLog operationLog = logFileOperate.openOperateFile(serverState.getWriteLastTime());
        while (operationLog != null) {
            if (OperationLog.OP_TYPE_ADD_FILE_FINISH.equals(operationLog.getOpType())
                    || OperationLog.OP_TYPE_UPDATE_FILE.equals(operationLog.getOpType())) {
                if (!masterManager.sendFileInfoSync(serverState,
                        dfsFileUtils.readFileInfo(operationLog.getPath(), operationLog.getFileName()))){
                    logFileOperate.close();
                    return;
                }
            } else if (OperationLog.OP_TYPE_DELETE_FILE.equals(operationLog.getOpType())) {
                if (!masterManager.sendFileDeleteSync(serverState,
                        new TokenInfo(operationLog.getPath(), operationLog.getFileName()))){
                    logFileOperate.close();
                    return;
                }
            }
            operationLog = logFileOperate.readNextOperationLog();
        }
        logFileOperate.close();
        serverState.setReady(true);
        operationLog = new OperationLog(OperationLog.OP_TYPE_UPDATE_FINISH, "", "");
        masterManager.sendFileOperationLog(serverState, operationLog);
        masterManager.getMasterUpdateFileInfo().remove(serverState);
    }
}
