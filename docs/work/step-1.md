# Work log

## Our goal

Build an open university department information system that **non-technical people can maintain**.

They only ever touch JSON files. A change is made, GitHub checks it, builds the site and publishes it automatically. Nobody touches code, the terminal or Git. Budget: $0.

---

## Step 1: Design the JSON schema and prove it works

Done on the `testing` branch, from scratch.

**Done**
- Designed the JSON structure for three tiers: university, department (general), and curriculum (one file per year).
- Built sample data for 2 universities, 3 departments and 2 academic years.
- Built a checker that explains mistakes in plain language, for example `did you mean "semesters"?`.
- Added automatic tests, and an automatic check on every change on GitHub.
- The schema accepts previous years: old curricula, old calendars, older versions of department files, and teachers who have left.
- Tests confirm the sample data uses every field the schema allows.

**Not done yet**
- Real data.
- Schema gaps: some lists are fixed (ranks, degree types), courses can't refer to other departments, and a department can't have several programmes.
- The website itself, and search.
