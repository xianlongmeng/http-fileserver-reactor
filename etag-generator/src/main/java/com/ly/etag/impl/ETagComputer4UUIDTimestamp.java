package com.ly.etag.impl;

import java.nio.file.Path;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.StringUtils;

import com.fasterxml.uuid.Generators;
import com.ly.etag.ETagAccess;
import com.ly.etag.ETagComputer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ETagComputer4UUIDTimestamp implements ETagComputer {

    private ETagAccess eTagAccess;

    public void setETagAccess(ETagAccess eTagAccess) {
        this.eTagAccess = eTagAccess;
    }

    @Override
    public Mono<String> etagFile(String filePath, int chunk) {
        if (eTagAccess != null) {
            String etag = eTagAccess.readEtag(filePath, chunk);
            if (StringUtils.hasLength(etag))
                return Mono.just(etag);
        }
        return Mono.just(Generators.timeBasedGenerator().generate().toString()).map(etag -> {
            if (eTagAccess != null) {
                eTagAccess.saveEtag(filePath, etag, chunk);
            }
            return etag;
        });
    }

    @Override
    public Mono<String> etagFile(Path filePath, int chunk) {
        return etagFile(filePath.toString());
    }

    @Override
    public Mono<String> etagFile(String filePath, Flux<DataBuffer> dataBufferFlux, int chunk) {
        return etagFile(filePath);
    }
}
