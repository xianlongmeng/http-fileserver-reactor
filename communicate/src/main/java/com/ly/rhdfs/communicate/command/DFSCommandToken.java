package com.ly.rhdfs.communicate.command;

import com.ly.common.domain.token.TokenInfo;

public class DFSCommandToken extends DFSCommand{
    private TokenInfo tokenInfo;

    public DFSCommandToken(){
        commandType=DFSCommand.CT_TOKEN;
    }
    public TokenInfo getTokenInfo() {
        return tokenInfo;
    }

    public void setTokenInfo(TokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
    }
}
