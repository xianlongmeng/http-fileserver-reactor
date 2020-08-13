package com.ly.rhdfs.file.server.dfs.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@Component
public class UploadDfsHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public Mono<ServerResponse> uploadFileMasterRequest(ServerRequest request) {
        // param：filename,directName,size,--reserved:user,token
        // distribution：优先级队列，设定算法
        // send：fileInfo chunkInfo ；失败处理:设置StoreServer状态。异步TCP操作方法
        // 返回主Server
    }

    public Mono<ServerResponse> uploadFileServerChunkMasterRequest(ServerRequest request) {
        // 主Server错误，切换分配Server，验证备Server，设置一个为主Server，重新选择一个备Server
        // 返回主StoreServer
    }

    public Mono<ServerResponse> uploadFileServerFinish(ServerRequest request) {
        // parameter:path/file/size/etag/
        // 分片上传完成后，同Master确认MD5信息，MD5为分段MD5及各段MD5的封装
        // 验证是否完成，MD5是否正确
        // 返回是否成功
    }
}
