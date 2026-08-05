package com.filemanagement.exception;

public class InvalidTableSchemaException extends RuntimeException {

    public InvalidTableSchemaException(String message) {
        super(message);
    }
    private String headerName;
    public InvalidTableSchemaException(String message,String headerName) {
        super(message);
        this.headerName=headerName;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
}
