package br.com.minispring.exemplo.web;

import java.util.List;

/** Erro de negócio/HTTP lançado pelos Services; o {@link GlobalExceptionHandler} converte em resposta JSON. */
public final class HttpException extends RuntimeException {
    private final int status;
    private final List<String> details;

    public HttpException(int status, String message) { this(status, message, List.of()); }

    public HttpException(int status, String message, List<String> details) {
        super(message);
        this.status = status;
        this.details = List.copyOf(details);
    }

    public int status() { return status; }
    public List<String> details() { return details; }
}
