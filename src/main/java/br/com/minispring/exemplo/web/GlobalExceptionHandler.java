package br.com.minispring.exemplo.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Mantém o formato de erro da API: timestamp, status, message, path, details e requestId. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpException.class)
    ResponseEntity<Map<String, Object>> http(HttpException e, HttpServletRequest request) {
        return body(e.status(), e.getMessage(), e.details(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + " " + f.getDefaultMessage()).sorted().toList();
        return body(400, "Falha de validação", details, request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return body(400, "JSON inválido ou incompatível com os campos esperados", List.of(), request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, Object>> mismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return body(400, "Parâmetro inválido: " + e.getName(), List.of(), request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Map<String, Object>> mediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return body(415, "Use Content-Type: application/json", List.of(), request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, Object>> method(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        var headers = new HttpHeaders();
        if (e.getSupportedMethods() != null) headers.set(HttpHeaders.ALLOW, String.join(", ", e.getSupportedMethods()));
        return body(405, "Método não permitido", List.of(), request, headers);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, Object>> notFound(NoResourceFoundException e, HttpServletRequest request) {
        return body(404, "Rota não encontrada", List.of(), request, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception e, HttpServletRequest request) {
        log.error("Falha na requisição {}", request.getAttribute(RequestIdFilter.REQUEST_ID), e);
        return body(500, "Erro interno do servidor", List.of(), request, null);
    }

    private ResponseEntity<Map<String, Object>> body(int status, String message, List<String> details,
                                                     HttpServletRequest request, HttpHeaders headers) {
        var map = new LinkedHashMap<String, Object>();
        map.put("timestamp", Instant.now().toString());
        map.put("status", status);
        map.put("message", message);
        map.put("path", request.getRequestURI());
        map.put("details", details);
        map.put("requestId", Objects.toString(request.getAttribute(RequestIdFilter.REQUEST_ID), ""));
        var builder = ResponseEntity.status(HttpStatus.valueOf(status));
        if (headers != null) builder.headers(headers);
        return builder.body(map);
    }
}
