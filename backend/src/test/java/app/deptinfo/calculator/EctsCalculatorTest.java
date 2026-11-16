package app.deptinfo.calculator;

import app.deptinfo.courses.Course;
import app.deptinfo.data.DataLoader;
import app.deptinfo.data.DataValidator;
import app.deptinfo.tenant.TenantSnapshot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** The calculator against the real AUEB CS rules and courses. */
class EctsCalculatorTest {

    private static TenantSnapshot tenant;
    private final EctsCalculator calculator = new EctsCalculator();

    @BeforeAll
    static void load() {
        tenant = new DataLoader(new DataValidator()).loadAll(Path.of("..", "data-seed")).get(0).snapshot();
    }

    private CalculatorResult calc(String... codes) {
        return calculator.calculate(tenant, List.of(codes));
    }

    private static CalculatorResult.RuleStatus rule(CalculatorResult r, String id) {
        return r.rules().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
    }

    private static List<String> codesOfLevel(String level) {
        return tenant.courses().stream().filter(c -> level.equals(c.level())).map(Course::code).collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- totals and rules

    @Test
    void nothingPassed() {
        CalculatorResult r = calc();
        assertThat(r.totals().earned()).isZero();
        assertThat(r.totals().required()).isEqualTo(240.0);
        assertThat(r.totals().remaining()).isEqualTo(240.0);
        assertThat(r.allCalculableRulesMet()).isFalse();
        assertThat(rule(r, "level1-all-mandatory").missing()).hasSize(22);
        assertThat(rule(r, "level2-min-courses").remaining()).isEqualTo(4.0);
        assertThat(rule(r, "level3-ects").remaining()).isEqualTo(64.0);
        assertThat(r.manualChecks()).hasSize(7);                    // the FREE_TEXT rules
        assertThat(r.cycles().maxCompletable()).isZero();
    }

    @Test
    void allMandatoryCoursesGive152EctsAndSatisfyOnlyTheMandatoryRule() {
        CalculatorResult r = calc(codesOfLevel("CORE_MANDATORY").toArray(String[]::new));
        assertThat(r.totals().earned()).isEqualTo(152.0);
        assertThat(r.totals().remaining()).isEqualTo(88.0);
        assertThat(rule(r, "level1-all-mandatory").satisfied()).isTrue();
        assertThat(rule(r, "level1-all-mandatory").missing()).isEmpty();
        assertThat(rule(r, "total-ects").satisfied()).isFalse();
        assertThat(r.allCalculableRulesMet()).isFalse();
    }

    @Test
    void aCompleteDegreePlanMeetsEveryCalculableRule() {
        List<String> codes = new ArrayList<>(codesOfLevel("CORE_MANDATORY"));   // 152
        codes.addAll(codesOfLevel("CORE_ELECTIVE"));                            // +70 = 222
        codes.addAll(List.of("3612", "3814", "3584"));                          // +18 = 240 (three 6-ECTS electives)
        CalculatorResult r = calculator.calculate(tenant, codes);
        assertThat(r.totals().earned()).isEqualTo(240.0);
        assertThat(r.totals().remaining()).isZero();
        assertThat(rule(r, "level2-min-courses").satisfied()).isTrue();
        assertThat(rule(r, "level3-ects").current()).isEqualTo(88.0);           // 70 from ΕΠ + 18 from Ε
        assertThat(r.allCalculableRulesMet()).isTrue();
    }

    @Test
    void justUnderTheTotalIsNotEnough() {
        List<String> codes = new ArrayList<>(codesOfLevel("CORE_MANDATORY"));
        codes.addAll(codesOfLevel("CORE_ELECTIVE"));
        codes.addAll(List.of("3612", "3814"));                                  // 234
        CalculatorResult r = calculator.calculate(tenant, codes);
        assertThat(r.totals().remaining()).isEqualTo(6.0);
        assertThat(rule(r, "total-ects").satisfied()).isFalse();
        assertThat(r.allCalculableRulesMet()).isFalse();
    }

    @Test
    void fractionalEctsAddUpExactly() {
        CalculatorResult r = calc("6082", "6142", "6125");                      // 7.5 + 7.5 + 7
        assertThat(r.totals().earned()).isEqualTo(22.0);
        assertThat(r.levels().stream().filter(l -> l.level().equals("ELECTIVE")).findFirst().orElseThrow().ectsEarned()).isEqualTo(22.0);
    }

    @Test
    void levelBreakdownCountsCoursesAndEcts() {
        CalculatorResult r = calc("3117", "3119", "3515", "3612");
        assertThat(r.levels()).extracting(CalculatorResult.LevelProgress::level)
                .containsExactly("CORE_MANDATORY", "CORE_ELECTIVE", "ELECTIVE", "FREE");
        assertThat(r.levels().get(0).coursesPassed()).isEqualTo(2);
        assertThat(r.levels().get(0).ectsEarned()).isEqualTo(12.0);
        assertThat(r.levels().get(1).ectsEarned()).isEqualTo(7.0);
        assertThat(r.levels().get(2).ectsEarned()).isEqualTo(6.0);
    }

    // ---------------------------------------------------------------- bad and odd input

    @Test
    void unknownCodesDuplicatesAndWhitespaceAreHandled() {
        CalculatorResult r = calc("3117", " 3117 ", "9999", "9999", "3119");
        assertThat(r.totals().earned()).isEqualTo(12.0);
        assertThat(r.unknownCodes()).containsExactly("9999");
        assertThat(r.warnings()).anyMatch(w -> w.code().equals("DUPLICATE_CODE") && "3117".equals(w.courseCode()));
    }

    @Test
    void levelFourCoursesWithoutEctsAddNothingAndWarn() {
        CalculatorResult r = calc("8154");
        assertThat(r.unknownCodes()).isEmpty();
        assertThat(r.totals().earned()).isZero();
        assertThat(r.warnings()).anyMatch(w -> w.code().equals("ECTS_UNKNOWN") && "8154".equals(w.courseCode()));
        assertThat(r.levels().get(3).coursesPassed()).isEqualTo(1);
    }

    @Test
    void prerequisiteWarningsAreAdvisoryAndRespectOrGroups() {
        CalculatorResult alone = calc("3335");                                  // needs Python OR Java
        assertThat(alone.warnings()).anyMatch(w -> w.code().equals("PREREQUISITE_NOT_PASSED") && "3335".equals(w.courseCode()));
        assertThat(alone.totals().earned()).isEqualTo(7.0);                     // still counted

        assertThat(calc("3335", "3125").warnings()).noneMatch(w -> w.code().equals("PREREQUISITE_NOT_PASSED"));
        assertThat(calc("3335", "3222").warnings()).noneMatch(w -> w.code().equals("PREREQUISITE_NOT_PASSED"));

        // 3761 needs BOTH network courses: one is not enough.
        assertThat(calc("3761", "3571").warnings()).anyMatch(w -> "3761".equals(w.courseCode()));
        assertThat(calc("3761", "3571", "3672").warnings()).noneMatch(w -> "3761".equals(w.courseCode()));
    }

    // ---------------------------------------------------------------- cycles

    @Test
    void fiveCoursesOfOneCycleCompleteIt() {
        CalculatorResult r = calc("3155", "3513", "3745", "3543", "6127");      // all in cycle 1
        assertThat(r.cycles().minCourses()).isEqualTo(5);
        assertThat(r.cycles().maxCompletable()).isEqualTo(1);
        CalculatorResult.CycleStatus c1 = r.cycles().items().get(0);
        assertThat(c1.id()).isEqualTo("data-science-ml");
        assertThat(c1.completedInBestPlan()).isTrue();
        assertThat(c1.assignedCodes()).hasSize(5);
        assertThat(c1.missingToComplete()).isZero();
    }

    @Test
    void fourCoursesAreNotEnoughAndTheGapIsReported() {
        CalculatorResult r = calc("3155", "3513", "3745", "3543");
        assertThat(r.cycles().maxCompletable()).isZero();
        assertThat(r.cycles().items().get(0).missingToComplete()).isEqualTo(1);
    }

    /** Cycles 1 and 3 share 3155, 3513 and 3573. Each has 5+ eligible courses, but only 8 distinct courses exist. */
    @Test
    void aCourseCannotCountForTwoCycles() {
        CalculatorResult r = calc("3155", "3513", "3573", "6127", "6005", "6082", "6142", "6125");
        CalculatorResult.CycleReport cycles = r.cycles();
        assertThat(cycles.items().get(0).passedCodes()).hasSize(5);             // cycle 1 on its own: 5
        assertThat(cycles.items().get(2).passedCodes()).hasSize(6);             // cycle 3 on its own: 6
        assertThat(cycles.maxCompletable()).isEqualTo(1);                        // but not both: needs 10 distinct
    }

    @Test
    void twoCyclesAreCompletableWhenEnoughDistinctCoursesExist() {
        // Same as above plus two cycle-1-only courses: 10 distinct courses can be split 5 + 5.
        CalculatorResult r = calc("3155", "3513", "3573", "6127", "6005", "6082", "6142", "6125", "6023", "3743");
        assertThat(r.cycles().maxCompletable()).isEqualTo(2);
        CalculatorResult.CycleStatus c1 = r.cycles().items().get(0);
        CalculatorResult.CycleStatus c3 = r.cycles().items().get(2);
        assertThat(c1.completedInBestPlan()).isTrue();
        assertThat(c3.completedInBestPlan()).isTrue();
        assertThat(c1.assignedCodes()).hasSize(5);
        assertThat(c3.assignedCodes()).hasSize(5);
        assertThat(c1.assignedCodes()).doesNotContainAnyElementsOf(c3.assignedCodes());   // no course used twice
    }

    @Test
    void findsTheBestPlanEvenWhenAGreedyChoiceWouldFail() {
        // 3672 is in cycles 5, 6, 8. Cycle 8 can only be completed if 3672 goes there.
        // Cycle 8 courses: 3662, 3672, 3642, 3745, 3761, 3791. Pass 5 of them plus enough for cycle 5 without 3672.
        CalculatorResult r = calc("3662", "3672", "3642", "3745", "3761",       // cycle 8 (5 courses)
                "3771", "3634", "3818", "3573", "3862");                          // cycle 5 without 3672 (5 courses)
        assertThat(r.cycles().maxCompletable()).isEqualTo(2);
    }

    // ---------------------------------------------------------------- ECTS typed in by the user (free electives)

    private CalculatorResult calcWith(List<String> codes, Map<String, Double> typedIn, double other) {
        return calculator.calculate(tenant, codes, typedIn, other);
    }

    /** The guide gives no ECTS for other departments' courses, so the user supplies them. */
    @Test
    void typedInEctsForAFreeElectiveAreAdded() {
        CalculatorResult r = calcWith(List.of("8154", "3117"), Map.of("8154", 6.0), 0);
        assertThat(r.totals().earned()).isEqualTo(12.0);                       // 6 (typed) + 6 (3117 from the guide)
        assertThat(r.totals().userProvided()).isEqualTo(6.0);
        assertThat(r.warnings()).noneMatch(w -> w.code().equals("ECTS_UNKNOWN"));
        assertThat(r.levels().get(3).ectsEarned()).isEqualTo(6.0);             // shows under level 4 (ΕΛ)
    }

    @Test
    void aFreeElectiveWithNoTypedEctsStillWarnsAndAddsNothing() {
        CalculatorResult r = calcWith(List.of("8154", "5622"), Map.of("5622", 6.0), 0);
        assertThat(r.totals().earned()).isEqualTo(6.0);                        // only 5622 had a value
        assertThat(r.warnings()).anyMatch(w -> w.code().equals("ECTS_UNKNOWN") && "8154".equals(w.courseCode()));
    }

    @Test
    void theGuideWinsOverTypedInEcts() {
        CalculatorResult r = calcWith(List.of("3117"), Map.of("3117", 99.0), 0);   // 3117 has 6 ECTS in the guide
        assertThat(r.totals().earned()).isEqualTo(6.0);
        assertThat(r.totals().userProvided()).isZero();
    }

    @Test
    void typedInEctsForACourseThatWasNotTickedAreIgnored() {
        assertThat(calcWith(List.of(), Map.of("8154", 6.0), 0).totals().earned()).isZero();
    }

    @Test
    void creditsOutsideTheListCountTowardTheTotalOnly() {
        CalculatorResult r = calcWith(List.of(), Map.of(), 12.5);              // e.g. practical training
        assertThat(r.totals().earned()).isEqualTo(12.5);
        assertThat(r.totals().userProvided()).isEqualTo(12.5);
        assertThat(rule(r, "level3-ects").current()).isZero();                 // not part of ΕΠ + Ε
    }

    /** Free electives count toward the 240 total but never toward the 64 ECTS from ΕΠ + Ε. */
    @Test
    void freeElectivesFillTheTotalButNotTheLevelThreeRule() {
        List<String> codes = new ArrayList<>(codesOfLevel("CORE_MANDATORY"));  // 152
        codes.addAll(codesOfLevel("CORE_ELECTIVE"));                           // + 70 = 222
        codes.addAll(List.of("3612", "3814"));                                 // + 12 = 234
        codes.add("8154");
        CalculatorResult r = calcWith(codes, Map.of("8154", 6.0), 0);          // + 6 typed = 240
        assertThat(r.totals().earned()).isEqualTo(240.0);
        assertThat(rule(r, "total-ects").satisfied()).isTrue();
        assertThat(rule(r, "level3-ects").current()).isEqualTo(82.0);          // 70 + 12, the free elective is not in it
        assertThat(r.allCalculableRulesMet()).isTrue();

        // Thirty typed ECTS on a free elective must not satisfy the 64-ECTS rule by itself.
        CalculatorResult only = calcWith(List.of("8154"), Map.of("8154", 30.0), 0);
        assertThat(rule(only, "level3-ects").satisfied()).isFalse();
        assertThat(rule(only, "level3-ects").current()).isZero();
    }

    // ---------------------------------------------------------------- required-course rule kind

    @Test
    void mandatoryRuleListsExactlyTheMissingCourses() {
        CalculatorResult r = calc("3117");
        assertThat(rule(r, "level1-all-mandatory").missing()).hasSize(21)
                .noneMatch(m -> m.code().equals("3117"));
        assertThat(rule(r, "level1-all-mandatory").current()).isEqualTo(1.0);
        assertThat(rule(r, "level1-all-mandatory").required()).isEqualTo(22.0);
    }
}
