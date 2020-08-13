package com.ly.common.domain;

public class ResultTwoValueInfo<T, V> extends ResultValueInfo<T> {

    public static final int S_OK = 0;
    public static final int S_ERROR = -1;
    public static final int S_FAILED = 1;
    private int result;
    private String errorCode;
    private String errorDesc;
    private T source;
    private V destination;

    public ResultTwoValueInfo(int result) {
        super(result);
    }

    public ResultTwoValueInfo(int result, String errorCode, String errorDesc) {
        super(result, errorCode, errorDesc);
    }

    public ResultTwoValueInfo(int result, T source) {
        super(result);
        this.source = source;
    }

    public ResultTwoValueInfo(int result, T source, V destination) {
        super(result, source);
        this.destination = destination;
    }

    public ResultTwoValueInfo(int result, String errorCode, String errorDesc, T source) {
        super(result, errorCode, errorDesc, source);
    }

    public ResultTwoValueInfo(int result, String errorCode, String errorDesc, T source, V destination) {
        super(result, errorCode, errorDesc, source);
        this.destination = destination;
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

    public V getDestination() {
        return destination;
    }

    public void setDestination(V destination) {
        this.destination = destination;
    }
}
