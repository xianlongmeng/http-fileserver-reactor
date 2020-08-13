package com.ly.common.domain;

public class ResultInfo {

    public static final int S_OK = 0;
    public static final int S_ERROR = -1;
    public static final int S_FAILED = 1;
    public static final int S_FAILED_TIMEOUT = 1;
    private int result;
    private String errorCode;
    private String errorDesc;

    public ResultInfo(int result) {
        this.result = result;
    }

    public ResultInfo(int result, String errorCode, String errorDesc) {
        this.result = result;
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }
}
