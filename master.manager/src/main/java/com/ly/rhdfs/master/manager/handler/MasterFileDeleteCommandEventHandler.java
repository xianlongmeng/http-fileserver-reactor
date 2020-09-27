package com.ly.rhdfs.master.manager.handler;

import com.ly.common.domain.file.ChunkInfo;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.server.ServerAddressInfo;
import com.ly.rhdfs.master.manager.MasterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.ResultInfo;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandFileDelete;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;

public class MasterFileDeleteCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final MasterManager masterManager;

    public MasterFileDeleteCommandEventHandler(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileDelete)) {
            logger.error("Illegal command,not a server address command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandFileDelete dfsCommandFileDelete = (DFSCommandFileDelete) dfsCommand;
        FileInfo fileInfo=masterManager.getFileInfoManager().findFileInfo(dfsCommandFileDelete.getFileDeleteTokenInfo().getPath(), dfsCommandFileDelete.getFileDeleteTokenInfo().getFileName());
        if (fileInfo==null){
            logger.warn("file information is not found![{}/{}]",dfsCommandFileDelete.getFileDeleteTokenInfo().getPath(),dfsCommandFileDelete.getFileDeleteTokenInfo().getFileName());
            return ResultInfo.S_ERROR;
        }
        for (int i = 0; i < fileInfo.getFileChunkList().size(); i++) {
            ChunkInfo chunkInfo = fileInfo.getFileChunkList().get(i);
            for (ServerAddressInfo serverAddressInfo : chunkInfo.getChunkServerIdList()) {
                masterManager.getServerFileChunkUtil().writeDeleteServerFileChunk(serverAddressInfo.getServerId(),fileInfo.getPath(),fileInfo.getFileName(),chunkInfo.getIndex());
            }
        }
        if (masterManager.getDfsFileUtils().fileDelete(fileInfo.getPath(),fileInfo.getFileName()))
            return ResultInfo.S_OK;
        else
            return ResultInfo.S_FAILED;
    }
}
