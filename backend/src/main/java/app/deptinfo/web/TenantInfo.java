package app.deptinfo.web;

import app.deptinfo.courses.Course;
import app.deptinfo.tenant.I18n;
import app.deptinfo.tenant.TenantMeta;
import app.deptinfo.tenant.TenantSnapshot;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Everything the frontend needs to draw navigation for one department in a single call: labels, taxonomies and how
 * many courses / people sit under each browse option.
 */
public record TenantInfo(
        TenantMeta.University university,
        TenantMeta.Department department,
        List<SemesterInfo> semesters,
        List<LevelInfo> levels,
        List<CycleInfo> cycles,
        List<SectorInfo> sectors,
        List<TenantMeta.Rank> ranks,
        Map<String, Object> info,
        Counts counts) {

    public record SemesterInfo(int number, String label, long courseCount) {
    }

    public record LevelInfo(String id, int number, @JsonProperty("short") String shortLabel, I18n name, String color,
                            List<Integer> semesterRange, long courseCount) {
    }

    public record CycleInfo(String id, int number, I18n name, long courseCount) {
    }

    public record SectorInfo(String id, I18n name, String topics, long memberCount) {
    }

    public record Counts(long courses, long faculty) {
    }

    public static TenantInfo of(TenantSnapshot s) {
        TenantMeta m = s.meta();
        List<Course> active = s.courses().stream().filter(Course::isActive).toList();
        return new TenantInfo(
                m.university(),
                m.department(),
                m.semesters().stream().map(x -> new SemesterInfo(x.number(), x.label(),
                        active.stream().filter(c -> c.semesters().contains(x.number())).count())).toList(),
                m.levels().stream().map(x -> new LevelInfo(x.id(), x.number(), x.shortLabel(), x.name(), x.color(),
                        x.semesterRange(), active.stream().filter(c -> x.id().equals(c.level())).count())).toList(),
                m.cycles().stream().map(x -> new CycleInfo(x.id(), x.number(), x.name(),
                        active.stream().filter(c -> c.cycles().contains(x.id())).count())).toList(),
                m.sectors().stream().map(x -> new SectorInfo(x.id(), x.name(), x.topics(),
                        s.faculty().stream().filter(f -> x.id().equals(f.sector())).count())).toList(),
                m.ranks(),
                m.info(),
                new Counts(active.size(), s.faculty().size()));
    }
}
