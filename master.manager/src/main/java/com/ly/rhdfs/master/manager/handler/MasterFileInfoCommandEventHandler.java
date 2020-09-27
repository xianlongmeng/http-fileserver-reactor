package com.ly.rhdfs.master.manager.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.file.ChunkInfo;
import com.ly.common.domain.server.ServerAddressInfo;
import com.ly.common.domain.server.ServerRunState;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandFileInfo;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.master.manager.MasterManager;

public class MasterFileInfoCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final MasterManager masterManager;

    public MasterFileInfoCommandEventHandler(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileInfo)) {
            logger.error("Illegal command,not a server address command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandFileInfo dfsCommandFileInfo = (DFSCommandFileInfo) dfsCommand;
        if (masterManager.getFileInfoManager().submitFileInfo(dfsCommandFileInfo.getFileInfo()) == null) {
            logger.info("file information save failed.path[{}],file name[{}]",
                    dfsCommandFileInfo.getFileInfo().getPath(), dfsCommandFileInfo.getFileInfo().getFileName());
            for (int i = 0; i < dfsCommandFileInfo.getFileInfo().getFileChunkList().size(); i++) {
                ChunkInfo chunkInfo = dfsCommandFileInfo.getFileInfo().getFileChunkList().get(i);
                for (ServerAddressInfo serverAddressInfo : chunkInfo.getChunkServerIdList()) {
                    // 记录当前服务器chunk增加日志
                    masterManager.getServerFileChunkUtil().writeAddServerFileChunk(serverAddressInfo.getServerId(),
                            dfsCommandFileInfo.getFileInfo().getPath(), dfsCommandFileInfo.getFileInfo().getFileName(),
                            chunkInfo.getIndex());
                }
            }
            return ResultInfo.S_FAILED;
        } else {
            logger.info("file information is saved.path[{}],file name[{}]", dfsCommandFileInfo.getFileInfo().getPath(),
                    dfsCommandFileInfo.getFileInfo().getFileName());
            return ResultInfo.S_OK;
        }
    }
}
