package app.deptinfo.calculator;

import java.util.List;
import java.util.Map;

/**
 * Body of the calculator call.
 *
 * @param passedCourses the codes of the courses the user says they have passed
 * @param ectsOverrides ECTS typed in by the user for passed courses whose ECTS the guide does not give (free
 *                      electives of other departments). Ignored for any course that has ECTS in the data: the guide wins.
 * @param otherEcts     ECTS the user has that match no course in the list (e.g. practical training); added to the total only
 */
public record CalculatorRequest(List<String> passedCourses, Map<String, Double> ectsOverrides, Double otherEcts) {
}
