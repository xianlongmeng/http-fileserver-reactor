package com.ly.rhdfs.store.single;

import java.io.File;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.ly.rhdfs.config.ServerConfig;
import com.ly.rhdfs.store.AbstractFileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpRange;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;

import com.ly.common.domain.FileRanges;
import com.ly.common.domain.PartChunk;
import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.ResultValueInfo;
import com.ly.common.service.FileChunkManger;
import com.ly.common.util.ConvertUtil;
import com.ly.common.util.SpringContextUtil;
import com.ly.common.util.ToolUtils;
import com.ly.rhdfs.store.StoreFile;
import com.ly.rhdfs.store.single.config.SingleStoreConfig;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SingleFileStore extends AbstractFileStore {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private SingleStoreConfig config;

    public SingleFileStore() {
    }

    @Autowired
    public void setConfig(SingleStoreConfig config) {
        this.config = config;
    }

    @Override
    public LocalDateTime takeFileUpdateTime(String fileId, String path) {
        String uploadFilePath = buildFilePath(fileId, path);
        if (StringUtils.isEmpty(uploadFilePath)) {
            return null;
        }
        File file=new File(uploadFilePath);
        if (file.exists()){
            return ConvertUtil.toGMTLocalDateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()));
        }
        return null;
    }
    @Override
    public Instant takeFileInstant(String fileId, String path) {
        String uploadFilePath = buildFilePath(fileId, path);
        if (StringUtils.isEmpty(uploadFilePath)) {
            return null;
        }
        File file=new File(uploadFilePath);
        if (file.exists()){
            return Instant.ofEpochMilli(file.lastModified());
        }
        return null;
    }

    @Override
    public long takeFileSize(String fileId, String path) {
        String uploadFilePath = buildFilePath(fileId, path);
        if (StringUtils.isEmpty(uploadFilePath)) {
            return 0;
        }
        File file=new File(uploadFilePath);
        if (file.exists()){
            return file.length();
        }
        return 0;
    }

    @Override
    public Mono<ResultValueInfo<FilePart>> storeFile(FilePart filePart, String path, PartChunk partChunk) {
        String name = filePart.name();
        String fileName = filePart.filename();
        logger.debug("begin store file,name:{}-fileName:{}", name, fileName);
        String fileFullName = takeFilePath(fileName, path).toString();
        File file = new File(fileFullName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return Mono.create(sink -> {
            try {
                if (!partChunk.isChunked()) {
                    filePart.transferTo(takeFilePath(fileName, path)).subscribe(
                            v -> sink.success(new ResultValueInfo<>(ResultInfo.S_OK, filePart)),
                            e -> sink.success(new ResultValueInfo<>(ResultInfo.S_ERROR, "write.file.201", e.getMessage(),
                                    filePart)));

                } else {
                    Path fileTmpPath = takeFilePath(fileName, path, true);
                    AsynchronousFileChannel asynchronousFileChannel = AsynchronousFileChannel.open(fileTmpPath,
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    sink.onDispose(() -> ToolUtils.closeChannel(asynchronousFileChannel));
                    if (filePart.headers().getContentLength() > partChunk.getChunkSize()) {
                        sink.success(new ResultValueInfo<>(ResultInfo.S_ERROR, "part.size.221", "part size too many",
                                filePart));
                    } else if (partChunk.getChunk() < 0 || partChunk.getChunk() >= partChunk.getChunkCount()) {
                        sink.success(new ResultValueInfo<>(ResultInfo.S_ERROR, "part.index.220", "part index out range",
                                filePart));
                    } else {
                        DataBufferUtils
                                .write(filePart.content(), asynchronousFileChannel,
                                        partChunk.getChunk() * partChunk.getChunkSize())
                                .subscribe(DataBufferUtils::release,
                                        e -> sink.success(new ResultValueInfo<>(ResultInfo.S_ERROR, "write.file.201",
                                                e.getMessage(), filePart)),
                                        () -> {
                                            int count = fileChunkManger.setFileChunkState(fileFullName,
                                                    partChunk.getChunkCount(), partChunk.getChunk());
                                            if (count >= partChunk.getChunkCount()) {
                                                File fileTmp = new File(fileTmpPath.toUri());
                                                if (fileTmp.renameTo(file)) {
                                                    sink.success(new ResultValueInfo<>(ResultInfo.S_OK, filePart));
                                                } else {
                                                    sink.success(new ResultValueInfo<>(ResultInfo.S_ERROR,
                                                            "rename.file.211",
                                                            String.format(
                                                                    "upload file complete,but %s rename to %s is error",
                                                                    fileTmp.getAbsolutePath(), fileFullName),
                                                            filePart));
                                                }
                                            }
                                        });
                    }
                }
            } catch (IOException ex) {
                sink.success(new ResultValueInfo<>(ResultInfo.S_ERROR, "open.file.211", ex.getMessage(), filePart));
            }
        });
    }
}
