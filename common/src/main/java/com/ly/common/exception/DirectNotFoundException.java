package com.ly.common.exception;

public class DirectNotFoundException extends Exception{
    public DirectNotFoundException(){
        super();
    }
    public DirectNotFoundException(String message) {
        super(message);
    }
    public DirectNotFoundException(String message, Throwable cause){
        super(message,cause);
    }
}
