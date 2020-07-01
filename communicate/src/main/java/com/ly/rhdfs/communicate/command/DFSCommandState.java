package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.server.ServerState;

public class DFSCommandState extends DFSCommand{

    private ServerState serverState;

    public DFSCommandState(){
        commandType=DFSCommand.CT_STATE;
    }

    public ServerState getServerState() {
        return serverState;
    }

    public void setServerState(ServerState serverState) {
        this.serverState = serverState;
    }
}
