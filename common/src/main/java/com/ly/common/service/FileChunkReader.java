package com.ly.common.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileChunkReader {

    private static DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
    private static int bufferInitLen = 1024;

    public static Flux<DataBuffer> readFile2Buffer(String fileName) {
        return readFile2Buffer(Path.of(fileName));
    }

    public static Flux<DataBuffer> readFile2Buffer(Path filePath) {
        return readFile2Buffer(filePath, 0);
    }

    public static Flux<DataBuffer> readFile2Buffer(Resource resource) {
        return readFile2Buffer(resource, 0);
    }

    public static Flux<DataBuffer> readFile2Buffer(String fileName, long position) {
        return readFile2Buffer(Path.of(fileName), position);
    }

    public static Flux<DataBuffer> readFile2Buffer(Path filePath, long position) {
        return DataBufferUtils.readAsynchronousFileChannel(
                () -> AsynchronousFileChannel.open(filePath, StandardOpenOption.READ), position, dataBufferFactory,
                bufferInitLen);
    }

    public static Flux<DataBuffer> readFile2Buffer(Resource resource, long position) {
        return DataBufferUtils.readAsynchronousFileChannel(
                () -> AsynchronousFileChannel.open(resource.getFile().toPath(), StandardOpenOption.READ), position,
                dataBufferFactory, bufferInitLen);
    }

    public static Flux<DataBuffer> readFile2Buffer(String fileName, long position, long count) {
        return readFile2Buffer(Path.of(fileName), position, count);
    }

    public static Flux<DataBuffer> readFile2Buffer(Path filePath, long position, long count) {
        return DataBufferUtils
                .takeUntilByteCount(
                        DataBufferUtils.readAsynchronousFileChannel(() -> AsynchronousFileChannel
                                .open(filePath, StandardOpenOption.READ), position, dataBufferFactory, bufferInitLen),
                        count);
    }

    public static Flux<DataBuffer> readFile2Buffer(Resource resource, long position, long count) {
        return DataBufferUtils
                .takeUntilByteCount(
                        DataBufferUtils.readAsynchronousFileChannel(() -> AsynchronousFileChannel
                                .open(resource.getFile().toPath(), StandardOpenOption.READ), position, dataBufferFactory, bufferInitLen),
                        count);
    }
}
