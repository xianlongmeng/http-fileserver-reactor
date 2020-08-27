package com.ly.rhdfs.manager.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.ResultInfo;
import com.ly.common.util.DfsFileUtils;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandFileDelete;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;

public class FileDeleteCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerManager serverManager;

    public FileDeleteCommandEventHandler(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileDelete)) {
            logger.error("Illegal command,not a server address command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandFileDelete dfsCommandFileDelete = (DFSCommandFileDelete) dfsCommand;
        if (DfsFileUtils.fileDelete(dfsCommandFileDelete.getFileDeleteTokenInfo().getPath(),
                dfsCommandFileDelete.getFileDeleteTokenInfo().getFileName())) {
            logger.info("file is deleted.path[{}],file name[{}]", dfsCommandFileDelete.getFileDeleteTokenInfo().getPath(),
                    dfsCommandFileDelete.getFileDeleteTokenInfo().getFileName());
            return ResultInfo.S_OK;
        } else {
            logger.info("file delete failed.path[{}],file name[{}]", dfsCommandFileDelete.getFileDeleteTokenInfo().getPath(),
                    dfsCommandFileDelete.getFileDeleteTokenInfo().getFileName());
            return ResultInfo.S_FAILED;
        }
    }
}
