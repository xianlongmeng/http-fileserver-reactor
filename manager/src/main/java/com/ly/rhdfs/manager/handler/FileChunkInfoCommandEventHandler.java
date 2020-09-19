package com.ly.rhdfs.manager.handler;

import com.ly.rhdfs.communicate.command.DFSCommandReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.file.FileChunkInfo;
import com.ly.common.domain.file.FileInfo;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandFileChunkInfo;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;

public class FileChunkInfoCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerManager serverManager;

    public FileChunkInfoCommandEventHandler(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileChunkInfo)) {
            logger.error("Illegal command,not a file chunk information command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandFileChunkInfo dfsCommandFileChunkInfo = (DFSCommandFileChunkInfo) dfsCommand;
        if (dfsCommandFileChunkInfo.getFileChunkInfo() == null) {
            logger.error("Illegal command,not a file chunk information command.");
            serverManager.sendCommandReply(dfsCommand,DFSCommandReply.REPLY_STATE_FALSE,101);
            return ResultInfo.S_ERROR;
        }
        FileChunkInfo fileChunkInfo = dfsCommandFileChunkInfo.getFileChunkInfo();
        FileInfo fileInfo = serverManager.getFileInfoManager().findFileInfo(fileChunkInfo.getPath(),
                fileChunkInfo.getFileName());
        if (fileInfo == null || fileInfo.getFileChunkList() == null
                || fileInfo.getFileChunkList().size() <= fileChunkInfo.getChunk()
                || fileInfo.getFileChunkList().get(fileChunkInfo.getChunk()) == null) {
            logger.error("Illegal command,file information is valid,path[{}],file name[{}],chunk index[{}].",
                    fileChunkInfo.getPath(), fileChunkInfo.getFileName(), fileChunkInfo.getChunk());
            serverManager.sendCommandReply(dfsCommand,DFSCommandReply.REPLY_STATE_FALSE,101);
            return ResultInfo.S_ERROR;
        }
        fileInfo.getFileChunkList().get(fileChunkInfo.getChunk()).setChunkEtag(fileChunkInfo.getChunkEtag());
        if (serverManager.getFileInfoManager().submitFileInfo(fileInfo) == null) {
            logger.info("file information save failed.path[{}],file name[{}]",
                    fileChunkInfo.getPath(), fileChunkInfo.getFileName());
            serverManager.sendCommandReply(dfsCommand,DFSCommandReply.REPLY_STATE_FALSE,201);
            return ResultInfo.S_FAILED;
        } else {
            logger.info("file information is saved.path[{}],file name[{}]", fileChunkInfo.getPath(),
                    fileChunkInfo.getFileName());
            serverManager.sendCommandReply(dfsCommand,DFSCommandReply.REPLY_STATE_TRUE,0);
            return ResultInfo.S_OK;
        }
    }
}
