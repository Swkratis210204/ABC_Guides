package app.deptinfo.faculty;

import app.deptinfo.courses.CourseRef;
import app.deptinfo.tenant.I18n;

import java.util.List;

/** A faculty member plus the courses they teach (derived from courses.json). */
public record FacultyView(
        String id,
        I18n name,
        String category,
        String rank,
        String sector,
        String status,
        String statusNote,
        String email,
        String homepageUrl,
        List<CourseRef> courses) {
}
