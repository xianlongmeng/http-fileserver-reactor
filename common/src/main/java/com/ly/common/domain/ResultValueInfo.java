package com.ly.common.domain;

public class ResultValueInfo<T> extends ResultInfo{
    private int result;
    private String errorCode;
    private String errorDesc;
    private T source;

    public ResultValueInfo(int result) {
        super(result);
    }

    public ResultValueInfo(int result, String errorCode, String errorDesc){
        super(result,errorCode,errorDesc);
    }
    public ResultValueInfo(int result, T source){
        super(result);
        this.source=source;
    }
    public ResultValueInfo(int result, String errorCode, String errorDesc, T source){
        super(result,errorCode,errorDesc);
        this.source=source;
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

    public T getSource() {
        return source;
    }

    public void setSource(T source) {
        this.source = source;
    }
}
