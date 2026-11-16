"""Check every JSON file in content/ and explain any problem in plain language.

Run automatically by GitHub Actions on every change. Exit code 0 = all good.
"""
import difflib
import json
import re
import sys
from pathlib import Path

from jsonschema import Draft202012Validator
from referencing import Registry, Resource
from referencing.jsonschema import DRAFT202012

ROOT = Path(__file__).resolve().parent.parent
CONTENT = ROOT / "content"
SCHEMAS = ROOT / "schema"

# Which schema checks which file (path relative to content/<university>/).
FILE_RULES = [
    (r"university/university\.json", "university"),
    (r"university/schools\.json", "schools"),
    (r"university/grading_scale\.json", "grading_scale"),
    (r"university/free_electives\.json", "free_electives"),
    (r"university/calendar/(\d{4}-\d{4})\.json", "calendar"),
    (r"[^/]+/general/department\.json", "department"),
    (r"[^/]+/general/administration\.json", "administration"),
    (r"[^/]+/general/sectors\.json", "sectors"),
    (r"[^/]+/general/faculty\.json", "faculty"),
    (r"[^/]+/general/postgraduate_programs\.json", "postgraduate_programs"),
    (r"[^/]+/general/degree_requirements\.json", "degree_requirements"),
    (r"[^/]+/curriculum/(\d{4}-\d{4})\.json", "curriculum"),
]

problems = []


def problem(file, message, hint=None):
    problems.append((file, message, hint))


def load_registry():
    resources, schemas = [], {}
    for p in SCHEMAS.glob("*.schema.json"):
        s = json.loads(p.read_text(encoding="utf-8"))
        resources.append((s["$id"], Resource.from_contents(s, default_specification=DRAFT202012)))
        schemas[p.name.replace(".schema.json", "")] = s
    return Registry().with_resources(resources), schemas


def where(error):
    """Describe where in the file the problem is, in words."""
    parts, node = [], error.instance
    path = list(error.absolute_path)
    root = error.root_instance if hasattr(error, "root_instance") else None
    cur = root
    for step in path:
        if isinstance(step, int):
            label = f"item number {step + 1}"
            if isinstance(cur, list) and step < len(cur) and isinstance(cur[step], dict):
                ident = cur[step].get("code") or cur[step].get("id")
                if ident:
                    label += f' ("{ident}")'
            parts.append(label)
            cur = cur[step] if isinstance(cur, list) and step < len(cur) else None
        else:
            parts.append(f'"{step}"')
            cur = cur.get(step) if isinstance(cur, dict) else None
    return " > ".join(parts) if parts else "the top of the file"


def friendly(error):
    """Turn a technical schema error into a sentence a non-technical person can act on."""
    v, inst = error.validator, error.instance
    desc = error.schema.get("description") if isinstance(error.schema, dict) else None
    if v == "required":
        missing = re.findall(r"'([^']+)'", error.message)[0]
        return f'The field "{missing}" is missing. Add it.', None
    if v == "additionalProperties":
        extra = re.findall(r"'([^']+)'", error.message)
        known = list(error.schema.get("properties", {}).keys()) if isinstance(error.schema, dict) else []
        out = []
        for name in extra:
            close = difflib.get_close_matches(name, known, n=1)
            out.append(f'"{name}" is not a field we know' + (f' - did you mean "{close[0]}"?' if close else ". Check the spelling or remove it."))
        return " ".join(out), None
    if v == "enum":
        options = ", ".join(f'"{o}"' for o in error.validator_value)
        return f'"{inst}" is not an allowed value. Choose one of: {options}.', None
    if v == "pattern" and "propertyNames" in error.schema_path:
        return f'"{inst}" is not a language code. Use two small letters, like en or el.', None
    if v == "pattern":
        return f'"{inst}" is not written in the expected form.', desc
    if v == "type":
        wanted = {"string": "text in quotes", "number": "a number", "integer": "a whole number",
                  "array": "a list in [square brackets]", "object": "a group in {curly brackets}",
                  "boolean": "true or false"}.get(error.validator_value, str(error.validator_value))
        return f"This should be {wanted}.", desc
    if v in ("minLength", "minItems", "minProperties"):
        return "This is empty. Fill it in.", desc
    if v == "uniqueItems":
        return "The same value appears twice in this list. Remove the duplicate.", None
    if v in ("minimum", "maximum"):
        return f"{inst} is out of range ({v} is {error.validator_value}).", desc
    if v == "propertyNames":
        bad = re.findall(r"'([^']+)'", error.message)
        name = bad[0] if bad else "?"
        return f'"{name}" is not a language code. Use two small letters, like en or el.', None
    return error.message, desc


