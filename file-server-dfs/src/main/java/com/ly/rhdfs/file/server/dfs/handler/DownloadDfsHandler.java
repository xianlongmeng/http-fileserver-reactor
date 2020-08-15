package com.ly.rhdfs.file.server.dfs.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.ly.etag.ETagComputer;

import reactor.core.publisher.Mono;

@Component
@ComponentScan({"com.ly.rhdfs.config"})
public class DownloadDfsHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ETagComputer eTagComputer;

    @Autowired
    public void setETagComputer(ETagComputer eTagComputer) {
        this.eTagComputer = eTagComputer;
    }

    public Mono<ServerResponse> downloadFileMasterRequest(ServerRequest request) {

    }

    public Mono<ServerResponse> downloadFileChunkMasterRequest(ServerRequest request) {

    }

    public Mono<ServerResponse> downloadFileFinish(ServerRequest request) {

    }

}
