package app.deptinfo.requirements;

import app.deptinfo.tenant.I18n;

import java.util.List;
import java.util.Map;

/** Contents of requirements.json: the graduation rules, in a form the ECTS calculator can use where possible. */
public record Requirements(
        int schemaVersion,
        double totalEcts,
        List<Rule> rules,
        List<Transitional> transitionalProvisions,
        List<Change> recentChanges) {

    public Requirements {
        rules = rules == null ? List.of() : List.copyOf(rules);
        transitionalProvisions = transitionalProvisions == null ? List.of() : List.copyOf(transitionalProvisions);
        recentChanges = recentChanges == null ? List.of() : List.copyOf(recentChanges);
    }

    /**
     * One rule. Kinds: TOTAL_ECTS, ALL_MANDATORY, MIN_COURSES_BY_LEVEL, ECTS_FROM_LEVELS, CYCLES, COURSE_REQUIRED
     * (all evaluated by the calculator) and FREE_TEXT (display only).
     */
    public record Rule(String id, String kind, I18n title, I18n description, Map<String, Object> params) {
        public Rule {
            params = params == null ? Map.of() : Map.copyOf(params);
        }
    }

    public record Transitional(String validUntil, I18n description) {
    }

    public record Change(String year, I18n text) {
    }
}
