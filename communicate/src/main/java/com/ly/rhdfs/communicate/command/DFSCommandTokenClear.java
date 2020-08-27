package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.token.TokenInfo;

public class DFSCommandTokenClear extends DFSCommand{
    private TokenInfo tokenInfo;

    public DFSCommandTokenClear(){
        commandType=DFSCommand.CT_TOKEN_CLEAR;
    }
    public TokenInfo getTokenInfo() {
        return tokenInfo;
    }

    public void setTokenInfo(TokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
    }
}
