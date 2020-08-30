package com.ly.etag;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public interface ETagComputer {
    default Mono<String> etagFile(String filePath){
        return etagFile(filePath,-1);
    }
    default Mono<String> etagFile(Path filePath){
        return etagFile(filePath,-1);
    }
    default Mono<String> etagFile(String filePath,Flux<DataBuffer> dataBufferFlux){
        return etagFile(filePath,dataBufferFlux,-1);
    }
    Mono<String> etagFile(String filePath,int chunk);
    Mono<String> etagFile(Path filePath,int chunk);
    Mono<String> etagFile(String filePath,Flux<DataBuffer> dataBufferFlux,int chunk);

}
