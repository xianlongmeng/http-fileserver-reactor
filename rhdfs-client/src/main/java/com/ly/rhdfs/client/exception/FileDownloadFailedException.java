package com.ly.rhdfs.client.exception;

public class FileDownloadFailedException extends Exception{
    public FileDownloadFailedException(){
        super();
    }
    public FileDownloadFailedException(String message) {
        super(message);
    }
    public FileDownloadFailedException(String message, Throwable cause){
        super(message,cause);
    }
}
