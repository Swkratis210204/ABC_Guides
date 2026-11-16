package app.deptinfo.courses;

import app.deptinfo.tenant.TenantRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/universities/{uni}/departments/{dept}/courses")
public class CourseController {

    private final TenantRegistry registry;
    private final CourseService courses;

    public CourseController(TenantRegistry registry, CourseService courses) {
        this.registry = registry;
        this.courses = courses;
    }

    @GetMapping
    public List<CourseSummary> list(@PathVariable String uni, @PathVariable String dept,
                                    @RequestParam(required = false) Integer semester,
                                    @RequestParam(required = false) String level,
                                    @RequestParam(required = false) String cycle) {
        return courses.list(registry.require(uni, dept), semester, level, cycle);
    }

    @GetMapping("/{code}")
    public CourseDetail detail(@PathVariable String uni, @PathVariable String dept, @PathVariable String code) {
        return courses.detail(registry.require(uni, dept), code);
    }
}
