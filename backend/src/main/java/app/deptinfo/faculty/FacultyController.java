package app.deptinfo.faculty;

import app.deptinfo.tenant.TenantRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/universities/{uni}/departments/{dept}/faculty")
public class FacultyController {

    private final TenantRegistry registry;
    private final FacultyService faculty;

    public FacultyController(TenantRegistry registry, FacultyService faculty) {
        this.registry = registry;
        this.faculty = faculty;
    }

    @GetMapping
    public List<FacultyView> list(@PathVariable String uni, @PathVariable String dept,
                                  @RequestParam(required = false) String sector,
                                  @RequestParam(required = false) String category) {
        return faculty.list(registry.require(uni, dept), sector, category);
    }

    @GetMapping("/{id}")
    public FacultyView detail(@PathVariable String uni, @PathVariable String dept, @PathVariable String id) {
        return faculty.detail(registry.require(uni, dept), id);
    }
}
