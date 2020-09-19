package com.ly.common.exception;

public class FileNotFoundException extends Exception{
    public FileNotFoundException(){
        super();
    }
    public FileNotFoundException(String message) {
        super(message);
    }
    public FileNotFoundException(String message, Throwable cause){
        super(message,cause);
    }
}
