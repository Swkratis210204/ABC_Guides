// Browser-side API calls. They go to the same origin (/api/...), where the proxy route forwards them to the backend.
import type { CalculatorResult } from "./types";

/**
 * @param ectsOverrides ECTS the user typed for passed courses that have none in the guide (course code -> ECTS)
 * @param otherEcts     ECTS that belong to no listed course
 */
export async function calculateEcts(
  uni: string,
  dept: string,
  passedCourses: string[],
  ectsOverrides: Record<string, number> = {},
  otherEcts = 0,
): Promise<CalculatorResult> {
  const res = await fetch(`/api/universities/${encodeURIComponent(uni)}/departments/${encodeURIComponent(dept)}/ects-calculator`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ passedCourses, ectsOverrides, otherEcts }),
  });
  if (!res.ok) {
    let message = "Ο υπολογισμός δεν ήταν δυνατός. Δοκιμάστε ξανά.";
    try {
      const body = await res.json();
      if (body.error?.message) message = body.error.message;
    } catch {
      /* keep default */
    }
    throw new Error(message);
  }
  return (await res.json()) as CalculatorResult;
}
