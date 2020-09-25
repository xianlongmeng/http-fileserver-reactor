package com.ly.rhdfs.store.manager;

import com.fasterxml.uuid.Generators;
import com.ly.common.constant.ParamConstants;
import com.ly.common.domain.DFSPartChunk;
import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.file.DFSBackupStoreFileChunkInfo;
import com.ly.common.domain.file.FileChunkCopy;
import com.ly.common.domain.server.ServerAddressInfo;
import com.ly.common.domain.server.ServerState;
import com.ly.common.domain.token.TokenInfo;
import com.ly.common.service.FileChunkReader;
import com.ly.rhdfs.file.util.DfsFileUtils;
import com.ly.rhdfs.communicate.command.DFSCommandFileTransfer;
import com.ly.rhdfs.communicate.exception.TransferFileException;
import com.ly.rhdfs.communicate.socket.parse.DFSCommandParse;
import com.ly.rhdfs.manager.handler.CommandEventHandler;
import com.ly.rhdfs.manager.handler.FileTransferChunkCommandEventHandler;
import com.ly.rhdfs.manager.handler.ServerStateCommandEventHandler;
import com.ly.rhdfs.manager.server.ServerManager;
import com.ly.rhdfs.store.StoreFile;
import com.ly.rhdfs.store.manager.handler.FileChunkCopyCommandEventHandler;
import com.ly.rhdfs.store.manager.handler.TokenCommandEventHandler;
import com.ly.rhdfs.store.manager.task.ComputerStateTask;
import com.ly.rhdfs.store.manager.task.ComputerVoteTask;
import com.ly.rhdfs.store.manager.task.TransferBackupTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class StoreManager extends ServerManager {

    private final int backupPeriod = 60;
    private final Map<String, TokenInfo> tokenInfoMap = new ConcurrentHashMap<>();
    private DfsFileUtils dfsFileUtils;
    private DFSCommandParse dfsCommandParse;
    private StoreFile storeFile;
    private ScheduledThreadPoolExecutor backupStoreTaskScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
            32);

    public StoreManager() {
        scheduledThreadCount = 4;
    }

    public StoreFile getStoreFile(){
        return storeFile;
    }
    @Autowired
    private void setDfsFileUtils(DfsFileUtils dfsFileUtils) {
        this.dfsFileUtils = dfsFileUtils;
    }

    @Autowired
    private void setDfsCommandParse(DFSCommandParse dfsCommandParse) {
        this.dfsCommandParse = dfsCommandParse;
    }

    @Autowired
    private void setStoreFile(StoreFile storeFile) {
        this.storeFile = storeFile;
    }

    @Override
    protected void initCommandEventHandler() {
        super.initCommandEventHandler();
        commandEventHandler.setServerStateCommandEventHandler(new ServerStateCommandEventHandler(this));
        commandEventHandler.setTokenCommandEventHandler(new TokenCommandEventHandler(this));
        commandEventHandler.setFileTransferCommandEventHandler(new FileTransferChunkCommandEventHandler(this));
        commandEventHandler.setFileChunkCopyCommandEventHandler(new FileChunkCopyCommandEventHandler(this));
    }

    public void initial() {
        commandEventHandler = new CommandEventHandler(this);
        super.initial();
        if (!ParamConstants.ST_STORE.equals(serverConfig.getServerType()))
            return;
        // 初始化监听
        connectManager.startSocketListen(serverConfig.getPort(), new CommandEventHandler(this));
        // 投票，发送到
        scheduledThreadPoolExecutor.schedule(new ComputerVoteTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
        // 定时计算配置
        scheduledThreadPoolExecutor.schedule(new ComputerStateTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
    }

    public void computerVoteMaster() {
        if (getLocalServerState().getType() != ServerState.SIT_STORE || masterServerConfig == null)
            return;
        if (masterServerId != -1) {
            ServerState masterServerState = serverInfoMap.get(masterServerId);
            if (masterServerState.isOnline()) {
                return;
            }
            if (Instant.now().toEpochMilli() - masterServerState.getLastTime() > serverConfig
                    .getStoreServerDisconnectedMasterVote()) {
                masterServerId = -1;
            }
        }
        Long sid = Long.MAX_VALUE;
        if (masterServerId == -1) {
            for (Long i : masterServerConfig.getMasterServerMap().keySet()) {
                if (i >= sid)
                    continue;
                ServerState serverState = serverInfoMap.get(i);
                if (serverState != null && serverState.isOnline()) {
                    sid = i;
                }
            }
        }
        getLocalServerState().setVotedServerId(sid);
    }

    public void computerFreeSpaceSize() {
        getLocalServerState().setSpaceSize(dfsFileUtils.diskFreeSpace(serverConfig.getFileRootPath()));
    }

    public void putTokenInfo(TokenInfo tokenInfo) {
        if (tokenInfo == null || StringUtils.isEmpty(tokenInfo.getToken()))
            return;
        tokenInfoMap.put(tokenInfo.getToken(), tokenInfo);
    }

    public void removeTokenInfo(String token) {
        if (StringUtils.isEmpty(token))
            return;
        tokenInfoMap.remove(token);
    }

    public void removeTokenInfo(TokenInfo tokenInfo) {
        if (tokenInfo == null)
            return;
        removeTokenInfo(tokenInfo.getToken());
    }

    public TokenInfo findTokenInfo(String token) {
        if (StringUtils.isEmpty(token))
            return null;
        return tokenInfoMap.get(token);
    }

    @Override
    public void clearToken(TokenInfo tokenInfo) {
        removeTokenInfo(tokenInfo);
    }

    public DFSBackupStoreFileChunkInfo newBackupChunkInfo(DFSPartChunk dfsPartChunk) {
        if (dfsPartChunk == null)
            return null;
        DFSBackupStoreFileChunkInfo dfsBackupStoreFileChunkInfo = new DFSBackupStoreFileChunkInfo();
        dfsBackupStoreFileChunkInfo.setCreateTime(Instant.now().toEpochMilli());
        dfsBackupStoreFileChunkInfo.setDfsPartChunk(dfsPartChunk);
        return dfsBackupStoreFileChunkInfo;
    }

    public void sendBackupStoreFile(Flux<DataBuffer> dataBufferFlux,
                                    DFSBackupStoreFileChunkInfo dfsBackupStoreFileChunkInfo) {
        if (dataBufferFlux == null || dfsBackupStoreFileChunkInfo == null
                || dfsBackupStoreFileChunkInfo.getDfsPartChunk() == null
                || dfsBackupStoreFileChunkInfo.getDfsPartChunk().getTokenInfo() == null
                || dfsBackupStoreFileChunkInfo.getDfsPartChunk().getFileInfo() == null
                || dfsBackupStoreFileChunkInfo.getDfsPartChunk().getFileInfo().getFileChunkList() == null
                || dfsBackupStoreFileChunkInfo.getDfsPartChunk().getFileInfo().getFileChunkList()
                .size() <= dfsBackupStoreFileChunkInfo.getDfsPartChunk().getIndex())
            return;

        for (ServerAddressInfo serverAddressInfo : dfsBackupStoreFileChunkInfo.getDfsPartChunk().getFileInfo().getFileChunkList()
                .get(dfsBackupStoreFileChunkInfo.getDfsPartChunk().getIndex()).getChunkServerIdList()) {
            if (serverAddressInfo.getServerId() == getLocalServerId())
                continue;
            DFSCommandFileTransfer dfsCommandFileTransfer = dfsCommandParse
                    .convertCommandFileTransfer(dfsBackupStoreFileChunkInfo.getDfsPartChunk());
            dfsCommandFileTransfer.setUuid(Generators.timeBasedGenerator().generate());
            connectManager.sendCommandDataAsyncReply(findServerState(serverAddressInfo.getServerId()),
                    Flux.just(dfsCommandParse.packageCommandFileTransferHeader(dfsCommandFileTransfer)).mergeWith(
                            dataBufferFlux.map(dataBuffer -> dfsCommandParse.convertDataBuffer2ByteBuf(dataBuffer))),
                    dfsCommandFileTransfer, 300, TimeUnit.SECONDS).whenCompleteAsync((result, t) -> {
                if (result == ResultInfo.S_OK) {
                    // success,todo:
                    logger.info(
                            "file transfer success,serverId[{}],path[{}],file name[{}],index[{}],start position[{}],length[{}]",
                            serverAddressInfo.getServerId(), dfsBackupStoreFileChunkInfo.getDfsPartChunk().getTokenInfo().getPath(),
                            dfsBackupStoreFileChunkInfo.getDfsPartChunk().getTokenInfo().getFileName(),
                            dfsBackupStoreFileChunkInfo.getDfsPartChunk().getIndex(),
                            dfsBackupStoreFileChunkInfo.getDfsPartChunk().getChunk()
                                    * dfsBackupStoreFileChunkInfo.getDfsPartChunk().getChunkSize(),
                            dfsBackupStoreFileChunkInfo.getDfsPartChunk().getContentLength());
                } else {
                    // failed
                    backupStoreTaskScheduledThreadPoolExecutor.schedule(
                            new TransferBackupTask(this, dfsBackupStoreFileChunkInfo, serverAddressInfo.getServerId()), backupPeriod,
                            TimeUnit.SECONDS);
                }
            });
        }
    }

    public void sendBackupStoreFile(long serverId, DFSBackupStoreFileChunkInfo dfsBackupStoreFileChunkInfo) {
        if (dfsBackupStoreFileChunkInfo == null || dfsBackupStoreFileChunkInfo.getDfsPartChunk() == null)
            return;
        DFSPartChunk dfsPartChunk = dfsBackupStoreFileChunkInfo.getDfsPartChunk();
        sendBackupStoreFileAsync(serverId, dfsPartChunk).whenCompleteAsync((result, t) -> {
            if (result == ResultInfo.S_OK) {
                // success
                logger.info("file transfer success,path[{}],file name[{}],index[{}],start position[{}],length[{}]",
                        dfsPartChunk.getTokenInfo().getPath(), dfsPartChunk.getTokenInfo().getFileName(),
                        dfsPartChunk.getIndex(), dfsPartChunk.getChunk() * dfsPartChunk.getChunkSize(),
                        dfsPartChunk.getContentLength());
            } else {
                // failed
                backupStoreTaskScheduledThreadPoolExecutor.schedule(
                        new TransferBackupTask(this, dfsBackupStoreFileChunkInfo, serverId), backupPeriod,
                        TimeUnit.SECONDS);
            }
        });
    }

    public CompletableFuture<Integer> sendBackupStoreFileAsync(long serverId, DFSPartChunk dfsPartChunk) {
        if (dfsPartChunk == null) {
            CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
            completableFuture.complete(ResultInfo.S_ERROR);
            return completableFuture;
        }
        Flux<DataBuffer> dataBufferFlux = FileChunkReader.readFile2Buffer(dfsPartChunk.getFileFullName(),
                dfsPartChunk.getChunk() * dfsPartChunk.getChunkSize(), dfsPartChunk.getContentLength());
        DFSCommandFileTransfer dfsCommandFileTransfer = dfsCommandParse.convertCommandFileTransfer(dfsPartChunk);
        dfsCommandFileTransfer.setUuid(Generators.timeBasedGenerator().generate());
        return connectManager.sendCommandDataAsyncReply(findServerState(serverId),
                Flux.just(dfsCommandParse.packageCommandFileTransferHeader(dfsCommandFileTransfer)).mergeWith(
                        dataBufferFlux.map(dataBuffer -> dfsCommandParse.convertDataBuffer2ByteBuf(dataBuffer))),
                dfsCommandFileTransfer, 300, TimeUnit.SECONDS);
    }

    public CompletableFuture<Integer> sendBackupStoreFileAsync(FileChunkCopy fileChunkCopy) {
        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
        String fileName = String.format("%s.%d.%s", fileChunkCopy.getFileName(), fileChunkCopy.getChunk(),
                serverConfig.getFileChunkSuffix());
        String fileFullName = storeFile.takeFilePath(fileName, fileChunkCopy.getPath()).toString();
        File file = new File(fileFullName);
        if (!file.exists()) {
            completableFuture.complete(ResultInfo.S_ERROR);
            return completableFuture;
        }
        long size = file.length();
        int chunkSize = serverConfig.getChunkPieceSize();
        int chunkCount = (int) ((size + chunkSize - 1) / chunkSize);
        Flux.range(0, chunkCount)
                .flatMap(chunk -> Flux
                        .create((Consumer<FluxSink<Integer>>) fluxSink -> {
                            DFSCommandFileTransfer dfsCommandFileTransfer = dfsCommandParse.convertCommandFileTransfer(fileChunkCopy,
                                    chunk, chunkSize, chunkCount, chunk < chunkCount - 1 ? chunkSize : (int) (size % chunkSize));
                            Flux<DataBuffer> dataBufferFlux = FileChunkReader.readFile2Buffer(fileFullName, chunk * chunkSize,
                                    chunk < chunkCount - 1 ? chunkSize : (int) (size % chunkSize));
                            dfsCommandFileTransfer.setUuid(Generators.timeBasedGenerator().generate());
                            connectManager
                                    .sendCommandDataAsyncReply(findServerState(fileChunkCopy.getServerId()),
                                            Flux.just(dfsCommandParse.packageCommandFileTransferHeader(dfsCommandFileTransfer)).mergeWith(
                                                    dataBufferFlux.map(dataBuffer -> dfsCommandParse.convertDataBuffer2ByteBuf(dataBuffer))),
                                            dfsCommandFileTransfer, 300, TimeUnit.SECONDS)
                                    .whenCompleteAsync((result, t) -> {
                                        if (result == ResultInfo.S_OK) {
                                            // success
                                            logger.info(
                                                    "file transfer success,path[{}],file name[{}],index[{}],start position[{}],length[{}]",
                                                    fileChunkCopy.getPath(), fileChunkCopy.getFileName(), fileChunkCopy.getChunk(),
                                                    chunk * chunkSize, chunk < chunkCount - 1 ? chunkSize : (int) (size % chunkSize));
                                            fluxSink.next(1);
                                        } else {
                                            // failed
                                            fluxSink.next(0);
                                        }
                                    });
                        }))
                .filter(res -> res == 1)
                .count()
                .subscribe(count -> {
                    if (count == chunkCount)
                        completableFuture.complete(ResultInfo.S_OK);
                    else
                        completableFuture.complete(ResultInfo.S_FAILED);
                });
        return completableFuture;
    }

    public void sendBackupStoreFile(FluxSink<Long> sink, long serverId, Flux<DataBuffer> dataBufferFlux,
                                    DFSPartChunk dfsPartChunk) {
        if (dataBufferFlux == null || dfsPartChunk == null || dfsPartChunk.getTokenInfo() == null) {
            sink.error(new TransferFileException(String.format("send file to %s failed,data is invalid!", serverId)));
            return;
        }
        DFSCommandFileTransfer dfsCommandFileTransfer = dfsCommandParse.convertCommandFileTransfer(dfsPartChunk);
        dfsCommandFileTransfer.setUuid(Generators.timeBasedGenerator().generate());
        connectManager.sendCommandDataAsyncReply(findServerState(serverId),
                Flux.just(dfsCommandParse.packageCommandFileTransferHeader(dfsCommandFileTransfer)).mergeWith(
                        dataBufferFlux.map(dataBuffer -> dfsCommandParse.convertDataBuffer2ByteBuf(dataBuffer))),
                dfsCommandFileTransfer, 30, TimeUnit.SECONDS).whenCompleteAsync((result, t) -> {
            if (result == ResultInfo.S_OK) {
                // success
                sink.next(serverId);
            } else {
                // failed
                // backupStoreTaskScheduledThreadPoolExecutor.schedule(new
                // TransferBackupTask(this,dfsBackupStoreFileChunkInfo), backupPeriod, TimeUnit.SECONDS);
                sink.error(new TransferFileException(String.format("send file to %s failed!", serverId)));
            }
        });
    }

}