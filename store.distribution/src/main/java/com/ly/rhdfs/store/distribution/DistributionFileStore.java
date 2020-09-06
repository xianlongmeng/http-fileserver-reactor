package com.ly.rhdfs.store.distribution;

import com.ly.common.domain.*;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.file.FileTransferInfo;
import com.ly.common.util.ConvertUtil;
import com.ly.common.util.ToolUtils;
import com.ly.rhdfs.communicate.command.DFSCommandFileTransfer;
import com.ly.rhdfs.communicate.socket.parse.DFSCommandParse;
import com.ly.rhdfs.file.config.FileInfoManager;
import com.ly.rhdfs.store.AbstractFileStore;
import com.ly.rhdfs.store.distribution.config.DistributionStoreConfig;
import com.ly.rhdfs.store.exception.StoreFileException;
import com.ly.rhdfs.store.manager.StoreManager;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpRange;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class DistributionFileStore extends AbstractFileStore {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private DistributionStoreConfig config;
    private FileInfoManager fileInfoManager;
    private DFSCommandParse dfsCommandParse;
    private StoreManager storeManager;

    public DistributionFileStore() {
    }

    public void setConfig(DistributionStoreConfig config) {
        this.config = config;
    }

    public void setFileInfoManager(FileInfoManager fileInfoManager) {
        this.fileInfoManager = fileInfoManager;
    }

    public void setDfsCommandParse(DFSCommandParse dfsCommandParse) {
        this.dfsCommandParse = dfsCommandParse;
    }

    public void setStoreManager(StoreManager storeManager){
        this.storeManager=storeManager;
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
        DFSPartChunk dfsPartChunk=(DFSPartChunk)partChunk;
        String fileName=String.format("%s.%d.%s",dfsPartChunk.getTokenInfo().getFileName(),dfsPartChunk.getIndex(),serverConfig.getFileChunkSuffix());
        String fileFullName = takeFilePath(fileName, path).toString();
        dfsPartChunk.setFileFullName(fileFullName);
        File file = new File(fileFullName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
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
                                    .subscribe(DataBufferUtils::release,
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
    }
}
