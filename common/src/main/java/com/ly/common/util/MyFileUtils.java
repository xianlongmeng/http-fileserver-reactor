package com.ly.common.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.ly.common.domain.file.FileInfo;

public class MyFileUtils {

    public static final String FILE_CONFIG_SUFFIX = ".rfconf";
    public static final String FILE_TMP_CONFIG_SUFFIX = ".rfconf.tmp";

    public static List<String> findFile(String path, String pattern) {
        Collection<File> fileList = FileUtils.listFiles(new File(path), new RegexFileFilter(pattern),
                TrueFileFilter.INSTANCE);
        List<String> fileNameList = new ArrayList<>();
        for (File file : fileList) {
            if (file != null && file.exists()) {
                fileNameList.add(file.getAbsolutePath());
            }
        }
        return fileNameList;
    }

    public static long diskFreeSpace(String filePath) {
        File disk = new File(filePath);
        if (!disk.exists())
            return 0;
        return disk.getFreeSpace();
    }

    public static FileInfo JSONReadFileInfo(String fileName) {
        if (StringUtils.isEmpty(fileName))
            return null;
        try {
            String content = FileUtils.readFileToString(new File(fileName), StandardCharsets.UTF_8);
            return JSON.parseObject(content, FileInfo.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean JSONWriteFile(String fileName, Object obj) {
        if (StringUtils.isEmpty(fileName) || obj == null)
            return false;
        try {
            FileWriter fileWriter = new FileWriter(fileName);
            JSON.writeJSONString(fileWriter, obj);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String joinFileName(String path, String fileName) {
        if (StringUtils.isEmpty(path))
            return fileName;
        if (StringUtils.isEmpty(fileName))
            return path;
        return String.format("%s%s%s", path, File.separator, fileName);
    }

    public static String joinFileConfigName(String path, String fileName) {
        if (StringUtils.isEmpty(path))
            return fileName;
        if (StringUtils.isEmpty(fileName))
            return path;
        return String.format("%s%s%s%s", path, File.separator, fileName, FILE_CONFIG_SUFFIX);
    }

    public static String joinFileTempConfigName(String path, String fileName) {
        if (StringUtils.isEmpty(path))
            return fileName;
        if (StringUtils.isEmpty(fileName))
            return path;
        return String.format("%s%s%s%s", path, File.separator, fileName, FILE_TMP_CONFIG_SUFFIX);
    }

    public static boolean renameFile(String oldFileName, String newFileName) {
        if (StringUtils.isEmpty(oldFileName) || StringUtils.isEmpty(newFileName))
            return false;
        File file = new File(oldFileName);
        if (!file.exists()) {
            return false;
        }
        return file.renameTo(new File(newFileName));
    }
}
