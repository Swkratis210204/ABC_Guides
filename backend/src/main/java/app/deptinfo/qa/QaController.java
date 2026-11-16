package app.deptinfo.qa;

import app.deptinfo.tenant.TenantRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/universities/{uni}/departments/{dept}")
public class QaController {

    private final TenantRegistry registry;

    public QaController(TenantRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/faq")
    public QaPage faq(@PathVariable String uni, @PathVariable String dept) {
        return registry.require(uni, dept).faq();
    }

    @GetMapping("/internship")
    public QaPage internship(@PathVariable String uni, @PathVariable String dept) {
        return registry.require(uni, dept).internship();
    }
}
