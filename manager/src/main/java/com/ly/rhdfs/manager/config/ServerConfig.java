package com.ly.rhdfs.manager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.ly.common.constant.ParamConstants;

@Configuration
@PropertySource(value = "classpath:server.properties")
public class ServerConfig {

    @Value("${server.id}")
    private int currentServerId;
    @Value("${server.port}")
    private int port;
    @Value("${master.server.config.path}")
    private String configPath;
    @Value("${read.timeout}")
    private int readTimeout = 60;
    @Value("${server.type}")
    private String serverType = ParamConstants.ST_STORE;

    public int getCurrentServerId() {
        return currentServerId;
    }

    public void setCurrentServerId(int currentServerId) {
        this.currentServerId = currentServerId;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }
}
