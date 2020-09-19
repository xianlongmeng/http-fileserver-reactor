package com.ly.rhdfs.store.manager.handler;

import java.util.concurrent.CompletableFuture;

import com.ly.rhdfs.communicate.command.DFSCommandReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.file.FileChunkCopy;
import com.ly.common.domain.file.FileInfo;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandFileChunkCopy;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.store.StoreFile;
import com.ly.rhdfs.store.manager.StoreManager;

public class FileChunkCopyCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final StoreManager storeManager;
    private StoreFile storeFile;

    public FileChunkCopyCommandEventHandler(StoreManager storeManager) {
        this.storeManager = storeManager;
        storeFile = storeManager.getStoreFile();
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileChunkCopy)) {
            logger.error("Illegal command,not a file chunk copy command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandFileChunkCopy dfsCommandFileChunkCopy = (DFSCommandFileChunkCopy) dfsCommand;
        FileChunkCopy fileChunkCopy = dfsCommandFileChunkCopy.getFileChunkCopy();
        if (fileChunkCopy == null) {
            logger.error("Illegal command,file chunk copy is null.");
            return ResultInfo.S_ERROR;
        }
        FileInfo fileInfo = storeManager.getFileInfoManager().findFileInfo(
                dfsCommandFileChunkCopy.getFileChunkCopy().getPath(),
                dfsCommandFileChunkCopy.getFileChunkCopy().getFileName());
        if (fileInfo == null) {
            logger.error("file copy failed,file is not found.path[{}],file name[{}]",
                    fileChunkCopy.getPath(), fileChunkCopy.getFileName());
            return ResultInfo.S_ERROR;
        } else {
            CompletableFuture<Integer> completableFuture = storeManager.sendBackupStoreFileAsync(fileChunkCopy);
            completableFuture.whenCompleteAsync((result,t)->{
                if (result == ResultInfo.S_OK) {
                    // success
                    logger.info(
                            "file transfer success,path[{}],file name[{}],index[{}]",
                            fileChunkCopy.getPath(), fileChunkCopy.getFileName(), fileChunkCopy.getChunk());
                    storeManager.sendCommandReply(dfsCommandFileChunkCopy, DFSCommandReply.REPLY_STATE_TRUE,0);
                } else {
                    // failed
                    logger.warn(
                            "file transfer failed,path[{}],file name[{}],index[{}]",
                            fileChunkCopy.getPath(), fileChunkCopy.getFileName(), fileChunkCopy.getChunk());
                    storeManager.sendCommandReply(dfsCommandFileChunkCopy, DFSCommandReply.REPLY_STATE_FALSE,401);
                }
            });
        }
        return ResultInfo.S_OK;
    }
}
