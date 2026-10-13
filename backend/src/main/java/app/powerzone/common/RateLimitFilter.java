package app.powerzone.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limit en memoria por IP. Suficiente para una instancia; si el servicio
 * escala horizontalmente se reemplaza por Redis sin cambiar el contrato.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();
    private final int limit;
    private final long windowMillis;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(@Value("${powerzone.throttle.limit:120}") int limit,
                           @Value("${powerzone.throttle.window-seconds:60}") long windowSeconds,
                           ObjectMapper objectMapper) {
        this.limit = limit;
        this.windowMillis = windowSeconds * 1000;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientIp(request);
        long now = Instant.now().toEpochMilli();
        Deque<Long> hits = requests.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (hits) {
            while (!hits.isEmpty() && now - hits.peekFirst() > windowMillis) {
                hits.pollFirst();
            }
            if (hits.size() >= limit) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                        "Demasiadas solicitudes. Intenta de nuevo en unos segundos.");
                problem.setTitle("Limite de solicitudes");
                problem.setProperty("code", "RATE_LIMITED");
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), problem);
                return;
            }
            hits.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
