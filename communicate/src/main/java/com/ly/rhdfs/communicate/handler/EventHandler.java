package com.ly.rhdfs.communicate.handler;

import com.ly.rhdfs.communicate.command.DFSCommand;

public interface EventHandler {
    default void beforeCommandHandler(DFSCommand dfsCommand) {
    }

    default int afterCommandHandler(DFSCommand dfsCommand, int res) {
        return res;
    }

    int actorCommand(DFSCommand dfsCommand);

    default int processCommandHandler(DFSCommand dfsCommand) {
        beforeCommandHandler(dfsCommand);
        return afterCommandHandler(dfsCommand, actorCommand(dfsCommand));
    }
}
