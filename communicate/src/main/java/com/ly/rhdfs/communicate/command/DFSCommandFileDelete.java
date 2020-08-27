package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.token.TokenInfo;

public class DFSCommandFileDelete extends DFSCommand {
    private TokenInfo fileDeleteTokenInfo;

    public DFSCommandFileDelete() {
        commandType = DFSCommand.CT_FILE_DELETE;
    }

    public TokenInfo getFileDeleteTokenInfo() {
        return fileDeleteTokenInfo;
    }

    public void setFileDeleteTokenInfo(TokenInfo fileDeleteTokenInfo) {
        this.fileDeleteTokenInfo = fileDeleteTokenInfo;
    }
}
