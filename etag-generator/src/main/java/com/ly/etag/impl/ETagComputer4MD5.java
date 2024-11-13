package com.ly.etag.impl;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import com.ly.common.service.FileChunkReader;
import com.ly.common.util.MyStringUtils;
import com.ly.etag.ETagAccess;
import com.ly.etag.ETagComputer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ETagComputer4MD5 implements ETagComputer {

    private DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
    private int bufferInitLen = 1024;

    private ETagAccess eTagAccess;

    public void setETagAccess(ETagAccess eTagAccess) {
        this.eTagAccess = eTagAccess;
    }

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
    public Mono<String> etagFile(String filePath, int chunk) {
        return etagFile(Path.of(filePath), chunk);
    }

    @Override
    public Mono<String> etagFile(Path filePath, int chunk) {
        return etagFile(filePath.toString(), FileChunkReader.readFile2Buffer(filePath), chunk);
    }

    @Override
    public Mono<String> etagFile(String filePath, Flux<DataBuffer> dataBufferFlux, int chunk) {
        if (eTagAccess != null) {
            String etag = eTagAccess.readEtag(filePath, chunk);
            if (StringUtils.hasLength(etag))
                return Mono.just(etag);
        }
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
                }).map(etag -> {
                    if (eTagAccess != null) {
                        eTagAccess.saveEtag(filePath, etag, chunk);
                    }
                    return etag;
                });
    }
}
