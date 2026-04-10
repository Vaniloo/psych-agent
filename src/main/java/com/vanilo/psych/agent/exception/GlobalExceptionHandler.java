package com.vanilo.psych.agent.exception;

import com.vanilo.psych.agent.dto.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ErrorResponse handleRuntimeException(RuntimeException e){
        return new ErrorResponse(false,e.getMessage());

    }

}
