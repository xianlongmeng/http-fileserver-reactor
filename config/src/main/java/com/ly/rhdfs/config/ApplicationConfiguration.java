package com.ly.rhdfs.config;

import com.ly.rhdfs.file.util.DfsFileUtils;
import com.ly.etag.ETagAccess;
import com.ly.etag.ETagComputer;
import com.ly.rhdfs.authentication.AuthenticationVerify;
import com.ly.rhdfs.authentication.impl.DefaultAuthenticationImpl;
import com.ly.rhdfs.log.operate.LogFileOperate;
import com.ly.rhdfs.log.operate.LogOperateUtils;
import com.ly.rhdfs.log.server.file.ServerFileChunkUtil;
import com.ly.rhdfs.token.TokenFactory;
import com.ly.rhdfs.token.random.TokenRandomFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.ly.etag.impl.ETagComputer4MD5;
import com.ly.etag.impl.ETagComputer4UUIDTimestamp;
import org.springframework.util.StringUtils;

@Configuration
public class ApplicationConfiguration {

    private ServerConfig serverConfig;
    @Autowired
    private void setServerConfig(ServerConfig serverConfig){
        this.serverConfig=serverConfig;
    }
    private ETagAccess eTagAccess;
    private void setETagAccess(ETagAccess eTagAccess){
        this.eTagAccess=eTagAccess;
    }
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "etag", name = "type", havingValue = "MD5", matchIfMissing = true)
    public ETagComputer eTagComputer4MD5() {
        ETagComputer4MD5 eTagComputer=new ETagComputer4MD5();
        eTagComputer.setETagAccess(eTagAccess);
        return eTagComputer;
    }

    @Bean
    @ConditionalOnProperty(prefix = "etag", name = "type", havingValue = "UUIDTimestamp")
    public ETagComputer eTagComputer4UUIDTimestamp() {
        ETagComputer4UUIDTimestamp eTagComputer=new ETagComputer4UUIDTimestamp();
        eTagComputer.setETagAccess(eTagAccess);
        return eTagComputer;
    }

    @Bean
    public TokenFactory tokenRandomFactory(){
        TokenFactory tokenFactory= new TokenRandomFactory();
        tokenFactory.setDefaultTimeout(serverConfig.getTokenDefaultTimeout());
        tokenFactory.setReadTimeout(serverConfig.getReadTimeout());
        tokenFactory.setWriteTimeout(serverConfig.getTokenWriteTimeout());
        return tokenFactory;
    }
    @Bean
    public AuthenticationVerify authenticationVerify(){
        return new DefaultAuthenticationImpl();
    }

    @Bean
    public DfsFileUtils dfsFileUtils(){
        DfsFileUtils dfsFileUtils=new DfsFileUtils();
        dfsFileUtils.setFileRootPath(serverConfig.getFileRootPath());
        if (!StringUtils.hasLength(serverConfig.getFileConfigSuffix()))
            dfsFileUtils.setFileConfigSuffix(serverConfig.getFileConfigSuffix());
        if (!StringUtils.hasLength(serverConfig.getFileTmpConfigSuffix()))
            dfsFileUtils.setFileTmpConfigSuffix(serverConfig.getFileTmpConfigSuffix());
        return dfsFileUtils;
    }
    @Bean
    public LogOperateUtils logOperateUtils(){
        LogOperateUtils logOperateUtils= new LogOperateUtils();
        logOperateUtils.setDfsFileUtils(dfsFileUtils());
        return logOperateUtils;
    }
    @Bean
    public LogFileOperate logFileOperate(){
        LogFileOperate logFileOperate= new LogFileOperate(serverConfig.getLogPath());
        logFileOperate.setDfsFileUtils(dfsFileUtils());
        logFileOperate.setLogOperateUtils(logOperateUtils());
        return logFileOperate;
    }
    @Bean
    public ServerFileChunkUtil serverFileChunkUtil(){
        ServerFileChunkUtil serverFileChunkUtil=new ServerFileChunkUtil();
        serverFileChunkUtil.setBootPath(serverConfig.getServerFileLogPath());
        return serverFileChunkUtil;
    }
}
