package app.deptinfo.courses;

import app.deptinfo.tenant.TenantSnapshot;
import app.deptinfo.web.ApiException;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CourseService {

    /**
     * Lists active courses, optionally filtered. Filters combine with AND. An unknown semester, level or cycle id
     * is a 400, not an empty list, so a typo in a link is noticed.
     */
    public List<CourseSummary> list(TenantSnapshot s, Integer semester, String level, String cycle) {
        if (semester != null && s.meta().semesters().stream().noneMatch(x -> x.number() == semester)) {
            throw ApiException.badRequest("INVALID_FILTER", "Unknown semester " + semester);
        }
        if (level != null && s.meta().levels().stream().noneMatch(x -> x.id().equals(level))) {
            throw ApiException.badRequest("INVALID_FILTER", "Unknown level '" + level + "'");
        }
        if (cycle != null && s.meta().cycles().stream().noneMatch(x -> x.id().equals(cycle))) {
            throw ApiException.badRequest("INVALID_FILTER", "Unknown cycle '" + cycle + "'");
        }
        Collator greek = Collator.getInstance(Locale.forLanguageTag("el-GR"));
        // Ordered by the first semester a course is offered in; courses with no semester (free electives) go last.
        Comparator<Course> order = Comparator
                .comparing((Course c) -> c.semesters().stream().min(Integer::compare).orElse(Integer.MAX_VALUE))
                .thenComparing(c -> c.name().text(), greek)
                .thenComparing(Course::code);
        return s.courses().stream()
                .filter(Course::isActive)
                .filter(c -> semester == null || c.semesters().contains(semester))
                .filter(c -> level == null || level.equals(c.level()))
                .filter(c -> cycle == null || c.cycles().contains(cycle))
                .sorted(order)
                .map(CourseSummary::of)
                .toList();
    }

    public CourseDetail detail(TenantSnapshot s, String code) {
        Course c = s.course(code).filter(Course::isActive)
                .orElseThrow(() -> ApiException.notFound("COURSE_NOT_FOUND", "Unknown course '" + code + "'"));

        List<List<CourseRef>> prerequisites = c.prerequisites().stream()
                .map(group -> group.stream()
                        .map(p -> s.course(p).map(CourseRef::of).orElse(new CourseRef(p, null)))
                        .toList())
                .toList();

        List<CourseRef> requiredBy = s.courses().stream()
                .filter(Course::isActive)
                .filter(o -> o.prerequisites().stream().anyMatch(g -> g.stream().anyMatch(p -> p.equalsIgnoreCase(c.code()))))
                .sorted(Comparator.comparing(Course::code))
                .map(CourseRef::of)
                .toList();

        return new CourseDetail(c.code(), c.name(), c.level(), c.ects(), c.semesters(), c.cycles(), c.cyclesNote(),
                prerequisites, c.prerequisitesText(), c.instructors(), c.offeredBy(), c.language(), c.description(),
                c.aliases(), c.eclassUrl(), requiredBy);
    }

    /** Active courses taught by one faculty member, ordered by code. */
    public List<CourseRef> taughtBy(TenantSnapshot s, String facultyId) {
        return s.courses().stream()
                .filter(Course::isActive)
                .filter(c -> c.instructors().stream().anyMatch(i -> facultyId.equals(i.facultyId())))
                .sorted(Comparator.comparing(Course::code))
                .map(CourseRef::of)
                .toList();
    }
}
