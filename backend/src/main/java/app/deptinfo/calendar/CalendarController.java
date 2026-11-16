package app.deptinfo.calendar;

import app.deptinfo.tenant.TenantRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/universities/{uni}/departments/{dept}/calendar")
public class CalendarController {

    private final TenantRegistry registry;

    public CalendarController(TenantRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public AcademicCalendar get(@PathVariable String uni, @PathVariable String dept) {
        return registry.require(uni, dept).calendar();
    }
}
