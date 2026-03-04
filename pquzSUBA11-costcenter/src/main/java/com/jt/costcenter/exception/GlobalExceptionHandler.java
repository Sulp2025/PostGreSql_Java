package com.jt.costcenter.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice; 
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. 处理自定义的 BadRequestException (无需 import，因为在同一个包下)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
        // 400 错误通常是用户输入问题，info 级别日志即可
        logger.warn("Bad Request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("detail", ex.getMessage()));
    }

    // 2. 处理自定义的 NotFoundException(无需 import，因为在同一个包下)
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        logger.warn("Not Found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("detail", ex.getMessage()));
    }

    // 3. 处理所有未捕获异常 
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOthers(Exception ex) {
        // 必须打印堆栈跟踪，否则无法排查 BUG
        logger.error("Unhandled exception occurred", ex);

        // 安全起见，不要把 ex.getMessage() 返回给前端，防止泄露敏感信息
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("detail", "Internal Server Error. Please contact support."));
    }
}