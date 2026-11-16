package app.deptinfo.data;

import app.deptinfo.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/** Admin-only (see AdminKeyInterceptor): re-read the data folder without restarting. */
@RestController
@RequestMapping("/api/admin")
public class DataReloadController {

    private final DataReloadService reloads;

    public DataReloadController(DataReloadService reloads) {
        this.reloads = reloads;
    }

    @PostMapping("/reload")
    public DataReloadService.ReloadReport reload() {
        DataReloadService.ReloadReport report = reloads.reload();
        if (!report.ok()) {
            List<String> details = new ArrayList<>();
            for (DataReloadService.TenantReport t : report.tenants()) {
                t.errors().forEach(e -> details.add(t.tenant() + ": " + e));
            }
            if (details.isEmpty()) {
                details.add("No tenant folders were found in the data directory");
            }
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED",
                    "Reload rejected: the new data is invalid, the previous data is still live", details);
        }
        return report;
    }
}
