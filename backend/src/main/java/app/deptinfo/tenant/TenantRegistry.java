package app.deptinfo.tenant;

import app.deptinfo.web.ApiException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Holds the current snapshot of every tenant. Readers never lock; a reload swaps the whole map at once. */
@Component
public class TenantRegistry {

    private final AtomicReference<Map<TenantKey, TenantSnapshot>> current = new AtomicReference<>(Map.of());

    public void replaceAll(Map<TenantKey, TenantSnapshot> snapshots) {
        current.set(Map.copyOf(snapshots));
    }

    public boolean isEmpty() {
        return current.get().isEmpty();
    }

    public Optional<TenantSnapshot> find(String uni, String dept) {
        return Optional.ofNullable(current.get().get(new TenantKey(uni, dept)));
    }

    /** Like {@link #find} but throws a 404 that says whether the university or the department was unknown. */
    public TenantSnapshot require(String uni, String dept) {
        return find(uni, dept).orElseThrow(() -> hasUniversity(uni)
                ? ApiException.notFound("DEPARTMENT_NOT_FOUND", "Unknown department '" + dept + "' for university '" + uni + "'")
                : ApiException.notFound("UNIVERSITY_NOT_FOUND", "Unknown university '" + uni + "'"));
    }

    public boolean hasUniversity(String uni) {
        return current.get().keySet().stream().anyMatch(k -> k.uni().equals(uni));
    }

    public List<TenantSnapshot> all() {
        return current.get().values().stream()
                .sorted(Comparator.comparing(s -> s.key().toString()))
                .toList();
    }

    /** Distinct universities, ordered by slug. */
    public List<TenantMeta.University> universities() {
        return current.get().values().stream()
                .map(s -> s.meta().university())
                .collect(java.util.stream.Collectors.toMap(TenantMeta.University::slug, u -> u, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparing(TenantMeta.University::slug))
                .toList();
    }

    public List<TenantMeta.Department> departments(String uni) {
        return current.get().values().stream()
                .filter(s -> s.key().uni().equals(uni))
                .map(s -> s.meta().department())
                .sorted(Comparator.comparing(TenantMeta.Department::slug))
                .toList();
    }
}
