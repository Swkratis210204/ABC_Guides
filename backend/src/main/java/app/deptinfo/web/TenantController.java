package app.deptinfo.web;

import app.deptinfo.tenant.TenantMeta;
import app.deptinfo.tenant.TenantRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Top of the hierarchy: universities, their departments, and one department's navigation info. */
@RestController
@RequestMapping("/api/universities")
public class TenantController {

    private final TenantRegistry registry;

    public TenantController(TenantRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<TenantMeta.University> universities() {
        return registry.universities();
    }

    @GetMapping("/{uni}/departments")
    public List<TenantMeta.Department> departments(@PathVariable String uni) {
        if (!registry.hasUniversity(uni)) {
            throw ApiException.notFound("UNIVERSITY_NOT_FOUND", "Unknown university '" + uni + "'");
        }
        return registry.departments(uni);
    }

    @GetMapping("/{uni}/departments/{dept}")
    public TenantInfo department(@PathVariable String uni, @PathVariable String dept) {
        return TenantInfo.of(registry.require(uni, dept));
    }
}
