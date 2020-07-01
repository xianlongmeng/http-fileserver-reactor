package com.ly.rhdfs.store.single.config;

import com.ly.rhdfs.store.StoreFile;
import com.ly.rhdfs.store.single.SingleFileStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:store.properties")
@ConditionalOnProperty(prefix = "store",name="model",havingValue = "single")
public class SingleStoreConfig {
    @Value("${store.file.root.path}")
    private String fileRootPath;
    @Value("${store.file.temp.suffix}")
    private String tmpFileSuffix;

    public String getFileRootPath() {
        return fileRootPath;
    }

    public void setFileRootPath(String fileRootPath) {
        this.fileRootPath = fileRootPath;
    }

    public String getTmpFileSuffix() {
        return tmpFileSuffix;
    }

    public void setTmpFileSuffix(String tmpFileSuffix) {
        this.tmpFileSuffix = tmpFileSuffix;
    }

    @Bean
    public StoreFile storeFile(){
        SingleFileStore storeFile=new SingleFileStore();
        storeFile.setConfig(this);
        return storeFile;
    }
}
