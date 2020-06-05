package com.ly.common.service;

import com.ly.common.domain.FileChunkState;
import com.ly.common.constant.ParamConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FileChunkManger {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, FileChunkState> fileChunkStateMap = new ConcurrentHashMap<>();

    public FileChunkState getFileChunkState(String filePath, int size) {
        synchronized (fileChunkStateMap) {
            if (!fileChunkStateMap.containsKey(filePath)) {
                File file = new File(buildConfigFileName(filePath));
                if (file.exists()) {
                    try (FileInputStream fs = new FileInputStream(file); FileChannel channel = fs.getChannel()) {
                        ByteBuffer byteBuffer = ByteBuffer.allocate((int) channel.size());
                        while ((channel.read(byteBuffer)) > 0) {
                            // do nothing
                        }
                        return new FileChunkState(byteBuffer.array());
                    } catch (IOException e) {
                        logger.error("read {} config file chunk is error.", filePath, e);
                    }
                }
            }
            return fileChunkStateMap.computeIfAbsent(filePath, key -> new FileChunkState(size));
        }
    }

    public int setFileChunkState(String filePath, int size, int index) {
        return setFileChunkState(filePath, size, index, true);
    }

    public int setFileChunkState(String filePath, int size, int index, boolean state) {
        FileChunkState fileChunkState = getFileChunkState(filePath, size);
        if (fileChunkState == null) {
            return -1;
        }
        if (state) {
            return fileChunkState.setStateAndCount(index);
        } else {
            return fileChunkState.setStateAndCount(index, false);
        }
    }

    public FileChunkState removeFileChunkState(String filePath) {
        return fileChunkStateMap.remove(filePath);
    }

    private String buildConfigFileName(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return "";
        }
        return filePath + ParamConstants.PARAM_CONFIG_FILE_CHUNK_SUFFIX;
    }

    public void writeConfigFile(String filePath, FileChunkState fileChunkState) {
        if (StringUtils.isEmpty(filePath) || fileChunkState == null) {
            return;
        }
        String cfgFileName = buildConfigFileName(filePath);
        File file = new File(cfgFileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (AsynchronousFileChannel cfgFile = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(fileChunkState.getStates());
            cfgFile.write(byteBuffer, 0);
        } catch (IOException e) {
            logger.error("write {} config file chunk is error.", filePath, e);
        }
    }
}
