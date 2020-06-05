package com.ly.etag.impl;

import com.ly.etag.ETagComputer;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public class ETagComputer4UUIDTimestamp implements ETagComputer {
    @Override
    public Mono<String> etagFile(String filePath) {
        return null;
    }

    @Override
    public Mono<String> etagFile(Path filePath) {
        return null;
    }

    @Override
    public Mono<String> etagFile(Flux<DataBuffer> dataBufferFlux) {
        return null;
    }
}
