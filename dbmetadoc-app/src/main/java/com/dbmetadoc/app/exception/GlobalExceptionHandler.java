package com.dbmetadoc.app.exception;

import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.common.response.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.util.stream.Collectors;


/**
 * 全局异常处理器。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} - 请求路径: {}", e.getMessage(), request.getRequestURI());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常: {} - 请求路径: {}", message, request.getRequestURI());
        return R.fail(ResultCode.VALIDATION_FAILED, StrUtil.blankToDefault(message, ResultCode.VALIDATION_FAILED.getMessage()));
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数绑定异常: {} - 请求路径: {}", message, request.getRequestURI());
        return R.fail(ResultCode.BAD_REQUEST, StrUtil.blankToDefault(message, ResultCode.BAD_REQUEST.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数约束异常: {} - 请求路径: {}", message, request.getRequestURI());
        return R.fail(ResultCode.VALIDATION_FAILED, StrUtil.blankToDefault(message, ResultCode.VALIDATION_FAILED.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e,
                                                                 HttpServletRequest request) {
        String message = "缺少必需的请求参数: " + e.getParameterName();
        log.warn("缺少请求参数: {} - 请求路径: {}", message, request.getRequestURI());
        return R.fail(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体不可读: {} - 请求路径: {}", e.getMessage(), request.getRequestURI());
        return R.fail(ResultCode.BAD_REQUEST, "请求体格式错误或为空");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e,
                                                                HttpServletRequest request) {
        String message = "请求方法不支持: " + e.getMethod();
        log.warn("请求方法不支持: {} - 请求路径: {}", message, request.getRequestURI());
        return R.fail(ResultCode.METHOD_NOT_ALLOWED, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e,
                                                             HttpServletRequest request) {
        String message = "请求参数类型错误: " + e.getName();
        log.warn("参数类型错误: {} - 请求路径: {}", message, request.getRequestURI());
        return R.fail(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleSqlException(SQLException e, HttpServletRequest request) {
        log.error("数据库异常: {} - 请求路径: {}", e.getMessage(), request.getRequestURI(), e);
        return R.fail(ResultCode.DATABASE_ERROR, ResultCode.DATABASE_ERROR.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {} - 请求路径: {}", e.getMessage(), request.getRequestURI(), e);
        return R.fail(ResultCode.INTERNAL_SERVER_ERROR, ResultCode.INTERNAL_SERVER_ERROR.getMessage());
    }
}
