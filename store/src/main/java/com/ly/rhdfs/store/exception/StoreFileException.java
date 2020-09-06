package com.ly.rhdfs.store.exception;

public class StoreFileException extends Exception{
    public StoreFileException(){
        super();
    }
    public StoreFileException(String message) {
        super(message);
    }
    public StoreFileException(String message, Throwable cause){
        super(message,cause);
    }
}
