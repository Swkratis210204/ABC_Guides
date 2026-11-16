package app.deptinfo.data;

import app.deptinfo.config.AppProperties;
import app.deptinfo.tenant.TenantKey;
import app.deptinfo.tenant.TenantRegistry;
import app.deptinfo.tenant.TenantSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads data at startup and on admin request. Both follow build, validate, swap:
 * <ul>
 *   <li><b>Startup:</b> a tenant that fails validation is skipped and logged; the others still load. If none loads, startup fails.</li>
 *   <li><b>Reload:</b> if any tenant fails validation nothing changes and the full error list is returned.</li>
 * </ul>
 */
@Service
public class DataReloadService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataReloadService.class);

    private final AppProperties props;
    private final DataLoader loader;
    private final TenantRegistry registry;

    public DataReloadService(AppProperties props, DataLoader loader, TenantRegistry registry) {
        this.props = props;
        this.loader = loader;
        this.registry = registry;
    }

    public record TenantReport(String tenant, boolean loaded, List<String> errors, List<String> warnings,
                               int courses, int faculty) {
    }

    public record ReloadReport(boolean ok, List<TenantReport> tenants) {
    }

    @Override
    public void run(ApplicationArguments args) {
        Path dataDir = Path.of(props.dataDir()).toAbsolutePath().normalize();
        seedIfEmpty(dataDir);
        List<DataLoader.TenantLoadResult> results = loader.loadAll(dataDir);
        Map<TenantKey, TenantSnapshot> loaded = new HashMap<>();
        for (DataLoader.TenantLoadResult r : results) {
            report(r);
            if (r.ok()) {
                loaded.put(r.key(), r.snapshot());
            }
        }
        if (loaded.isEmpty()) {
            throw new IllegalStateException("No valid tenant data found under " + dataDir.resolve("tenants")
                    + " (" + results.size() + " tenant folder(s) checked). See the log above for the reasons.");
        }
        registry.replaceAll(loaded);
        log.info("Loaded {} of {} tenant(s) from {}", loaded.size(), results.size(), dataDir);
    }

    /** Re-reads all tenants. Replaces the live data only if every tenant is valid. */
    public ReloadReport reload() {
        Path dataDir = Path.of(props.dataDir()).toAbsolutePath().normalize();
        List<DataLoader.TenantLoadResult> results = loader.loadAll(dataDir);
        boolean allOk = !results.isEmpty() && results.stream().allMatch(DataLoader.TenantLoadResult::ok);
        List<TenantReport> reports = results.stream().map(DataReloadService::toReport).toList();
        if (allOk) {
            Map<TenantKey, TenantSnapshot> loaded = new HashMap<>();
            results.forEach(r -> loaded.put(r.key(), r.snapshot()));
            registry.replaceAll(loaded);
            log.info("Reloaded {} tenant(s)", loaded.size());
        } else {
            log.warn("Reload rejected: validation failed, keeping previous data");
        }
        return new ReloadReport(allOk, reports);
    }

    private void report(DataLoader.TenantLoadResult r) {
        if (r.ok()) {
            log.info("Tenant {}: {} courses, {} faculty", r.key(), r.snapshot().courses().size(), r.snapshot().faculty().size());
            r.warnings().forEach(w -> log.warn("Tenant {}: {}", r.key(), w));
        } else {
            log.error("Tenant {} skipped, {} problem(s):", r.key(), r.errors().size());
            r.errors().forEach(e -> log.error("  - {}", e));
        }
    }

    private static TenantReport toReport(DataLoader.TenantLoadResult r) {
        TenantSnapshot s = r.snapshot();
        return new TenantReport(r.key().toString(), r.ok(), r.errors(), r.warnings(),
                s == null ? 0 : s.courses().size(), s == null ? 0 : s.faculty().size());
    }

    /** First boot on an empty volume: copy the bundled seed data into the data directory. */
    private void seedIfEmpty(Path dataDir) {
        if (props.seedDir() == null || props.seedDir().isBlank()) {
            return;
        }
        Path tenants = dataDir.resolve("tenants");
        try {
            if (Files.isDirectory(tenants)) {
                try (Stream<Path> s = Files.list(tenants)) {
                    if (s.findAny().isPresent()) {
                        return;
                    }
                }
            }
            Path seed = Path.of(props.seedDir()).toAbsolutePath().normalize().resolve("tenants");
            if (!Files.isDirectory(seed)) {
                log.warn("seed-dir {} has no tenants folder; nothing to copy", props.seedDir());
                return;
            }
            log.info("Data directory is empty; seeding from {}", seed);
            try (Stream<Path> walk = Files.walk(seed)) {
                for (Path from : walk.toList()) {
                    Path to = tenants.resolve(seed.relativize(from).toString());
                    if (Files.isDirectory(from)) {
                        Files.createDirectories(to);
                    } else {
                        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not seed data directory " + dataDir, e);
        }
    }
}
