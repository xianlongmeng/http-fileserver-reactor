package com.ly.rhdfs.client.exception;

public class FileUploadFailedException extends Exception{
    public FileUploadFailedException(){
        super();
    }
    public FileUploadFailedException(String message) {
        super(message);
    }
    public FileUploadFailedException(String message, Throwable cause){
        super(message,cause);
    }
}
