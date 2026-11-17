"""Check every JSON file in content/ and explain any problem in plain language.

Run automatically by GitHub Actions on every change. Exit code 0 = all good.
Notes (such as "no calendar for this year yet") are shown but never stop a change.
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
# Optional first argument: check a different content folder (used by the tests).
CONTENT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT / "content"
SCHEMAS = ROOT / "schema"

YEAR = r"\d{4}-\d{4}"
# Which schema checks which file (path relative to content/<university>/).
FILE_RULES = [
    (r"university/university\.json", "university"),
    (r"university/schools\.json", "schools"),
    (r"university/grading_scale\.json", "grading_scale"),
    (rf"university/calendar/(?P<year>{YEAR})\.json", "calendar"),
    # These general files may have an older version in general/YYYY-YYYY/ (the plain one is the current version).
    (rf"[^/]+/general/(?:{YEAR}/)?department\.json", "department"),
    (rf"[^/]+/general/(?:{YEAR}/)?administration\.json", "administration"),
    (rf"[^/]+/general/(?:{YEAR}/)?sectors\.json", "sectors"),
    (r"[^/]+/general/faculty\.json", "faculty"),
    (rf"[^/]+/general/(?:{YEAR}/)?international_partners\.json", "international_partners"),
    (rf"[^/]+/general/(?:{YEAR}/)?free_electives\.json", "free_electives"),
    (rf"[^/]+/general/(?:{YEAR}/)?postgraduate_programs\.json", "postgraduate_programs"),
    (rf"[^/]+/curriculum/(?P<year>{YEAR})\.json", "curriculum"),  # undergraduate
    (rf"[^/]+/curriculum/(?P<program>[^/]+)/(?P<year>{YEAR})\.json", "curriculum"),  # one folder per postgraduate programme
]
CURRICULUM_PATH = re.compile(rf"(?P<dept>[^/]+)/curriculum/(?:(?P<program>[^/]+)/)?(?P<year>{YEAR})\.json")

problems = []
notes = []


def problem(file, message, hint=None):
    problems.append((file, message, hint))


def note(message):
    notes.append(message)


def load_registry():
    resources, schemas = [], {}
    for p in SCHEMAS.glob("*.schema.json"):
        s = json.loads(p.read_text(encoding="utf-8"))
        resources.append((s["$id"], Resource.from_contents(s, default_specification=DRAFT202012)))
        schemas[p.name.replace(".schema.json", "")] = s
    return Registry().with_resources(resources), schemas


def where(error):
    """Describe where in the file the problem is, in words."""
    parts = []
    cur = getattr(error, "root_instance", None)
    for step in error.absolute_path:
        if isinstance(step, int):
            label = f"item number {step + 1}"
            if isinstance(cur, list) and step < len(cur) and isinstance(cur[step], dict):
                ident = cur[step].get("code") or cur[step].get("id") or cur[step].get("course")
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
    if "propertyNames" in error.schema_path:
        return f'"{inst}" is not a supported language. Use "en" for English or "el" for Greek.', None
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
    if v == "pattern":
        return f'"{inst}" is not written in the expected form.', desc
    if v == "type":
        wanted = {"string": "text in quotes", "number": "a number", "integer": "a whole number",
                  "array": "a list in [square brackets]", "object": "a group in {curly brackets}",
                  "boolean": "true or false"}.get(error.validator_value, str(error.validator_value))
        return f"This should be {wanted}.", desc
    if v in ("minLength", "minItems", "minProperties"):
        return "This is empty. Fill it in.", desc
    if v == "maxItems":
        return f"There are too many items here. At most {error.validator_value} are allowed.", desc
    if v == "uniqueItems":
        return "The same value appears twice in this list. Remove the duplicate.", None
    if v in ("minimum", "maximum"):
        return f"{inst} is out of range ({v} is {error.validator_value}).", desc
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
    # Always print UTF-8, so Greek and other non-Latin text shows correctly on Windows too.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    if not CONTENT.is_dir():
        sys.exit("There is no content folder.")
    registry, schemas = load_registry()
    data = {}  # (university, relative path) -> parsed JSON
    checked = 0
    broken = set()  # files with problems; references to them are not checked

    for file in sorted(CONTENT.rglob("*.json")):
        rel = file.relative_to(CONTENT).as_posix()
        parts = rel.split("/")
        if rel == "index.json":
            uni, inner, match = "", "index.json", (re.fullmatch("index.json", rel), "index")
        elif len(parts) < 2:
            problem(rel, "Files must live inside a university folder, e.g. content/my-university/university/university.json.")
            continue
        else:
            uni, inner = parts[0], "/".join(parts[1:])
            match = next(((re.fullmatch(p, inner), s) for p, s in FILE_RULES if re.fullmatch(p, inner)), None)
        if not match:
            problem(rel, "This file is in a place or has a name we do not recognise, so it was ignored.",
                    "Known files: index.json at the top; per university: university.json, schools.json, grading_scale.json, "
                    "calendar/YYYY-YYYY.json; per department: general/{department,administration,sectors,faculty,"
                    "international_partners,free_electives,postgraduate_programs}.json and curriculum/YYYY-YYYY.json "
                    "(undergraduate) or curriculum/<programme-id>/YYYY-YYYY.json (postgraduate)")
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
        file_year = m.groupdict().get("year")
        if file_year and isinstance(instance, dict) and instance.get("academic_year") not in (None, file_year):
            problem(rel, f'The file name says {file_year} but "academic_year" inside says {instance["academic_year"]}. They must match.')

    # ---- Look-up helpers for references between files -------------------------------------
    curricula = {}  # (university, department) -> list of curricula
    for (uni, inner), d in data.items():
        cm = CURRICULUM_PATH.fullmatch(inner)
        if cm and isinstance(d, dict):
            curricula.setdefault((uni, cm["dept"]), []).append({
                "program": cm["program"] or "undergraduate", "year": cm["year"],
                "codes": ids_of(d.get("courses", []), "code"), "key": (uni, inner)})

    def find_person(uni, dept, ref):
        """Returns (status, department). status: ok, missing, nodept, nofaculty."""
        tdept, pid = ref.split(":", 1) if ":" in ref else (dept, ref)
        if not (CONTENT / uni / tdept).is_dir():
            return "nodept", tdept
        key = (uni, f"{tdept}/general/faculty.json")
        if key in broken:
            return "ok", tdept  # that file has its own problem, reported there
        f = data.get(key)
        if f is None:
            return "nofaculty", tdept
        return ("ok" if pid in ids_of(f.get("members", [])) else "missing"), tdept

    def person_problem(rel, uni, dept, who, ref):
        status, tdept = find_person(uni, dept, ref)
        local = ":" not in ref
        if status == "missing":
            where_ = "faculty.json" if local else f'the faculty.json of the department "{tdept}"'
            problem(rel, f'{who} "{ref}", who is not in {where_}.')
        elif status == "nodept":
            problem(rel, f'{who} "{ref}", but there is no department folder "{tdept}".')
        elif status == "nofaculty":
            problem(rel, f'{who} "{ref}", but the department "{tdept}" has no faculty.json.')

    def find_course(uni, dept, year, ref, local_codes):
        """Returns (status, detail). year=None means any year. status: ok, missing, nodept, nocurriculum, ambiguous."""
        if ":" not in ref:
            return ("ok", None) if ref in local_codes else ("missing", None)
        prefix, code = ref.split(":", 1)
        tdept, _, prog = prefix.partition("/")
        if not (CONTENT / uni / tdept).is_dir():
            return "nodept", tdept
        cands = [c for c in curricula.get((uni, tdept), []) if (year is None or c["year"] == year) and (not prog or c["program"] == prog)]
        if any(c["key"] in broken for c in cands):
            return "ok", None
        if not cands:
            return "nocurriculum", tdept
        hits = sorted({c["program"] for c in cands if code in c["codes"]})
        if not hits:
            return "missing", tdept
        if len(hits) > 1 and not prog:
            return "ambiguous", (tdept, hits, code)
        return "ok", None

    def course_ref_problem(rel, uni, dept, year, who, ref, local_codes, here_programme):
        status, detail = find_course(uni, dept, year, ref, local_codes)
        if status == "ok":
            return
        if status == "missing" and ":" not in ref:
            hint = ""
            others = [c["program"] for c in curricula.get((uni, dept), []) if c["year"] == year and ref in c["codes"] and c["program"] != here_programme]
            if others:
                hint = f' A course of another programme of this department is written like "{dept}/{others[0]}:{ref}".'
            problem(rel, f'{who} "{ref}", but no course with that code exists in this file.{hint}')
        elif status == "missing":
            problem(rel, f'{who} "{ref}", but no course with that code exists there.')
        elif status == "nodept":
            problem(rel, f'{who} "{ref}", but there is no department folder "{detail}".')
        elif status == "nocurriculum":
            when = f" for {year}" if year else ""
            problem(rel, f'{who} "{ref}", but the department "{detail}" has no curriculum{when}.')
        elif status == "ambiguous":
            tdept, hits, code = detail
            problem(rel, f'{who} "{ref}", but the code "{code}" exists in several programmes of "{tdept}": {", ".join(hits)}. Say which one, for example "{tdept}/{hits[0]}:{code}".')

    # ---- Cross-checks: things that refer to other things ---------------------------------
    for (uni, inner), d in data.items():
        rel = f"{uni}/{inner}" if uni else inner
        dept = inner.split("/")[0]
        if not isinstance(d, dict):
            continue
        if inner == "index.json":
            for u in d.get("universities", []):
                if not (CONTENT / u).is_dir():
                    problem(rel, f'The university "{u}" is listed here, but there is no folder content/{u}.')
            for folder in sorted(p.name for p in CONTENT.iterdir() if p.is_dir()):
                if folder not in d.get("universities", []):
                    note(f'The university folder "{folder}" is not listed in index.json, so it will not be shown.')
        if inner == "university/university.json":
            langs = d.get("languages")
            if langs and d.get("default_language") and d["default_language"] not in langs:
                problem(rel, f'"default_language" is "{d["default_language"]}", but "languages" only lists {", ".join(langs)}. Add it to the list or change it.')
            listed = d.get("departments", [])
            for dp in listed:
                if not (CONTENT / uni / dp / "general" / "department.json").exists():
                    problem(rel, f'The department "{dp}" is listed here, but there is no folder {uni}/{dp} with a general/department.json.')
            for folder in sorted(p.name for p in (CONTENT / uni).iterdir() if p.is_dir() and p.name != "university"):
                if folder not in listed:
                    note(f'The department folder "{uni}/{folder}" is not listed in university.json, so it will not be shown.')
        if inner.endswith("schools.json"):
            dupes(rel, d.get("schools", []), "id", "school id")
        if inner.endswith("international_partners.json"):
            dupes(rel, d.get("partners", []), "id", "partner id")
        if inner.endswith("department.json"):
            sch_key = (uni, "university/schools.json")
            sch = data.get(sch_key)
            if d.get("school") and sch_key not in broken:
                if sch is None:
                    problem(rel, f'The department is in the school "{d["school"]}", but there is no university/schools.json. Create it and list the schools there.')
                elif d["school"] not in ids_of(sch.get("schools", [])):
                    problem(rel, f'The school "{d["school"]}" does not exist in schools.json. Add it there or fix the id.')
        if "/calendar/" in inner:
            dupes(rel, d.get("terms", []), "id", "term id")
            date_order(rel, d.get("terms", []), "Term")
            semesters = sorted(t["semester"] for t in d.get("terms", []) if isinstance(t, dict) and t.get("semester") in (1, 2))
            if len(semesters) == 2 and semesters != [1, 2]:
                problem(rel, 'The two terms must be semester 1 (winter) and semester 2 (spring), one of each.')
            windows = {1: ("winter", {10, 11, 12, 1}, "October to January"), 2: ("spring", set(range(2, 7)), "February to June")}
            for t in d.get("terms", []):
                if not isinstance(t, dict) or t.get("semester") not in windows:
                    continue
                label, months, span = windows[t["semester"]]
                for key in ("start", "end"):
                    value = t.get(key)
                    if isinstance(value, str) and len(value) == 10 and value[5:7].isdigit() and int(value[5:7]) not in months:
                        problem(rel, f'Semester {t["semester"]} is the {label} semester and must run {span}, but its {key} date is {value}.')
            date_order(rel, d.get("exam_periods", []), "Exam period")
            date_order(rel, d.get("holidays", []), "Holiday")
            terms = ids_of(d.get("terms", []))
            for ep in d.get("exam_periods", []):
                if ep.get("term") and ep["term"] not in terms:
                    problem(rel, f'Exam period "{ep.get("id")}" refers to the term "{ep["term"]}", which is not in the terms list.')
        if inner.endswith("general/faculty.json"):
            dupes(rel, d.get("members", []), "id", "person id")
            sec_key = (uni, f"{dept}/general/sectors.json")
            sec_f = data.get(sec_key)
            secs = ids_of(sec_f.get("sectors", [])) if sec_f else None
            for mbr in d.get("members", []):
                if secs is None and sec_key not in broken and mbr.get("sector"):
                    problem(rel, f'{mbr.get("id")} is in the sector "{mbr["sector"]}", but this department has no sectors.json. Create it and list the sectors there.')
                who = mbr.get("id")
                if mbr.get("first_year") and mbr.get("last_year") and mbr["last_year"] < mbr["first_year"]:
                    problem(rel, f'{who}: "last_year" ({mbr["last_year"]}) is earlier than "first_year" ({mbr["first_year"]}).')
                if mbr.get("other_title") and mbr.get("rank") != "other":
                    problem(rel, f'{who} has an "other_title", but the rank is not "other". Use "other_title" only with the rank "other".')
                if secs is not None and mbr.get("sector") and mbr["sector"] not in secs:
                    problem(rel, f'{who} is in the sector "{mbr["sector"]}", which is not in sectors.json.')
        if inner.endswith("/sectors.json"):
            dupes(rel, d.get("sectors", []), "id", "sector id")
        if inner.endswith("/administration.json"):
            dupes(rel, d.get("roles", []), "id", "role id")
            for r in d.get("roles", []):
                rid = r.get("id")
                if r.get("faculty_id") and r.get("person"):
                    problem(rel, f'The role "{rid}" has both "faculty_id" and "person". Use only one: faculty_id for people in a faculty list, person for everyone else.')
                elif not r.get("faculty_id") and not r.get("person"):
                    problem(rel, f'The role "{rid}" needs either "faculty_id" or "person".')
                if r.get("faculty_id"):
                    person_problem(rel, uni, dept, f'The role "{rid}" is linked to', r["faculty_id"])
        if inner.endswith("postgraduate_programs.json"):
            dupes(rel, d.get("programs", []), "id", "program id")
            for p in d.get("programs", []):
                if p.get("director"):
                    person_problem(rel, uni, dept, f'The director of "{p.get("id")}" is', p["director"])
        if inner.endswith("free_electives.json"):
            for n, item in enumerate(d.get("courses", []), start=1):
                own = [k for k in ("code", "name", "ects", "offered_by") if k in item]
                label = item.get("course") or item.get("code") or f"number {n}"
                if "course" in item and own:
                    problem(rel, f'Free elective {label} uses "course" together with {", ".join(own)}. Use either "course" alone, or "code", "name" and "ects" together.')
                elif "course" not in item and not all(k in item for k in ("code", "name", "ects")):
                    problem(rel, f'Free elective {label} must have either "course", or all of "code", "name" and "ects".')
                if item.get("course"):
                    course_ref_problem(rel, uni, dept, None, f"Free elective {label} points at", item["course"], set(), None)
        if CURRICULUM_PATH.fullmatch(inner):
            cm = CURRICULUM_PATH.fullmatch(inner)
            folder = cm["program"]
            year = d.get("academic_year") or cm["year"]
            degree, program = d.get("degree"), d.get("program")
            if degree == "undergraduate":
                if program or folder:
                    problem(rel, 'An undergraduate curriculum belongs directly in the "curriculum" folder and must not name a "program".')
            elif degree == "postgraduate":
                if not program:
                    problem(rel, 'A postgraduate curriculum must name its programme in "program" (an id from postgraduate_programs.json).')
                else:
                    pgkey = (uni, f"{dept}/general/postgraduate_programs.json")
                    pg = data.get(pgkey)
                    if pgkey in broken:
                        pass
                    elif pg is None or program not in ids_of(pg.get("programs", [])):
                        known = ", ".join(sorted(ids_of(pg.get("programs", [])))) if pg else "none, there is no postgraduate_programs.json"
                        problem(rel, f'The programme "{program}" is not in postgraduate_programs.json. Known programmes: {known}.')
                    if folder != program:
                        problem(rel, f'A postgraduate curriculum must be saved in curriculum/{program}/ (the folder named like its programme id), but it is in {"curriculum/" + folder + "/" if folder else "curriculum/"}.')
            here = program if degree == "postgraduate" else "undergraduate"
            dupes(rel, (d.get("requirements") or {}).get("rules", []), "id", "rule id")
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
                sems = c.get("semesters") or []
                if c.get("available_from") and sems and c["available_from"] > min(sems):
                    problem(rel, f'Course {code}: "available_from" ({c["available_from"]}) is later than the first semester it is taught in ({min(sems)}). It should be the same or earlier.')
                for pre in c.get("prerequisites", []):
                    if pre == code:
                        problem(rel, f"Course {code} lists itself as a prerequisite.")
                    else:
                        course_ref_problem(rel, uni, dept, year, f"Course {code} needs course", pre, codes, here)
                for ins in c.get("instructors", []):
                    person_problem(rel, uni, dept, f"Course {code} is taught by", ins)
            if not (CONTENT / uni / "university" / "calendar" / f"{d.get('academic_year')}.json").exists() and d.get("academic_year"):
                note(f"There is no calendar file for {d['academic_year']} yet ({rel}).")

    for message in notes:
        print(f"Note: {message}")
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
