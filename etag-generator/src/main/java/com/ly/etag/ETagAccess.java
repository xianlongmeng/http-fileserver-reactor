package com.ly.etag;

public interface ETagAccess {

    String getEtagFileSuffix();

    void setEtagFileSuffix(String etagFileSuffix);

    default String readEtag(String filePath) {
        return readEtag(filePath, -1);
    }

    default void saveEtag(String filePath, String etag) {
        saveEtag(filePath, etag, -1);
    }

    /**
     * 读取计算好的ETag
     * 
     * @param filePath
     * @param chunk
     * @return
     */
    String readEtag(String filePath, int chunk);

    /**
     * 保存计算好的ETag
     * 
     * @param filePath
     * @param chunk
     * @return
     */
    void saveEtag(String filePath, String etag, int chunk);
}
