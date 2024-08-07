package com.ly.rhdfs.store;

import com.ly.common.domain.FileRanges;
import com.ly.common.service.FileChunkManager;
import com.ly.rhdfs.file.util.DfsFileUtils;
import com.ly.common.util.SpringContextUtil;
import com.ly.rhdfs.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpRange;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.List;

public abstract class AbstractFileStore implements StoreFile {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final FileChunkManager fileChunkManager;
    protected ServerConfig serverConfig;
    protected DfsFileUtils dfsFileUtils;

    protected AbstractFileStore() {
       fileChunkManager = SpringContextUtil.getBean(FileChunkManager.class);
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void setDfsFileUtils(DfsFileUtils dfsFileUtils) {
        this.dfsFileUtils = dfsFileUtils;
    }
    @Override
    public boolean existed(String fileId, String path) {
        String uploadFilePath = buildFilePath(fileId, path);
        if (StringUtils.isEmpty(uploadFilePath)) {
            return false;
        }
        return new File(uploadFilePath).exists();
    }
    @Override
    public String takeFilePathString(String fileId, String path, boolean temp) {
        String uploadFilePath = buildFilePath(fileId, path);
        if (StringUtils.isEmpty(uploadFilePath)) {
            return null;
        }
        if (temp) {
            if (serverConfig.getTmpFileSuffix().startsWith("."))
                return uploadFilePath + serverConfig.getTmpFileSuffix();
            else
                return uploadFilePath + "." + serverConfig.getTmpFileSuffix();
        } else {
            return uploadFilePath;
        }
    }

    protected String buildFilePath(String fileId, String path) {
        return dfsFileUtils.joinFileName(serverConfig.getFileRootPath(), path, fileId);
    }

    @Override
    public Mono<FileRanges> loadFile(String fileId, String path, List<HttpRange> ranges) {
        return Mono.just(new FileRanges(new FileSystemResource(takeFilePath(fileId,path))));
    }
}
