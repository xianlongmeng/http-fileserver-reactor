package com.ly.etag;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public interface ETagComputer {
    Mono<String> etagFile(String filePath);
    Mono<String> etagFile(Path filePath);
    Mono<String> etagFile(Flux<DataBuffer> dataBufferFlux);

}
