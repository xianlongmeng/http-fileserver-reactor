package com.ly.rhdfs.manager.handler;

import com.ly.common.domain.ResultInfo;
import com.ly.common.domain.server.ServerInfoConfiguration;
import com.ly.common.domain.server.ServerState;
import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.command.DFSCommandServerAddress;
import com.ly.rhdfs.communicate.handler.EventHandler;
import com.ly.rhdfs.manager.server.ServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ServerAddressCommandEventHandler implements EventHandler {
    private final Logger logger= LoggerFactory.getLogger(getClass());
    private final ServerManager serverManager;
    public ServerAddressCommandEventHandler(ServerManager serverManager){
        this.serverManager=serverManager;
    }
    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        if (!(dfsCommand instanceof DFSCommandServerAddress)){
            logger.error("Illegal command,not a server address command.");
            return ResultInfo.S_ERROR;
        }
        DFSCommandServerAddress dfsCommandServerAddress=(DFSCommandServerAddress)dfsCommand;
        List<ServerInfoConfiguration> serverInfoConfigurationList=dfsCommandServerAddress.getServerInfoConfigurations();
        if (serverInfoConfigurationList==null || serverInfoConfigurationList.isEmpty()){
            logger.warn("A server address command,does not contain address content.");
            return ResultInfo.S_FAILED;
        }
        long updateAddressLastTime=serverManager.getServerAddressUpdateLastTime();
        for (ServerInfoConfiguration serverInfoConfiguration:serverInfoConfigurationList){
            if (serverInfoConfiguration==null)
                continue;
            ServerInfoConfiguration sic=serverManager.getMasterServerConfig().getServerInfoConfiguration(serverInfoConfiguration.getServerId());
            if (sic==null){
                serverManager.getMasterServerConfig().putServerInfoConfiguration(serverInfoConfiguration);
                serverManager.getServerInfoMap().put(serverInfoConfiguration.getServerId(),serverManager.newServerInfo(serverInfoConfiguration));
                serverManager.saveMasterServerConfig();
            }else if (sic.getUpdateLastTime()<serverInfoConfiguration.getUpdateLastTime()){
                serverManager.getMasterServerConfig().putServerInfoConfiguration(serverInfoConfiguration);
                ServerState serverState=serverManager.getServerInfoMap().get(serverInfoConfiguration.getServerId());
                if (serverState!=null){
                    serverState.setUpdateAddressLastTime(serverInfoConfiguration.getUpdateLastTime());
                }
                serverManager.saveMasterServerConfig();
            }
            if (serverInfoConfiguration.getUpdateLastTime()>updateAddressLastTime){
                updateAddressLastTime=serverInfoConfiguration.getUpdateLastTime();
            }
        }
        synchronized (serverManager.getLocalServerState()){
            if (serverManager.getServerAddressUpdateLastTime()<updateAddressLastTime){
                serverManager.setServerAddressUpdateLastTime(updateAddressLastTime);
            }
        }

        return ResultInfo.S_OK;
    }
}
