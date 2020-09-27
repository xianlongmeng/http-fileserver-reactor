package com.ly.rhdfs.store.manager.handler;

import com.ly.common.domain.ResultInfo;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandFileInfo;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileInfoCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerManager serverManager;

    public FileInfoCommandEventHandler(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileInfo)) {
            logger.error("Illegal command,not a server address command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandFileInfo dfsCommandFileInfo = (DFSCommandFileInfo) dfsCommand;
        if (serverManager.getFileInfoManager().submitFileInfo(dfsCommandFileInfo.getFileInfo()) == null) {
            logger.info("file information save failed.path[{}],file name[{}]",
                    dfsCommandFileInfo.getFileInfo().getPath(), dfsCommandFileInfo.getFileInfo().getFileName());
            return ResultInfo.S_FAILED;
        } else {
            logger.info("file information is saved.path[{}],file name[{}]", dfsCommandFileInfo.getFileInfo().getPath(),
                    dfsCommandFileInfo.getFileInfo().getFileName());
            return ResultInfo.S_OK;
        }
    }
}
