package com.ly.etag.impl;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

import com.ly.common.service.FileChunkReader;
import com.ly.common.util.MyStringUtils;
import com.ly.etag.ETagComputer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ETagComputer4MD5 implements ETagComputer {

    private DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
    private int bufferInitLen = 1024;

    public DataBufferFactory getDataBufferFactory() {
        return dataBufferFactory;
    }

    public void setDataBufferFactory(DataBufferFactory dataBufferFactory) {
        this.dataBufferFactory = dataBufferFactory;
    }

    public int getBufferInitLen() {
        return bufferInitLen;
    }

    public void setBufferInitLen(int bufferInitLen) {
        this.bufferInitLen = bufferInitLen;
    }

    @Override
    public Mono<String> etagFile(String filePath) {
        return etagFile(Path.of(filePath));
    }

    @Override
    public Mono<String> etagFile(Path filePath) {
        return etagFile(FileChunkReader.readFile2Buffer(filePath));
    }

    @Override
    public Mono<String> etagFile(Flux<DataBuffer> dataBufferFlux) {
        return dataBufferFlux.collect(() -> {
            try {
                return Optional.of(MessageDigest.getInstance("MD5"));
            } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                return Optional.empty();
            }
        }, (optional, dataBuffer) -> optional
                .ifPresent(md5 -> ((MessageDigest) md5).update(dataBuffer.asByteBuffer().array()))).map(optional -> {
                    if (optional.isPresent()) {
                        return MyStringUtils.bytesToHexStr(((MessageDigest) optional.get()).digest());
                    } else {
                        return "";
                    }
                });
    }
}
