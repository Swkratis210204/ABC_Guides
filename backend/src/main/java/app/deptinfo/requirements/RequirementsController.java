package app.deptinfo.requirements;

import app.deptinfo.tenant.TenantRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/universities/{uni}/departments/{dept}/requirements")
public class RequirementsController {

    private final TenantRegistry registry;

    public RequirementsController(TenantRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public Requirements get(@PathVariable String uni, @PathVariable String dept) {
        return registry.require(uni, dept).requirements();
    }
}
