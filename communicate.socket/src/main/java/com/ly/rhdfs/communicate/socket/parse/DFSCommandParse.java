package com.ly.rhdfs.communicate.socket.parse;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Arrays;

import com.alibaba.fastjson.JSON;
import com.ly.common.domain.file.DirectInfo;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.file.FileTransferInfo;
import com.ly.common.domain.server.ServerState;
import com.ly.common.domain.token.TokenInfo;
import com.ly.common.util.SpringContextUtil;
import com.ly.rhdfs.communicate.command.*;
import com.ly.rhdfs.manager.server.ServerManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

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
            case DFSCommand.CT_FILE_TRANSFER:
                return parseFileTransfer(byteBuf, dfsCommand);
            case DFSCommand.CT_STATE:
                return parseState(byteBuf, dfsCommand);
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
            case DFSCommand.CT_FILE_TRANSFER:
                return new DFSCommandFileTransfer();
            case DFSCommand.CT_STATE:
                return new DFSCommandState();
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
        } else if (dfsCommand instanceof DFSCommandDirectFileItems) {
            return packageCommandDirectInfo((DFSCommandDirectFileItems) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandState) {
            return packageCommandState((DFSCommandState) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandToken) {
            return packageCommandToken((DFSCommandToken) dfsCommand);
        } else if (dfsCommand instanceof DFSCommandFileTransfer) {
            return packageCommandFileTransfer((DFSCommandFileTransfer) dfsCommand);
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

    public ByteBuf packageCommandFileTransfer(DFSCommandFileTransfer dfsCommandFileTransfer) {
        if (dfsCommandFileTransfer == null || dfsCommandFileTransfer.getFileTransferInfo() == null)
            return null;
        byte[] pathBytes=dfsCommandFileTransfer.getFileTransferInfo().getPath().getBytes();
        byte[] fileNameBytes=dfsCommandFileTransfer.getFileTransferInfo().getFileName().getBytes();
        dfsCommandFileTransfer.getFileTransferInfo().setPathLength((short)pathBytes.length);
        dfsCommandFileTransfer.getFileTransferInfo().setFileNameLength((short)fileNameBytes.length);
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
}
