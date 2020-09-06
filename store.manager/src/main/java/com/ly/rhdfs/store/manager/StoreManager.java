package com.ly.rhdfs.store.manager;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.file.DFSBackupStoreFileChunkInfo;
import com.ly.rhdfs.store.manager.task.TransferBackupTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.uuid.Generators;
import com.ly.common.constant.ParamConstants;
import com.ly.common.domain.DFSPartChunk;
import com.ly.common.domain.server.ServerState;
import com.ly.common.domain.token.TokenInfo;
import com.ly.common.util.DfsFileUtils;
import com.ly.rhdfs.communicate.command.DFSCommandFileTransfer;
import com.ly.rhdfs.communicate.socket.parse.DFSCommandParse;
import com.ly.rhdfs.manager.handler.CommandEventHandler;
import com.ly.rhdfs.manager.handler.ServerStateCommandEventHandler;
import com.ly.rhdfs.manager.server.ServerManager;
import com.ly.rhdfs.store.manager.handler.TokenCommandEventHandler;
import com.ly.rhdfs.store.manager.task.ComputerStateTask;
import com.ly.rhdfs.store.manager.task.ComputerVoteTask;

import reactor.core.publisher.Flux;

@Component
public class StoreManager extends ServerManager {

    private final int backupPeriod=60;
    private final Map<String, TokenInfo> tokenInfoMap = new ConcurrentHashMap<>();
    private DfsFileUtils dfsFileUtils;
    private DFSCommandParse dfsCommandParse;
    private ScheduledThreadPoolExecutor backupStoreTaskScheduledThreadPoolExecutor=new ScheduledThreadPoolExecutor(32);

    public StoreManager() {
        scheduledThreadCount = 4;
    }

    @Autowired
    private void setDfsFileUtils(DfsFileUtils dfsFileUtils) {
        this.dfsFileUtils = dfsFileUtils;
    }

    @Autowired
    private void setDfsCommandParse(DFSCommandParse dfsCommandParse) {
        this.dfsCommandParse = dfsCommandParse;
    }

    @Override
    protected void initCommandEventHandler() {
        super.initCommandEventHandler();
        commandEventHandler.setServerStateCommandEventHandler(new ServerStateCommandEventHandler(this));
        commandEventHandler.setTokenCommandEventHandler(new TokenCommandEventHandler(this));
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

    public DFSBackupStoreFileChunkInfo newBackupChunkInfo(DFSPartChunk dfsPartChunk){
        if (dfsPartChunk==null)
            return null;
        DFSBackupStoreFileChunkInfo dfsBackupStoreFileChunkInfo=new DFSBackupStoreFileChunkInfo();
        dfsBackupStoreFileChunkInfo.setCreateTime(Instant.now().toEpochMilli());
        dfsBackupStoreFileChunkInfo.setDfsPartChunk(dfsPartChunk);
        return dfsBackupStoreFileChunkInfo;
    }
    public void sendBackupStoreFile(Flux<DataBuffer> dataBufferFlux, DFSBackupStoreFileChunkInfo dfsBackupStoreFileChunkInfo) {
        if (dataBufferFlux == null || dfsBackupStoreFileChunkInfo==null || dfsBackupStoreFileChunkInfo.getDfsPartChunk() == null || dfsBackupStoreFileChunkInfo.getDfsPartChunk().getTokenInfo() == null
                || dfsBackupStoreFileChunkInfo.getDfsPartChunk().getFileInfo() == null || dfsBackupStoreFileChunkInfo.getDfsPartChunk().getFileInfo().getFileChunkList() == null
                || dfsBackupStoreFileChunkInfo.getDfsPartChunk().getFileInfo().getFileChunkList().size() <= dfsBackupStoreFileChunkInfo.getDfsPartChunk().getIndex())
            return;

        for (long serverId : dfsBackupStoreFileChunkInfo.getDfsPartChunk().getFileInfo().getFileChunkList().get(dfsBackupStoreFileChunkInfo.getDfsPartChunk().getIndex())
                .getChunkServerIdList()) {
            if (serverId == getLocalServerId())
                continue;
            DFSCommandFileTransfer dfsCommandFileTransfer = dfsCommandParse.convertCommandFileTransfer(dfsBackupStoreFileChunkInfo.getDfsPartChunk());
            dfsCommandFileTransfer.setUuid(Generators.timeBasedGenerator().generate());
            connectManager.sendCommandDataAsyncReply(findServerState(serverId),
                    Flux.just(dfsCommandParse.packageCommandFileTransferHeader(dfsCommandFileTransfer))
                            .mergeWith(dataBufferFlux.map(dataBuffer -> dfsCommandParse.convertDataBuffer2ByteBuf(dataBuffer))),
                    dfsCommandFileTransfer, 30, TimeUnit.SECONDS)
                    .whenCompleteAsync((result, t) -> {
                        if (result== ResultInfo.S_OK){
                            //success
                        }else{
                            //failed
                            backupStoreTaskScheduledThreadPoolExecutor.schedule(new TransferBackupTask(this,dfsBackupStoreFileChunkInfo), backupPeriod, TimeUnit.SECONDS);
                        }
                    });
        }
    }
}