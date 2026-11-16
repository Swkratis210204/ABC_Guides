package app.deptinfo.web;

import java.util.List;

/** The body of every non-2xx response: {"error": {"code": "...", "message": "...", "details": [...]}}. */
public record ApiError(Body error) {

    public record Body(String code, String message, List<String> details) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(new Body(code, message, null));
    }

    public static ApiError of(String code, String message, List<String> details) {
        return new ApiError(new Body(code, message, details));
    }
}
