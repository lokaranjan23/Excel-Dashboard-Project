package com.filemanagement.exception;

public class StoredFileNotFoundException extends RuntimeException {
    public StoredFileNotFoundException(String message) {
        super(message);
    }
}
