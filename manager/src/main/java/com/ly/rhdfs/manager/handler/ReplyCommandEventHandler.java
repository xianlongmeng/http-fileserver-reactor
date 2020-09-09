package com.ly.rhdfs.manager.handler;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.util.SpringContextUtil;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandFileTransfer;
import com.ly.rhdfs.communicate.command.DFSCommandReply;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;
import com.ly.rhdfs.store.StoreFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.StandardOpenOption;

public class ReplyCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerManager serverManager;

    public ReplyCommandEventHandler(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandReply)) {
            logger.error("Illegal command,not a server address command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandReply dfsCommandReply = (DFSCommandReply) dfsCommand;
        serverManager.receiveReply(dfsCommandReply);
        return ResultInfo.S_OK;
    }
}
