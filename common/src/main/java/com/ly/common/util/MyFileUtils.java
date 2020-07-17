package com.ly.common.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
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
}
