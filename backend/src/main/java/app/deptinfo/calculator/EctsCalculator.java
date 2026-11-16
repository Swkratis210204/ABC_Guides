package app.deptinfo.calculator;

import app.deptinfo.courses.Course;
import app.deptinfo.courses.CourseRef;
import app.deptinfo.requirements.Requirements;
import app.deptinfo.tenant.TenantMeta;
import app.deptinfo.tenant.TenantSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Pure function over a tenant snapshot: no state, nothing stored. Every graduation rule in requirements.json is
 * evaluated by its {@code kind}; rules of kind FREE_TEXT cannot be and are listed as manual checks.
 */
@Component
public class EctsCalculator {

    public CalculatorResult calculate(TenantSnapshot s, List<String> input) {
        return calculate(s, input, Map.of(), 0);
    }

    /**
     * @param overrides ECTS the user typed in for passed courses that have none in the data (key = course code)
     * @param otherEcts ECTS that belong to no listed course; counts toward the total only
     */
    public CalculatorResult calculate(TenantSnapshot s, List<String> input, Map<String, Double> overrides, double otherEcts) {
        List<CalculatorResult.Warning> warnings = new ArrayList<>();
        Set<String> unknown = new LinkedHashSet<>();
        Map<String, Course> passed = new LinkedHashMap<>();

        for (String raw : input) {
            String code = raw == null ? "" : raw.trim();
            Optional<Course> course = s.course(code).filter(Course::isActive);
            if (course.isEmpty()) {
                unknown.add(code);
            } else if (passed.putIfAbsent(key(course.get().code()), course.get()) != null) {
                warnings.add(new CalculatorResult.Warning("DUPLICATE_CODE",
                        "Course " + course.get().code() + " was listed more than once and is counted once", course.get().code()));
            }
        }

        // The ECTS each passed course contributes: the guide's value if it has one, else what the user typed, else 0.
        Map<String, Double> typedIn = new LinkedHashMap<>();
        overrides.forEach((code, value) -> {
            if (code != null && value != null) {
                typedIn.put(key(code), value);
            }
        });
        Map<String, Double> effective = new LinkedHashMap<>();
        double earned = 0;
        double userProvided = 0;
        for (Course c : passed.values()) {
            double ects;
            if (c.ects() != null) {
                ects = c.ects();                                   // the guide wins over anything typed in
            } else if (typedIn.containsKey(key(c.code()))) {
                ects = typedIn.get(key(c.code()));
                userProvided += ects;
            } else {
                ects = 0;
                warnings.add(new CalculatorResult.Warning("ECTS_UNKNOWN",
                        "The guide gives no ECTS for course " + c.code() + " and none were entered; it is counted as passed but adds 0 ECTS", c.code()));
            }
            effective.put(key(c.code()), ects);
            earned += ects;
        }
        earned += otherEcts;
        userProvided += otherEcts;
        addPrerequisiteWarnings(passed, warnings);

        Requirements req = s.requirements();
        List<CalculatorResult.RuleStatus> rules = new ArrayList<>();
        List<CalculatorResult.ManualCheck> manual = new ArrayList<>();
        CalculatorResult.CycleReport cycles = null;

        for (Requirements.Rule rule : req.rules()) {
            switch (rule.kind()) {
                case "TOTAL_ECTS" -> {
                    double min = number(rule, "min");
                    rules.add(status(rule, earned >= min, earned, min, List.of()));
                }
                case "ALL_MANDATORY" -> {
                    List<Course> all = activeInLevels(s, List.of(string(rule, "level")));
                    List<CourseRef> missing = all.stream().filter(c -> !passed.containsKey(key(c.code())))
                            .sorted(Comparator.comparing(Course::code)).map(CourseRef::of).toList();
                    rules.add(status(rule, missing.isEmpty(), all.size() - missing.size(), all.size(), missing));
                }
                case "MIN_COURSES_BY_LEVEL" -> {
                    double min = number(rule, "min");
                    long count = passedInLevels(passed, List.of(string(rule, "level"))).size();
                    rules.add(status(rule, count >= min, count, min, List.of()));
                }
                case "ECTS_FROM_LEVELS" -> {
                    double min = number(rule, "min");
                    double sum = passedInLevels(passed, strings(rule, "levels")).stream()
                            .mapToDouble(c -> effective.getOrDefault(key(c.code()), 0.0)).sum();
                    rules.add(status(rule, sum >= min, sum, min, List.of()));
                }
                case "COURSE_REQUIRED" -> {
                    String code = string(rule, "code");
                    boolean done = passed.containsKey(key(code));
                    List<CourseRef> missing = done ? List.of() : s.course(code).map(c -> List.of(CourseRef.of(c))).orElse(List.of());
                    rules.add(status(rule, done, done ? 1 : 0, 1, missing));
                }
                case "CYCLES" -> cycles = cycleReport(s, passed, (int) number(rule, "minCourses"));
                default -> manual.add(new CalculatorResult.ManualCheck(rule.id(), rule.title()));
            }
        }

        List<CalculatorResult.LevelProgress> levels = new ArrayList<>();
        for (TenantMeta.Level l : s.meta().levels()) {
            List<Course> inLevel = passedInLevels(passed, List.of(l.id()));
            levels.add(new CalculatorResult.LevelProgress(l.id(), inLevel.size(),
                    inLevel.stream().mapToDouble(c -> effective.getOrDefault(key(c.code()), 0.0)).sum()));
        }

        double required = req.totalEcts();
        boolean allMet = !rules.isEmpty() && rules.stream().allMatch(CalculatorResult.RuleStatus::satisfied);
        return new CalculatorResult(
                new CalculatorResult.Totals(earned, required, Math.max(0, required - earned), userProvided),
                levels, rules, cycles, manual, List.copyOf(unknown), warnings, allMet);
    }

