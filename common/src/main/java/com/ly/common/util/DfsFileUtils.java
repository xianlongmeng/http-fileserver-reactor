package com.ly.common.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.file.ItemInfo;

public class DfsFileUtils {

    public static final String FILE_CONFIG_SUFFIX = ".rfconf";
    public static final String FILE_TMP_CONFIG_SUFFIX = ".rfconf.tmp";

    public static List<String> findFilePath(String path, String pattern, boolean isSubDirect) {
        return findFilePath(path, new RegexFileFilter(pattern), isSubDirect);
    }

    public static List<String> findFilePath(String path, IOFileFilter ioFileFilter, boolean isSubDirect) {
        List<String> fileNameList = new ArrayList<>();
        try {
            Collection<File> fileList = FileUtils.listFiles(new File(path), ioFileFilter,
                    isSubDirect ? TrueFileFilter.INSTANCE : null);

            for (File file : fileList) {
                if (file != null && file.exists()) {
                    fileNameList.add(file.getAbsolutePath());
                }
            }
        } catch (Exception ignored) {
        }
        return fileNameList;
    }

    public static List<ItemInfo> findFileItem(String path, String pattern, boolean isSubDirect) {
        List<ItemInfo> itemList = new ArrayList<>();
        try {
            Collection<File> fileList = FileUtils.listFiles(new File(path), new RegexFileFilter(pattern),
                    isSubDirect ? TrueFileFilter.INSTANCE : null);

            for (File file : fileList) {
                if (file != null && file.exists()) {
                    itemList.add(new ItemInfo(file.getName(), file.isDirectory()));
                }
            }
        } catch (Exception ignored) {

        }
        return itemList;
    }

    public static List<ItemInfo> findDirectInfoItem(String path, boolean isSubDirect) {
        List<ItemInfo> itemList = new ArrayList<>();
        try {
            Collection<File> fileList = FileUtils.listFilesAndDirs(new File(path),
                    new SuffixFileFilter(FILE_CONFIG_SUFFIX), isSubDirect ? TrueFileFilter.INSTANCE : null);
            for (File file : fileList) {
                if (file != null && file.exists()) {
                    itemList.add(new ItemInfo(file.getName(), file.isDirectory()));
                }
            }
        } catch (Exception ignored) {

        }
        return itemList;
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
            return StringUtils.cleanPath(fileName);
        if (StringUtils.isEmpty(fileName))
            return StringUtils.cleanPath(path);
        return StringUtils.cleanPath(String.format("%s%s%s%s", path, File.separator, fileName, FILE_CONFIG_SUFFIX));
    }

    public static String joinFileTempConfigName(String path, String fileName) {
        if (StringUtils.isEmpty(path))
            return StringUtils.cleanPath(fileName);
        if (StringUtils.isEmpty(fileName))
            return StringUtils.cleanPath(path);
        return StringUtils.cleanPath(String.format("%s%s%s%s", path, File.separator, fileName, FILE_TMP_CONFIG_SUFFIX));
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

    public static boolean fileConfigExist(String path, String fileName) {
        File file = new File(joinFileConfigName(path, fileName));
        return file.exists();
    }

    public static boolean fileExist(String path, String fileName) {
        File file = new File(joinFileName(path, fileName));
        if (file.exists())
            return true;
        file = new File(joinFileConfigName(path, fileName));
        if (file.exists())
            return true;
        file = new File(joinFileTempConfigName(path, fileName));
        return file.exists();
    }

    public static boolean fileDelete(String path, String fileName) {
        boolean res = true;
        File file = new File(joinFileName(path, fileName));
        if (file.exists())
            res = file.delete();
        file = new File(joinFileConfigName(path, fileName));
        if (file.exists())
            res &= file.delete();
        file = new File(joinFileTempConfigName(path, fileName));
        if (file.exists())
            res &= file.delete();
        return res;
    }

    public static byte[] readFileInfo(String path, String fileName) {
        String filePath = joinFileConfigName(path, fileName);
        if (StringUtils.isEmpty(filePath))
            return null;
        File file = new File(filePath);
        if (!file.exists())
            return null;

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length())) {

            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
