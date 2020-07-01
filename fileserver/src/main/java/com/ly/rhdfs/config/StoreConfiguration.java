package com.ly.rhdfs.config;

import com.ly.common.constant.ParamConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:store.properties")
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
}