def check_schema(path, instance, validator):
    errors = sorted(validator.iter_errors(instance), key=lambda e: list(map(str, e.absolute_path)))
    for e in errors:
        e.root_instance = instance
        msg, hint = friendly(e)
        problem(path, f"In {where(e)}: {msg}", hint)


def dupes(path, items, key, label):
    seen = set()
    for it in items:
        k = it.get(key) if isinstance(it, dict) else None
        if k is None:
            continue
        if k in seen:
            problem(path, f'The {label} "{k}" appears more than once. Each one must be different.')
        seen.add(k)


def ids_of(items, key="id"):
    return {i.get(key) for i in items if isinstance(i, dict) and i.get(key) is not None}


def date_order(path, items, label):
    for it in items:
        if isinstance(it, dict) and it.get("start") and it.get("end") and it["end"] < it["start"]:
            problem(path, f'{label} "{it.get("id")}" ends ({it["end"]}) before it starts ({it["start"]}).')


def main():
    if not CONTENT.is_dir():
        sys.exit("There is no content folder.")
    registry, schemas = load_registry()
    data = {}  # (university, relative path) -> parsed JSON
    checked = 0
    broken = set()  # files with problems; references to them are not checked

    for file in sorted(CONTENT.rglob("*.json")):
        rel = file.relative_to(CONTENT).as_posix()
        parts = rel.split("/")
        if len(parts) < 2:
            problem(rel, "Files must live inside a university folder, e.g. content/my-university/university/university.json.")
            continue
        uni, inner = parts[0], "/".join(parts[1:])
        match = next(((re.fullmatch(p, inner), s) for p, s in FILE_RULES if re.fullmatch(p, inner)), None)
        if not match:
            problem(rel, "This file is in a place or has a name we do not recognise, so it was ignored.",
                    "Known files: university.json, schools.json, grading_scale.json, free_electives.json, calendar/YYYY-YYYY.json, "
                    "and per department: general/{department,administration,sectors,faculty,postgraduate_programs,degree_requirements}.json "
                    "and curriculum/YYYY-YYYY.json")
            continue
        m, kind = match
        try:
            instance = json.loads(file.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            broken.add((uni, inner))
            problem(rel, f"The file is not valid JSON. Problem near line {e.lineno}, column {e.colno}: {e.msg}.",
                    "Usual causes: a missing or extra comma, a missing quote, or a bracket that was not closed.")
            continue
        checked += 1
        before = len(problems)
        check_schema(rel, instance, Draft202012Validator(schemas[kind], registry=registry))
        data[(uni, inner)] = instance
        if len(problems) > before:
            broken.add((uni, inner))
        if m.groups() and isinstance(instance, dict) and instance.get("academic_year") not in (None, m.group(1)):
            problem(rel, f'The file name says {m.group(1)} but "academic_year" inside says {instance["academic_year"]}. They must match.')

    # Cross-checks: things that refer to other things
    for (uni, inner), d in data.items():
        rel = f"{uni}/{inner}"
        dept = inner.split("/")[0]
        if not isinstance(d, dict):
            continue
        if inner.endswith("schools.json"):
            dupes(rel, d.get("schools", []), "id", "school id")
        if inner.endswith("department.json"):
            sch = data.get((uni, "university/schools.json"))
            if d.get("school") and sch and d["school"] not in ids_of(sch.get("schools", [])):
                problem(rel, f'The school "{d["school"]}" does not exist in schools.json. Add it there or fix the id.')
        if inner.endswith("calendar") or "/calendar/" in inner:
            dupes(rel, d.get("terms", []), "id", "term id")
            date_order(rel, d.get("terms", []), "Term")
            date_order(rel, d.get("exam_periods", []), "Exam period")
            date_order(rel, d.get("holidays", []), "Holiday")
            terms = ids_of(d.get("terms", []))
            for ep in d.get("exam_periods", []):
                if ep.get("term") and ep["term"] not in terms:
                    problem(rel, f'Exam period "{ep.get("id")}" refers to the term "{ep["term"]}", which is not in the terms list.')
        fac_f = data.get((uni, f"{dept}/general/faculty.json"), {})
        fac_ok = (uni, f"{dept}/general/faculty.json") not in broken
        fac_ids = ids_of(fac_f.get("members", [])) if isinstance(fac_f, dict) else set()
        if inner.endswith("general/faculty.json"):
            dupes(rel, d.get("members", []), "id", "person id")
            sec_f = data.get((uni, f"{dept}/general/sectors.json"))
            if sec_f:
                secs = ids_of(sec_f.get("sectors", []))
                for mbr in d.get("members", []):
                    if mbr.get("sector") and mbr["sector"] not in secs:
                        problem(rel, f'{mbr.get("name", mbr.get("id"))} is in the sector "{mbr["sector"]}", which is not in sectors.json.')
        if inner.endswith("general/sectors.json"):
            dupes(rel, d.get("sectors", []), "id", "sector id")
        if inner.endswith("general/administration.json"):
            dupes(rel, d.get("roles", []), "id", "role id")
            for r in d.get("roles", []):
                if fac_ok and r.get("faculty_id") and r["faculty_id"] not in fac_ids:
                    problem(rel, f'{r.get("person")} is linked to "{r["faculty_id"]}", who is not in faculty.json.')
        if inner.endswith("postgraduate_programs.json"):
            dupes(rel, d.get("programs", []), "id", "program id")
            for p in d.get("programs", []):
                if fac_ok and p.get("director") and p["director"] not in fac_ids:
                    problem(rel, f'The director of "{p.get("id")}" is "{p["director"]}", who is not in faculty.json.')
        if inner.endswith("degree_requirements.json"):
            dupes(rel, d.get("rules", []), "id", "rule id")
        if "/curriculum/" in f"/{inner}":
            levels, cycles = ids_of(d.get("levels", [])), ids_of(d.get("cycles", []))
            dupes(rel, d.get("levels", []), "id", "level id")
            dupes(rel, d.get("cycles", []), "id", "cycle id")
            dupes(rel, d.get("courses", []), "code", "course code")
            codes = ids_of(d.get("courses", []), "code")
            for c in d.get("courses", []):
                code = c.get("code")
                if c.get("level") and c["level"] not in levels:
                    problem(rel, f'Course {code} has the level "{c["level"]}", which is not in the levels list. Known levels: {", ".join(sorted(levels))}.')
                for cy in c.get("cycles", []):
                    if cy not in cycles:
                        problem(rel, f'Course {code} is in the cycle "{cy}", which is not in the cycles list.')
                for pre in c.get("prerequisites", []):
                    if pre == code:
                        problem(rel, f"Course {code} lists itself as a prerequisite.")
                    elif pre not in codes:
                        problem(rel, f'Course {code} needs course "{pre}" first, but no course with that code exists in this file.')
                for ins in c.get("instructors", []):
                    if fac_ok and ins not in fac_ids:
                        problem(rel, f'Course {code} is taught by "{ins}", who is not in faculty.json.')
            cal = CONTENT / uni / "university" / "calendar" / f"{d.get('academic_year')}.json"
            if d.get("academic_year") and not cal.exists():
                print(f"Note: there is no calendar file for {d['academic_year']} yet ({rel}).")

    for file, message, hint in problems:
        print(f"\nPROBLEM in content/{file}\n  {message}")
        if hint:
            print(f"  Help: {hint}")
    if problems:
        print(f"\n{len(problems)} problem(s) found. Fix them and try again. The site was NOT updated.")
        sys.exit(1)
    print(f"All good: {checked} files checked, no problems.")


if __name__ == "__main__":
    main()
