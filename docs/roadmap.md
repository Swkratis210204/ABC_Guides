# Roadmap

Goal: a university department information system that **non-technical people can maintain** by editing JSON files only. See [step-1.md](work/step-1.md) for the full goal and what is finished.

Status: ✅ done, 🔄 in progress, ⬜ not started.

| # | Step | Status |
|---|---|---|
| 1 | Design the first JSON schema, checker and tests | ✅ |
| 2 | Finish the schema and freeze version 1 | ✅ |
| 3 | Refuse changes that don't match the schema | ✅ |
| 4 | Instructions for people editing JSON | ⬜ |
| 5 | Put in real data | ⬜ |
| 6 | Build the website (MkDocs) | ⬜ |
| 7 | Publish the website (GitHub Pages) | ⬜ |
| 8 | Add search | ⬜ |
| 9 | Full documentation (README first draft is written) | 🔄 |
| 10 | Try it with a real non-technical person | ⬜ |

---

### 1. First schema, checker and tests ✅
Schema for the three tiers, sample data, a checker with plain-language messages, automatic tests. Details in [step-1.md](work/step-1.md).

### 2. Finish the schema and freeze version 1 ✅
Every gap was reviewed one by one. The decisions are in [work/step-2-decisions.md](work/step-2-decisions.md).
- Ranks and degree types: the fixed lists stay, with an optional own title for rank `other`. Labels for the website come in step 6.
- Several programmes per department: done. Each curriculum says if it is undergraduate or postgraduate and carries its own requirements.
- Courses and teachers shared across departments: done, by writing the department first.
- Credits and grades: ECTS and numeric grades stay for now. A calculator comes later.
- Free electives per department: done. Degree requirements stay plain sentences, inside each curriculum.
- List of universities and departments: done (`content/index.json`).

Done when: sample data covers all of it and the tests pass. Both are true (66 tests).

### 3. Refuse changes that don't match the schema ✅
Nothing goes live unless the check passes.
- ✅ Decided how people submit changes: as a proposal (pull request) that is merged only when the check is green. Direct commits to main are not allowed. The editor clicks Commit, then Create pull request, then Merge once the check is green. Auto-merge may come later, after step 10.
- ✅ Turned on the GitHub rule (`.github/rulesets/protect-testing.json`, applied to `testing`): a pull request is required, the `tests` check must be green, no force-push or deletion. Everyone with access can merge a green proposal (0 approvals required).
- ✅ The content check runs first in `checks.yml`, so a content mistake shows only the plain-language message, not test output.
- Still to polish in step 4: the wording uses Python's "Expecting ',' delimiter", and the line number can point one line after the real mistake. Explain each message in the common-mistakes list.

Done when: a deliberately wrong change is blocked on GitHub and the message is understandable. Both are true (tested with a broken `content/index.json` through the GitHub web editor).

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

### 9. Full documentation 🔄
- **README:** first draft written (what the project is, how it works, how to check content). Keep it up to date as steps are finished.
- **Documentation:** how the folders and files are organised, what each field means, how to add a university or department.
- **For maintainers:** how the checks and the build work, how to change the schema safely.

### 10. Try it with a real non-technical person
Watch them make a change without help. Everything they stumble on goes back into steps 3, 4 or 2.

---

## Decisions still open
- Docusaurus or MkDocs? (Recommended: MkDocs, one toolchain with the checker.)
- Do degree requirements stay plain text or become rules a program can check? (Step 2.)
