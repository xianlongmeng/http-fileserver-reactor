package com.ly.rhdfs.manager.file;

import com.ly.common.domain.file.DirectInfo;
import com.ly.common.domain.file.FileInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
    private final Map<String, DirectInfo> directInfoCacheMap=new ConcurrentHashMap<>();
    private final Map<String, FileInfo> fileInfoCacheMap=new ConcurrentHashMap<>();
}
