package app.deptinfo.data;

import app.deptinfo.tenant.TenantKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Each test writes a tiny valid tenant, breaks exactly one thing, and checks the error that comes back. */
class DataValidatorTest {

    private static final String TENANT = """
            {"schemaVersion":1,
             "university":{"slug":"u","name":{"el":"U"}},
             "department":{"slug":"d","name":{"el":"D"}},
             "semesters":[{"number":1,"label":"A"},{"number":2,"label":"B"}],
             "levels":[{"id":"CORE_MANDATORY","number":1,"short":"YP","name":{"el":"x"},"color":"blue","semesterRange":[1,2]},
                       {"id":"FREE","number":4,"short":"EL","name":{"el":"f"},"color":null,"semesterRange":[1,2]}],
             "cycles":[{"id":"c1","number":1,"name":{"el":"c"}}],
             "sectors":[{"id":"s1","name":{"el":"s"},"topics":"t"}],
             "ranks":[{"id":"R","label":{"el":"r"}}],
             "info":{}}""";

    private static final String COURSES = """
            [{"code":"A1","name":{"el":"A"},"level":"CORE_MANDATORY","ects":6,"semesters":[1],"cycles":[],"prerequisites":[],"instructors":[]},
             {"code":"A2","name":{"el":"B"},"level":"CORE_MANDATORY","ects":6,"semesters":[2],"cycles":["c1"],"prerequisites":[["A1"]],"instructors":[{"name":"F","facultyId":"f1"}]}]""";

    private static final String FACULTY = """
            [{"id":"f1","name":{"el":"F"},"category":"DEP","rank":"R","sector":"s1","status":"ACTIVE"}]""";

    private static final String REQUIREMENTS = """
            {"schemaVersion":1,"totalEcts":12,"rules":[
              {"id":"r1","kind":"TOTAL_ECTS","title":{"el":"t"},"description":{"el":"d"},"params":{"min":12}},
              {"id":"r2","kind":"ALL_MANDATORY","title":{"el":"t"},"description":{"el":"d"},"params":{"level":"CORE_MANDATORY"}}]}""";

    private static final String CALENDAR = """
            {"schemaVersion":1,"academicYear":"2026-2027",
             "terms":[{"id":"t1","name":{"el":"T"},"semesterType":"WINTER","start":"2026-10-05","end":"2027-01-15"}],
             "examPeriods":[{"id":"e1","name":{"el":"E"},"termId":"t1","start":"2027-01-18","end":"2027-02-12"},
                            {"id":"e2","name":{"el":"R"},"termId":null,"start":null,"end":null}],
             "holidays":[{"id":"h1","name":{"el":"H"},"start":"2026-10-28","end":null,"kind":"PUBLIC_HOLIDAY"}]}""";

    private final DataLoader loader = new DataLoader(new DataValidator());

    private Path write(Path dir, String tenant, String courses, String faculty, String requirements, String calendar) throws IOException {
        Path d = Files.createDirectories(dir.resolve("tenants").resolve("u").resolve("d"));
        Files.writeString(d.resolve("tenant.json"), tenant, StandardCharsets.UTF_8);
        Files.writeString(d.resolve("courses.json"), courses, StandardCharsets.UTF_8);
        Files.writeString(d.resolve("faculty.json"), faculty, StandardCharsets.UTF_8);
        Files.writeString(d.resolve("requirements.json"), requirements, StandardCharsets.UTF_8);
        Files.writeString(d.resolve("calendar.json"), calendar, StandardCharsets.UTF_8);
        return d;
    }

    private DataLoader.TenantLoadResult load(Path dir, String courses, String faculty, String requirements, String calendar) throws IOException {
        Path d = write(dir, TENANT, courses, faculty, requirements, calendar);
        return loader.loadTenant(new TenantKey("u", "d"), d);
    }

    private DataLoader.TenantLoadResult loadCourses(Path dir, String courses) throws IOException {
        return load(dir, courses, FACULTY, REQUIREMENTS, CALENDAR);
    }

    @Test
    void emptyQaPagesWhenFilesAreMissing(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = loadCourses(dir, COURSES);
        assertThat(r.errors()).isEmpty();
        assertThat(r.snapshot().faq().sections()).isEmpty();
        assertThat(r.snapshot().internship().sections()).isEmpty();
    }

