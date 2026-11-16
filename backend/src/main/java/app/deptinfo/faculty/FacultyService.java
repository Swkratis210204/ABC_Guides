package app.deptinfo.faculty;

import app.deptinfo.courses.CourseService;
import app.deptinfo.tenant.TenantSnapshot;
import app.deptinfo.web.ApiException;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FacultyService {

    private static final Set<String> CATEGORIES = Set.of("DEP", "EDIP");

    private final CourseService courses;

    public FacultyService(CourseService courses) {
        this.courses = courses;
    }

    /** Lists staff, optionally by research sector and/or category (DEP or EDIP), ordered by name. */
    public List<FacultyView> list(TenantSnapshot s, String sector, String category) {
        if (sector != null && s.meta().sectors().stream().noneMatch(x -> x.id().equals(sector))) {
            throw ApiException.badRequest("INVALID_FILTER", "Unknown sector '" + sector + "'");
        }
        if (category != null && !CATEGORIES.contains(category)) {
            throw ApiException.badRequest("INVALID_FILTER", "Unknown category '" + category + "' (expected DEP or EDIP)");
        }
        Collator greek = Collator.getInstance(Locale.forLanguageTag("el-GR"));
        return s.faculty().stream()
                .filter(f -> sector == null || sector.equals(f.sector()))
                .filter(f -> category == null || category.equals(f.category()))
                .sorted(Comparator.comparing((FacultyMember f) -> f.name().text(), greek).thenComparing(FacultyMember::id))
                .map(f -> view(s, f))
                .toList();
    }

    public FacultyView detail(TenantSnapshot s, String id) {
        FacultyMember f = s.facultyMember(id)
                .orElseThrow(() -> ApiException.notFound("FACULTY_NOT_FOUND", "Unknown faculty member '" + id + "'"));
        return view(s, f);
    }

    private FacultyView view(TenantSnapshot s, FacultyMember f) {
        return new FacultyView(f.id(), f.name(), f.category(), f.rank(), f.sector(), f.status(), f.statusNote(),
                f.email(), f.homepageUrl(), courses.taughtBy(s, f.id()));
    }
}
