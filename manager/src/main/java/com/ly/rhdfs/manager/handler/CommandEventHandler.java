package com.ly.rhdfs.manager.handler;

import com.ly.rhdfs.communicate.command.DFSCommand;
import com.ly.rhdfs.communicate.handler.EventHandler;

public class CommandEventHandler implements EventHandler {
    @Override
    public int actorCommand(DFSCommand dfsCommand) {
        return 0;
    }
}
