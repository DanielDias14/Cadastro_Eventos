package br.com.minispring.exemplo.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Gera o X-Request-Id, registra o log da requisição e rejeita corpos acima de 1 MiB (413). */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID = "requestId";
    private static final long MAX_BODY_BYTES = 1_048_576;
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        String id = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID, id);
        response.setHeader("X-Request-Id", id);
        try {
            if (request.getContentLengthLong() > MAX_BODY_BYTES) {
                response.setStatus(413);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status\":413,\"message\":\"Corpo excede 1 MiB\",\"requestId\":\"" + id + "\"}");
                return;
            }
            chain.doFilter(request, response);
        } finally {
            log.info("{} {} -> {} ({} ms) requestId={}", request.getMethod(), request.getRequestURI(),
                response.getStatus(), (System.nanoTime() - start) / 1_000_000, id);
        }
    }
}
