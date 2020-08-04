package com.ly.common.domain.server;

public class StoreServerRunState {
    // 正在运行的任务数量，读和写的和
    private int runningCount;
    // 正在运行的写任务数量
    private int writingCount;

    public int getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(int runningCount) {
        this.runningCount = runningCount;
    }

    public int getWritingCount() {
        return writingCount;
    }

    public void setWritingCount(int writingCount) {
        this.writingCount = writingCount;
    }
}
