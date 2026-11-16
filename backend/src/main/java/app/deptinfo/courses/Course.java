package app.deptinfo.courses;

import app.deptinfo.tenant.I18n;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * One course as stored in courses.json.
 *
 * <p>{@code prerequisites} is a list of groups: a student must satisfy every group (AND), and a group is
 * satisfied by passing any one of its codes (OR). {@code prerequisitesText} keeps the guide's own wording.
 * {@code ects} is null, and {@code semesters} empty, only for level-4 (free elective) courses. A course offered in
 * either of two semesters (e.g. the thesis, "Ζ΄/Η΄") lists both.
 */
public record Course(
        String code,
        I18n name,
        String level,
        Double ects,
        List<Integer> semesters,
        List<String> cycles,
        List<List<String>> prerequisites,
        String prerequisitesText,
        List<Instructor> instructors,
        String offeredBy,
        String language,
        I18n description,
        List<String> aliases,
        String cyclesNote,
        String eclassUrl,
        Boolean active) {

    public Course {
        semesters = semesters == null ? List.of() : List.copyOf(semesters);
        cycles = cycles == null ? List.of() : List.copyOf(cycles);
        prerequisites = prerequisites == null ? List.of()
                : prerequisites.stream().map(g -> g == null ? List.<String>of() : List.copyOf(g)).toList();
        instructors = instructors == null ? List.of() : List.copyOf(instructors);
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        if (active == null) {
            active = Boolean.TRUE;
        }
    }

    @JsonIgnore
    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    /** A teaching person; {@code facultyId} is null for people who are not in faculty.json. */
    public record Instructor(String name, String facultyId) {
    }
}
