package com.ly.rhdfs.communicate.socket.parse;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.ly.common.domain.file.*;
import io.netty.buffer.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ly.common.domain.log.OperationLog;
import com.ly.common.domain.server.ServerInfoConfiguration;
import com.ly.common.domain.server.ServerState;
import com.ly.common.domain.token.TokenInfo;
import com.ly.common.util.SpringContextUtil;
import com.ly.rhdfs.communicate.command.*;
import com.ly.rhdfs.manager.server.ServerManager;

@Component
public class DFSCommandParse {

    public static final byte[] DC_HEAD = new byte[] { (byte) 0xFE, (byte) 0xFA, (byte) 0x8A, (byte) 0xCF };

    private ServerManager serverManager;

    {
        serverManager = SpringContextUtil.getBean(ServerManager.class);
    }

    public DFSCommand parse(ByteBuf byteBuf) {
        if (byteBuf.readableBytes() <= 8) {
            return null;
        }
        byteBuf.markReaderIndex();
        byte[] head = new byte[4];
        byteBuf.readBytes(head);
        if (!Arrays.equals(head, DC_HEAD)) {
            return null;
        }
        // 解析包长，所有数据包为head+包长+content，可变内容在最后
        int length = byteBuf.readInt();
        if (byteBuf.readableBytes() < length) {
            byteBuf.resetReaderIndex();
            return null;
        }

        // read commandType
        int commandType = byteBuf.readInt();
        DFSCommand dfsCommand = newDFSCommand(commandType);
        dfsCommand.setServerId(byteBuf.readInt());
        dfsCommand.setTimestamp(byteBuf.readLong());
        dfsCommand.setReply(byteBuf.readByte());
        long mostSigBits=byteBuf.readLong();
        long leastSigBits=byteBuf.readLong();
        dfsCommand.setUuid(new UUID(mostSigBits,leastSigBits));
        dfsCommand.setLength(length);

        switch (commandType) {
            case DFSCommand.CT_FILE_INFO:
                return parseFileInfo(byteBuf, dfsCommand);
            case DFSCommand.CT_FILE_DELETE:
                return parseFileDelete(byteBuf, dfsCommand);
            case DFSCommand.CT_FILE_CHUNK:
                return parseChunkInfo(byteBuf, dfsCommand);
            case DFSCommand.CT_FILE_OPERATE:
                return parseFileOperate(byteBuf, dfsCommand);
            case DFSCommand.CT_FILE_TRANSFER:
                return parseFileTransfer(byteBuf, dfsCommand);
            case DFSCommand.CT_STATE:
                return parseState(byteBuf, dfsCommand);
            case DFSCommand.CT_SERVER_ADDRESS:
                return parseServerAddress(byteBuf, dfsCommand);
            case DFSCommand.CT_TOKEN:
                return parseToken(byteBuf, dfsCommand);
            case DFSCommand.CT_DIRECT_FILE_ITEM:
                return parseDirectFileItems(byteBuf, dfsCommand);
            case DFSCommand.CT_FILE_TRANSFER_STATE:
                return parseFileTransferState(byteBuf, dfsCommand);
            default:
                return parseExpand(byteBuf, dfsCommand);
        }
    }

    private DFSCommand newDFSCommand(int commandType) {
        switch (commandType) {
            case DFSCommand.CT_FILE_INFO:
                return new DFSCommandFileInfo();
            case DFSCommand.CT_FILE_CHUNK:
                return new DFSCommandChunkInfo();
            case DFSCommand.CT_FILE_OPERATE:
                return new DFSCommandFileOperate();
            case DFSCommand.CT_FILE_TRANSFER:
                return new DFSCommandFileTransfer();
            case DFSCommand.CT_STATE:
                return new DFSCommandState();
            case DFSCommand.CT_SERVER_ADDRESS:
                return new DFSCommandServerAddress();
            case DFSCommand.CT_TOKEN:
                return new DFSCommandToken();
            case DFSCommand.CT_DIRECT_FILE_ITEM:
                return new DFSCommandDirectFileItems();
            case DFSCommand.CT_FILE_TRANSFER_STATE:
                return new DFSCommandFileTransferState();
            default:
                return new DFSCommandExpand();
        }
    }

