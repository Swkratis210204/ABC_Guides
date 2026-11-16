package app.deptinfo.tenant;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Contents of tenant.json: identity plus the taxonomies the other files refer to. */
public record TenantMeta(
        int schemaVersion,
        University university,
        Department department,
        List<Semester> semesters,
        List<Level> levels,
        List<Cycle> cycles,
        List<Sector> sectors,
        List<Rank> ranks,
        Map<String, Object> info) {

    public TenantMeta {
        semesters = semesters == null ? List.of() : List.copyOf(semesters);
        levels = levels == null ? List.of() : List.copyOf(levels);
        cycles = cycles == null ? List.of() : List.copyOf(cycles);
        sectors = sectors == null ? List.of() : List.copyOf(sectors);
        ranks = ranks == null ? List.of() : List.copyOf(ranks);
        info = info == null ? Map.of() : Map.copyOf(info);
    }

    public record University(String slug, I18n name, String shortName, String website) {
    }

    public record Department(String slug, I18n name) {
    }

    public record Semester(int number, String label) {
    }

    /** A course level: ΥΠ / ΕΠ / Ε / ΕΛ. The JSON key for the short label is "short". */
    public record Level(
            String id,
            int number,
            @JsonProperty("short") String shortLabel,
            I18n name,
            String color,
            List<Integer> semesterRange) {
    }

    public record Cycle(String id, int number, I18n name) {
    }

    public record Sector(String id, I18n name, String topics) {
    }

    public record Rank(String id, I18n label) {
    }
}
