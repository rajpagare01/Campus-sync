package com.campussync.backend.Exception;

import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends ApiException {

    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, "forbidden-operation", message);
    }
}
