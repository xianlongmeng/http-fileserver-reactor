package com.ly.rhdfs.store.distribution;

import com.ly.common.domain.DFSPartChunk;
import com.ly.common.domain.PartChunk;
import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.ResultValueInfo;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.util.ConvertUtil;
import com.ly.common.util.ToolUtils;
import com.ly.rhdfs.communicate.socket.parse.DFSCommandParse;
import com.ly.rhdfs.file.config.FileInfoManager;
import com.ly.rhdfs.store.AbstractFileStore;
import com.ly.rhdfs.store.distribution.config.DistributionStoreConfig;
import com.ly.rhdfs.store.exception.StoreFileException;
import com.ly.rhdfs.store.manager.StoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DistributionFileStore extends AbstractFileStore {

    private final static  String STORE_BACKUP_MODE_SYNC="SYNC";
    private final static  String STORE_BACKUP_MODE_ASYNC="ASYNC";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private FileInfoManager fileInfoManager;
    private StoreManager storeManager;

    public DistributionFileStore() {
    }

    public void setFileInfoManager(FileInfoManager fileInfoManager) {
        this.fileInfoManager = fileInfoManager;
    }

    public void setStoreManager(StoreManager storeManager) {
        this.storeManager = storeManager;
    }

    @Override
    public LocalDateTime takeFileUpdateTime(String fileId, String path) {
        FileInfo fileInfo = fileInfoManager.findFileInfo(dfsFileUtils.joinFileName(path, fileId));
        if (fileId == null)
            return null;
        return ConvertUtil.toGMTLocalDateTime(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(fileInfo.getLastModifyTime()), ZoneId.systemDefault()));
    }

    @Override
    public Instant takeFileInstant(String fileId, String path) {
        FileInfo fileInfo = fileInfoManager.findFileInfo(dfsFileUtils.joinFileName(path, fileId));
        if (fileId == null)
            return null;
        return Instant.ofEpochMilli(fileInfo.getLastModifyTime());
    }

    @Override
    public long takeFileSize(String fileId, String path) {
        FileInfo fileInfo = fileInfoManager.findFileInfo(dfsFileUtils.joinFileName(path, fileId));
        if (fileId == null)
            return 0;
        return fileInfo.getSize();
    }

    @Override
    public Mono<ResultValueInfo<FilePart>> storeFile(FilePart filePart, String path, PartChunk partChunk) {
        // chunk如何处理
        if (!(partChunk instanceof DFSPartChunk)) {
            return Mono.error(new StoreFileException("part chunk info error"));
        }
        DFSPartChunk dfsPartChunk = (DFSPartChunk) partChunk;
        String fileName = String.format("%s.%d.%s", dfsPartChunk.getTokenInfo().getFileName(), dfsPartChunk.getIndex(), serverConfig.getFileChunkSuffix());
        String fileFullName = takeFilePath(fileName, path).toString();
        dfsPartChunk.setFileFullName(fileFullName);
        File file = new File(fileFullName);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            return Mono.error(new StoreFileException("file write failed!"));
        }
        if (STORE_BACKUP_MODE_ASYNC.equals(storeManager.getServerConfig().getStoreBackupMode())){
            return Mono
                    .create(sink -> {
                        try {
                            Path filePath = takeFilePath(fileName, path);
                            AsynchronousFileChannel asynchronousFileChannel = AsynchronousFileChannel.open(filePath,
                                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                            sink.onDispose(() -> ToolUtils.closeChannel(asynchronousFileChannel));
                            if (filePart.headers().getContentLength() > partChunk.getChunkSize()) {
                                sink.error(new StoreFileException("part size too many"));
                            } else if (partChunk.getChunk() < 0 || partChunk.getChunk() >= partChunk.getChunkCount()) {
                                sink.error(new StoreFileException("part index out range"));
                            } else {
                                Flux<DataBuffer> dataCache=filePart.content();
                                storeManager.sendBackupStoreFile(dataCache,storeManager.newBackupChunkInfo(dfsPartChunk));
                                DataBufferUtils
                                        .write(dataCache, asynchronousFileChannel,
                                                partChunk.getChunk() * partChunk.getChunkSize())
                                        .subscribe(DataBufferUtils::release,//release memory is exception???
                                                e -> sink.error(new StoreFileException(e.getMessage(), e)), () -> {
                                                    // 修改为分块数据的上传信息
                                                    int count = fileChunkManger.setFileChunkState(fileFullName,
                                                            partChunk.getChunkCount(), partChunk.getChunk());
                                                    if (count >= partChunk.getChunkCount()) {
                                                        fileChunkManger.removeFileChunkState(fileFullName);
                                                        sink.success(new ResultValueInfo<>(ResultInfo.S_OK, filePart));
                                                    }
                                                });
                            }
                        } catch (IOException ex) {
                            sink.error(new StoreFileException(ex.getMessage(), ex));
                        }
                    });
        }else{
            return Mono
                    .create(sink -> {
                        try {
                            Path filePath = takeFilePath(fileName, path);
                            AsynchronousFileChannel asynchronousFileChannel = AsynchronousFileChannel.open(filePath,
                                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                            sink.onDispose(() -> ToolUtils.closeChannel(asynchronousFileChannel));
                            if (filePart.headers().getContentLength() > partChunk.getChunkSize()) {
                                sink.error(new StoreFileException("part size too many"));
                            } else if (partChunk.getChunk() < 0 || partChunk.getChunk() >= partChunk.getChunkCount()) {
                                sink.error(new StoreFileException("part index out range"));
                            } else {
                                Flux<DataBuffer> dataCache = filePart.content().cache();
                                Flux.create((FluxSink<Long> fluxSink) -> Flux
                                        .fromIterable(dfsPartChunk.getFileInfo().getFileChunkList().get(dfsPartChunk.getIndex())
                                                .getChunkServerIdList())
                                        .parallel(3)
                                        .runOn(Schedulers.parallel())
                                        .subscribe(serverId -> {
                                                    if (serverId == serverConfig.getCurrentServerId()) {
                                                        DataBufferUtils
                                                                .write(dataCache, asynchronousFileChannel,
                                                                        partChunk.getChunk() * partChunk.getChunkSize())
                                                                .subscribe(DataBufferUtils::release,
                                                                        e -> fluxSink.error(new StoreFileException(e.getMessage(), e)),
                                                                        () -> {
                                                                            // 修改为分块数据的上传信息
                                                                            int count = fileChunkManger.setFileChunkState(fileFullName,
                                                                                    partChunk.getChunkCount(), partChunk.getChunk());
                                                                            if (count >= partChunk.getChunkCount()) {
                                                                                fileChunkManger.removeFileChunkState(fileFullName);
                                                                                fluxSink.next(serverId);
                                                                            }
                                                                        });
                                                    } else {
                                                        storeManager.sendBackupStoreFile(fluxSink, serverId, dataCache, dfsPartChunk);
                                                    }
                                                }
                                        ))
                                        .doFinally(signalType -> {
                                            dataCache.subscribe(DataBufferUtils.releaseConsumer());
                                        })
                                        .subscribe(null,
                                                e -> sink.error(new StoreFileException("part index out range", e)),
                                                () -> sink.success(new ResultValueInfo<>(ResultInfo.S_OK, filePart)));

                            }
                        } catch (IOException ex) {
                            sink.error(new StoreFileException(ex.getMessage(), ex));
                        }
                    });
        }
    }
}
