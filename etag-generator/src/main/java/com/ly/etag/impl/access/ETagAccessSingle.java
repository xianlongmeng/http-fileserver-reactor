package com.ly.etag.impl.access;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.ly.rhdfs.file.config.FileInfoManager;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;

import com.ly.etag.ETagAccess;

public class ETagAccessSingle implements ETagAccess {

    private String etagFileSuffix;

    @Override
    public String getEtagFileSuffix() {
        return etagFileSuffix;
    }

    @Override
    public void setEtagFileSuffix(String etagFileSuffix) {
        this.etagFileSuffix = etagFileSuffix;
    }

    private String buildEtagFileName(String filePath, int chunk) {
        if (StringUtils.isEmpty(filePath))
            return null;
        if (chunk >= 0) {
            return String.format("%s_%d.%s", filePath, chunk, getEtagFileSuffix());
        } else {
            return String.format("%s.%s", filePath, getEtagFileSuffix());
        }
    }

    @Override
    public String readEtag(String filePath, int chunk) {
        if (StringUtils.isEmpty(filePath))
            return null;
        String fileEtagPath = buildEtagFileName(filePath, chunk);
        if (fileEtagPath == null)
            return null;
        File etagFile = new File(fileEtagPath);
        if (!etagFile.exists())
            return null;
        try {
            return FileUtils.readFileToString(etagFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void saveEtag(String filePath, String etag, int chunk) {
        String fileEtagPath = buildEtagFileName(filePath, chunk);
        if (fileEtagPath == null)
            return;
        try {
            FileUtils.writeStringToFile(new File(fileEtagPath), etag, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
