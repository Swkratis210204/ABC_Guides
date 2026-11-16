package app.deptinfo.calculator;

import app.deptinfo.tenant.TenantRegistry;
import app.deptinfo.web.ApiException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/universities/{uni}/departments/{dept}/ects-calculator")
public class CalculatorController {

    /** More than the number of courses any department has; protects against absurd payloads. */
    static final int MAX_CODES = 200;
    static final int MAX_CODE_LENGTH = 32;
    /** Sanity limits for numbers the user types in; far above any real course or credit total. */
    static final double MAX_COURSE_ECTS = 60;
    static final double MAX_OTHER_ECTS = 240;

    private final TenantRegistry registry;
    private final EctsCalculator calculator;

    public CalculatorController(TenantRegistry registry, EctsCalculator calculator) {
        this.registry = registry;
        this.calculator = calculator;
    }

    @PostMapping
    public CalculatorResult calculate(@PathVariable String uni, @PathVariable String dept,
                                      @RequestBody CalculatorRequest request) {
        var tenant = registry.require(uni, dept);
        if (request.passedCourses() == null) {
            throw ApiException.badRequest("INVALID_BODY", "passedCourses is required (a list of course codes)");
        }
        if (request.passedCourses().size() > MAX_CODES) {
            throw ApiException.badRequest("INVALID_BODY", "passedCourses may contain at most " + MAX_CODES + " codes");
        }
        for (String code : request.passedCourses()) {
            if (code != null && code.length() > MAX_CODE_LENGTH) {
                throw ApiException.badRequest("INVALID_BODY", "A course code is longer than " + MAX_CODE_LENGTH + " characters");
            }
        }

        Map<String, Double> overrides = request.ectsOverrides() == null ? Map.of() : request.ectsOverrides();
        if (overrides.size() > MAX_CODES) {
            throw ApiException.badRequest("INVALID_BODY", "ectsOverrides may contain at most " + MAX_CODES + " entries");
        }
        overrides.forEach((code, value) -> {
            if (code == null || code.length() > MAX_CODE_LENGTH) {
                throw ApiException.badRequest("INVALID_BODY", "An ectsOverrides key is not a valid course code");
            }
            if (value == null || !Double.isFinite(value) || value <= 0 || value > MAX_COURSE_ECTS) {
                throw ApiException.badRequest("INVALID_BODY",
                        "ectsOverrides values must be numbers greater than 0 and at most " + MAX_COURSE_ECTS + " (course " + code + ")");
            }
        });

        double other = request.otherEcts() == null ? 0 : request.otherEcts();
        if (!Double.isFinite(other) || other < 0 || other > MAX_OTHER_ECTS) {
            throw ApiException.badRequest("INVALID_BODY", "otherEcts must be a number between 0 and " + MAX_OTHER_ECTS);
        }
        return calculator.calculate(tenant, request.passedCourses(), overrides, other);
    }
}
