## Project Context — Read Before Anything Else

We are building an open university department information system.
The entire project is driven by one core principle:

NON-TECHNICAL people are the primary users and maintainers.
The only thing they will ever touch is JSON files.

### How it works
1. All content lives in JSON files (single source of truth)
2. A non-technical person makes a change to the JSON
   (edit locally or via GitHub web editor, then paste + commit)
3. GitHub Actions automatically validates the JSON,
   builds the site, and deploys it
4. The live site updates — no one touches code

### What this means for every decision you make
- If a change requires editing code → wrong approach, find another way
- If a change requires the terminal → wrong approach, find another way  
- If a change requires knowing Git → wrong approach, simplify it
- The JSON schema must be human-readable and self-explanatory
- Validation errors must be written in plain language, not technical jargon
- The site generation must be fully automatic, zero manual steps

### Stack (decided, do not change)
- Content: JSON files (the only thing non-technical people touch)
- Site: Docusaurus (or MkDocs — TBD)
- CI/CD: GitHub Actions (validate → build → deploy)
- Hosting: GitHub Pages (free)
- Search: Algolia DocSearch (free for educational projects)
- Budget: $0

### JSON Architecture (3 tiers, decided, do not change)

University (truly general)
├── calendar
├── grading_scale
├── schools
└── free_electives pool

Department — general (stable year to year)
├── administration
├── sectors
├── faculty
├── postgraduate_programs
└── degree_requirements

Department — curriculum (one per academic year)
├── academic_year
├── ects_caps
├── levels (UP / EP / E / EL)
├── cycles
└── courses

### Previous years (may be added later)
We may also want to publish content from previous academic years, not only the current one.
- Each academic year keeps its own curriculum file, so old years are never overwritten.
- Adding an old year should work the same way as adding a new one (one new JSON file).
- The schema must not assume there is only one current year.

### Current status
- Phase 0 in progress: designing the JSON schema
- One university populated: AUEB, CS department
- All data available: courses, faculty, requirements, cycles, calendar

### What to never do
- Never suggest a backend, a database, or a server for content management
- Never suggest a CMS platform (Contentful, Sanity, Strapi etc.)
- Never add complexity that requires a developer for routine updates
- Never assume the person updating JSON has technical knowledge