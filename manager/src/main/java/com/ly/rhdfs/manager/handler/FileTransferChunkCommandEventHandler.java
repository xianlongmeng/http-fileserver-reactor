package com.ly.rhdfs.manager.handler;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.service.FileChunkManager;
import com.ly.common.util.SpringContextUtil;
import com.ly.etag.ETagComputer;
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

public class FileTransferChunkCommandEventHandler implements EventHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerManager serverManager;
    private final FileChunkManager fileChunkManager;
    private StoreFile storeFile;
    private ETagComputer eTagComputer;

    public FileTransferChunkCommandEventHandler(ServerManager serverManager) {
        this.serverManager = serverManager;
        storeFile = SpringContextUtil.getBean(StoreFile.class);
        fileChunkManager = SpringContextUtil.getBean(FileChunkManager.class);
        eTagComputer = SpringContextUtil.getBean(ETagComputer.class);
    }

    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileTransfer)) {
            logger.error("Illegal command,not a file transfer command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandFileTransfer dfsCommandFileTransfer = (DFSCommandFileTransfer) dfsCommand;
        FileInfo fileInfo = serverManager.getFileInfoManager().findFileInfo(
                dfsCommandFileTransfer.getFileTransferInfo().getPath(),
                dfsCommandFileTransfer.getFileTransferInfo().getFileName());
        if (fileInfo == null) {
            logger.error("file transfer failed,file is not found.path[{}],file name[{}]",
                    dfsCommandFileTransfer.getFileTransferInfo().getPath(), dfsCommandFileTransfer.getFileTransferInfo().getFileName());
            return ResultInfo.S_ERROR;
        } else {
            String fileName = String.format("%s.%d.%s", dfsCommandFileTransfer.getFileTransferInfo().getFileName(), dfsCommandFileTransfer.getFileTransferInfo().getChunkIndex(), serverManager.getServerConfig().getFileChunkSuffix());
            String fileFullName = storeFile.takeFilePath(fileName, dfsCommandFileTransfer.getFileTransferInfo().getPath()).toString();

            AsynchronousFileChannel asynchronousFileChannel;
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
                                        logger.info("file is saved.path[{}],file name[{}],chunk[{}],position[{}],size[{}]",
                                                dfsCommandFileTransfer.getFileTransferInfo().getPath(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getFileName(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getChunkIndex(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getStartPos(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getSize());
                                        int count = fileChunkManager.setFileChunkState(fileFullName,
                                                dfsCommandFileTransfer.getFileTransferInfo().getChunkPieceCount(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getChunkPieceIndex());
                                        if (count == dfsCommandFileTransfer.getFileTransferInfo().getChunkPieceCount()) {
                                            //verify etag
                                            eTagComputer
                                                    .etagFile(serverManager.getDfsFileUtils()
                                                            .joinFileName(dfsCommandFileTransfer.getFileTransferInfo().getPath(),
                                                                    dfsCommandFileTransfer.getFileTransferInfo().getFileName()))
                                                    .subscribe(etag -> {
                                                        if (etag.equals(dfsCommandFileTransfer.getFileTransferInfo().getEtag())) {
                                                            logger.info("file is completed.path[{}],file name[{}],chunk[{}]",
                                                                    dfsCommandFileTransfer.getFileTransferInfo().getPath(),
                                                                    dfsCommandFileTransfer.getFileTransferInfo().getFileName(),
                                                                    dfsCommandFileTransfer.getFileTransferInfo().getChunkIndex());
                                                            serverManager.sendCommandReply(dfsCommandFileTransfer, DFSCommandReply.REPLY_STATE_TRUE, 0);
                                                        } else {
                                                            logger.warn("file verify failed.path[{}],file name[{}],chunk[{}]",
                                                                    dfsCommandFileTransfer.getFileTransferInfo().getPath(),
                                                                    dfsCommandFileTransfer.getFileTransferInfo().getFileName(),
                                                                    dfsCommandFileTransfer.getFileTransferInfo().getChunkIndex());
                                                            serverManager.sendCommandReply(dfsCommandFileTransfer, DFSCommandReply.REPLY_STATE_FALSE, 501);
                                                        }
                                                    });
                                        } else {
                                            serverManager.sendCommandReply(dfsCommandFileTransfer, DFSCommandReply.REPLY_STATE_TRUE, 0);
                                        }
                                    }

                                    @Override
                                    public void failed(Throwable exc, ByteBuffer attachment) {
                                        logger.error(String.format("file is saved.path[%s],file name[%s],position[%d],size[%d]",
                                                dfsCommandFileTransfer.getFileTransferInfo().getPath(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getFileName(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getStartPos(),
                                                dfsCommandFileTransfer.getFileTransferInfo().getSize()),
                                                exc);
                                        serverManager.sendCommandReply(dfsCommandFileTransfer, DFSCommandReply.REPLY_STATE_FALSE, 401);
                                    }
                                });
            } catch (IOException e) {
                logger.error(String.format("file is saved.path[%s],file name[%s],position[%d],size[%d]",
                        dfsCommandFileTransfer.getFileTransferInfo().getPath(),
                        dfsCommandFileTransfer.getFileTransferInfo().getFileName(),
                        dfsCommandFileTransfer.getFileTransferInfo().getStartPos(),
                        dfsCommandFileTransfer.getFileTransferInfo().getSize()),
                        e);
                serverManager.sendCommandReply(dfsCommandFileTransfer, DFSCommandReply.REPLY_STATE_FALSE, 1);
            }
        }
        return ResultInfo.S_OK;
    }
}