    @Test
    void rejectsBadQaPage(@TempDir Path dir) throws IOException {
        Path d = write(dir, TENANT, COURSES, FACULTY, REQUIREMENTS, CALENDAR);
        Files.writeString(d.resolve("faq.json"), """
                {"schemaVersion":1,"sections":[{"id":"s1","title":{"el":"T"},"items":[
                  {"id":"q1","question":{"el":"Q"},"answer":{"el":""}},
                  {"id":"q1","question":{"el":"Q2"},"answer":{"el":"A"},"links":[{"label":"x","url":"javascript:alert(1)"}]}]}]}""",
                StandardCharsets.UTF_8);
        DataLoader.TenantLoadResult r = loader.loadTenant(new TenantKey("u", "d"), d);
        assertThat(r.ok()).isFalse();
        assertThat(r.errors()).anyMatch(e -> e.contains("faq.json item[q1]: answer is missing"))
                .anyMatch(e -> e.contains("duplicate id"))
                .anyMatch(e -> e.contains("https: or mailto:"));
    }

    @Test
    void validDataLoads(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = loadCourses(dir, COURSES);
        assertThat(r.errors()).isEmpty();
        assertThat(r.snapshot().courses()).hasSize(2);
    }

    @Test
    void duplicateCourseCodeIsReportedCaseInsensitively(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = loadCourses(dir, COURSES.replace("\"A2\"", "\"a1\"").replace("[[\"A1\"]]", "[]"));
        assertThat(r.errors()).anyMatch(e -> e.contains("duplicate course code"));
    }

    @Test
    void unknownLevelIsReported(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = loadCourses(dir, COURSES.replaceFirst("CORE_MANDATORY", "NOPE"));
        assertThat(r.errors()).anyMatch(e -> e.contains("unknown level 'NOPE'"));
    }

    @Test
    void unknownPrerequisiteIsReported(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = loadCourses(dir, COURSES.replace("[[\"A1\"]]", "[[\"ZZZ\"]]"));
        assertThat(r.errors()).anyMatch(e -> e.contains("prerequisite 'ZZZ' is not a course"));
    }

    @Test
    void prerequisiteCycleIsReported(@TempDir Path dir) throws IOException {
        String cyclic = COURSES.replaceFirst("\"prerequisites\":\\[\\]", "\"prerequisites\":[[\"A2\"]]");
        DataLoader.TenantLoadResult r = loadCourses(dir, cyclic);
        assertThat(r.errors()).anyMatch(e -> e.contains("prerequisite cycle"));
    }

    @Test
    void selfPrerequisiteIsReported(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = loadCourses(dir, COURSES.replace("[[\"A1\"]]", "[[\"A2\"]]"));
        assertThat(r.errors()).anyMatch(e -> e.contains("own prerequisite"));
    }

    @Test
    void unknownInstructorFacultyIdIsReported(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = loadCourses(dir, COURSES.replace("\"facultyId\":\"f1\"", "\"facultyId\":\"ghost\""));
        assertThat(r.errors()).anyMatch(e -> e.contains("facultyId 'ghost'"));
    }

    @Test
    void misspelledPropertyIsRejected(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = loadCourses(dir, COURSES.replaceFirst("\"ects\"", "\"ectss\""));
        assertThat(r.errors()).anyMatch(e -> e.startsWith("courses.json") && e.contains("ectss"));
    }

    @Test
    void zeroEctsAndMissingEctsAreReported(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult zero = loadCourses(dir, COURSES.replaceFirst("\"ects\":6", "\"ects\":0"));
        assertThat(zero.errors()).anyMatch(e -> e.contains("ects must be greater than 0"));

        DataLoader.TenantLoadResult missing = loadCourses(dir, COURSES.replaceFirst("\"ects\":6", "\"ects\":null"));
        assertThat(missing.errors()).anyMatch(e -> e.contains("ects is required"));
    }

    @Test
    void freeLevelMayOmitEctsAndSemester(@TempDir Path dir) throws IOException {
        String free = COURSES.replaceFirst("\\]$", ",{\"code\":\"X1\",\"name\":{\"el\":\"X\"},\"level\":\"FREE\",\"ects\":null,\"semesters\":[],\"cycles\":[],\"prerequisites\":[],\"instructors\":[]}]");
        assertThat(loadCourses(dir, free).errors()).isEmpty();
    }

