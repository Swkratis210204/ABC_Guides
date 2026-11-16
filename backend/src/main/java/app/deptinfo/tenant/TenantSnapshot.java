package app.deptinfo.tenant;

import app.deptinfo.calendar.AcademicCalendar;
import app.deptinfo.courses.Course;
import app.deptinfo.faculty.FacultyMember;
import app.deptinfo.requirements.Requirements;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Everything known about one tenant, immutable. A reload builds a new snapshot and swaps it in, so a request
 * in flight always sees one consistent dataset.
 */
public record TenantSnapshot(
        TenantKey key,
        TenantMeta meta,
        List<Course> courses,
        List<FacultyMember> faculty,
        Requirements requirements,
        AcademicCalendar calendar,
        Map<String, Course> coursesByCode,
        Map<String, FacultyMember> facultyById) {

    public static TenantSnapshot of(TenantKey key, TenantMeta meta, List<Course> courses, List<FacultyMember> faculty,
                                    Requirements requirements, AcademicCalendar calendar) {
        Map<String, Course> byCode = new HashMap<>();
        for (Course c : courses) {
            byCode.put(normalize(c.code()), c);
        }
        Map<String, FacultyMember> byId = new HashMap<>();
        for (FacultyMember f : faculty) {
            byId.put(normalize(f.id()), f);
        }
        return new TenantSnapshot(key, meta, List.copyOf(courses), List.copyOf(faculty), requirements, calendar,
                Map.copyOf(byCode), Map.copyOf(byId));
    }

    /** Course lookup, case-insensitive. */
    public Optional<Course> course(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(coursesByCode.get(normalize(code)));
    }

    public Optional<FacultyMember> facultyMember(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(facultyById.get(normalize(id)));
    }

    static String normalize(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }
}
