package app.deptinfo.courses;

import app.deptinfo.tenant.I18n;

import java.util.List;

/** One row of a course list. ects is absent, and semesters empty, for level-4 courses where the guide gives none. */
public record CourseSummary(
        String code,
        I18n name,
        Double ects,
        List<Integer> semesters,
        String level,
        List<String> cycles,
        String offeredBy,
        String language) {

    public static CourseSummary of(Course c) {
        return new CourseSummary(c.code(), c.name(), c.ects(), c.semesters(), c.level(), c.cycles(), c.offeredBy(), c.language());
    }
}