    public DFSCommandFileInfo parseFileInfo(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileInfo)) {
            return null;
        }
        DFSCommandFileInfo dfsCommandFileInfo = (DFSCommandFileInfo) dfsCommand;
        byte[] bytes = new byte[dfsCommandFileInfo.getLength() - dfsCommandFileInfo.getFixLength()];
        byteBuf.readBytes(bytes);
        String fileInfoStr = new String(bytes);
        FileInfo fileInfo = JSON.parseObject(fileInfoStr, FileInfo.class);
        dfsCommandFileInfo.setFileInfo(fileInfo);
        return dfsCommandFileInfo;
    }
    public DFSCommandFileDelete parseFileDelete(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileDelete)) {
            return null;
        }
        DFSCommandFileDelete dfsCommandFileDelete = (DFSCommandFileDelete) dfsCommand;
        byte[] bytes = new byte[dfsCommandFileDelete.getLength() - dfsCommandFileDelete.getFixLength()];
        byteBuf.readBytes(bytes);
        String fileInfoStr = new String(bytes);
        FileDelete fileDelete = JSON.parseObject(fileInfoStr, FileDelete.class);
        dfsCommandFileDelete.setFileDelete(fileDelete);
        return dfsCommandFileDelete;
    }
    public DFSCommandChunkInfo parseChunkInfo(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandChunkInfo)) {
            return null;
        }
        DFSCommandChunkInfo dfsCommandChunkInfo = (DFSCommandChunkInfo) dfsCommand;
        byte[] bytes = new byte[dfsCommandChunkInfo.getLength() - dfsCommandChunkInfo.getFixLength()];
        byteBuf.readBytes(bytes);
        String chunkInfoStr = new String(bytes);
        ChunkInfo chunkInfo = JSON.parseObject(chunkInfoStr, ChunkInfo.class);
        dfsCommandChunkInfo.setChunkInfo(chunkInfo);
        return dfsCommandChunkInfo;
    }

    public DFSCommandFileOperate parseFileOperate(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileOperate)) {
            return null;
        }
        DFSCommandFileOperate dfsCommandFileOperate = (DFSCommandFileOperate) dfsCommand;
        byte[] bytes = new byte[dfsCommandFileOperate.getLength() - dfsCommandFileOperate.getFixLength()];
        byteBuf.readBytes(bytes);
        String fileOperateStr = new String(bytes);
        OperationLog operationLog = JSON.parseObject(fileOperateStr, OperationLog.class);
        dfsCommandFileOperate.setOperationLog(operationLog);
        return dfsCommandFileOperate;
    }

    public DFSCommandFileTransfer parseFileTransfer(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileTransfer)) {
            return null;
        }
        DFSCommandFileTransfer dfsCommandFileTransfer = (DFSCommandFileTransfer) dfsCommand;
        FileTransferInfo fileTransferInfo = new FileTransferInfo();
        dfsCommandFileTransfer.setFileTransferInfo(fileTransferInfo);
        fileTransferInfo.setPathLength(byteBuf.readShort());
        fileTransferInfo.setFileNameLength(byteBuf.readShort());
        fileTransferInfo.setEtagLength(byteBuf.readShort());
        fileTransferInfo.setChunkIndex(byteBuf.readInt());
        fileTransferInfo.setChunkSize(byteBuf.readInt());
        fileTransferInfo.setChunkCount(byteBuf.readInt());
        fileTransferInfo.setChunkPieceIndex(byteBuf.readInt());
        fileTransferInfo.setChunkPieceSize(byteBuf.readInt());
        fileTransferInfo.setChunkPieceCount(byteBuf.readInt());
        fileTransferInfo.setStartPos(byteBuf.readInt());
        fileTransferInfo.setPackageLength(byteBuf.readInt());
        byte[] bytes = new byte[fileTransferInfo.getPathLength()];
        byteBuf.readBytes(bytes);
        fileTransferInfo.setPath(new String((bytes)));
        bytes = new byte[fileTransferInfo.getFileNameLength()];
        byteBuf.readBytes(bytes);
        fileTransferInfo.setFileName(new String((bytes)));
        bytes = new byte[fileTransferInfo.getEtagLength()];
        byteBuf.readBytes(bytes);
        fileTransferInfo.setEtag(new String(bytes));
        fileTransferInfo.setSize(dfsCommandFileTransfer.getLength() - dfsCommandFileTransfer.getFixLength()
                - fileTransferInfo.getPathLength() - fileTransferInfo.getFileNameLength());
        fileTransferInfo.setByteBuf(byteBuf.retainedSlice(byteBuf.readerIndex(), fileTransferInfo.getSize()));
        return dfsCommandFileTransfer;
    }

    public DFSCommandFileTransferState parseFileTransferState(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandFileTransfer)) {
            return null;
        }
        DFSCommandFileTransferState dfsCommandFileTransferState = (DFSCommandFileTransferState) dfsCommand;
        FileTransferState fileTransferState = new FileTransferState();
        dfsCommandFileTransferState.setFileTransferState(fileTransferState);
        fileTransferState.setPathLength(byteBuf.readShort());
        fileTransferState.setFileNameLength(byteBuf.readShort());
        fileTransferState.setEtagLength(byteBuf.readShort());
        fileTransferState.setChunkIndex(byteBuf.readInt());
        fileTransferState.setChunkSize(byteBuf.readInt());
        fileTransferState.setChunkCount(byteBuf.readInt());
        fileTransferState.setChunkPieceIndex(byteBuf.readInt());
        fileTransferState.setChunkPieceSize(byteBuf.readInt());
        fileTransferState.setChunkPieceCount(byteBuf.readInt());
        fileTransferState.setStartPos(byteBuf.readInt());
        fileTransferState.setPackageLength(byteBuf.readInt());
        fileTransferState.setState(byteBuf.readInt());
        byte[] bytes = new byte[fileTransferState.getPathLength()];
        byteBuf.readBytes(bytes);
        fileTransferState.setPath(new String((bytes)));
        bytes = new byte[fileTransferState.getFileNameLength()];
        byteBuf.readBytes(bytes);
        fileTransferState.setFileName(new String((bytes)));
        bytes = new byte[fileTransferState.getEtagLength()];
        byteBuf.readBytes(bytes);
        fileTransferState.setEtag(new String(bytes));

        return dfsCommandFileTransferState;
    }

    public DFSCommandState parseState(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandState)) {
            return null;
        }
        DFSCommandState dfsCommandState = (DFSCommandState) dfsCommand;
        byte[] bytes = new byte[dfsCommandState.getLength() - dfsCommandState.getFixLength()];
        byteBuf.readBytes(bytes);
        String serverStateStr = new String(bytes);
        ServerState serverState = JSON.parseObject(serverStateStr, ServerState.class);
        dfsCommandState.setServerState(serverState);
        return dfsCommandState;
    }

    public DFSCommandServerAddress parseServerAddress(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandServerAddress)) {
            return null;
        }
        DFSCommandServerAddress dfsCommandServerAddress = (DFSCommandServerAddress) dfsCommand;
        byte[] bytes = new byte[dfsCommandServerAddress.getLength() - dfsCommandServerAddress.getFixLength()];
        byteBuf.readBytes(bytes);
        String serverStateStr = new String(bytes);
        List<ServerInfoConfiguration> serverInfoConfigurationList = JSON.parseObject(serverStateStr,
                new TypeReference<>() {
                });
        dfsCommandServerAddress.setServerInfoConfigurations(serverInfoConfigurationList);
        return dfsCommandServerAddress;
    }

    public DFSCommandToken parseToken(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandToken)) {
            return null;
        }
        DFSCommandToken dfsCommandToken = (DFSCommandToken) dfsCommand;
        byte[] bytes = new byte[dfsCommandToken.getLength() - dfsCommandToken.getFixLength()];
        byteBuf.readBytes(bytes);
        String tokenStr = new String(bytes);
        TokenInfo tokenInfo = JSON.parseObject(tokenStr, TokenInfo.class);
        dfsCommandToken.setTokenInfo(tokenInfo);
        return dfsCommandToken;
    }

    public DFSCommandDirectFileItems parseDirectFileItems(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandDirectFileItems)) {
            return null;
        }
        DFSCommandDirectFileItems dfsCommandDirectFileItems = (DFSCommandDirectFileItems) dfsCommand;
        byte[] bytes = new byte[dfsCommandDirectFileItems.getLength() - dfsCommandDirectFileItems.getFixLength()];
        byteBuf.readBytes(bytes);
        String fileItemsStr = new String(bytes);
        DirectInfo directInfo = JSON.parseObject(fileItemsStr, DirectInfo.class);
        dfsCommandDirectFileItems.setDirectInfo(directInfo);
        return dfsCommandDirectFileItems;
    }

    public DFSCommandExpand parseExpand(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandExpand)) {
            return null;
        }
        DFSCommandExpand dfsCommandExpand = (DFSCommandExpand) dfsCommand;
        dfsCommandExpand.setBytes(byteBuf.slice(byteBuf.readerIndex(),
                dfsCommandExpand.getLength() - dfsCommandExpand.getFixLength()).array());
        return dfsCommandExpand;
    }

    public ByteBuf packageCommand(DFSCommand dfsCommand) {
        if (dfsCommand instanceof DFSCommandFileInfo) {
            return packageCommandFileInfo((DFSCommandFileInfo) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandFileDelete) {
            return packageCommandFileDelete((DFSCommandFileDelete) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandChunkInfo) {
            return packageCommandChunkInfo((DFSCommandChunkInfo) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandDirectFileItems) {
            return packageCommandDirectInfo((DFSCommandDirectFileItems) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandFileOperate) {
            return packageCommandFileOperate((DFSCommandFileOperate) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandState) {
            return packageCommandState((DFSCommandState) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandServerAddress) {
            return packageCommandServerAddress((DFSCommandServerAddress) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandToken) {
            return packageCommandToken((DFSCommandToken) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandFileTransfer) {
            return packageCommandFileTransfer((DFSCommandFileTransfer) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandFileTransferState) {
            return packageCommandFileTransferState((DFSCommandFileTransferState) dfsCommand);
        } else {
            return null;
        }
    }

    public DFSCommand convertCommandObject(Object commandObj) {
        if (commandObj instanceof FileInfo) {
            return convertCommandFileInfo((FileInfo) commandObj);
        } else if (commandObj instanceof FileDelete) {
            return convertCommandFileDelete((FileDelete) commandObj);
        } else if (commandObj instanceof ChunkInfo) {
            return convertCommandChunkInfo((ChunkInfo) commandObj);
        } else if (commandObj instanceof DirectInfo) {
            return convertCommandDirectInfo((DirectInfo) commandObj);
        } else if (commandObj instanceof OperationLog) {
            return convertCommandFileOperate((OperationLog) commandObj);
        } else if (commandObj instanceof ServerState) {
            return convertCommandState((ServerState) commandObj);
        } else if (commandObj instanceof List) {
            List<?> objList = (List<?>) commandObj;
            if (!objList.isEmpty()) {
                Object obj1 = objList.get(1);
                if (obj1 instanceof ServerInfoConfiguration) {
                    return convertCommandServerAddress((List<ServerInfoConfiguration>) commandObj);
                }
            }
            return null;
        } else if (commandObj instanceof TokenInfo) {
            return convertCommandToken((TokenInfo) commandObj);
        } else if (commandObj instanceof FileTransferInfo) {
            return convertCommandFileTransfer((FileTransferInfo) commandObj);
        } else if (commandObj instanceof FileTransferState) {
            return convertCommandFileTransferState((FileTransferState) commandObj);
        } else if (commandObj instanceof byte[]) {
            return convertCommandExpand((byte[]) commandObj);
        } else {
            return null;
        }
    }

    private void packageCommandHeader(ByteBuf byteBuf, DFSCommand dfsCommand) {
        if (byteBuf == null || dfsCommand == null)
            return;
        byteBuf.writeBytes(DC_HEAD);
        byteBuf.writeInt(dfsCommand.getLength());
        byteBuf.writeInt(dfsCommand.getCommandType());
        byteBuf.writeLong(dfsCommand.getServerId());
        byteBuf.writeLong(dfsCommand.getTimestamp());
        byteBuf.writeByte(dfsCommand.getReply());
        byteBuf.writeLong(dfsCommand.getMostSigBits());
        byteBuf.writeLong(dfsCommand.getLeastSigBits());
    }

    public DFSCommand convertCommandFileInfo(FileInfo fileInfo) {
        if (fileInfo == null)
            return null;
        DFSCommandFileInfo dfsCommandFileInfo = new DFSCommandFileInfo();
        dfsCommandFileInfo.setServerId(serverManager.getLocalServerId());
        dfsCommandFileInfo.setFileInfo(fileInfo);
        dfsCommandFileInfo.setTimestamp(Instant.now().toEpochMilli());
        return dfsCommandFileInfo;
    }

    public ByteBuf packageCommandFileInfo(DFSCommandFileInfo dfsCommandFileInfo) {
        if (dfsCommandFileInfo == null || dfsCommandFileInfo.getFileInfo() == null)
            return null;
        byte[] bytes = JSON.toJSONString(dfsCommandFileInfo.getFileInfo()).getBytes();
        dfsCommandFileInfo.setLength(dfsCommandFileInfo.getFixLength() + bytes.length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(dfsCommandFileInfo.getLength() + 8);
        packageCommandHeader(byteBuf, dfsCommandFileInfo);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

    public DFSCommand convertCommandFileDelete(FileDelete fileDelte) {
        if (fileDelte == null)
            return null;
        DFSCommandFileDelete dfsCommandFileDelete = new DFSCommandFileDelete();
        dfsCommandFileDelete.setServerId(serverManager.getLocalServerId());
        dfsCommandFileDelete.setFileDelete(fileDelte);
        dfsCommandFileDelete.setTimestamp(Instant.now().toEpochMilli());
        return dfsCommandFileDelete;
    }

    public ByteBuf packageCommandFileDelete(DFSCommandFileDelete dfsCommandFileDelete) {
        if (dfsCommandFileDelete == null || dfsCommandFileDelete.getFileDelete() == null)
            return null;
        byte[] bytes = JSON.toJSONString(dfsCommandFileDelete.getFileDelete()).getBytes();
        dfsCommandFileDelete.setLength(dfsCommandFileDelete.getFixLength() + bytes.length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(dfsCommandFileDelete.getLength() + 8);
        packageCommandHeader(byteBuf, dfsCommandFileDelete);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }
    public DFSCommand convertCommandChunkInfo(ChunkInfo chunkInfo) {
        if (chunkInfo == null)
            return null;
        DFSCommandChunkInfo dfsCommandChunkInfo = new DFSCommandChunkInfo();
        dfsCommandChunkInfo.setServerId(serverManager.getLocalServerId());
        dfsCommandChunkInfo.setChunkInfo(chunkInfo);
        dfsCommandChunkInfo.setTimestamp(Instant.now().toEpochMilli());
        return dfsCommandChunkInfo;
    }

    public ByteBuf packageCommandChunkInfo(DFSCommandChunkInfo dfsCommandChunkInfo) {
        if (dfsCommandChunkInfo == null || dfsCommandChunkInfo.getChunkInfo() == null)
            return null;
        byte[] bytes = JSON.toJSONString(dfsCommandChunkInfo.getChunkInfo()).getBytes();
        dfsCommandChunkInfo.setLength(dfsCommandChunkInfo.getFixLength() + bytes.length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(dfsCommandChunkInfo.getLength() + 8);
        packageCommandHeader(byteBuf, dfsCommandChunkInfo);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

    public DFSCommand convertCommandFileOperate(OperationLog operationLog) {
        if (operationLog == null)
            return null;
        DFSCommandFileOperate dfsCommandFileOperate = new DFSCommandFileOperate();
        dfsCommandFileOperate.setServerId(serverManager.getLocalServerId());
        dfsCommandFileOperate.setOperationLog(operationLog);
        dfsCommandFileOperate.setTimestamp(Instant.now().toEpochMilli());
        return dfsCommandFileOperate;
    }

    public ByteBuf packageCommandFileOperate(DFSCommandFileOperate dfsCommandFileOperate) {
        if (dfsCommandFileOperate == null || dfsCommandFileOperate.getOperationLog() == null)
            return null;
        byte[] bytes = JSON.toJSONString(dfsCommandFileOperate.getOperationLog()).getBytes();
        dfsCommandFileOperate.setLength(dfsCommandFileOperate.getFixLength() + bytes.length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(dfsCommandFileOperate.getLength() + 8);
        packageCommandHeader(byteBuf, dfsCommandFileOperate);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

    public DFSCommand convertCommandDirectInfo(DirectInfo directInfo) {
        if (directInfo == null)
            return null;
        DFSCommandDirectFileItems dfsCommandDirectFileItems = new DFSCommandDirectFileItems();
        dfsCommandDirectFileItems.setServerId(serverManager.getLocalServerId());
        dfsCommandDirectFileItems.setDirectInfo(directInfo);
        dfsCommandDirectFileItems.setTimestamp(Instant.now().toEpochMilli());
        return dfsCommandDirectFileItems;
    }

    public ByteBuf packageCommandDirectInfo(DFSCommandDirectFileItems dfsCommandDirectFileItems) {
        if (dfsCommandDirectFileItems == null || dfsCommandDirectFileItems.getDirectInfo() == null)
            return null;
        byte[] bytes = JSON.toJSONString(dfsCommandDirectFileItems.getDirectInfo()).getBytes();
        dfsCommandDirectFileItems.setLength(dfsCommandDirectFileItems.getFixLength() + bytes.length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(dfsCommandDirectFileItems.getLength() + 8);
        packageCommandHeader(byteBuf, dfsCommandDirectFileItems);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

    public DFSCommand convertCommandState(ServerState serverState) {
        if (serverState == null)
            return null;
        DFSCommandState dfsCommandState = new DFSCommandState();
        dfsCommandState.setServerId(serverManager.getLocalServerId());
        dfsCommandState.setServerState(serverState);
        dfsCommandState.setTimestamp(Instant.now().toEpochMilli());
        return dfsCommandState;
    }

    public ByteBuf packageCommandState(DFSCommandState dfsCommandState) {
        if (dfsCommandState == null || dfsCommandState.getServerState() == null)
            return null;
        byte[] bytes = JSON.toJSONString(dfsCommandState.getServerState()).getBytes();
        dfsCommandState.setLength(dfsCommandState.getFixLength() + bytes.length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(dfsCommandState.getLength() + 8);
        packageCommandHeader(byteBuf, dfsCommandState);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

    public DFSCommand convertCommandServerAddress(List<ServerInfoConfiguration> serverInfoConfigurations) {
        if (serverInfoConfigurations == null || serverInfoConfigurations.isEmpty())
            return null;
        DFSCommandServerAddress dfsCommandServerAddress = new DFSCommandServerAddress();
        dfsCommandServerAddress.setServerId(serverManager.getLocalServerId());
        dfsCommandServerAddress.setServerInfoConfigurations(serverInfoConfigurations);
        dfsCommandServerAddress.setTimestamp(Instant.now().toEpochMilli());
        return dfsCommandServerAddress;
    }

    public ByteBuf packageCommandServerAddress(DFSCommandServerAddress dfsCommandServerAddress) {
        if (dfsCommandServerAddress == null || dfsCommandServerAddress.getServerInfoConfigurations() == null
                || dfsCommandServerAddress.getServerInfoConfigurations().isEmpty())
            return null;
        byte[] bytes = JSON.toJSONString(dfsCommandServerAddress.getServerInfoConfigurations()).getBytes();
        dfsCommandServerAddress.setLength(dfsCommandServerAddress.getFixLength() + bytes.length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(dfsCommandServerAddress.getLength() + 8);
        packageCommandHeader(byteBuf, dfsCommandServerAddress);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

    public DFSCommand convertCommandToken(TokenInfo tokenInfo) {
        if (tokenInfo == null)
            return null;
        DFSCommandToken dfsCommandToken = new DFSCommandToken();
        dfsCommandToken.setServerId(serverManager.getLocalServerId());
        dfsCommandToken.setTokenInfo(tokenInfo);
        dfsCommandToken.setTimestamp(Instant.now().toEpochMilli());
        return dfsCommandToken;
    }

    public ByteBuf packageCommandToken(DFSCommandToken dfsCommandToken) {
        if (dfsCommandToken == null || dfsCommandToken.getTokenInfo() == null)
            return null;
        byte[] bytes = JSON.toJSONString(dfsCommandToken.getTokenInfo()).getBytes();
        dfsCommandToken.setLength(dfsCommandToken.getFixLength() + bytes.length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(dfsCommandToken.getLength() + 8);
        packageCommandHeader(byteBuf, dfsCommandToken);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

    public DFSCommand convertCommandExpand(byte[] bytes) {
        if (bytes == null)
            return null;
        DFSCommandExpand dfsCommandExpand = new DFSCommandExpand();
        dfsCommandExpand.setServerId(serverManager.getLocalServerId());
        dfsCommandExpand.setBytes(bytes);
        dfsCommandExpand.setTimestamp(Instant.now().toEpochMilli());
        return dfsCommandExpand;
    }

    public ByteBuf packageCommandExpand(DFSCommandExpand dfsCommandExpand) {
        if (dfsCommandExpand == null || dfsCommandExpand.getBytes() == null)
            return null;
        dfsCommandExpand.setLength(dfsCommandExpand.getFixLength() + dfsCommandExpand.getBytes().length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(dfsCommandExpand.getLength() + 8);
        packageCommandHeader(byteBuf, dfsCommandExpand);
        byteBuf.writeBytes(dfsCommandExpand.getBytes());
        return byteBuf;
    }

    public ByteBuf convertDataBuffer2ByteBuf(DataBuffer dataBuffer){
        if(dataBuffer==null)
            return null;
        return NettyDataBufferFactory.toByteBuf(dataBuffer);
    }

    public DFSCommand convertCommandFileTransfer(FileTransferInfo fileTransferInfo) {
        if (fileTransferInfo == null)
            return null;
        DFSCommandFileTransfer dfsCommandFileTransfer = new DFSCommandFileTransfer();
        dfsCommandFileTransfer.setServerId(serverManager.getLocalServerId());
        dfsCommandFileTransfer.setFileTransferInfo(fileTransferInfo);
        dfsCommandFileTransfer.setTimestamp(Instant.now().toEpochMilli());
        return dfsCommandFileTransfer;
    }

    public ByteBuf packageCommandFileTransfer(DFSCommandFileTransfer dfsCommandFileTransfer) {
        if (dfsCommandFileTransfer == null || dfsCommandFileTransfer.getFileTransferInfo() == null)
            return null;
        byte[] pathBytes = dfsCommandFileTransfer.getFileTransferInfo().getPath().getBytes();
        byte[] fileNameBytes = dfsCommandFileTransfer.getFileTransferInfo().getFileName().getBytes();
        byte[] etagBytes = dfsCommandFileTransfer.getFileTransferInfo().getEtag().getBytes();
        dfsCommandFileTransfer.getFileTransferInfo().setPathLength((short) pathBytes.length);
        dfsCommandFileTransfer.getFileTransferInfo().setFileNameLength((short) fileNameBytes.length);
        int length = dfsCommandFileTransfer.getFixLength()
                + dfsCommandFileTransfer.getFileTransferInfo().getFileNameLength()
                + dfsCommandFileTransfer.getFileTransferInfo().getPathLength()
                + dfsCommandFileTransfer.getFileTransferInfo().getEtagLength()
                + dfsCommandFileTransfer.getFileTransferInfo().getSize();
        dfsCommandFileTransfer.setLength(length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(length + 8);
        byteBuf.writeBytes(DC_HEAD);
        byteBuf.writeInt(length);
        byteBuf.writeInt(DFSCommand.CT_REQUEST_EXPAND);
        byteBuf.writeLong(serverManager.getLocalServerId());
        byteBuf.writeLong(Instant.now().toEpochMilli());
        byteBuf.writeShort(dfsCommandFileTransfer.getFileTransferInfo().getPathLength());
        byteBuf.writeShort(dfsCommandFileTransfer.getFileTransferInfo().getFileNameLength());
        byteBuf.writeShort(dfsCommandFileTransfer.getFileTransferInfo().getEtagLength());
        byteBuf.writeInt(dfsCommandFileTransfer.getFileTransferInfo().getChunkIndex());
        byteBuf.writeInt(dfsCommandFileTransfer.getFileTransferInfo().getChunkSize());
        byteBuf.writeInt(dfsCommandFileTransfer.getFileTransferInfo().getChunkCount());
        byteBuf.writeInt(dfsCommandFileTransfer.getFileTransferInfo().getChunkPieceIndex());
        byteBuf.writeInt(dfsCommandFileTransfer.getFileTransferInfo().getChunkPieceSize());
        byteBuf.writeInt(dfsCommandFileTransfer.getFileTransferInfo().getChunkPieceCount());
        byteBuf.writeInt(dfsCommandFileTransfer.getFileTransferInfo().getStartPos());
        byteBuf.writeInt(dfsCommandFileTransfer.getFileTransferInfo().getPackageLength());
        byteBuf.writeBytes(pathBytes);
        byteBuf.writeBytes(fileNameBytes);
        byteBuf.writeBytes(etagBytes);
        byteBuf.writeBytes(dfsCommandFileTransfer.getFileTransferInfo().getByteBuf());
        CompositeByteBuf resByteBuf=PooledByteBufAllocator.DEFAULT.compositeBuffer();
        resByteBuf.addComponent(true,byteBuf);
        resByteBuf.addComponent(true,dfsCommandFileTransfer.getFileTransferInfo().getByteBuf());
        return resByteBuf;
    }
    public DFSCommand convertCommandFileTransferState(FileTransferState fileTransferState) {
        if (fileTransferState == null)
            return null;
        DFSCommandFileTransferState dfsCommandFileTransferState = new DFSCommandFileTransferState();
        dfsCommandFileTransferState.setServerId(serverManager.getLocalServerId());
        dfsCommandFileTransferState.setFileTransferState(fileTransferState);
        dfsCommandFileTransferState.setTimestamp(Instant.now().toEpochMilli());
        return dfsCommandFileTransferState;
    }

    public ByteBuf packageCommandFileTransferState(DFSCommandFileTransferState dfsCommandFileTransferState) {
        if (dfsCommandFileTransferState == null || dfsCommandFileTransferState.getFileTransferState() == null)
            return null;
        byte[] pathBytes = dfsCommandFileTransferState.getFileTransferState().getPath().getBytes();
        byte[] fileNameBytes = dfsCommandFileTransferState.getFileTransferState().getFileName().getBytes();
        byte[] etagBytes = dfsCommandFileTransferState.getFileTransferState().getEtag().getBytes();
        dfsCommandFileTransferState.getFileTransferState().setPathLength((short) pathBytes.length);
        dfsCommandFileTransferState.getFileTransferState().setFileNameLength((short) fileNameBytes.length);
        int length = dfsCommandFileTransferState.getFixLength()
                + dfsCommandFileTransferState.getFileTransferState().getFileNameLength()
                + dfsCommandFileTransferState.getFileTransferState().getPathLength()
                + dfsCommandFileTransferState.getFileTransferState().getEtagLength();
        dfsCommandFileTransferState.setLength(length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(length + 8);
        byteBuf.writeBytes(DC_HEAD);
        byteBuf.writeInt(length);
        byteBuf.writeInt(DFSCommand.CT_REQUEST_EXPAND);
        byteBuf.writeLong(serverManager.getLocalServerId());
        byteBuf.writeLong(Instant.now().toEpochMilli());
        byteBuf.writeShort(dfsCommandFileTransferState.getFileTransferState().getPathLength());
        byteBuf.writeShort(dfsCommandFileTransferState.getFileTransferState().getFileNameLength());
        byteBuf.writeShort(dfsCommandFileTransferState.getFileTransferState().getEtagLength());
        byteBuf.writeInt(dfsCommandFileTransferState.getFileTransferState().getChunkIndex());
        byteBuf.writeInt(dfsCommandFileTransferState.getFileTransferState().getChunkSize());
        byteBuf.writeInt(dfsCommandFileTransferState.getFileTransferState().getChunkCount());
        byteBuf.writeInt(dfsCommandFileTransferState.getFileTransferState().getChunkPieceIndex());
        byteBuf.writeInt(dfsCommandFileTransferState.getFileTransferState().getChunkPieceSize());
        byteBuf.writeInt(dfsCommandFileTransferState.getFileTransferState().getChunkPieceCount());
        byteBuf.writeInt(dfsCommandFileTransferState.getFileTransferState().getStartPos());
        byteBuf.writeInt(dfsCommandFileTransferState.getFileTransferState().getPackageLength());
        byteBuf.writeBytes(pathBytes);
        byteBuf.writeBytes(fileNameBytes);
        byteBuf.writeBytes(etagBytes);
        byteBuf.writeInt(dfsCommandFileTransferState.getFileTransferState().getState());
        return byteBuf;
    }
    // fileTransfer 需要使用组合bytebuf拼接基本信息和databuffer的文件信息
    // file finish,chunk finish,chunk server update
}
