package com.ly.common.util;

import com.alibaba.fastjson.JSON;
import com.ly.common.domain.file.FileInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MyFileUtils {

    public static List<String> findFile(String path,String pattern){
        Collection<File> fileList=FileUtils.listFiles(new File(path),new RegexFileFilter(pattern), TrueFileFilter.INSTANCE);
        List<String> fileNameList=new ArrayList<>();
        for (File file:fileList){
            if (file!=null && file.exists()){
                fileNameList.add(file.getAbsolutePath());
            }
        }
        return fileNameList;
    }

    public static long diskFreeSpace(String filePath){
        File disk=new File(filePath);
        if (!disk.exists())
            return 0;
        return disk.getFreeSpace();
    }
    public static FileInfo JSONReadFileInfo(String fileName){
        if (StringUtils.isEmpty(fileName))
            return null;
        try {
            String content = FileUtils.readFileToString(new File(fileName), StandardCharsets.UTF_8);
            return JSON.parseObject(content,FileInfo.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static boolean JSONWriteFile(String fileName,Object obj){
        if (StringUtils.isEmpty(fileName) || obj==null)
            return false;
        try {
            FileWriter fileWriter=new FileWriter(fileName);
            JSON.writeJSONString(fileWriter,obj);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    public static String joinFileName(String path,String fileName){
        if (StringUtils.isEmpty(path))
            return fileName;
        if (StringUtils.isEmpty(fileName))
            return path;
        return String.format("%s%s%s",path,File.separator,fileName);
    }
}
