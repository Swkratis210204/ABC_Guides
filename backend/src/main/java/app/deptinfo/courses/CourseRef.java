package app.deptinfo.courses;

import app.deptinfo.tenant.I18n;

/** A short reference to a course, used for links (prerequisites, "required for", courses taught). */
public record CourseRef(String code, I18n name) {

    public static CourseRef of(Course c) {
        return new CourseRef(c.code(), c.name());
    }
}
