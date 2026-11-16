<#
  Redeploys the site to Fly.io. Run it in PowerShell from the project folder:

      .\deploy.ps1                 # BOTH the data API and the website (API first). The safe default.
      .\deploy.ps1 -What web       # the website only: only if you changed nothing in backend/
      .\deploy.ps1 -What api       # the data API only: only if you changed nothing in frontend/
      .\deploy.ps1 -DryRun         # shows what would happen, changes nothing

  When unsure, run it with no options. A new feature often touches both halves; deploying only one of them
  leaves the live site half-updated (for example: the new input boxes appear but the old API ignores them).

  Changing only the data files (data-seed/) needs no deploy: see DEPLOY.md, "Updating the data".
  Needs: Docker Desktop running, and `flyctl auth login` done once. See DEPLOY.md.
#>
param(
    [ValidateSet('web', 'api', 'both')][string]$What = 'both',
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$fly = Join-Path $env:USERPROFILE '.fly\bin\flyctl.exe'
$site = 'https://deptinfo-aueb-web.fly.dev'

# Docker's helper programs are not on PATH on this machine; the image build needs them.
$env:PATH = "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin;$env:PATH"

function Stop-With($message) { Write-Host "`nSTOP: $message" -ForegroundColor Red; exit 1 }

# ---- checks (read-only) -------------------------------------------------------------------------
if (-not (Test-Path $fly)) { Stop-With "flyctl not found at $fly" }

docker info *> $null
if ($LASTEXITCODE -ne 0) { Stop-With "Docker is not running. Start Docker Desktop, wait until it says it is running, then try again." }

& $fly auth whoami *> $null
if ($LASTEXITCODE -ne 0) { Stop-With "You are not logged in to Fly. Run:  & `"$fly`" auth login   (then try again)" }

Write-Host "Checks passed: Docker is running and you are logged in to Fly." -ForegroundColor Green
Write-Host "Deploying: $What$(if ($DryRun) { '  (DRY RUN: nothing will be changed)' })`n"

function Invoke-Deploy($label, [scriptblock]$command, $text) {
    Write-Host "== $label ==" -ForegroundColor Cyan
    Write-Host "   $text"
    if ($DryRun) { return }
    & $command
    if ($LASTEXITCODE -ne 0) { Stop-With "The $label deploy failed (see the messages above). The previous version is still running." }
}

# ---- deploy (the exact commands that worked for the first deploy) -------------------------------
Set-Location $root

if ($What -in 'api', 'both') {
    Invoke-Deploy 'API (data backend)' {
        & $fly deploy . --config deploy/backend.fly.toml --dockerfile backend/Dockerfile --no-public-ips --ha=false --local-only --yes
    } 'flyctl deploy . --config deploy/backend.fly.toml --dockerfile backend/Dockerfile --no-public-ips --ha=false --local-only'
}

if ($What -in 'web', 'both') {
    Invoke-Deploy 'Website' {
        & $fly deploy "$root\frontend" --config "$root\deploy\frontend.fly.toml" --ha=false --local-only --yes
    } 'flyctl deploy frontend --config deploy\frontend.fly.toml --ha=false --local-only'
}

# ---- check the live site ------------------------------------------------------------------------
if ($DryRun) { Write-Host "`nDry run finished. Nothing was deployed." -ForegroundColor Yellow; exit 0 }

Write-Host "`n== Checking the live site ==" -ForegroundColor Cyan
# The needles are plain ASCII on purpose: Windows PowerShell mangles non-ASCII characters in script files.
$checks = @(
    @{ Path = '/aueb/cs';                          Needle = 'infotech@aueb.gr' },   # department page (data from the API)
    @{ Path = '/aueb/cs/courses/semester/3';       Needle = '/courses/3335' },      # a course link on the semester page
    @{ Path = '/aueb/cs/requirements/calculator';  Needle = 'calc-filter' },        # the calculator form
    @{ Path = '/api/universities';                 Needle = 'aueb' }                # through the website to the private API
)
$failed = $false
foreach ($c in $checks) {
    try {
        $r = Invoke-WebRequest "$site$($c.Path)" -UseBasicParsing -TimeoutSec 60
        $text = [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())
        $ok = ($r.StatusCode -eq 200) -and $text.Contains($c.Needle)
    } catch { $ok = $false }
    Write-Host ("  {0}  {1}" -f $(if ($ok) { 'OK  ' } else { 'FAIL' }), $c.Path) -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' })
    if (-not $ok) { $failed = $true }
}
# A real feature check, not just "the page loads": the calculator must add ECTS that the user typed in.
# This fails when the website is new but the API is old (or the other way round).
try {
    $body = '{"passedCourses":["8154"],"ectsOverrides":{"8154":6},"otherEcts":4}'
    $r = Invoke-WebRequest "$site/api/universities/aueb/departments/cs/ects-calculator" -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 60
    $earned = ($r.Content | ConvertFrom-Json).totals.earned
    $ok = ($earned -eq 10)
} catch { $ok = $false }
Write-Host ("  {0}  calculator adds typed-in ECTS (expects 10, got {1})" -f $(if ($ok) { 'OK  ' } else { 'FAIL' }), $earned) -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' })
if (-not $ok) { $failed = $true; Write-Host "       The website and the API are on different versions. Run .\deploy.ps1 with no options to update both." -ForegroundColor Yellow }

if ($failed) { Stop-With "Some checks failed. Look at the logs:  & `"$fly`" logs --app deptinfo-aueb-web   (or ...-api)" }
Write-Host "`nDone. The live site is updated: $site" -ForegroundColor Green
