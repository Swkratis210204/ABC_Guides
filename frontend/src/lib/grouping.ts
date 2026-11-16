import { t } from "./labels";
import type { CourseSummary, DepartmentInfo, LevelInfo, Requirements, Rule } from "./types";

export interface CourseGroup {
  key: string;
  title: string;
  level?: LevelInfo;
  courses: CourseSummary[];
}

/** One group per level (ΥΠ, ΕΠ, Ε, ΕΛ) in the department's own order; empty levels are left out. */
export function groupByLevel(courses: CourseSummary[], info: DepartmentInfo): CourseGroup[] {
  return [...info.levels]
    .sort((a, b) => a.number - b.number)
    .map((level) => ({ key: level.id, title: t(level.name), level, courses: courses.filter((c) => c.level === level.id) }))
    .filter((g) => g.courses.length > 0);
}

/**
 * One group per semester. A course offered in two semesters (e.g. Ζ΄/Η΄) is listed once, under the first,
 * so totals are not counted twice. Courses with no semester (free electives) form a last group.
 */
export function groupBySemester(courses: CourseSummary[], info: DepartmentInfo): CourseGroup[] {
  const label = new Map(info.semesters.map((s) => [s.number, s.label]));
  const by = new Map<number, CourseSummary[]>();
  const none: CourseSummary[] = [];
  for (const c of courses) {
    if (c.semesters.length === 0) none.push(c);
    else {
      const first = Math.min(...c.semesters);
      by.set(first, [...(by.get(first) ?? []), c]);
    }
  }
  const groups: CourseGroup[] = [...by.entries()]
    .sort(([a], [b]) => a - b)
    .map(([n, list]) => ({ key: `s${n}`, title: `${label.get(n) ?? n} εξάμηνο`, courses: list }));
  if (none.length) groups.push({ key: "none", title: "Χωρίς συγκεκριμένο εξάμηνο", courses: none });
  return groups;
}

/** Totals for a group: course count, summed ECTS, and how many courses have no known ECTS. */
export function totals(courses: CourseSummary[]) {
  const known = courses.filter((c) => c.ects !== undefined);
  return { count: courses.length, ects: known.reduce((n, c) => n + (c.ects ?? 0), 0), unknownEcts: courses.length - known.length };
}

/**
 * Graduation rules that apply to a level, so a level page can say what the department requires of it: rules that
 * name the level directly, plus the overall ECTS total on the highest level (the guide calls it "Level 4 (total)").
 */
export function rulesForLevel(requirements: Requirements, levelId: string, info: DepartmentInfo): Rule[] {
  const lastLevel = info.levels.reduce((a, b) => (b.number > a.number ? b : a), info.levels[0]);
  return requirements.rules.filter((r) => {
    if (r.params.level === levelId) return true;
    const levels = r.params.levels;
    if (Array.isArray(levels) && levels.includes(levelId)) return true;
    return r.kind === "TOTAL_ECTS" && lastLevel?.id === levelId;
  });
}

export function rulesOfKind(requirements: Requirements, kind: string): Rule[] {
  return requirements.rules.filter((r) => r.kind === kind);
}
