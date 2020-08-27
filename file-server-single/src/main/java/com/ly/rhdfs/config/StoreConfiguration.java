package com.ly.rhdfs.config;

import com.ly.common.constant.ParamConstants;
import com.ly.rhdfs.store.StoreFile;
import com.ly.rhdfs.store.single.SingleFileStore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:store.single.properties")
@ConfigurationProperties(prefix = "store.file")
public class StoreConfiguration {
    private boolean rewrite;

    private String pathParamName = ParamConstants.PARAM_PATH_NAME;

    public boolean isRewrite() {
        return rewrite;
    }

    public void setRewrite(boolean rewrite) {
        this.rewrite = rewrite;
    }

    public String getPathParamName() {
        return pathParamName;
    }

    public void setPathParamName(String pathParamName) {
        this.pathParamName = pathParamName;
    }

    @Bean
    public StoreFile getStoreFile(){
        return new SingleFileStore();
    }
}
