# Department information system

A multi-tenant site for browsing a university department's information: courses, faculty, graduation requirements
(with an ECTS calculator) and the academic calendar. The first tenant is AUEB Computer Science, built from the
2026-27 undergraduate study guide (PDF) and the department's spreadsheet.

**Status:** Phase 1 (hierarchical browser) is built and tested. Phase 2 (fuzzy search + RAG chatbot) is not started.
It is deployed on Fly.io at https://deptinfo-aueb-web.fly.dev (see `DEPLOY.md` for how, and for updating it).
`DESIGN.md` is the short, high-level explanation of how it works and why (with a picture); the original detailed design is
kept in `docs/design-detailed-archive.md`.

## Layout

| Path | What |
|------|------|
| `data-seed/tenants/{uni}/{dept}/` | The data: five JSON files per department. `DATA-NOTES.md` there lists every place the sources disagree or were interpreted. |
| `backend/` | Spring Boot 3 / Java 21 API. Loads and validates the JSON at startup; serves it read-only. |
| `frontend/` | Next.js (TypeScript). Browses the data and runs the calculator. Calls the backend through its own `/api` proxy. |
| `docker-compose.yml`, `docker-compose.dev.yml` | Run both parts in Docker, the way production will. |
| `deploy/`, `DEPLOY.md` | Fly.io configs and the deployment steps. |
| `*.pdf`, `*.xlsx` | The two source documents for the data. |

## Run it with Docker (closest to production)

Docker Desktop must be running. On this machine its tools are not on PATH, so add them for the session first:

```powershell
$env:PATH = "C:\Users\swkra\AppData\Local\Programs\DockerDesktop\resources\bin;$env:PATH"
docker compose up --build        # first build takes a few minutes
```

Open <http://localhost:3000>. Stop with `Ctrl+C`, or `docker compose down` (add `-v` to also delete the data volume).

- The data lives in a Docker volume, seeded from `data-seed/` on first start. To edit the JSON files and see changes without rebuilding, add the overlay:
  `docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build`, then reload with the admin key from `.env` (see "Changing the data").
- `.env` (not committed) holds `ADMIN_API_KEY`; `.env.example` shows the format.

## Run it without Docker (Windows / PowerShell)

JDK 21 and Maven are not on PATH on this machine; use the ones below (see the notes at the end).

```powershell
# 1. Backend on http://localhost:8080
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$mvn = "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.8.4-bin\52ccbt68d252mdldqsfsn03jlf\apache-maven-3.8.4\bin\mvn.cmd"
cd backend
& $mvn package            # runs the tests, builds target\deptinfo.jar
& "$env:JAVA_HOME\bin\java.exe" -jar target\deptinfo.jar

# 2. Frontend on http://localhost:3000 (second terminal)
cd frontend
npm install
npm run build
npm start                 # or `npm run dev` while developing
```

The frontend finds the backend at `BACKEND_URL` (default `http://localhost:8080`).
Run the backend from the `backend/` folder: its default data folder is `../data-seed`.

## Tests

```powershell
cd backend
& $mvn test               # 69 tests: data validation, the real AUEB data, every endpoint, the ECTS calculator
cd ..\frontend
npm run typecheck
```

There is also a 61-step test that drives a real browser (Edge, headless) the way a user would: ticking courses, typing ECTS,
reloading, following the sidebar, reading each page. It runs against the Docker stack or the live site. How to run it is at
the top of `tools/browser-test/cdp-test.mjs`.

## Changing the data

Edit the JSON files in `data-seed/tenants/aueb/cs/`, then either restart the backend or call
`POST /api/admin/reload` with the header `X-Admin-Key` (set the key with the `ADMIN_API_KEY` environment variable;
with no key set, admin endpoints are disabled). A reload that finds invalid data changes nothing and returns the
full list of problems. The public site cannot reach admin endpoints: the frontend proxy blocks `/api/admin`.

To add a department, add a folder `data-seed/tenants/{university}/{department}/` with the same five files. No code changes.

## Environment variables

| Name | Where | Purpose |
|------|-------|---------|
| `DATA_DIR` | backend | Folder containing `tenants/` (default `../data-seed`; `/data` in Docker) |
| `SEED_DIR` | backend | If `DATA_DIR/tenants` is empty at startup, copy this folder into it (set in the Docker image) |
| `ADMIN_API_KEY` | backend | Enables and protects `/api/admin/**` |
| `SERVER_ADDRESS` | backend | Address to listen on; Fly sets `::` (IPv6 private network) |
| `BACKEND_URL` | frontend | Where the frontend reaches the backend (default `http://localhost:8080`) |

## Notes

- JDK 21 used here is the one bundled with Android Studio; Maven 3.8.4 is the copy in `~/.m2/wrapper`. Any JDK 21 and Maven 3.6.3+ work.
- Fly's CLI is at `C:\Users\swkra\.fly\bin\flyctl.exe` (not on PATH).

## Reporting mistakes

Every department page ends with an "Αναφορά λάθους στο GitHub" link. It opens a pre-filled issue (form: `.github/ISSUE_TEMPLATE/data-error.yml`) that names the page. Issues need a GitHub account and the repository must allow issues (public, or the reporter invited). To send reports to another repository, set `NEXT_PUBLIC_ISSUES_URL` when building the frontend.