    @Test
    void otherLevelsNeedAtLeastOneValidSemester(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult none = loadCourses(dir, COURSES.replaceFirst("\"semesters\":\\[1\\]", "\"semesters\":[]"));
        assertThat(none.errors()).anyMatch(e -> e.contains("semesters is required"));

        DataLoader.TenantLoadResult unknown = loadCourses(dir, COURSES.replaceFirst("\"semesters\":\\[1\\]", "\"semesters\":[9]"));
        assertThat(unknown.errors()).anyMatch(e -> e.contains("unknown semester 9"));

        DataLoader.TenantLoadResult twice = loadCourses(dir, COURSES.replaceFirst("\"semesters\":\\[1\\]", "\"semesters\":[1,1]"));
        assertThat(twice.errors()).anyMatch(e -> e.contains("listed twice"));
    }

    @Test
    void aCourseMayBeOfferedInTwoSemesters(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = loadCourses(dir, COURSES.replaceFirst("\"semesters\":\\[1\\]", "\"semesters\":[1,2]"));
        assertThat(r.errors()).isEmpty();
        assertThat(r.snapshot().course("A1").orElseThrow().semesters()).containsExactly(1, 2);
    }

    @Test
    void depStaffNeedSectorButEdipDoNot(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult dep = load(dir, COURSES, FACULTY.replace("\"sector\":\"s1\",", ""), REQUIREMENTS, CALENDAR);
        assertThat(dep.errors()).anyMatch(e -> e.contains("sector is required for DEP"));

        String edip = """
                [{"id":"f1","name":{"el":"F"},"category":"EDIP","rank":null,"sector":null,"status":"ACTIVE"}]""";
        assertThat(load(dir, COURSES, edip, REQUIREMENTS, CALENDAR).errors()).isEmpty();
    }

    @Test
    void ruleReferencingUnknownLevelIsReported(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = load(dir, COURSES, FACULTY, REQUIREMENTS.replace("\"level\":\"CORE_MANDATORY\"", "\"level\":\"WAT\""), CALENDAR);
        assertThat(r.errors()).anyMatch(e -> e.contains("'WAT' is not a known level"));
    }

    @Test
    void unknownRuleKindIsReported(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = load(dir, COURSES, FACULTY, REQUIREMENTS.replace("ALL_MANDATORY", "MAGIC"), CALENDAR);
        assertThat(r.errors()).anyMatch(e -> e.contains("kind 'MAGIC'"));
    }

    @Test
    void calendarEndBeforeStartIsReported(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = load(dir, COURSES, FACULTY, REQUIREMENTS,
                CALENDAR.replace("\"end\":\"2027-01-15\"", "\"end\":\"2026-01-15\""));
        assertThat(r.errors()).anyMatch(e -> e.contains("is before start"));
    }

    @Test
    void examPeriodWithUnknownTermIsReported(@TempDir Path dir) throws IOException {
        DataLoader.TenantLoadResult r = load(dir, COURSES, FACULTY, REQUIREMENTS, CALENDAR.replace("\"termId\":\"t1\"", "\"termId\":\"nope\""));
        assertThat(r.errors()).anyMatch(e -> e.contains("unknown termId 'nope'"));
    }

    @Test
    void folderNameMustMatchSlug(@TempDir Path dir) throws IOException {
        Path d = write(dir, TENANT, COURSES, FACULTY, REQUIREMENTS, CALENDAR);
        DataLoader.TenantLoadResult r = loader.loadTenant(new TenantKey("u", "other"), d);
        assertThat(r.errors()).anyMatch(e -> e.contains("does not match its folder name"));
    }

    @Test
    void missingFileIsReported(@TempDir Path dir) throws IOException {
        Path d = write(dir, TENANT, COURSES, FACULTY, REQUIREMENTS, CALENDAR);
        Files.delete(d.resolve("faculty.json"));
        DataLoader.TenantLoadResult r = loader.loadTenant(new TenantKey("u", "d"), d);
        assertThat(r.errors()).contains("faculty.json: file is missing");
    }

    @Test
    void allProblemsAreCollectedInOnePass(@TempDir Path dir) throws IOException {
        String broken = COURSES.replaceFirst("CORE_MANDATORY", "NOPE").replace("[[\"A1\"]]", "[[\"ZZZ\"]]");
        DataLoader.TenantLoadResult r = loadCourses(dir, broken);
        assertThat(r.errors()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(r.snapshot()).isNull();
    }
}
