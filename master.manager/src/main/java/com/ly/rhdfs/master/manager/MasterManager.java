package com.ly.rhdfs.master.manager;

import com.ly.common.constant.ParamConstants;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.log.OperationLog;
import com.ly.common.domain.server.ServerInfoConfiguration;
import com.ly.common.domain.server.ServerRunState;
import com.ly.common.domain.server.ServerState;
import com.ly.common.domain.token.TokenInfo;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.manager.server.ServerManager;
import com.ly.rhdfs.master.manager.handler.ServerStateCommandMasterEventHandler;
import com.ly.rhdfs.master.manager.runstate.FileServerRunManager;
import com.ly.rhdfs.master.manager.runstate.ServerAssignException;
import com.ly.rhdfs.master.manager.task.*;
import com.ly.rhdfs.token.TokenFactory;
import io.netty.channel.ChannelFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class MasterManager extends ServerManager {

    private final Map<ServerState, Future> masterUpdateFileInfo = new ConcurrentHashMap<>();
    private FileServerRunManager fileServerRunManager;

    public MasterManager() {
        scheduledThreadCount = 7;
    }

    public FileServerRunManager getFileServerRunManager() {
        return fileServerRunManager;
    }

    @Autowired
    private void setFileServerRunManager(FileServerRunManager fileServerRunManager) {
        this.fileServerRunManager = fileServerRunManager;
    }

    @Override
    protected void initCommandEventHandler() {
        commandEventHandler.setServerStateCommandEventHandler(new ServerStateCommandMasterEventHandler(this));
    }

    public void initial() {
        super.initial();
        if (!ParamConstants.ST_MASTER.equals(serverConfig.getServerType()))
            return;

        // 初始化监听
        connectManager.startSocketListen(serverConfig.getPort(), commandEventHandler);
        // 定时配置更新
        scheduledThreadPoolExecutor.schedule(new MasterQualificationVerify(this), initThreadSecondDelay,
                TimeUnit.SECONDS);
        // 定时统计投票计数，主从状态
        scheduledThreadPoolExecutor.schedule(new ComputerVoteTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
        // 定时发送MasterServer和StoreServer的地址和端口信息
        scheduledThreadPoolExecutor.schedule(new UpdateServerAddressTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
        // 定时处理下载超时任务
        scheduledThreadPoolExecutor.schedule(new DownloadClearTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
        // 定时处理上传超时任务
        scheduledThreadPoolExecutor.schedule(new UploadClearTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
        // 定时处理删除文件命令
        scheduledThreadPoolExecutor.schedule(new ClearTokenTask(this), initThreadThirdDelay, TimeUnit.SECONDS);
        // 定时处理有效服务负载排序状态
        scheduledThreadPoolExecutor.schedule(new AvailableOrderlyServerRunStateTask(this), initThreadThirdDelay,
                TimeUnit.SECONDS);
    }

    public Map<ServerState, Future> getMasterUpdateFileInfo() {
        return masterUpdateFileInfo;
    }

    public boolean isContainUpdateFileInfo(ServerState serverState) {
        return masterUpdateFileInfo.containsKey(serverState);
    }

    public void startMasterUpdateFileInfo(ServerState serverState) {
        if (serverState == null)
            return;
        masterUpdateFileInfo.put(serverState,
                threadPoolExecutor.submit(new UpdateMasterFileInfoTask(this, serverState)));
    }

    @Override
    protected void addServerInfo(ServerInfoConfiguration serverInfoConfiguration) {
        if (serverInfoConfiguration != null) {
            ServerState serverState = newServerInfo(serverInfoConfiguration);
            serverInfoMap.put(serverInfoConfiguration.getServerId(), serverState);
            ServerRunState serverRunState = new ServerRunState(serverState);
            fileServerRunManager.putServerRunState(serverRunState);
        }
    }

    @Override
    public void clearToken(TokenInfo tokenInfo) {
        if(tokenInfo==null)
            return;
        if (tokenInfo.getTokenType()==TokenInfo.TOKEN_WRITE){
            fileServerRunManager.clearUploadFile(tokenInfo,true);
        }else if (tokenInfo.getTokenType()==TokenInfo.TOKEN_READ) {
            fileServerRunManager.clearUploadFile(tokenInfo,true);
        }
    }

    public void resetServerState() {
        verifyMasterQualification();
    }

    public void verifyMasterQualification() {
        // verify count
        int count = 0;

        // master失联后如何处理？
        // 1、master；多于copies连接不上，记录错误时间，超过一定时间，取消master状态
        // 2、master-backup：多于copies连接不上，取消ready状态；master连接失败，设置lost contact状态，恢复后重新验证writeTime
        for (ServerState serverState : serverInfoMap.values()) {
            if (!serverState.isOnline()) {
                if (serverState.getType() == ServerState.SIT_STORE)
                    // store lost count
                    count++;
                else if (localServerState.getType() != ServerState.SIT_MASTER
                        && serverState.getType() == ServerState.SIT_MASTER
                        && serverState.getServerId() == masterServerId) {
                    localServerState.setState(localServerState.getState() | ServerState.SIS_MASTER_LOST_CONTACT);
                    localServerState.setLastTime(Instant.now().toEpochMilli());
                }
            } else if (localServerState.getType() != ServerState.SIT_MASTER
                    && serverState.getType() == ServerState.SIT_MASTER && serverState.getServerId() == masterServerId
                    && (localServerState.getState() == ServerState.SIS_UNKNOWN
                    || (localServerState.getState() & ServerState.SIS_MASTER_LOST_CONTACT) != 0)
                    && serverState.getWriteLastTime() > localServerState.getWriteLastTime()) {
                // verify writeLastTime,clear lost contact mark after verify writeLastTime.
                localServerState.setState(localServerState.getState() & ~ServerState.SIS_MASTER_LOST_CONTACT);
                localServerState.setReady(false);
                localServerState.setLastTime(Instant.now().toEpochMilli());
                return;
            }
        }
        if (count >= serverConfig.getFileCopies()) {
            if (localServerState.getType() == ServerState.SIT_MASTER_BACKUP && localServerState.isReady()) {
                localServerState.setReady(false);
                localServerState.setLastTime(Instant.now().toEpochMilli());
            } else if (localServerState.getType() == ServerState.SIT_MASTER) {
                if ((localServerState.getState() & ServerState.SIS_MASTER_NOT_ENOUGH_STORE) != 0) {
                    if (Instant.now().toEpochMilli() - localServerState.getLastTime() > serverConfig
                            .getStoreServerDTMCancelMaster()) {
                        // clear master state,too many store server disconnected too long
                        localServerState.setType(ServerState.SIT_MASTER_BACKUP);
                        localServerState.setReady(false);
                        masterServerId = -1;
                    }
                } else {
                    // set not enough store state.
                    localServerState.setState(localServerState.getState() | ServerState.SIS_MASTER_NOT_ENOUGH_STORE);
                    localServerState.setLastTime(Instant.now().toEpochMilli());
                }
            }
        } else if ((localServerState.getState() & ServerState.SIS_MASTER_NOT_ENOUGH_STORE) != 0) {
            // restore enough store state
            localServerState.setReady(true);
            localServerState.setLastTime(Instant.now().toEpochMilli());
            localServerState.setState(localServerState.getState() & ~ServerState.SIS_MASTER_NOT_ENOUGH_STORE);
        }

    }

    public List<ServerInfoConfiguration> collectChangeAddressServer(ServerState serverState) {
        if (serverState == null) {
            return new ArrayList<>();
        }
        return collectChangeAddressServer(serverState.getUpdateAddressLastTime());
    }

    public List<ServerInfoConfiguration> collectChangeAddressServer(long lastTime) {
        List<ServerInfoConfiguration> serverInfoConfigurationList = new ArrayList<>();
        for (ServerState serverState : serverInfoMap.values()) {
            if (serverState == null)
                continue;
            if (serverState.getUpdateAddressLastTime() > lastTime) {
                serverInfoConfigurationList
                        .add(masterServerConfig.getServerInfoConfiguration(serverState.getServerId()));
            }
        }
        return serverInfoConfigurationList;
    }

    public void computerVoteCount() {
        if (masterServerConfig == null)
            return;
        int count = 0;
        for (ServerState serverState : serverInfoMap.values()) {
            if (serverState.getType() != ServerState.SIT_STORE)
                continue;
            if (serverState.getVotedServerId() == getLocalServerId())
                count++;
        }
        if (count > masterServerConfig.getStoreServerMap().size() * 2 / 3) {
            localServerState.setType(ServerState.SIT_MASTER);
            masterServerId = getLocalServerId();
        }
    }

    public void sendServerAddressUpdate(ServerState serverState,
                                        List<ServerInfoConfiguration> serverInfoConfigurations) {
        if (serverState == null)
            return;
        connectManager.sendCommunicationObject(serverState, serverInfoConfigurations, DFSCommand.CT_SERVER_ADDRESS);
    }

    public ChannelFuture sendFileDeleteAsync(long serverId, TokenInfo fileDeleteTokenInfo) {
        return sendFileDeleteAsync(serverInfoMap.get(serverId), fileDeleteTokenInfo);
    }

    public ChannelFuture sendFileDeleteAsync(ServerState serverState, TokenInfo fileDeleteTokenInfo) {
        if (serverState == null || fileDeleteTokenInfo == null)
            return null;
        return connectManager.sendCommunicationObjectAsync(serverState, fileDeleteTokenInfo,DFSCommand.CT_FILE_DELETE);
    }

    public ChannelFuture sendClearTokenAsync(long serverId, TokenInfo tokenInfo) {
        return sendClearTokenAsync(serverInfoMap.get(serverId), tokenInfo);
    }

    public ChannelFuture sendClearTokenAsync(ServerState serverState, TokenInfo tokenInfo) {
        if (serverState == null || tokenInfo == null)
            return null;
        return connectManager.sendCommunicationObjectAsync(serverState, tokenInfo,DFSCommand.CT_TOKEN_CLEAR);
    }

    public boolean sendFileDeleteSync(ServerState serverState, TokenInfo fileDeleteTokenInfo) {
        if (serverState == null || fileDeleteTokenInfo == null)
            return false;
        return connectManager.sendCommunicationObjectSync(serverState, fileDeleteTokenInfo,DFSCommand.CT_FILE_DELETE);
    }

    public ChannelFuture sendFileOperationLog(ServerState serverState, OperationLog operationLog) {
        if (serverState == null || operationLog == null)
            return null;
        return connectManager.sendCommunicationObjectAsync(serverState, operationLog,DFSCommand.CT_FILE_OPERATE);
    }

    public ChannelFuture sendFileInfoAsync(ServerState serverState, FileInfo fileInfo) {
        if (serverState == null || fileInfo == null)
            return null;
        return connectManager.sendCommunicationObjectAsync(serverState, fileInfo,DFSCommand.CT_FILE_INFO);
    }

    public ChannelFuture sendToken(ServerState serverState, TokenInfo tokenInfo) {
        if (serverState == null || tokenInfo == null)
            return null;
        return connectManager.sendCommunicationObjectAsync(serverState, tokenInfo,DFSCommand.CT_TOKEN);
    }

    public Mono<Boolean> sendFileInfo(ServerState serverState, FileInfo fileInfo) {
        ChannelFuture channelFuture = sendFileInfoAsync(serverState, fileInfo);
        if (channelFuture == null)
            return Mono.just(false);
        return Mono.create(monoSink -> channelFuture.addListener(future -> monoSink.success(future.isSuccess())));
    }

    public Mono<FileInfo> apportionFileServer(TokenInfo tokenInfo, long size) {
        if (tokenInfo == null || size <= 0)
            return Mono.error(new NullPointerException());
        AtomicReference<FileInfo> fileInfoAtomic = new AtomicReference<>();
        AtomicReference<Set<Long>> serverIds = new AtomicReference<>();
        return Mono
                .defer(() -> {
                    // 分配Chunk存储服务器
                    fileInfoAtomic.set(fileServerRunManager.assignUploadFileServer(tokenInfo, size));
                    return Mono.just(fileInfoAtomic.get());
                })
                .flux().flatMap(fileInfo -> {
                    // 整理涉及的chunk服务器列表
                    serverIds.set(fileServerRunManager.tidyServerId(fileInfo));
                    return Flux.fromIterable(serverIds.get());
                })
                .flatMap(serverId -> Mono
                        .create((Consumer<MonoSink<Long>>) monoSink ->
                                //发送Token到chunk server
                                sendToken(serverInfoMap.get(serverId), tokenInfo)
                                        .addListener(future -> {
                                            //发送成功发射ServerId，不成功发射-1
                                            if (future.isSuccess())
                                                monoSink.success(serverId);
                                            else
                                                monoSink.success(-1L);
                                        })))
                .collect((Supplier<TreeSet<Long>>) TreeSet::new,
                        (treeSet, serverId) -> {
                            //收集发送成功的ServerId
                            if (serverId > 0)
                                treeSet.add(serverId);
                        })
                .flatMap(treeSet -> {
                    if (treeSet.size() < serverIds.get().size()) {
                        if (!fileServerRunManager.resetInvalidServerId(fileInfoAtomic.get(), treeSet)) {
                            //发送成功的服务器数量不足保存份数，触发异常
                            fileServerRunManager.clearUploadFile(tokenInfo,true);
                            return Mono.error(new ServerAssignException("Server count is not enough!"));
                        }
                    }
                    // 成功，发射FileInfo信息
                    return Mono.just(serverIds.get());
                })
                .flux().flatMap(Flux::fromIterable)
                .flatMap(serverId->sendFileInfo(serverInfoMap.get(serverId),fileInfoAtomic.get()))
                .flatMap(sendResult->{
                    if (!sendResult) {
                        fileServerRunManager.clearUploadFile(tokenInfo, true);
                        return Mono.error(new ServerAssignException("Server count is not enough!"));
                    }
                    return Mono.just(true);
                })
                .then(Mono.just(fileInfoAtomic.get()));

    }
    public Mono<FileInfo> questFileServer(TokenInfo tokenInfo) {
        if (tokenInfo == null)
            return Mono.error(new NullPointerException());
        return Mono
                .defer(() -> {
                    // 分配Chunk存储服务器
                    FileInfo fileInfo=fileServerRunManager.assignDownloadFileServer(tokenInfo);
                    Set<Long> serverIds=fileServerRunManager.tidyServerId(fileInfo);
                    for (long serverId:serverIds){
                        sendToken(serverInfoMap.get(serverId), tokenInfo);
                    }
                    return Mono.just(fileInfo);
                });
    }
}
