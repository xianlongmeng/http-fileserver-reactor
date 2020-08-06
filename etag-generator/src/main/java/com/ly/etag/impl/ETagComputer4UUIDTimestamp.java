package com.ly.etag.impl;

import com.fasterxml.uuid.Generators;
import com.ly.etag.ETagComputer;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public class ETagComputer4UUIDTimestamp implements ETagComputer {
    @Override
    public Mono<String> etagFile(String filePath) {
        return Mono.just(Generators.timeBasedGenerator().generate().toString());
    }

    @Override
    public Mono<String> etagFile(Path filePath) {
        return Mono.just(Generators.timeBasedGenerator().generate().toString());
    }

    @Override
    public Mono<String> etagFile(Flux<DataBuffer> dataBufferFlux) {
        return Mono.just(Generators.timeBasedGenerator().generate().toString());
    }
}
