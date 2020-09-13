package com.ly.rhdfs.communicate.exception;

public class TransferFileException extends Exception{
    public TransferFileException(){
        super();
    }
    public TransferFileException(String message) {
        super(message);
    }
    public TransferFileException(String message, Throwable cause){
        super(message,cause);
    }
}
