package com.phoenix.ResumeBackend.exception;
public class ResourceExistsException extends RuntimeException{
    public ResourceExistsException(String message){
        super(message);
    }
}
