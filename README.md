# Open University Department Information System

A free, open information site for university departments (courses, faculty, calendar, requirements) that **non-technical staff can maintain by editing JSON files only**.

> **Status: early development.** The data schema and its checker are in place. The website, publishing and search are not built yet. See the [roadmap](docs/roadmap.md).

## How it works

1. All content lives in JSON files in [`content/`](content/). They are the single source of truth.
2. Someone edits a file, either locally or in the GitHub web editor.
3. GitHub Actions checks the change against the schema and explains any mistake in plain language.
4. Once the check passes, the site is built and published automatically. *(planned)*

No backend, no database, no CMS. Everything is static files and GitHub, so the running cost is $0.

## Repository layout

```
content/<university>/
  university/        calendar, grading scale, schools, free electives
  <department>/
    general/         administration, sectors, faculty, programmes, degree requirements
    curriculum/      one file per academic year
schema/              JSON Schemas that define what valid content looks like
scripts/validate.py  checks content and reports problems in plain language
tests/               automatic tests for the checker and the sample content
docs/                project plan, scope and work log
```

`content/` currently holds **made-up sample data** (two universities, three departments, several years) used to prove the schema works.

## Check your content

Requires Python 3.10+.

```
pip install -r requirements.txt
python scripts/validate.py        # check all content
python -m pytest tests            # run the automated tests
```

A clean run prints `All good`. Otherwise each problem names the file, the place in the file and what to fix, for example:

```
PROBLEM in content/example-university/computer-science/curriculum/2026-2027.json
  In "courses" > item number 4 ("3050"): "semester" is not a field we know - did you mean "semesters"?
```

The same check runs on GitHub for every change (`.github/workflows/deploy.yml`).

## Documentation

- [Roadmap](docs/roadmap.md): all planned steps and their status
- [Schema map](docs/schema-map.md): a diagram of the files and how they refer to each other
- [Scope](docs/scope.md): goals, rules and the content architecture
- [Work log](docs/work/step-1.md): what has been done so far

## Contributing

Content changes are validated automatically before they are accepted. A change that does not match the schema is not accepted. A plain-language editing guide is planned (roadmap step 4).
