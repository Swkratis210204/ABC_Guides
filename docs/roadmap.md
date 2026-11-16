# Roadmap

Goal: a university department information system that **non-technical people can maintain** by editing JSON files only. See [step-1.md](step-1.md) for the full goal and what is finished.

Status: ✅ done, 🔄 in progress, ⬜ not started.

| # | Step | Status |
|---|---|---|
| 1 | Design the first JSON schema, checker and tests | ✅ |
| 2 | Finish the schema and freeze version 1 | ⬜ |
| 3 | Refuse changes that don't match the schema | ⬜ |
| 4 | Instructions for people editing JSON | ⬜ |
| 5 | Put in real data | ⬜ |
| 6 | Build the website (MkDocs) | ⬜ |
| 7 | Publish the website (GitHub Pages) | ⬜ |
| 8 | Add search | ⬜ |
| 9 | README and documentation | ⬜ |
| 10 | Try it with a real non-technical person | ⬜ |

---

### 1. First schema, checker and tests ✅
Schema for the three tiers, sample data, a checker with plain-language messages, automatic tests. Details in [step-1.md](step-1.md).

### 2. Finish the schema and freeze version 1
Fix the known gaps, then stop changing the shape.
- Open lists for ranks and degree types, not fixed English words.
- Several programmes or degrees per department.
- Courses and teachers shared across departments.
- Credits and grades that are not only ECTS and numbers.
- Free electives per department. Decide whether degree requirements stay plain text.
- Add a list of which universities and departments exist.

Done when: sample data covers all of it and the tests pass.

### 3. Refuse changes that don't match the schema
Nothing goes live unless the check passes.
- Decide how people submit changes: straight to the main branch, or as a proposal that is merged only when the check is green. (Recommended: proposal with a required check. GitHub's web editor creates it with one click.)
- Turn on the GitHub rule that blocks merging when the check fails.
- The error message must say in plain words what to fix and where.

Done when: a deliberately wrong change is blocked on GitHub and the message is understandable.

### 4. Instructions for people editing JSON
A short guide, no jargon, for someone who has never used Git.
- How to edit a file in the browser and save the change.
- How to check a change **before** saving, without a terminal. Ideas: the editor underlines mistakes using the schema, or the GitHub check runs on every change.
- A list of common mistakes and what the messages mean.
- Copy-and-fill templates for the most common edits (add a course, add a teacher, add a new year).

Done when: someone follows it without help.

### 5. Put in real data
Load one real department (AUEB Computer Science) in the new format. Whatever does not fit goes back to step 2. Include previous years if available.

### 6. Build the website (MkDocs)
- Generate the pages automatically from the JSON: calendar, courses by year, faculty, requirements, programmes.
- No manual step. The pages are rebuilt on every change.
- Greek and English where the data has both.

Done when: the real data shows as a readable site on one machine.

### 7. Publish the website
GitHub builds and publishes it automatically to GitHub Pages after every approved change.

Done when: a change in the JSON appears on the live site within minutes with no one else involved.

### 8. Add search
Start with MkDocs' built-in search. Apply for Algolia DocSearch once the site is public.

### 9. README and documentation
- **README:** what the project is, how it works, where to start.
- **Documentation:** how the folders and files are organised, what each field means, how to add a university or department.
- **For maintainers:** how the checks and the build work, how to change the schema safely.

### 10. Try it with a real non-technical person
Watch them make a change without help. Everything they stumble on goes back into steps 3, 4 or 2.

---

## Decisions still open
- Docusaurus or MkDocs? (Recommended: MkDocs, one toolchain with the checker.)
- Direct commits or proposals with a required check? (Step 3.)
- Do degree requirements stay plain text or become rules a program can check? (Step 2.)