    // ------------------------------------------------------------------ prerequisites (advisory only)

    /** A passed course whose prerequisite group has none of its codes passed. The guide relaxes these, so it is a hint. */
    private void addPrerequisiteWarnings(Map<String, Course> passed, List<CalculatorResult.Warning> warnings) {
        for (Course c : passed.values()) {
            for (List<String> group : c.prerequisites()) {
                boolean satisfied = group.stream().anyMatch(code -> passed.containsKey(key(code)));
                if (!satisfied) {
                    warnings.add(new CalculatorResult.Warning("PREREQUISITE_NOT_PASSED",
                            "Course " + c.code() + " lists a prerequisite (" + String.join(" or ", group)
                                    + ") that is not in your list of passed courses", c.code()));
                }
            }
        }
    }

    // ------------------------------------------------------------------ cycles

    /**
     * Each cycle needs {@code min} distinct passed courses from its own list, and one course may count for only one
     * cycle. Tries sets of cycles from largest to smallest and checks whether the courses can be shared out
     * (a bipartite matching between "slots" of each cycle and courses). Cheap: 8 cycles means at most 255 sets.
     */
    private CalculatorResult.CycleReport cycleReport(TenantSnapshot s, Map<String, Course> passed, int min) {
        List<TenantMeta.Cycle> cycles = s.meta().cycles();
        List<Course> courses = new ArrayList<>(passed.values());
        int n = cycles.size();

        int[][] eligible = new int[n][];
        for (int i = 0; i < n; i++) {
            String id = cycles.get(i).id();
            List<Integer> idx = new ArrayList<>();
            for (int j = 0; j < courses.size(); j++) {
                if (courses.get(j).cycles().contains(id)) {
                    idx.add(j);
                }
            }
            eligible[i] = idx.stream().mapToInt(Integer::intValue).toArray();
        }

        Map<Integer, List<Integer>> plan = Map.of();
        search:
        for (int k = n; k >= 1; k--) {
            for (int mask = 1; mask < (1 << n); mask++) {
                if (Integer.bitCount(mask) != k) {
                    continue;
                }
                Map<Integer, List<Integer>> attempt = assign(mask, n, min, eligible, courses.size());
                if (attempt != null) {
                    plan = attempt;
                    break search;
                }
            }
        }

        List<CalculatorResult.CycleStatus> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            TenantMeta.Cycle c = cycles.get(i);
            List<String> passedCodes = Arrays.stream(eligible[i]).mapToObj(j -> courses.get(j).code()).sorted().toList();
            List<String> assigned = plan.getOrDefault(i, List.of()).stream().map(j -> courses.get(j).code()).sorted().toList();
            items.add(new CalculatorResult.CycleStatus(c.id(), c.number(), c.name(), passedCodes,
                    Math.max(0, min - passedCodes.size()), plan.containsKey(i), assigned));
        }
        return new CalculatorResult.CycleReport(min, plan.size(), items);
    }

    /** Returns cycle index -> assigned course indexes if every cycle in {@code mask} gets {@code min} courses, else null. */
    private Map<Integer, List<Integer>> assign(int mask, int n, int min, int[][] eligible, int courseCount) {
        List<Integer> slotCycle = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if ((mask & (1 << i)) != 0) {
                if (eligible[i].length < min) {
                    return null;
                }
                for (int r = 0; r < min; r++) {
                    slotCycle.add(i);
                }
            }
        }
        int[] courseSlot = new int[courseCount];
        Arrays.fill(courseSlot, -1);
        for (int slot = 0; slot < slotCycle.size(); slot++) {
            if (!augment(slot, slotCycle, eligible, courseSlot, new boolean[courseCount])) {
                return null;
            }
        }
        Map<Integer, List<Integer>> result = new HashMap<>();
        for (int j = 0; j < courseCount; j++) {
            if (courseSlot[j] >= 0) {
                result.computeIfAbsent(slotCycle.get(courseSlot[j]), x -> new ArrayList<>()).add(j);
            }
        }
        return result;
    }

    /** Standard augmenting-path step of bipartite matching (Kuhn's algorithm). */
    private boolean augment(int slot, List<Integer> slotCycle, int[][] eligible, int[] courseSlot, boolean[] seen) {
        for (int j : eligible[slotCycle.get(slot)]) {
            if (seen[j]) {
                continue;
            }
            seen[j] = true;
            if (courseSlot[j] < 0 || augment(courseSlot[j], slotCycle, eligible, courseSlot, seen)) {
                courseSlot[j] = slot;
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ helpers

    private static CalculatorResult.RuleStatus status(Requirements.Rule rule, boolean satisfied, double current,
                                                      double required, List<CourseRef> missing) {
        return new CalculatorResult.RuleStatus(rule.id(), rule.kind(), rule.title(), satisfied, current, required,
                Math.max(0, required - current), missing);
    }

    private static List<Course> activeInLevels(TenantSnapshot s, List<String> levels) {
        return s.courses().stream().filter(Course::isActive).filter(c -> levels.contains(c.level())).toList();
    }

    private static List<Course> passedInLevels(Map<String, Course> passed, List<String> levels) {
        return passed.values().stream().filter(c -> levels.contains(c.level())).toList();
    }

    private static String key(String code) {
        return code.trim().toLowerCase(Locale.ROOT);
    }

    // Rule params were checked by DataValidator at load time, so these casts are safe.
    private static double number(Requirements.Rule rule, String param) {
        return ((Number) rule.params().get(param)).doubleValue();
    }

    private static String string(Requirements.Rule rule, String param) {
        return String.valueOf(rule.params().get(param));
    }

    private static List<String> strings(Requirements.Rule rule, String param) {
        return ((List<?>) rule.params().get(param)).stream().map(String::valueOf).toList();
    }
}
