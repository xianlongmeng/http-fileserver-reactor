package com.ly.rhdfs.store;

import com.ly.common.domain.FileRanges;
import com.ly.common.domain.PartChunk;
import com.ly.common.domain.ResultValueInfo;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpRange;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface StoreFile {
    boolean existed(String fileId,String path);

    default Path takeFilePath(String fileId, String path){
        return takeFilePath(fileId,path,false);
    }

    default Path takeFilePath(String fileId, String path, boolean temp){
        String filePathString=takeFilePathString(fileId,path,temp);
        if (StringUtils.isEmpty(filePathString))
            return null;
        return new File(filePathString).toPath();
    }

    default String takeFilePathString(String fileId, String path){
        return takeFilePathString(fileId,path,false);
    }
    String takeFilePathString(String fileId, String path, boolean temp);

    LocalDateTime takeFileUpdateTime(String fileId, String path);

    Instant takeFileInstant(String fileId, String path);

    long takeFileSize(String fileId, String path);

//    default Mono<ResultValueInfo<FilePart>> storeFile(FilePart filePart, String path, String fileName) {
//        return storeFile(filePart, path,fileName, new PartChunk(false));
//    }

    Mono<ResultValueInfo<FilePart>> storeFile(FilePart filePart, String path, PartChunk partChunk);

    Mono<FileRanges> loadFile(String fileId, String path, List<HttpRange> ranges);
}
