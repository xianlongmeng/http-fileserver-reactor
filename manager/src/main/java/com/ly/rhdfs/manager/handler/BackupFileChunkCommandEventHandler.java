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

public class BackupFileChunkCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerManager serverManager;
    private StoreFile storeFile;

    public BackupFileChunkCommandEventHandler(ServerManager serverManager) {
        this.serverManager = serverManager;
        storeFile = SpringContextUtil.getBean(StoreFile.class);
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileTransfer)) {
            logger.error("Illegal command,not a server address command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandFileTransfer dfsCommandFileTransfer = (DFSCommandFileTransfer) dfsCommand;
        FileInfo fileInfo = serverManager.getFileInfoManager().findFileInfo(serverManager.getDfsFileUtils().joinFileConfigName(dfsCommandFileTransfer.getFileTransferInfo().getPath(), dfsCommandFileTransfer.getFileTransferInfo().getFileName()));
        if (fileInfo == null) {
            logger.error("file transfer failed,file is not authority.path[{}],file name[{}]",
                    dfsCommandFileTransfer.getFileTransferInfo().getPath(), dfsCommandFileTransfer.getFileTransferInfo().getFileName());
            return ResultInfo.S_ERROR;
        } else {
            String fileName = String.format("%s.%d.%s", dfsCommandFileTransfer.getFileTransferInfo().getFileName(), dfsCommandFileTransfer.getFileTransferInfo().getChunkIndex(), serverManager.getServerConfig().getFileChunkSuffix());
            String fileFullName = storeFile.takeFilePath(fileName, dfsCommandFileTransfer.getFileTransferInfo().getPath()).toString();

            AsynchronousFileChannel asynchronousFileChannel = null;
            try {
                asynchronousFileChannel = AsynchronousFileChannel.open(
                        new File(fileFullName).toPath(), StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE);
                ByteBuffer byteBuffer = dfsCommandFileTransfer.getFileTransferInfo().getByteBuf().nioBuffer();
                asynchronousFileChannel
                        .write(byteBuffer,
                                dfsCommandFileTransfer.getFileTransferInfo().getStartPos(),
                                byteBuffer,
                                new CompletionHandler<>() {
                                    @Override
                                    public void completed(Integer result, ByteBuffer attachment) {
                                        logger.info("file is saved.path[{}],file name[{}],position[{}],size[{}]",
                                                dfsCommandFileTransfer.getFileTransferInfo().getPath(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getFileName(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getStartPos(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getSize());
                                        serverManager.sendCommandReply(dfsCommandFileTransfer, DFSCommandReply.REPLY_STATE_TRUE);
                                    }

                                    @Override
                                    public void failed(Throwable exc, ByteBuffer attachment) {
                                        logger.error(String.format("file is saved.path[%s],file name[%s],position[%d],size[%d]",
                                                dfsCommandFileTransfer.getFileTransferInfo().getPath(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getFileName(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getStartPos(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getSize()),
                                                exc);
                                        serverManager.sendCommandReply(dfsCommandFileTransfer, DFSCommandReply.REPLY_STATE_FALSE);
                                    }
                                });
            } catch (IOException e) {
                logger.error(String.format("file is saved.path[%s],file name[%s],position[%d],size[%d]",
                        dfsCommandFileTransfer.getFileTransferInfo().getPath(),
                        dfsCommandFileTransfer.getFileTransferInfo().getFileName(),
                        dfsCommandFileTransfer.getFileTransferInfo().getStartPos(),
                        dfsCommandFileTransfer.getFileTransferInfo().getSize()),
                        e);
                serverManager.sendCommandReply(dfsCommandFileTransfer, DFSCommandReply.REPLY_STATE_FALSE);
            }
        }
        return ResultInfo.S_OK;
    }
}
