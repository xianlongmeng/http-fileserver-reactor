package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.server.ServerInfoConfiguration;

import java.util.List;

public class DFSCommandServerAddress extends DFSCommand{
    private List<ServerInfoConfiguration> serverInfoConfigurations;
    public DFSCommandServerAddress(){
        this.commandType=DFSCommand.CT_SERVER_ADDRESS;
    }
    public DFSCommandServerAddress(List<ServerInfoConfiguration> serverInfoConfigurations){
        this.commandType=DFSCommand.CT_SERVER_ADDRESS;
        this.serverInfoConfigurations = serverInfoConfigurations;
    }

    public List<ServerInfoConfiguration> getServerInfoConfigurations() {
        return serverInfoConfigurations;
    }

    public void setServerInfoConfigurations(List<ServerInfoConfiguration> serverInfoConfigurations) {
        this.serverInfoConfigurations = serverInfoConfigurations;
    }
}
