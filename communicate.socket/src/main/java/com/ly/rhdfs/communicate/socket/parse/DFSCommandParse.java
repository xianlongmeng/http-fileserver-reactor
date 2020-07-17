package com.ly.rhdfs.communicate.socket.parse;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ly.common.domain.file.ChunkInfo;
import com.ly.common.domain.file.DirectInfo;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.file.FileTransferInfo;
import com.ly.common.domain.log.OperationLog;
import com.ly.common.domain.server.ServerInfoConfiguration;
import com.ly.common.domain.server.ServerState;
import com.ly.common.domain.token.TokenInfo;
import com.ly.common.util.SpringContextUtil;
import com.ly.rhdfs.communicate.command.*;
import com.ly.rhdfs.manager.server.ServerManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

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
        // read serverId
        int serverId = byteBuf.readInt();
        // read timestamp
        long timestamp = byteBuf.readLong();
        dfsCommand.setServerId(serverId);
        dfsCommand.setTimestamp(timestamp);
        dfsCommand.setLength(length);

        switch (commandType) {
            case DFSCommand.CT_FILE_INFO:
                return parseFileInfo(byteBuf, dfsCommand);
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
        fileTransferInfo.setChunk(byteBuf.readInt());
        fileTransferInfo.setChunkSize(byteBuf.readInt());
        fileTransferInfo.setChunkCount(byteBuf.readInt());
        byte[] bytes = new byte[fileTransferInfo.getPathLength()];
        byteBuf.readBytes(bytes);
        fileTransferInfo.setPath(new String((bytes)));
        bytes = new byte[fileTransferInfo.getFileNameLength()];
        byteBuf.readBytes(bytes);
        fileTransferInfo.setFileName(new String((bytes)));
        fileTransferInfo.setSize(dfsCommandFileTransfer.getLength() - dfsCommandFileTransfer.getFixLength()
                - fileTransferInfo.getPathLength() - fileTransferInfo.getFileNameLength());
        dfsCommandFileTransfer.setByteBuf(byteBuf.retainedSlice(byteBuf.readerIndex(), fileTransferInfo.getSize()));
        return dfsCommandFileTransfer;
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
        dfsCommandExpand.setByteBuf(byteBuf.retainedSlice(byteBuf.readerIndex(),
                dfsCommandExpand.getLength() - dfsCommandExpand.getFixLength()));
        return dfsCommandExpand;
    }

    public ByteBuf packageCommand(DFSCommand dfsCommand) {
        if (dfsCommand instanceof DFSCommandFileInfo) {
            return packageCommandFileInfo((DFSCommandFileInfo) dfsCommand);
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
        } else {
            return null;
        }
    }

    public ByteBuf packageCommandObject(Object commandObj) {
        if (commandObj instanceof FileInfo) {
            return packageCommandFileInfo((FileInfo) commandObj);
        } else if (commandObj instanceof ChunkInfo) {
            return packageCommandChunkInfo((ChunkInfo) commandObj);
        } else if (commandObj instanceof DirectInfo) {
            return packageCommandDirectInfo((DirectInfo) commandObj);
        } else if (commandObj instanceof OperationLog) {
            return packageCommandFileOperate((OperationLog) commandObj);
        } else if (commandObj instanceof ServerState) {
            return packageCommandState((ServerState) commandObj);
        } else if (commandObj instanceof List) {
            List<?> objList = (List<?>) commandObj;
            if (!objList.isEmpty()) {
                Object obj1 = objList.get(1);
                if (obj1 instanceof ServerInfoConfiguration) {
                    return packageCommandServerAddress((List<ServerInfoConfiguration>) commandObj);
                }
            }
            return null;
        } else if (commandObj instanceof TokenInfo) {
            return packageCommandToken((TokenInfo) commandObj);
        } else if (commandObj instanceof byte[]) {
            return packageCommandExpand((byte[]) commandObj);
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
        byteBuf.writeInt(dfsCommand.getServerId());
        byteBuf.writeLong(dfsCommand.getTimestamp());
    }

    public ByteBuf packageCommandFileInfo(FileInfo fileInfo) {
        if (fileInfo == null)
            return null;
        DFSCommandFileInfo dfsCommandFileInfo = new DFSCommandFileInfo();
        dfsCommandFileInfo.setServerId(serverManager.getLocalServerId());
        dfsCommandFileInfo.setFileInfo(fileInfo);
        dfsCommandFileInfo.setTimestamp(Instant.now().toEpochMilli());
        return packageCommandFileInfo(dfsCommandFileInfo);
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

    public ByteBuf packageCommandChunkInfo(ChunkInfo chunkInfo) {
        if (chunkInfo == null)
            return null;
        DFSCommandChunkInfo dfsCommandChunkInfo = new DFSCommandChunkInfo();
        dfsCommandChunkInfo.setServerId(serverManager.getLocalServerId());
        dfsCommandChunkInfo.setChunkInfo(chunkInfo);
        dfsCommandChunkInfo.setTimestamp(Instant.now().toEpochMilli());
        return packageCommandChunkInfo(dfsCommandChunkInfo);
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

    public ByteBuf packageCommandFileOperate(OperationLog operationLog) {
        if (operationLog == null)
            return null;
        DFSCommandFileOperate dfsCommandFileOperate = new DFSCommandFileOperate();
        dfsCommandFileOperate.setServerId(serverManager.getLocalServerId());
        dfsCommandFileOperate.setOperationLog(operationLog);
        dfsCommandFileOperate.setTimestamp(Instant.now().toEpochMilli());
        return packageCommandFileOperate(dfsCommandFileOperate);
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

    public ByteBuf packageCommandDirectInfo(DirectInfo directInfo) {
        if (directInfo == null)
            return null;
        DFSCommandDirectFileItems dfsCommandDirectFileItems = new DFSCommandDirectFileItems();
        dfsCommandDirectFileItems.setServerId(serverManager.getLocalServerId());
        dfsCommandDirectFileItems.setDirectInfo(directInfo);
        dfsCommandDirectFileItems.setTimestamp(Instant.now().toEpochMilli());
        return packageCommandDirectInfo(dfsCommandDirectFileItems);
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

    public ByteBuf packageCommandState(ServerState serverState) {
        if (serverState == null)
            return null;
        DFSCommandState dfsCommandState = new DFSCommandState();
        dfsCommandState.setServerId(serverManager.getLocalServerId());
        dfsCommandState.setServerState(serverState);
        dfsCommandState.setTimestamp(Instant.now().toEpochMilli());
        return packageCommandState(dfsCommandState);
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

    public ByteBuf packageCommandServerAddress(List<ServerInfoConfiguration> serverInfoConfigurations) {
        if (serverInfoConfigurations == null || serverInfoConfigurations.isEmpty())
            return null;
        DFSCommandServerAddress dfsCommandServerAddress = new DFSCommandServerAddress();
        dfsCommandServerAddress.setServerId(serverManager.getLocalServerId());
        dfsCommandServerAddress.setServerInfoConfigurations(serverInfoConfigurations);
        dfsCommandServerAddress.setTimestamp(Instant.now().toEpochMilli());
        return packageCommandServerAddress(dfsCommandServerAddress);
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

    public ByteBuf packageCommandToken(TokenInfo tokenInfo) {
        if (tokenInfo == null)
            return null;
        DFSCommandToken dfsCommandToken = new DFSCommandToken();
        dfsCommandToken.setServerId(serverManager.getLocalServerId());
        dfsCommandToken.setTokenInfo(tokenInfo);
        dfsCommandToken.setTimestamp(Instant.now().toEpochMilli());
        return packageCommandToken(dfsCommandToken);
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

    public ByteBuf packageCommandExpand(byte[] bytes) {
        if (bytes == null)
            return null;
        int length = 16 + bytes.length;
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(length + 8);
        byteBuf.writeBytes(DC_HEAD);
        byteBuf.writeInt(length);
        byteBuf.writeInt(DFSCommand.CT_REQUEST_EXPAND);
        byteBuf.writeInt(serverManager.getLocalServerId());
        byteBuf.writeLong(Instant.now().toEpochMilli());
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

    public ByteBuf packageCommandFileTransfer(FileTransferInfo fileTransferInfo, ByteBuf byteBuf) {
        if (fileTransferInfo == null || byteBuf == null)
            return null;
        DFSCommandFileTransfer dfsCommandFileTransfer = new DFSCommandFileTransfer();
        dfsCommandFileTransfer.setServerId(serverManager.getLocalServerId());
        dfsCommandFileTransfer.setFileTransferInfo(fileTransferInfo);
        dfsCommandFileTransfer.setByteBuf(byteBuf);
        dfsCommandFileTransfer.setTimestamp(Instant.now().toEpochMilli());
        return packageCommandFileTransfer(dfsCommandFileTransfer);
    }

    public ByteBuf packageCommandFileTransfer(DFSCommandFileTransfer dfsCommandFileTransfer) {
        if (dfsCommandFileTransfer == null || dfsCommandFileTransfer.getFileTransferInfo() == null)
            return null;
        byte[] pathBytes = dfsCommandFileTransfer.getFileTransferInfo().getPath().getBytes();
        byte[] fileNameBytes = dfsCommandFileTransfer.getFileTransferInfo().getFileName().getBytes();
        dfsCommandFileTransfer.getFileTransferInfo().setPathLength((short) pathBytes.length);
        dfsCommandFileTransfer.getFileTransferInfo().setFileNameLength((short) fileNameBytes.length);
        int length = dfsCommandFileTransfer.getFixLength()
                + dfsCommandFileTransfer.getFileTransferInfo().getFileNameLength()
                + dfsCommandFileTransfer.getFileTransferInfo().getPathLength()
                + dfsCommandFileTransfer.getFileTransferInfo().getSize();
        dfsCommandFileTransfer.setLength(length);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(length + 8);
        byteBuf.writeBytes(DC_HEAD);
        byteBuf.writeInt(length);
        byteBuf.writeInt(DFSCommand.CT_REQUEST_EXPAND);
        byteBuf.writeInt(serverManager.getLocalServerId());
        byteBuf.writeLong(Instant.now().toEpochMilli());
        byteBuf.writeShort(dfsCommandFileTransfer.getFileTransferInfo().getPathLength());
        byteBuf.writeShort(dfsCommandFileTransfer.getFileTransferInfo().getFileNameLength());
        byteBuf.writeInt(dfsCommandFileTransfer.getFileTransferInfo().getChunk());
        byteBuf.writeInt(dfsCommandFileTransfer.getFileTransferInfo().getChunkSize());
        byteBuf.writeInt(dfsCommandFileTransfer.getFileTransferInfo().getChunkCount());
        byteBuf.writeBytes(pathBytes);
        byteBuf.writeBytes(fileNameBytes);
        byteBuf.writeBytes(dfsCommandFileTransfer.getByteBuf());
        return byteBuf;
    }
    // fileTransfer 需要使用组合bytebuf拼接基本信息和databuffer的文件信息
}
