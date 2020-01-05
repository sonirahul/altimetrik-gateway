package com.altimetrik.gateway.exception;

import org.springframework.http.HttpStatus;

import java.util.Date;

public class ExceptionResponse {

    private HttpStatus error;
    private String message;
    private Integer status;
    private Date timestamp;
    private String path;

    public ExceptionResponse(Date timestamp, String message, String path, HttpStatus status) {
        this.timestamp = timestamp;
        this.message = message;
        this.path = path;
        this.error = status;
        this.status = status.value();
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public HttpStatus getError() {
        return error;
    }

    public void setError(HttpStatus error) {
        this.error = error;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
