package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.TaskInfo;
import com.ly.common.domain.token.TokenInfo;

public class DFSCommandTask extends DFSCommand{
    private TaskInfo taskInfo;

    public DFSCommandTask(){
        commandType=DFSCommand.CT_TASK_INFO;
    }

    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    public void setTaskInfo(TaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }
}
