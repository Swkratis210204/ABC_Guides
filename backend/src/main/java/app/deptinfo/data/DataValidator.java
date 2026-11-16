package app.deptinfo.data;

import app.deptinfo.calendar.AcademicCalendar;
import app.deptinfo.courses.Course;
import app.deptinfo.faculty.FacultyMember;
import app.deptinfo.qa.QaPage;
import app.deptinfo.requirements.Requirements;
import app.deptinfo.tenant.TenantKey;
import app.deptinfo.tenant.TenantMeta;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Checks one tenant's files against each other: required fields, enum values, unique ids and every cross-reference.
 * It collects <em>all</em> problems so an admin can fix them in one pass.
 */
@Component
public class DataValidator {

    public static final int SCHEMA_VERSION = 1;
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9-]+");
    private static final Set<String> CATEGORIES = Set.of("DEP", "EDIP");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "ON_LEAVE", "PENDING_APPOINTMENT");
    private static final Set<String> RULE_KINDS = Set.of("TOTAL_ECTS", "ALL_MANDATORY", "MIN_COURSES_BY_LEVEL",
            "ECTS_FROM_LEVELS", "CYCLES", "COURSE_REQUIRED", "FREE_TEXT");
    private static final Set<String> SEMESTER_TYPES = Set.of("WINTER", "SPRING");
    private static final Set<String> HOLIDAY_KINDS = Set.of("BREAK", "PUBLIC_HOLIDAY");
    /** The level whose courses may lack ECTS and semester (free electives from other departments). */
    public static final String FREE_LEVEL = "FREE";

    public record Result(List<String> errors, List<String> warnings) {
        public boolean ok() {
            return errors.isEmpty();
        }
    }

    public Result validate(TenantKey key, TenantMeta meta, List<Course> courses, List<FacultyMember> faculty,
                           Requirements requirements, AcademicCalendar calendar) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateMeta(key, meta, errors);
        Set<Integer> semesters = ids(meta.semesters(), TenantMeta.Semester::number);
        Set<String> levels = ids(meta.levels(), TenantMeta.Level::id);
        Set<String> cycles = ids(meta.cycles(), TenantMeta.Cycle::id);
        Set<String> sectors = ids(meta.sectors(), TenantMeta.Sector::id);
        Set<String> ranks = ids(meta.ranks(), TenantMeta.Rank::id);

        Set<String> facultyIds = validateFaculty(faculty, sectors, ranks, errors);
        validateCourses(courses, semesters, levels, cycles, facultyIds, errors, warnings);
        validateRequirements(requirements, courses, levels, errors, warnings);
        validateCalendar(calendar, errors);
        return new Result(errors, warnings);
    }

    // ------------------------------------------------------------------ faq.json / internship.json

    /** Checks a Q&A page: every section and question needs an id (unique) and non-blank Greek text. */
    public void validateQa(String file, QaPage page, List<String> errors) {
        if (page == null) {
            return;
        }
        if (page.schemaVersion() != SCHEMA_VERSION) {
            errors.add(file + ": unsupported schemaVersion " + page.schemaVersion());
        }
        Set<String> sectionIds = new HashSet<>();
        Set<String> itemIds = new HashSet<>();
        for (QaPage.Section sec : page.sections()) {
            String where = file + " section[" + sec.id() + "]";
            if (sec.id() == null || !sectionIds.add(sec.id())) {
                errors.add(where + ": missing or duplicate id");
            }
            if (sec.title() == null || sec.title().isBlank()) {
                errors.add(where + ": title is missing");
            }
            for (QaPage.Item it : sec.items()) {
                String iw = file + " item[" + it.id() + "]";
                if (it.id() == null || !itemIds.add(it.id())) {
                    errors.add(iw + ": missing or duplicate id");
                }
                if (it.question() == null || it.question().isBlank()) {
                    errors.add(iw + ": question is missing");
                }
                if (it.answer() == null || it.answer().isBlank()) {
                    errors.add(iw + ": answer is missing");
                }
                for (QaPage.Link l : it.links()) {
                    if (l.label() == null || l.label().isBlank() || l.url() == null
                            || !(l.url().startsWith("https://") || l.url().startsWith("mailto:"))) {
                        errors.add(iw + ": link needs a label and an https: or mailto: url");
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ tenant.json

    private void validateMeta(TenantKey key, TenantMeta meta, List<String> errors) {
        String f = "tenant.json";
        if (meta.schemaVersion() != SCHEMA_VERSION) {
            errors.add(f + ": unsupported schemaVersion " + meta.schemaVersion() + " (expected " + SCHEMA_VERSION + ")");
        }
        if (meta.university() == null || meta.university().slug() == null) {
            errors.add(f + ": university.slug is missing");
        } else {
            if (!SLUG.matcher(meta.university().slug()).matches()) {
                errors.add(f + ": university.slug '" + meta.university().slug() + "' must be lowercase letters, digits and hyphens");
            }
            if (!meta.university().slug().equals(key.uni())) {
                errors.add(f + ": university.slug '" + meta.university().slug() + "' does not match its folder name '" + key.uni() + "'");
            }
            if (meta.university().name() == null || meta.university().name().isBlank()) {
                errors.add(f + ": university.name is missing");
            }
        }
        if (meta.department() == null || meta.department().slug() == null) {
            errors.add(f + ": department.slug is missing");
        } else {
            if (!SLUG.matcher(meta.department().slug()).matches()) {
                errors.add(f + ": department.slug '" + meta.department().slug() + "' must be lowercase letters, digits and hyphens");
            }
            if (!meta.department().slug().equals(key.dept())) {
                errors.add(f + ": department.slug '" + meta.department().slug() + "' does not match its folder name '" + key.dept() + "'");
            }
            if (meta.department().name() == null || meta.department().name().isBlank()) {
                errors.add(f + ": department.name is missing");
            }
        }
        checkUnique(f + " semesters", meta.semesters(), TenantMeta.Semester::number, errors);
        checkUnique(f + " levels", meta.levels(), TenantMeta.Level::id, errors);
        checkUnique(f + " cycles", meta.cycles(), TenantMeta.Cycle::id, errors);
        checkUnique(f + " sectors", meta.sectors(), TenantMeta.Sector::id, errors);
        checkUnique(f + " ranks", meta.ranks(), TenantMeta.Rank::id, errors);
        if (meta.semesters().isEmpty()) {
            errors.add(f + ": at least one semester is required");
        }
        if (meta.levels().isEmpty()) {
            errors.add(f + ": at least one level is required");
        }
    }

    // ------------------------------------------------------------------ faculty.json

    private Set<String> validateFaculty(List<FacultyMember> faculty, Set<String> sectors, Set<String> ranks,
                                        List<String> errors) {
        Set<String> seen = new HashSet<>();
        for (FacultyMember m : faculty) {
            String where = "faculty.json[" + m.id() + "]";
            if (m.id() == null || !SLUG.matcher(m.id()).matches()) {
                errors.add("faculty.json: id '" + m.id() + "' must be lowercase letters, digits and hyphens");
                continue;
            }
            if (!seen.add(m.id())) {
                errors.add(where + ": duplicate id");
            }
            if (m.name() == null || m.name().isBlank()) {
                errors.add(where + ": name is missing");
            }
            if (m.category() == null || !CATEGORIES.contains(m.category())) {
                errors.add(where + ": category '" + m.category() + "' must be one of " + CATEGORIES);
            }
            if (m.status() == null || !STATUSES.contains(m.status())) {
                errors.add(where + ": status '" + m.status() + "' must be one of " + STATUSES);
            }
            boolean dep = "DEP".equals(m.category());
            if (dep && m.rank() == null) {
                errors.add(where + ": rank is required for DEP staff");
            }
            if (dep && m.sector() == null) {
                errors.add(where + ": sector is required for DEP staff");
            }
            if (m.rank() != null && !ranks.contains(m.rank())) {
                errors.add(where + ": unknown rank '" + m.rank() + "'");
            }
            if (m.sector() != null && !sectors.contains(m.sector())) {
                errors.add(where + ": unknown sector '" + m.sector() + "'");
            }
        }
        return seen;
    }

    // ------------------------------------------------------------------ courses.json

    private void validateCourses(List<Course> courses, Set<Integer> semesters, Set<String> levels, Set<String> cycles,
                                 Set<String> facultyIds, List<String> errors, List<String> warnings) {
        Map<String, Course> byCode = new HashMap<>();
        for (Course c : courses) {
            if (c.code() == null || !CODE.matcher(c.code()).matches()) {
                errors.add("courses.json: code '" + c.code() + "' must be letters, digits and hyphens only");
                continue;
            }
            String key = c.code().toLowerCase(Locale.ROOT);
            if (byCode.putIfAbsent(key, c) != null) {
                errors.add("courses.json[" + c.code() + "]: duplicate course code (codes are case-insensitive)");
            }
        }

        for (Course c : courses) {
            if (c.code() == null || !CODE.matcher(c.code()).matches()) {
                continue;
            }
            String where = "courses.json[" + c.code() + "]";
            if (c.name() == null || c.name().isBlank()) {
                errors.add(where + ": name is missing");
            }
            boolean levelOk = c.level() != null && levels.contains(c.level());
            if (!levelOk) {
                errors.add(where + ": unknown level '" + c.level() + "'");
            }
            boolean free = FREE_LEVEL.equals(c.level());
            if (c.ects() == null) {
                if (levelOk && !free) {
                    errors.add(where + ": ects is required (only level " + FREE_LEVEL + " may leave it null)");
                }
            } else if (c.ects() <= 0) {
                errors.add(where + ": ects must be greater than 0 (was " + c.ects() + ")");
            }
            if (c.semesters().isEmpty() && levelOk && !free) {
                errors.add(where + ": semesters is required (only level " + FREE_LEVEL + " may leave it empty)");
            }
            Set<Integer> seenSemesters = new HashSet<>();
            for (Integer sem : c.semesters()) {
                if (sem == null || !semesters.contains(sem)) {
                    errors.add(where + ": unknown semester " + sem);
                } else if (!seenSemesters.add(sem)) {
                    errors.add(where + ": semester " + sem + " is listed twice");
                }
            }
            Set<String> seenCycles = new HashSet<>();
            for (String cy : c.cycles()) {
                if (!cycles.contains(cy)) {
                    errors.add(where + ": unknown cycle '" + cy + "'");
                } else if (!seenCycles.add(cy)) {
                    warnings.add(where + ": cycle '" + cy + "' is listed twice");
                }
            }
            for (List<String> group : c.prerequisites()) {
                if (group.isEmpty()) {
                    errors.add(where + ": a prerequisite group is empty");
                }
                for (String p : group) {
                    if (p == null || !byCode.containsKey(p.toLowerCase(Locale.ROOT))) {
                        errors.add(where + ": prerequisite '" + p + "' is not a course in this department");
                    } else if (p.equalsIgnoreCase(c.code())) {
                        errors.add(where + ": a course cannot be its own prerequisite");
                    }
                }
            }
            for (Course.Instructor i : c.instructors()) {
                if (i.name() == null || i.name().isBlank()) {
                    errors.add(where + ": an instructor has no name");
                }
                if (i.facultyId() != null && !facultyIds.contains(i.facultyId())) {
                    errors.add(where + ": instructor facultyId '" + i.facultyId() + "' is not in faculty.json");
                }
            }
            if (c.eclassUrl() != null && !c.eclassUrl().startsWith("https://")) {
                errors.add(where + ": eclassUrl must start with https://");
            }
        }
        findPrerequisiteCycles(byCode, errors);
    }

    /** Every code in any group is treated as an edge, which is conservative: it can only over-report. */
    private void findPrerequisiteCycles(Map<String, Course> byCode, List<String> errors) {
        Map<String, Integer> state = new HashMap<>(); // 1 = visiting, 2 = done
        Set<String> reported = new HashSet<>();
        for (String start : byCode.keySet()) {
            dfs(start, byCode, state, new ArrayList<>(), reported, errors);
        }
    }

    private void dfs(String code, Map<String, Course> byCode, Map<String, Integer> state, List<String> path,
                     Set<String> reported, List<String> errors) {
        Integer s = state.get(code);
        if (s != null && s == 2) {
            return;
        }
        if (s != null && s == 1) {
            List<String> cycle = new ArrayList<>(path.subList(path.indexOf(code), path.size()));
            String id = String.join(",", new java.util.TreeSet<>(cycle));
            if (reported.add(id)) {
                cycle.add(code);
                errors.add("courses.json: prerequisite cycle " + String.join(" -> ", cycle));
            }
            return;
        }
        state.put(code, 1);
        path.add(code);
        Course c = byCode.get(code);
        if (c != null) {
            for (List<String> group : c.prerequisites()) {
                for (String p : group) {
                    if (p != null && byCode.containsKey(p.toLowerCase(Locale.ROOT))) {
                        dfs(p.toLowerCase(Locale.ROOT), byCode, state, path, reported, errors);
                    }
                }
            }
        }
        path.remove(path.size() - 1);
        state.put(code, 2);
    }

    // ------------------------------------------------------------------ requirements.json

    private void validateRequirements(Requirements req, List<Course> courses, Set<String> levels,
                                      List<String> errors, List<String> warnings) {
        String f = "requirements.json";
        if (req.schemaVersion() != SCHEMA_VERSION) {
            errors.add(f + ": unsupported schemaVersion " + req.schemaVersion());
        }
        if (req.totalEcts() <= 0) {
            errors.add(f + ": totalEcts must be greater than 0");
        }
        Set<String> codes = new HashSet<>();
        courses.forEach(c -> codes.add(c.code().toLowerCase(Locale.ROOT)));
        Set<String> seen = new HashSet<>();
        for (Requirements.Rule r : req.rules()) {
            String where = f + "[" + r.id() + "]";
            if (r.id() == null || r.id().isBlank()) {
                errors.add(f + ": a rule has no id");
                continue;
            }
            if (!seen.add(r.id())) {
                errors.add(where + ": duplicate rule id");
            }
            if (r.title() == null || r.title().isBlank()) {
                errors.add(where + ": title is missing");
            }
            if (r.kind() == null || !RULE_KINDS.contains(r.kind())) {
                errors.add(where + ": kind '" + r.kind() + "' must be one of " + RULE_KINDS);
                continue;
            }
            switch (r.kind()) {
                case "TOTAL_ECTS" -> {
                    requirePositive(r, "min", where, errors);
                    Double min = number(r.params().get("min"));
                    if (min != null && min != req.totalEcts()) {
                        warnings.add(where + ": min (" + min + ") differs from totalEcts (" + req.totalEcts() + ")");
                    }
                }
                case "ALL_MANDATORY" -> requireLevel(r, "level", levels, where, errors);
                case "MIN_COURSES_BY_LEVEL" -> {
                    requireLevel(r, "level", levels, where, errors);
                    requirePositive(r, "min", where, errors);
                }
                case "ECTS_FROM_LEVELS" -> {
                    requirePositive(r, "min", where, errors);
                    Object ls = r.params().get("levels");
                    if (!(ls instanceof List<?> list) || list.isEmpty()) {
                        errors.add(where + ": params.levels must be a non-empty list");
                    } else {
                        for (Object l : list) {
                            if (!levels.contains(String.valueOf(l))) {
                                errors.add(where + ": unknown level '" + l + "' in params.levels");
                            }
                        }
                    }
                }
                case "CYCLES" -> requirePositive(r, "minCourses", where, errors);
                case "COURSE_REQUIRED" -> {
                    Object code = r.params().get("code");
                    if (code == null || !codes.contains(String.valueOf(code).toLowerCase(Locale.ROOT))) {
                        errors.add(where + ": params.code '" + code + "' is not a course in this department");
                    }
                }
                default -> {
                    // FREE_TEXT: display only, nothing to check.
                }
            }
        }
    }

    private void requirePositive(Requirements.Rule r, String param, String where, List<String> errors) {
        Double v = number(r.params().get(param));
        if (v == null || v <= 0) {
            errors.add(where + ": params." + param + " must be a number greater than 0");
        }
    }

    private void requireLevel(Requirements.Rule r, String param, Set<String> levels, String where, List<String> errors) {
        Object v = r.params().get(param);
        if (v == null || !levels.contains(String.valueOf(v))) {
            errors.add(where + ": params." + param + " '" + v + "' is not a known level");
        }
    }

    private Double number(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    // ------------------------------------------------------------------ calendar.json

    private void validateCalendar(AcademicCalendar cal, List<String> errors) {
        String f = "calendar.json";
        if (cal.schemaVersion() != SCHEMA_VERSION) {
            errors.add(f + ": unsupported schemaVersion " + cal.schemaVersion());
        }
        if (cal.academicYear() == null || cal.academicYear().isBlank()) {
            errors.add(f + ": academicYear is missing");
        }
        Set<String> termIds = new HashSet<>();
        for (AcademicCalendar.Term t : cal.terms()) {
            String where = f + " term[" + t.id() + "]";
            if (t.id() == null || !termIds.add(t.id())) {
                errors.add(where + ": missing or duplicate id");
            }
            if (t.semesterType() == null || !SEMESTER_TYPES.contains(t.semesterType())) {
                errors.add(where + ": semesterType '" + t.semesterType() + "' must be one of " + SEMESTER_TYPES);
            }
            checkRange(where, t.start(), t.end(), true, errors);
        }
        Set<String> examIds = new HashSet<>();
        for (AcademicCalendar.ExamPeriod e : cal.examPeriods()) {
            String where = f + " examPeriod[" + e.id() + "]";
            if (e.id() == null || !examIds.add(e.id())) {
                errors.add(where + ": missing or duplicate id");
            }
            if (e.termId() != null && !termIds.contains(e.termId())) {
                errors.add(where + ": unknown termId '" + e.termId() + "'");
            }
            checkRange(where, e.start(), e.end(), false, errors);
        }
        Set<String> holidayIds = new HashSet<>();
        for (AcademicCalendar.Holiday h : cal.holidays()) {
            String where = f + " holiday[" + h.id() + "]";
            if (h.id() == null || !holidayIds.add(h.id())) {
                errors.add(where + ": missing or duplicate id");
            }
            if (h.kind() == null || !HOLIDAY_KINDS.contains(h.kind())) {
                errors.add(where + ": kind '" + h.kind() + "' must be one of " + HOLIDAY_KINDS);
            }
            if (h.start() == null) {
                errors.add(where + ": start is required");
            }
            checkRange(where, h.start(), h.end(), false, errors);
        }
    }

    /** start/end must be in order; both may be null only when {@code required} is false (e.g. the September resit). */
    private void checkRange(String where, LocalDate start, LocalDate end, boolean required, List<String> errors) {
        if (required && (start == null || end == null)) {
            errors.add(where + ": start and end are required");
        }
        if (start != null && end != null && end.isBefore(start)) {
            errors.add(where + ": end " + end + " is before start " + start);
        }
        if ((start == null) != (end == null) && !required && end != null) {
            errors.add(where + ": end is set but start is missing");
        }
    }

    // ------------------------------------------------------------------ helpers

    private <T, K> Set<K> ids(List<T> items, Function<T, K> id) {
        Set<K> out = new LinkedHashSet<>();
        for (T t : items) {
            out.add(id.apply(t));
        }
        return out;
    }

    private <T, K> void checkUnique(String what, List<T> items, Function<T, K> id, List<String> errors) {
        Set<K> seen = new HashSet<>();
        for (T t : items) {
            if (!seen.add(id.apply(t))) {
                errors.add(what + ": duplicate id '" + id.apply(t) + "'");
            }
        }
    }
}
