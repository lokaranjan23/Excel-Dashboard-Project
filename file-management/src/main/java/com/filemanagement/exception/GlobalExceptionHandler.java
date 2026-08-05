package com.filemanagement.exception;

import com.filemanagement.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;

@RestControllerAdvice
public class GlobalExceptionHandler {

    Logger log= LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> resourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request){
        log.error("Error occurred: {} ",ex.getMessage(),ex);

        ErrorResponse response=new ErrorResponse(
                HttpStatus.CONFLICT,
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        ) ;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> invalidFileException(
            InvalidFileException ex,
            HttpServletRequest request){
        log.error("Error occurred: {} ",ex.getMessage(),ex);

        ErrorResponse response=new ErrorResponse(
                HttpStatus.CONFLICT,
                "INVALID_FILE_EXCEPTION",
                ex.getMessage(),
                request.getRequestURI()
        ) ;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(StoredFileNotFoundException.class)
    public ResponseEntity<ErrorResponse> storedFileNotFoundException(
            StoredFileNotFoundException ex, HttpServletRequest request){
        log.error("Error occurred: {} ",ex.getMessage(),ex);

        ErrorResponse response=new ErrorResponse(
                HttpStatus.CONFLICT,
                "STORED_FILE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        ) ;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidSortFieldException.class)
    public ResponseEntity<ErrorResponse> invalidSortFieldException(
            InvalidSortFieldException ex, HttpServletRequest request){
        log.error("Error occurred: {} ",ex.getMessage(),ex);

        ErrorResponse response=new ErrorResponse(
                HttpStatus.CONFLICT,
                "INVALID_SORT_FIELD",
                ex.getMessage(),
                request.getRequestURI()
        ) ;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidTableSchemaException.class)
    public ResponseEntity<ErrorResponse> invalidTableSchemaException(
            InvalidTableSchemaException ex, HttpServletRequest request){
        log.error("Error occurred: {} ",ex.getMessage(),ex);

        ErrorResponse response=new ErrorResponse(
                HttpStatus.CONFLICT,
                "INVALID_TABLE_SCHEMA",
                ex.getMessage(),
                request.getRequestURI()
        ) ;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidColumnException.class)
    public ResponseEntity<ErrorResponse> invalidColumnException(
            InvalidColumnException ex, HttpServletRequest request){
        log.error("Error occurred: {} ",ex.getMessage(),ex);

        ErrorResponse response=new ErrorResponse(
                HttpStatus.CONFLICT,
                "INVALID_COLUMN_DATA",
                ex.getMessage(),
                request.getRequestURI()
        ) ;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidSheetException.class)
    public ResponseEntity<ErrorResponse> invalidSheetException(
            InvalidSheetException ex, HttpServletRequest request){
        log.error("Error occurred: {} ",ex.getMessage(),ex);

        ErrorResponse response=new ErrorResponse(
                HttpStatus.CONFLICT,
                "INVALID_SHEET_NAME",
                ex.getMessage(),
                request.getRequestURI()
        ) ;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidRangeException.class)
    public ResponseEntity<ErrorResponse> invalidRangeException(
            InvalidRangeException ex, HttpServletRequest request){
        log.error("Error occurred: {} ",ex.getMessage(),ex);

        ErrorResponse response=new ErrorResponse(
                HttpStatus.CONFLICT,
                "INVALID_RANGE_ENTERED",
                ex.getMessage(),
                request.getRequestURI()
        ) ;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DuplicateFileException.class)
    public ResponseEntity<ErrorResponse> duplicateFileException(
            DuplicateFileException ex,
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }


}
