package controller.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Intercepts every HTTP request to assign a short correlation ID (reqId) via MDC,
 * log the incoming request, and log the completed response with its HTTP status and elapsed time.
 *
 * <p>Lives in {@code controller.logging} (a sub-package of {@code controller}) so that Logback
 * routes its output to {@code requests.log} instead of {@code controllers.log}.
 */
@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        MDC.put("reqId", UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        request.setAttribute("startTime", System.currentTimeMillis());
        log.info("→ {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        long elapsed = System.currentTimeMillis() - (long) request.getAttribute("startTime");
        log.info("← {} {} | status={} | {}ms",
                request.getMethod(), request.getRequestURI(), response.getStatus(), elapsed);
        MDC.clear();
    }
}
