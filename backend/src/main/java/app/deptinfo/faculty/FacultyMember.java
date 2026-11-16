package app.deptinfo.faculty;

import app.deptinfo.tenant.I18n;

/**
 * One person from faculty.json. {@code category} is DEP (teaching/research staff) or EDIP (laboratory teaching
 * staff). Rank and sector are required for DEP only. Courses taught are derived from courses.json, not stored here.
 */
public record FacultyMember(
        String id,
        I18n name,
        String category,
        String rank,
        String sector,
        String status,
        String statusNote,
        String email,
        String homepageUrl) {
}
