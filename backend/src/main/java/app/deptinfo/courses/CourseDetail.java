package app.deptinfo.courses;

import app.deptinfo.tenant.I18n;

import java.util.List;

/**
 * A full course page. {@code prerequisites} are groups (all groups required; within a group any one code is
 * enough), with each code resolved to a name. {@code requiredBy} is the reverse: courses that list this one.
 */
public record CourseDetail(
        String code,
        I18n name,
        String level,
        Double ects,
        List<Integer> semesters,
        List<String> cycles,
        String cyclesNote,
        List<List<CourseRef>> prerequisites,
        String prerequisitesText,
        List<Course.Instructor> instructors,
        String offeredBy,
        String language,
        I18n description,
        List<String> aliases,
        String eclassUrl,
        List<CourseRef> requiredBy) {
}
