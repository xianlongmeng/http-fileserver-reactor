package com.ly.rhdfs.master.manager;

import com.ly.common.constant.ParamConstants;
import com.ly.common.domain.TaskInfo;
import com.ly.common.domain.file.BackupMasterFileInfo;
import com.ly.common.domain.file.DirectInfo;
import com.ly.common.domain.file.FileChunkCopy;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.log.OperationLog;
import com.ly.common.domain.server.ServerAddressInfo;
import com.ly.common.domain.server.ServerInfoConfiguration;
import com.ly.common.domain.server.ServerRunState;
import com.ly.common.domain.server.ServerState;
import com.ly.common.domain.token.TokenInfo;
import com.ly.common.exception.DirectNotFoundException;
import com.ly.common.exception.FileNotFoundException;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.file.config.FileInfoManager;
import com.ly.rhdfs.file.util.DfsFileUtils;
import com.ly.rhdfs.log.server.file.ServerFileChunkUtil;
import com.ly.rhdfs.manager.server.ServerManager;
import com.ly.rhdfs.master.manager.handler.MasterFileDeleteCommandEventHandler;
import com.ly.rhdfs.master.manager.handler.MasterFileInfoCommandEventHandler;
import com.ly.rhdfs.master.manager.handler.OperateLogCommandMasterEventHandler;
import com.ly.rhdfs.master.manager.handler.ServerStateCommandMasterEventHandler;
import com.ly.rhdfs.master.manager.runstate.FileServerRunManager;
import com.ly.rhdfs.master.manager.runstate.ServerAssignException;
import com.ly.rhdfs.master.manager.task.*;
import io.netty.channel.ChannelFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.function.Tuples;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class MasterManager extends ServerManager {

    private final Map<ServerState, Future> masterUpdateFileInfo = new ConcurrentHashMap<>();
    private final Map<Long, BlockingDeque<BackupMasterFileInfo>> backupMasterFileInfoBlockingQueue = new ConcurrentHashMap<>();
    private final FileServerRunManager fileServerRunManager=new FileServerRunManager();
    private DfsFileUtils dfsFileUtils;
    private ServerFileChunkUtil serverFileChunkUtil;
    private RecoverStoreServerTask recoverStoreServerTask;

    private List<ServerRunState> availableOrderlyServerRunStates;
    private FileInfoManager fileInfoManager;
    @Autowired
    private void setFileInfoManager(FileInfoManager fileInfoManager) {
        this.fileInfoManager = fileInfoManager;
    }

    public MasterManager() {
        scheduledThreadCount = 9;
    }

    public FileServerRunManager getFileServerRunManager() {
        return fileServerRunManager;
    }

    @Autowired
    private void setDfsFileUtils(DfsFileUtils dfsFileUtils) {
        this.dfsFileUtils = dfsFileUtils;
    }

    public ServerFileChunkUtil getServerFileChunkUtil() {
        return serverFileChunkUtil;
    }

    @Autowired
    private void setServerFileChunkUtil(ServerFileChunkUtil serverFileChunkUtil) {
        this.serverFileChunkUtil = serverFileChunkUtil;
    }

    @Override
    protected void initCommandEventHandler() {
        super.initCommandEventHandler();
        commandEventHandler.setServerStateCommandEventHandler(new ServerStateCommandMasterEventHandler(this));
        commandEventHandler.setOperationLogCommandEventHandler(new OperateLogCommandMasterEventHandler(this));
        commandEventHandler.setFileInfoCommandEventHandler(new MasterFileInfoCommandEventHandler(this));
        commandEventHandler.setFileDeleteCommandEventHandler(new MasterFileDeleteCommandEventHandler(this));
    }

    @Override
    public void initial() {
        fileServerRunManager.setMasterManager(this);
        fileServerRunManager.setFileInfoManager(fileInfoManager);
        fileServerRunManager.setDfsFileUtils(dfsFileUtils);

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
        recoverStoreServerTask = new RecoverStoreServerTask(this);
        scheduledThreadPoolExecutor.schedule(recoverStoreServerTask, initThreadThirdDelay, TimeUnit.SECONDS);
        // 定时处理FileInfo发送到backup master server
        scheduledThreadPoolExecutor.schedule(new BackupMasterFileInfoTask(this), initThreadThirdDelay,
                TimeUnit.SECONDS);
    }

    public Map<ServerState, Future> getMasterUpdateFileInfo() {
        return masterUpdateFileInfo;
    }

    public Map<Long, BlockingDeque<BackupMasterFileInfo>> getBackupMasterFileInfoBlockingQueue() {
        return backupMasterFileInfoBlockingQueue;
    }

    public boolean isContainUpdateFileInfo(ServerState serverState) {
        return masterUpdateFileInfo.containsKey(serverState);
    }

    public void startMasterUpdateFileInfo(ServerState serverState) {
        if (serverState == null)
            return;
        masterUpdateFileInfo.put(serverState,
                threadPoolExecutor.submit(new UpdateMasterFileInfoTask(this, dfsFileUtils, serverState)));
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
        if (tokenInfo == null)
            return;
        if (localServerState.getType() == ServerState.SIT_MASTER) {
            if (tokenInfo.getTokenType() == TokenInfo.TOKEN_WRITE) {
                fileServerRunManager.clearUploadFile(tokenInfo, true);
            } else if (tokenInfo.getTokenType() == TokenInfo.TOKEN_READ) {
                fileServerRunManager.clearDownloadFile(tokenInfo, true);
            }
        } else if (localServerState.getType() == ServerState.SIT_MASTER_BACKUP) {
            if (tokenInfo.getTokenType() == TokenInfo.TOKEN_WRITE) {
                fileServerRunManager.getUploadRunningTask().remove(tokenInfo);
            } else if (tokenInfo.getTokenType() == TokenInfo.TOKEN_READ) {
                fileServerRunManager.getDownloadRunningTask().remove(tokenInfo);
            }
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
                else if (getLocalServerState().getType() != ServerState.SIT_MASTER
                        && serverState.getType() == ServerState.SIT_MASTER
                        && serverState.getServerId() == masterServerId) {
                    getLocalServerState()
                            .setState(getLocalServerState().getState() | ServerState.SIS_MASTER_LOST_CONTACT);
                    getLocalServerState().setLastTime(Instant.now().toEpochMilli());
                }
            } else if (getLocalServerState().getType() != ServerState.SIT_MASTER
                    && serverState.getType() == ServerState.SIT_MASTER && serverState.getServerId() == masterServerId
                    && (getLocalServerState().getState() == ServerState.SIS_UNKNOWN
                    || (getLocalServerState().getState() & ServerState.SIS_MASTER_LOST_CONTACT) != 0)
                    && serverState.getWriteLastTime() > getLocalServerState().getWriteLastTime()
                    + serverConfig.getBackupMasterServerUpdateTimeout()) {
                // verify writeLastTime,clear lost contact mark after verify writeLastTime.
                getLocalServerState().setState(getLocalServerState().getState() & ~ServerState.SIS_MASTER_LOST_CONTACT);
                getLocalServerState().setReady(false);
                getLocalServerState().setLastTime(Instant.now().toEpochMilli());
                return;
            }
        }
        if (count >= serverConfig.getFileCopies()) {
            if (getLocalServerState().getType() == ServerState.SIT_MASTER_BACKUP && getLocalServerState().isReady()) {
                getLocalServerState().setReady(false);
                getLocalServerState().setLastTime(Instant.now().toEpochMilli());
            } else if (getLocalServerState().getType() == ServerState.SIT_MASTER) {
                if ((getLocalServerState().getState() & ServerState.SIS_MASTER_NOT_ENOUGH_STORE) != 0) {
                    if (Instant.now().toEpochMilli() - getLocalServerState().getLastTime() > serverConfig
                            .getStoreServerDTMCancelMaster()) {
                        // clear master state,too many store server disconnected too long
                        getLocalServerState().setType(ServerState.SIT_MASTER_BACKUP);
                        getLocalServerState().setReady(false);
                        masterServerId = -1;
                    }
                } else {
                    // set not enough store state.
                    getLocalServerState()
                            .setState(getLocalServerState().getState() | ServerState.SIS_MASTER_NOT_ENOUGH_STORE);
                    getLocalServerState().setLastTime(Instant.now().toEpochMilli());
                }
            }
        } else if ((getLocalServerState().getState() & ServerState.SIS_MASTER_NOT_ENOUGH_STORE) != 0) {
            // restore enough store state
            getLocalServerState().setReady(true);
            getLocalServerState().setLastTime(Instant.now().toEpochMilli());
            getLocalServerState().setState(getLocalServerState().getState() & ~ServerState.SIS_MASTER_NOT_ENOUGH_STORE);
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
            getLocalServerState().setType(ServerState.SIT_MASTER);
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
        return connectManager.sendCommunicationObjectAsync(serverState, fileDeleteTokenInfo, DFSCommand.CT_FILE_DELETE);
    }

    public ChannelFuture sendClearTokenAsync(long serverId, TokenInfo tokenInfo) {
        return sendClearTokenAsync(serverInfoMap.get(serverId), tokenInfo);
    }

    public ChannelFuture sendClearTokenAsync(ServerState serverState, TokenInfo tokenInfo) {
        if (serverState == null || tokenInfo == null)
            return null;
        return connectManager.sendCommunicationObjectAsync(serverState, tokenInfo, DFSCommand.CT_TOKEN_CLEAR);
    }

    public boolean sendFileDeleteSync(Long serverId, TokenInfo fileDeleteTokenInfo) {
        return sendFileDeleteSync(findServerState(serverId), fileDeleteTokenInfo);
    }

    public boolean sendFileDeleteSync(ServerState serverState, TokenInfo fileDeleteTokenInfo) {
        if (serverState == null || fileDeleteTokenInfo == null)
            return false;
        return connectManager.sendCommunicationObjectSync(serverState, fileDeleteTokenInfo, DFSCommand.CT_FILE_DELETE);
    }

    public ChannelFuture sendFileOperationLog(ServerState serverState, OperationLog operationLog) {
        if (serverState == null || operationLog == null)
            return null;
        return connectManager.sendCommunicationObjectAsync(serverState, operationLog, DFSCommand.CT_FILE_OPERATE);
    }

    public ChannelFuture sendFileInfoAsync(ServerState serverState, FileInfo fileInfo) {
        if (serverState == null || fileInfo == null)
            return null;
        return connectManager.sendCommunicationObjectAsync(serverState, fileInfo, DFSCommand.CT_FILE_INFO);
    }

    public ChannelFuture sendToken(ServerState serverState, TokenInfo tokenInfo) {
        if (serverState == null || tokenInfo == null)
            return null;
        return connectManager.sendCommunicationObjectAsync(serverState, tokenInfo, DFSCommand.CT_TOKEN);
    }

    public Mono<Boolean> sendFileInfo(ServerState serverState, FileInfo fileInfo) {
        ChannelFuture channelFuture = sendFileInfoAsync(serverState, fileInfo);
        if (channelFuture == null)
            return Mono.just(false);
        return Mono.create(monoSink -> channelFuture.addListener(future -> monoSink.success(future.isSuccess())));
    }

    public void sendBackupTaskInfo(TaskInfo taskInfo) {
        if (taskInfo == null || masterServerConfig.getMasterServerMap() == null
                || masterServerConfig.getMasterServerMap().isEmpty())
            return;
        for (Long serverId : masterServerConfig.getMasterServerMap().keySet()) {
            ServerState serverState = findServerState(serverId);
            if (serverState == null || serverState.getType() != ServerState.SIT_MASTER_BACKUP || !serverState.isReady())
                continue;
            connectManager.sendCommunicationObjectAsync(serverState, taskInfo, DFSCommand.CT_TASK_INFO)
                    .addListener(future -> {
                        if (future.isSuccess()) {
                            logger.debug("send backup token success.serverId:{} path:{} file name:{}", serverId, taskInfo.getTokenInfo().getPath(), taskInfo.getTokenInfo().getFileName());
                        }else{
                            logger.warn("send backup token failed.serverId:{} path:{} file name:{}", serverId, taskInfo.getTokenInfo().getPath(), taskInfo.getTokenInfo().getFileName());
                        }
                    });
        }
    }

    public void sendBackupClearToken(TokenInfo tokenInfo) {
        if (tokenInfo == null || masterServerConfig.getMasterServerMap() == null
                || masterServerConfig.getMasterServerMap().isEmpty())
            return;
        for (Long serverId : masterServerConfig.getMasterServerMap().keySet()) {
            ServerState serverState = findServerState(serverId);
            if (serverState == null || serverState.getType() != ServerState.SIT_MASTER_BACKUP || !serverState.isReady())
                continue;
            connectManager.sendCommunicationObjectAsync(serverState, tokenInfo, DFSCommand.CT_TOKEN_CLEAR)
                    .addListener(future -> {
                        if (future.isSuccess()) {
                            logger.debug("send clear backup token success.serverId:{} path:{} file name:{}", serverId, tokenInfo.getPath(), tokenInfo.getFileName());
                        }else{
                            logger.warn("send clear backup token failed.serverId:{} path:{} file name:{}", serverId, tokenInfo.getPath(), tokenInfo.getFileName());
                        }
                    });
        }
    }

    public Mono<FileInfo> apportionFileServer(TokenInfo tokenInfo, long size) {
        if (tokenInfo == null || size <= 0)
            return Mono.error(new NullPointerException());
        AtomicReference<FileInfo> fileInfoAtomic = new AtomicReference<>();
        AtomicReference<Set<ServerAddressInfo>> serverAddressSet = new AtomicReference<>();
        // 收集发送成功的ServerId
        return Mono.defer(() -> {
            // 分配Chunk存储服务器
            fileInfoAtomic.set(fileServerRunManager.assignUploadFileServer(tokenInfo, size));
            return Mono.just(fileInfoAtomic.get());
        }).flux().flatMap(fileInfo -> {
            // 整理涉及的chunk服务器列表
            serverAddressSet.set(fileServerRunManager.tidyServerId(fileInfo));
            return Flux.fromIterable(serverAddressSet.get());
        }).flatMap(serverAddressInfo -> Mono.create((Consumer<MonoSink<ServerAddressInfo>>) monoSink ->
                // 发送Token到chunk server
                sendToken(serverInfoMap.get(serverAddressInfo.getServerId()), tokenInfo).addListener(future -> {
                    // 发送成功发射ServerId，不成功发射-1
                    if (future.isSuccess())
                        monoSink.success(serverAddressInfo);
                })))
                .collect((Supplier<TreeSet<ServerAddressInfo>>) TreeSet::new, TreeSet::add).flatMap(treeSet -> {
                    if (treeSet.size() < serverAddressSet.get().size()
                            && !fileServerRunManager.resetInvalidServerId(fileInfoAtomic.get(), treeSet)) {
                        // 发送成功的服务器数量不足保存份数，触发异常
                        fileServerRunManager.clearUploadFile(tokenInfo, true);
                        return Mono.error(new ServerAssignException("Server count is not enough!"));
                    }
                    // 成功，发射FileInfo信息
                    return Mono.just(serverAddressSet.get());
                }).flux().flatMap(Flux::fromIterable)
                .flatMap(serverAddressInfo -> sendFileInfo(serverInfoMap.get(serverAddressInfo.getServerId()),
                        fileInfoAtomic.get()))
                .flatMap(sendResult -> {
                    if (sendResult) {
                        sendBackupTaskInfo(new TaskInfo(tokenInfo, fileInfoAtomic.get()));
                        return Mono.just(true);
                    } else {
                        fileServerRunManager.clearUploadFile(tokenInfo, true);
                        return Mono.error(new ServerAssignException("Server count is not enough!"));
                    }
                }).then(Mono.just(fileInfoAtomic.get()));

    }

    public Mono<FileInfo> apportionFileServerChunk(TokenInfo tokenInfo, int chunk) {
        if (tokenInfo == null || chunk <= 0)
            return Mono.error(new NullPointerException());
        AtomicReference<FileInfo> fileInfoAtomic = new AtomicReference<>();
        return Mono.defer(() -> {
            // 分配Chunk存储服务器
            fileInfoAtomic.set(fileServerRunManager.assignUploadFileServerChunk(tokenInfo, chunk));
            return Mono.just(fileInfoAtomic.get());
        }).flux()
                // 整理涉及的chunk服务器列表
                .flatMap(fileInfo -> Flux.fromIterable(fileInfo.getFileChunkList().get(chunk).getChunkServerIdList()))
                .flatMap(serverAddressInfo -> Flux.create((Consumer<FluxSink<Long>>) fluxSink ->
                        // 发送Token到chunk server
                        sendToken(serverInfoMap.get(serverAddressInfo.getServerId()), tokenInfo).addListener(future -> {
                            // 发送成功发射ServerId，不成功发射-1
                            if (future.isSuccess())
                                fluxSink.next(serverAddressInfo.getServerId());
                            else
                                fluxSink.next(-1L);
                        }))).flatMap(serverId -> sendFileInfo(serverInfoMap.get(serverId), fileInfoAtomic.get()))
                .flatMap(sendResult -> {
                    if (sendResult) {
                        sendBackupTaskInfo(new TaskInfo(tokenInfo, fileInfoAtomic.get()));
                        return Mono.just(true);
                    } else {
                        // fileServerRunManager.clearUploadFile(tokenInfo, true);
                        return Mono.error(new ServerAssignException("Server file information send failed!"));
                    }
                }).then(Mono.just(fileInfoAtomic.get()));

    }

    public Mono<FileInfo> questFileServer(TokenInfo tokenInfo) {
        if (tokenInfo == null)
            return Mono.error(new NullPointerException());
        return Mono.defer(() -> {
            // 分配Chunk存储服务器
            FileInfo fileInfo = fileServerRunManager.assignDownloadFileServer(tokenInfo);
            Set<ServerAddressInfo> serverIds = fileServerRunManager.tidyServerId(fileInfo);
            for (ServerAddressInfo serverAddressInfo : serverIds) {
                sendToken(serverInfoMap.get(serverAddressInfo.getServerId()), tokenInfo);
            }
            sendBackupTaskInfo(new TaskInfo(tokenInfo, fileInfo));
            return Mono.just(fileInfo);
        });
    }

    public CompletableFuture<Integer> sendFileChunkCopyAsyncReply(long serverId, FileChunkCopy fileChunkCopy,
                                                                  long timeout, TimeUnit timeUnit) {
        return connectManager.sendDataAsyncReply(serverInfoMap.get(serverId), fileChunkCopy,
                DFSCommand.CT_FILE_CHUNK_COPY, timeout, timeUnit);
    }

    public Mono<DirectInfo> findDirectInfoAsync(String path) {
        return Mono.create(monoSink -> CompletableFuture.supplyAsync(() -> fileInfoManager.findDirectInfo(path))
                .whenCompleteAsync((result, t) -> {
                    if (result == null)
                        monoSink.error(new DirectNotFoundException(String.format("Direct %s not found.", path)));
                    else
                        monoSink.success(result);
                }));
    }

    public Mono<FileInfo> findFileInfoAsync(String path, String fileName) {
        return Mono.create(monoSink -> CompletableFuture.supplyAsync(() -> fileInfoManager.findFileInfo(path, fileName))
                .whenCompleteAsync((result, t) -> {
                    if (result == null)
                        monoSink.error(new FileNotFoundException(String.format("Direct %s not found.", path)));
                    else
                        monoSink.success(result);
                }));
    }

    public boolean addBackupServerId(long serverId) {
        ServerState serverState = findServerState(serverId);
        if (serverState != null && !serverState.isOnline()) {
            recoverStoreServerTask.addBackupServerId(serverId);
            return true;
        }
        return false;
    }

    public boolean addRecoverServerId(long oldServerId, long newServerId) {
        ServerState serverState = findServerState(newServerId);
        if (serverState == null || !serverState.isOnline()) {
            return false;
        }
        recoverStoreServerTask.addRecoverServerId(Tuples.of(oldServerId, newServerId));
        return true;
    }

    public void addFileInfo2BackupMaster(int type, FileInfo fileInfo) {
        if (fileInfo == null || masterServerConfig.getMasterServerMap() == null
                || masterServerConfig.getMasterServerMap().isEmpty())
            return;
        for (Long serverId : masterServerConfig.getMasterServerMap().keySet()) {
            addFileInfo2BackupMaster(serverId, type, fileInfo);
        }
    }

    public void addFileInfo2BackupMaster(long serverId, int type, FileInfo fileInfo) {
        ServerState serverState = findServerState(serverId);
        if (serverState == null || !serverState.isReady())
            return;
        BlockingDeque<BackupMasterFileInfo> backupMasterFileInfos = backupMasterFileInfoBlockingQueue
                .computeIfAbsent(serverId, key -> new LinkedBlockingDeque<>());
        backupMasterFileInfos.addLast(new BackupMasterFileInfo(type, fileInfo));
    }

    public void submitFileFinish(TokenInfo tokenInfo, FileInfo fileInfo) {
        fileInfoManager.submitFileInfo(fileInfo,
                dfsFileUtils.joinFileTempConfigName(fileInfo.getPath(), fileInfo.getFileName()));
        long writeLogDateTime = Instant.now().toEpochMilli();
        getLogFileOperate().writeOperateLog(new OperationLog(writeLogDateTime, OperationLog.OP_TYPE_ADD_FILE_FINISH,
                fileInfo.getPath(), fileInfo.getFileName()));
        getLocalServerState().setWriteLastTime(writeLogDateTime);
        sendBackupClearToken(tokenInfo);
        addFileInfo2BackupMaster(BackupMasterFileInfo.TYPE_ADD, fileInfo);
    }

    public void deleteFileInfo(TokenInfo tokenInfo, FileInfo fileInfo) {
        fileInfoManager.deleteFile(fileInfo.getPath(), fileInfo.getFileName());
        sendBackupClearToken(tokenInfo);
        addFileInfo2BackupMaster(BackupMasterFileInfo.TYPE_DELETE, fileInfo);
    }
}
