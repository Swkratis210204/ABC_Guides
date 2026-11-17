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
content/index.json   which universities to show, in order
content/<university>/
  university/        about the university, schools, calendar, grading scale
  <department>/
    general/         administration, sectors, faculty, free electives, international partners, programmes
    curriculum/      one file per programme per year, including its requirements
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

## Checks before a change is accepted

- **On GitHub:** `.github/workflows/checks.yml` runs all the tests and the content check on every push and every proposed change. A red result means the change must not be accepted.
- **On your computer:** `.githooks/pre-push` runs the same checks before every `git push` and cancels the push if anything fails. Turn it on once per copy of the repository:

  ```
  git config core.hooksPath .githooks
  ```

- **To stop broken changes from reaching `main` at all**, the repository owner turns on branch protection on GitHub (Settings, Branches, require the `tests` check to pass before merging). Changes then go in as proposals that are merged only when the check is green. This is roadmap step 3.

## Documentation

- [Roadmap](docs/roadmap.md): all planned steps and their status
- [Schema map](docs/schema-map.md): a diagram of the files and how they refer to each other
- [Scope](docs/scope.md): goals, rules and the content architecture
- [Work log](docs/work/step-1.md): what has been done so far

## Contributing

Content changes are validated automatically before they are accepted. A change that does not match the schema is not accepted. A plain-language editing guide is planned (roadmap step 4).
