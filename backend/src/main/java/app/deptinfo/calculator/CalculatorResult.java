package app.deptinfo.calculator;

import app.deptinfo.courses.CourseRef;
import app.deptinfo.tenant.I18n;

import java.util.List;

/**
 * Answer of the ECTS calculator. It is an estimate from the courses the user claims to have passed: grades,
 * exemptions and the rules in {@code manualChecks} are not evaluated.
 *
 * @param allCalculableRulesMet true when every rule in {@code rules} is satisfied; says nothing about manual checks
 */
public record CalculatorResult(
        Totals totals,
        List<LevelProgress> levels,
        List<RuleStatus> rules,
        CycleReport cycles,
        List<ManualCheck> manualChecks,
        List<String> unknownCodes,
        List<Warning> warnings,
        boolean allCalculableRulesMet) {

    /** {@code userProvided} is the part of {@code earned} that came from ECTS the user typed in, not from the guide. */
    public record Totals(double earned, double required, double remaining, double userProvided) {
    }

    public record LevelProgress(String level, int coursesPassed, double ectsEarned) {
    }

    /** One evaluated graduation rule. {@code missing} lists courses still needed, for rules that name specific courses. */
    public record RuleStatus(String id, String kind, I18n title, boolean satisfied, double current, double required,
                             double remaining, List<CourseRef> missing) {
    }

    /**
     * Cycle progress. {@code maxCompletable} is the largest number of cycles that can be completed at once when each
     * passed course may count for only one cycle; {@code completedInBestPlan} / {@code assignedCodes} describe one such plan.
     */
    public record CycleReport(int minCourses, int maxCompletable, List<CycleStatus> items) {
    }

    public record CycleStatus(String id, int number, I18n name, List<String> passedCodes, int missingToComplete,
                              boolean completedInBestPlan, List<String> assignedCodes) {
    }

    public record ManualCheck(String id, I18n title) {
    }

    /** Advisory messages: DUPLICATE_CODE, ECTS_UNKNOWN, PREREQUISITE_NOT_PASSED. */
    public record Warning(String code, String message, String courseCode) {
    }
}
