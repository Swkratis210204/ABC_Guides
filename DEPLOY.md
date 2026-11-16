# Deploying to Fly.io

**Status: DEPLOYED on 2026-11-11** at <https://deptinfo-aueb-web.fly.dev> (apps `deptinfo-aueb-web` and `deptinfo-aueb-api`, region `fra`,
account `personal`). Verified live: every page, the 404s, the calculator through the proxy, http to https redirect, a 24-step real-browser test,
admin reload (right key 200, no key 401), the admin route blocked on the public site, and the API not reachable from the internet.
The production admin key is in `.env.production` in the project root (not committed, never printed).

## Redeploying after a change (the everyday case)

Open **PowerShell** (not the old Command Prompt) in the project folder, make sure **Docker Desktop is running**, then:

```powershell
.\deploy.ps1                # BOTH the API and the website, API first. Use this unless you are sure only one changed.
.\deploy.ps1 -What web      # the website only (nothing changed in backend/)
.\deploy.ps1 -What api      # the data API only (nothing changed in frontend/)
.\deploy.ps1 -DryRun        # shows what it would do and changes nothing
```

**When unsure, run it with no options.** A new feature often touches both halves, and deploying only one leaves the live site half-updated
(this happened once: the new calculator input boxes appeared, but the old API ignored what was typed into them).

The script checks that Docker is running and that you are logged in to Fly, deploys, then checks the live site: four pages, plus a real
feature check (the calculator must add typed-in ECTS), which fails if the website and the API are on different versions.
If a deploy fails, the previous version keeps running. If you are not logged in to Fly it tells you the login command.
If PowerShell refuses to run the script, start it as `powershell -ExecutionPolicy Bypass -File .\deploy.ps1`.

Changing **only data** (the JSON files) needs no deploy: see "Updating the data after deployment" below.

## What gets deployed

| App | What | Public? | Config |
|-----|------|---------|--------|
| `deptinfo-aueb-web` | The website (Next.js) | Yes, HTTPS | `deploy/frontend.fly.toml` |
| `deptinfo-aueb-api` | The data API (Spring Boot) + a 1 GB volume for the data files | No: reachable only from the web app, over Fly's private network | `deploy/backend.fly.toml` |

The app names must be unique across all of Fly.io. If `apps create` says a name is taken, pick another and change it in
**both** toml files (the web app's `BACKEND_URL` contains the API app's name).

## Cost ⚠

I could not verify Fly's current pricing or free allowance from here. As far as I know, new accounts no longer get a
permanent free tier: expect a card on file and a small monthly bill for two small machines (512 MB each) and a 1 GB volume.
The web machine sleeps when idle, which lowers the cost. Check the current prices at fly.io/docs/about/pricing before you deploy.

## Steps (PowerShell, from the repository root)

```powershell
$fly = "$env:USERPROFILE\.fly\bin\flyctl.exe"

# 0. Log in once (opens a browser)
& $fly auth login

# 1. The API app, its data volume, and its admin key
& $fly apps create deptinfo-aueb-api
& $fly volumes create deptinfo_data --app deptinfo-aueb-api --region fra --size 1
& $fly secrets set ADMIN_API_KEY=<a long random value> --app deptinfo-aueb-api --stage

# 2. Deploy the API (built on this machine with Docker; no public address; one machine only, because the volume is single-machine)
& $fly deploy . --config deploy/backend.fly.toml --dockerfile backend/Dockerfile --no-public-ips --ha=false --local-only

# 3. The web app. The config path must be absolute here, because ./frontend is the working directory.
$root = (Get-Location).Path
& $fly apps create deptinfo-aueb-web
& $fly deploy ./frontend --config "$root\deploy\frontend.fly.toml" --ha=false --local-only
```

Docker must be running, and on this machine its folder must be on PATH first (see README), or the image build cannot log in to its registry helper.
To redeploy after a code change, run only step 2 and/or step 3 again.

Then open `https://deptinfo-aueb-web.fly.dev`.

On first start the API finds an empty volume and copies the starting data (`data-seed`) into it.

## What the first deploy confirmed

1. **Private networking: works.** The web app reads data from the API over `http://deptinfo-aueb-api.internal:8080`. The API listens on `::`
   (`SERVER_ADDRESS` in `backend.fly.toml`) because Fly's private network is IPv6-only.
2. **Build context: works.** `fly deploy . … --dockerfile backend/Dockerfile` used the repository root, and the image contains `data-seed/`.
3. **Health checks: passing** on both machines (`fly status --app <name>`).
4. **Memory: not measured on Fly.** Both machines have 512 MB. Locally the API used about 110 MB and the web app about 50 MB. If either ever
   runs out of memory, raise `memory` in its toml.
5. **Cost: still not verified.** Check the Fly dashboard's billing page for the real amount.

## Updating the data after deployment

The data lives on the API's volume, not in the image, so changing the JSON files does not need a redeploy:

1. Upload the changed files into `/data/tenants/aueb/cs/` on the API machine (`fly ssh sftp shell --app deptinfo-aueb-api`).
2. Tell the API to reload (the public website cannot do this; admin calls are blocked there):
   ```powershell
   & $fly ssh console --app deptinfo-aueb-api -C "wget -qO- --post-data= --header=X-Admin-Key:<your key> http://localhost:8080/api/admin/reload"
   ```
   If the new data is invalid, nothing changes and the answer lists every problem.

⚠ The data on the volume replaces the starting data for good: after the first boot, changing `data-seed/` in the repository
and redeploying does **not** change what the site shows. Updates go through the volume as above.

## Backups

Fly takes automatic daily snapshots of volumes (retention is short; check the current terms). The authoritative copy of the data is the
`data-seed/` folder in this repository plus the department's spreadsheet.

## Rolling back / removing

```powershell
& $fly releases --app deptinfo-aueb-web          # see versions
& $fly apps destroy deptinfo-aueb-web            # remove an app (asks for confirmation); do the same for the API app
```
Destroying the API app also deletes its volume and the data on it.
