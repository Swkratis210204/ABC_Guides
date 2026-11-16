package app.deptinfo.data;

import app.deptinfo.courses.Course;
import app.deptinfo.tenant.TenantSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Loads the real AUEB CS seed data and checks it against numbers stated in the study guide. */
class SeedDataTest {

    private static final Path SEED = Path.of("..", "data-seed");

    private TenantSnapshot load() {
        DataLoader loader = new DataLoader(new DataValidator());
        List<DataLoader.TenantLoadResult> results = loader.loadAll(SEED);
        assertThat(results).hasSize(1);
        DataLoader.TenantLoadResult r = results.get(0);
        assertThat(r.errors()).as("validation errors").isEmpty();
        assertThat(r.key().toString()).isEqualTo("aueb/cs");
        return r.snapshot();
    }

    @Test
    void seedDataIsValidAndMatchesTheGuide() {
        TenantSnapshot s = load();

        Map<String, Long> perLevel = s.courses().stream()
                .collect(Collectors.groupingBy(Course::level, Collectors.counting()));
        assertThat(perLevel).containsEntry("CORE_MANDATORY", 22L)   // "22 υποχρεωτικά μαθήματα πυρήνα"
                .containsEntry("CORE_ELECTIVE", 10L)                // "ΕΠ: 10 διαθέσιμα"
                .containsEntry("ELECTIVE", 36L)
                .containsEntry("FREE", 20L);

        assertThat(s.meta().cycles()).hasSize(8);                  // "8 Κύκλοι Σπουδών"
        assertThat(s.meta().sectors()).hasSize(3);                 // "Τρεις Ερευνητικοί Τομείς"
        assertThat(s.meta().semesters()).hasSize(8);
        assertThat(s.faculty()).hasSize(34);                       // 23 ΔΕΠ + 11 ΕΔΙΠ
        assertThat(s.requirements().totalEcts()).isEqualTo(240.0);

        assertThat(s.courses().stream().filter(c -> "CORE_ELECTIVE".equals(c.level()))
                .allMatch(c -> c.ects() == 7.0)).as("every ΕΠ course is 7 ECTS").isTrue();
    }

    @Test
    void specificCoursesAreTranscribedCorrectly() {
        TenantSnapshot s = load();

        Course ds = s.course("3335").orElseThrow();           // Δομές Δεδομένων: Python ή Java
        assertThat(ds.name().el()).isEqualTo("Δομές Δεδομένων");
        assertThat(ds.ects()).isEqualTo(7.0);
        assertThat(ds.semesters()).containsExactly(3);
        assertThat(ds.prerequisites()).containsExactly(List.of("3125", "3222"));

        Course linalg2 = s.course("6082").orElseThrow();      // fractional ECTS
        assertThat(linalg2.ects()).isEqualTo(7.5);
        assertThat(linalg2.offeredBy()).isEqualTo("ΣΤΑ");

        Course security = s.course("3761").orElseThrow();     // Ασφάλεια Δικτύων: both network courses
        assertThat(security.prerequisites()).containsExactly(List.of("3571"), List.of("3672"));
        assertThat(security.semesters()).containsExactly(7);

        Course graphics = s.course("3781").orElseThrow();
        assertThat(graphics.language()).isEqualTo("en");

        Course external = s.course("8154").orElseThrow();     // level 4, ECTS unknown
        assertThat(external.level()).isEqualTo("FREE");
        assertThat(external.ects()).isNull();
        assertThat(external.semesters()).isEmpty();

        assertThat(s.course("3151").orElseThrow().instructors())
                .singleElement().satisfies(i -> assertThat(i.facultyId()).isNull()); // external lecturer
        assertThat(s.course("3117").orElseThrow().instructors())
                .singleElement().satisfies(i -> assertThat(i.facultyId()).isEqualTo("amanatidis-georgios"));
    }

    /** The department's spreadsheet marks these three as offered in either semester (Ζ΄/Η΄). */
    @Test
    void thesisAndLinearModelsAreOfferedInEitherLastSemester() {
        TenantSnapshot s = load();
        for (String code : List.of("6023", "3802", "3850")) {
            assertThat(s.course(code).orElseThrow().semesters()).as(code).containsExactly(7, 8);
        }
        assertThat(s.course("6082").orElseThrow().semesters()).containsExactly(8);    // a normal Η΄-only course
    }

    /** The core electives' prerequisites come from the department's spreadsheet; the PDF lists none for them. */
    @Test
    void coreElectivesCarryThePrerequisitesFromTheSpreadsheet() {
        TenantSnapshot s = load();
        Course dataMgmt = s.course("3543").orElseThrow();
        assertThat(dataMgmt.prerequisitesText()).isEqualTo("ΔΔ, ΒΔ");
        assertThat(dataMgmt.prerequisites()).containsExactly(List.of("3335"), List.of("3436"));     // both required
        assertThat(s.course("3541").orElseThrow().prerequisites()).containsExactly(List.of("3125"), List.of("3222"));
        assertThat(s.course("3515").orElseThrow().prerequisites()).isEmpty();                        // Λογική: none
        long withPrereqs = s.courses().stream().filter(c -> "CORE_ELECTIVE".equals(c.level()) && !c.prerequisites().isEmpty()).count();
        assertThat(withPrereqs).isEqualTo(9);
    }

    /** Cycle membership for the 4 courses where the guide contradicts itself; values decided by the project owner. */
    @Test
    void contestedCycleMembershipFollowsTheOwnersDecision() {
        TenantSnapshot s = load();
        assertThat(s.course("3155").orElseThrow().cycles()).containsExactly("data-science-ml", "applied-math");
        assertThat(s.course("3515").orElseThrow().cycles()).containsExactly("theoretical-cs", "data-knowledge-mgmt");
        assertThat(s.course("3672").orElseThrow().cycles()).containsExactly("systems-networks", "software-systems", "cybersecurity");
        assertThat(s.course("3842").orElseThrow().cycles()).containsExactly("data-knowledge-mgmt");
    }

    @Test
    void courseLookupIsCaseInsensitive() {
        TenantSnapshot s = load();
        assertThat(s.course(" 3335 ")).isPresent();
        assertThat(s.course("9999")).isEmpty();
        assertThat(s.facultyMember("AMANATIDIS-GEORGIOS")).isPresent();
    }
}
