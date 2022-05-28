package com.ly.rhdfs.file.util;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.ly.common.domain.file.FileInfo;
import com.ly.common.domain.file.ItemInfo;

public class DfsFileUtils {

    private Logger logger= LoggerFactory.getLogger(getClass());
    private static final String FILE_CONFIG_SUFFIX = ".rfconf";
    private static final String FILE_TMP_CONFIG_SUFFIX = ".rfconf.tmp";

    private String fileConfigSuffix=FILE_CONFIG_SUFFIX;
    private String fileTmpConfigSuffix=FILE_TMP_CONFIG_SUFFIX;
    private String fileRootPath;

    public String getFileConfigSuffix() {
        return fileConfigSuffix;
    }

    public void setFileConfigSuffix(String fileConfigSuffix) {
        this.fileConfigSuffix = fileConfigSuffix;
    }

    public String getFileTmpConfigSuffix() {
        return fileTmpConfigSuffix;
    }

    public void setFileTmpConfigSuffix(String fileTmpConfigSuffix) {
        this.fileTmpConfigSuffix = fileTmpConfigSuffix;
    }

    public String getFileRootPath() {
        return fileRootPath;
    }

    public void setFileRootPath(String fileRootPath) {
        this.fileRootPath = fileRootPath;
    }

    public List<String> findFilePath(String path, String pattern, boolean isSubDirect) {
        return findFilePath(joinFileName(fileRootPath,path), new RegexFileFilter(pattern), isSubDirect);
    }

    public List<String> findFilePath(String path, IOFileFilter ioFileFilter, boolean isSubDirect) {
        List<String> fileNameList = new ArrayList<>();
        try {
            Collection<File> fileList = FileUtils.listFiles(new File(joinFileName(fileRootPath,path)), ioFileFilter,
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

    public List<ItemInfo> findFileItem(String path, String pattern, boolean isSubDirect) {
        List<ItemInfo> itemList = new ArrayList<>();
        try {
            Collection<File> fileList = FileUtils.listFiles(new File(joinFileName(fileRootPath,path)), new RegexFileFilter(pattern),
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

    public List<ItemInfo> findDirectInfoItem(String path, boolean isSubDirect) {
        List<ItemInfo> itemList = new ArrayList<>();
        try {
            Collection<File> fileList = FileUtils.listFilesAndDirs(new File(joinFileName(fileRootPath,path)),
                    new SuffixFileFilter(getFileConfigSuffix()), isSubDirect ? TrueFileFilter.INSTANCE : null);
            for (File file : fileList) {
                if (file != null && file.exists()) {
                    itemList.add(new ItemInfo(file.getName(), file.isDirectory()));
                }
            }
        } catch (Exception ignored) {

        }
        return itemList;
    }

    public long diskFreeSpace(String filePath) {
        File disk = new File(filePath);
        if (!disk.exists())
            return 0;
        return disk.getFreeSpace();
    }

    public FileInfo jsonReadFileInfo(String fileName) {
        if (StringUtils.isEmpty(fileName))
            return null;
        return jsonReadConfigFileInfo(joinFileName(fileRootPath,fileName));
    }

    public FileInfo jsonReadConfigFileInfo(String configFileName) {
        if (StringUtils.isEmpty(configFileName))
            return null;
        try {
            String content = FileUtils.readFileToString(new File(configFileName), StandardCharsets.UTF_8);
            return JSON.parseObject(content, FileInfo.class);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            return null;
        }
    }

    public boolean jsonWriteFile(String fileName, Object obj) {
        if (StringUtils.isEmpty(fileName) || obj == null)
            return false;
        return jsonWriteConfigFile(joinFileName(fileRootPath,fileName),obj);
    }
    public boolean jsonWriteConfigFile(String configFileName, Object obj) {
        if (StringUtils.isEmpty(configFileName) || obj == null)
            return false;
        try {
            FileWriter fileWriter = new FileWriter(configFileName);
            JSON.writeJSONString(fileWriter, obj);
            return true;
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            return false;
        }
    }

    public String joinFileName(String ... paths) {
        if (paths.length==0)
            return "";
        String path="";
        for (String p : paths) {
            if (StringUtils.isEmpty(p))
                continue;
            if (StringUtils.isEmpty(path))
                path = p;
            else {
                while (p.startsWith("/") || p.startsWith("\\")){
                    p=p.substring(1);
                }
                path = String.format("%s%s%s", path, File.separator, p);
            }
        }
        return StringUtils.cleanPath(path);
    }

    public String joinFileConfigName(String path, String fileName) {
        if (StringUtils.isEmpty(path))
            return StringUtils.cleanPath(fileName);
        if (StringUtils.isEmpty(fileName))
            return StringUtils.cleanPath(path);
        return StringUtils.cleanPath(String.format("%s%s%s%s", path, File.separator, fileName, getFileConfigSuffix()));
    }
    public String joinFileConfigName(String fileFullName) {
        if (StringUtils.isEmpty(fileFullName))
            return null;
        return StringUtils.cleanPath(String.format("%s%s", fileFullName, getFileConfigSuffix()));
    }

    public String joinFileTempConfigName(String path, String fileName) {
        if (StringUtils.isEmpty(path))
            return StringUtils.cleanPath(fileName);
        if (StringUtils.isEmpty(fileName))
            return StringUtils.cleanPath(path);
        return StringUtils.cleanPath(String.format("%s%s%s%s", path, File.separator, fileName, getFileTmpConfigSuffix()));
    }

    public boolean renameFile(String oldFileName, String newFileName) {
        if (StringUtils.isEmpty(oldFileName) || StringUtils.isEmpty(newFileName))
            return false;
        File file = new File(joinFileName(fileRootPath,oldFileName));
        if (!file.exists()) {
            return false;
        }
        return file.renameTo(new File(joinFileName(fileRootPath,newFileName)));
    }

    public boolean fileConfigExist(String path, String fileName) {
        File file = new File(joinFileName(fileRootPath,joinFileConfigName(path, fileName)));
        return file.exists();
    }

    public boolean fileExist(String path, String fileName) {
        File file = new File(joinFileName(fileRootPath,joinFileName(path, fileName)));
        if (file.exists())
            return true;
        file = new File(joinFileName(fileRootPath,joinFileConfigName(path, fileName)));
//        if (file.exists())
//            return true;
//        file = new File(joinFileName(fileRootPath,joinFileTempConfigName(path, fileName)));
        return file.exists();
    }

    public boolean fileDelete(String path, String fileName) {
        boolean res = true;
        File file = new File(joinFileName(fileRootPath,joinFileName(path, fileName)));
        if (file.exists())
            res = file.delete();
        file = new File(joinFileName(fileRootPath,joinFileConfigName(path, fileName)));
        if (file.exists())
            res &= file.delete();
        file = new File(joinFileName(fileRootPath,joinFileTempConfigName(path, fileName)));
        if (file.exists())
            res &= file.delete();
        return res;
    }

    public byte[] readFileInfo(String path, String fileName) {
        String filePath = joinFileConfigName(path, fileName);
        if (StringUtils.isEmpty(filePath))
            return null;
        File file = new File(joinFileName(fileRootPath,filePath));
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
