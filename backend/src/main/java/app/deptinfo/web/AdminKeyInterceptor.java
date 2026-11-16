package app.deptinfo.web;

import app.deptinfo.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Guards /api/admin/** with the X-Admin-Key header. With no key configured, admin endpoints are switched off. */
@Component
public class AdminKeyInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Admin-Key";

    private final AppProperties props;

    public AdminKeyInterceptor(AppProperties props) {
        this.props = props;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String configured = props.adminApiKey();
        if (configured == null || configured.isBlank()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_DISABLED",
                    "Admin endpoints are disabled: no ADMIN_API_KEY is configured", null);
        }
        String supplied = request.getHeader(HEADER);
        // Constant-time comparison so response timing does not reveal how much of the key matched.
        if (supplied == null || !MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8), configured.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Missing or invalid " + HEADER + " header", null);
        }
        return true;
    }
}
