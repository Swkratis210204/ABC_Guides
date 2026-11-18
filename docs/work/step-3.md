# Step 3: refuse changes that don't match the schema

Goal: nothing reaches the `testing` branch unless the check passes. Done and tested on GitHub.

## Decisions

| # | Issue | Decision | Status |
|---|---|---|---|
| 1 | How people submit changes | A proposal (pull request) that merges only when the check is green. Direct commits are not allowed. The editor clicks Commit, then Create pull request, then Merge once the check is green. Auto-merge may come later, after step 10. | decided |
| 2 | Who may merge | Everyone with access, with 0 required approvals. The check is the gate. Revisit after step 10 if wrong-but-valid content gets through. | decided |
| 3 | Where the rule applies | Only the `testing` branch for now. Not every branch, because GitHub's web editor creates a new branch for each proposal, and a pull-request-only rule on all branches would block that. `main` is the old project and is not protected. | decided |
| 4 | Repository visibility | Public, so rulesets are enforced on the free plan (budget is $0). | decided |
| 5 | Admin bypass | None. The owner also uses pull requests, which tests the real flow. It can be added later if it gets in the way. | decided |
| 6 | Order of the checks | The content check runs before the tests. A content mistake now stops at the first step with only the plain-language message, instead of seven test failures and hundreds of lines of output. | done |

## What was built

- `.github/rulesets/protect-testing.json`: the ruleset, imported in GitHub under Settings, Rules, Rulesets. It requires a pull request, requires the `tests` check, requires the branch to be up to date, and blocks force-pushes and deletion of `testing`.
- `.github/workflows/checks.yml`: one job named `tests` (the ruleset requires this name; renaming it needs a ruleset change). Steps: content check, then all tests. It runs on every push and every pull request.
- `.githooks/pre-push`: runs the same checks on the developer's computer before a push. Optional, for convenience only; the pull request check is the one that is enforced.

## How it was tested

- A pull request with a deliberately broken `content/index.json` (a missing bracket), made through the GitHub web editor: both `tests` runs failed and the Merge button was blocked.
- Before the reorder, the log began with test output and tracebacks. After it, the log showed only:

```
PROBLEM in content/index.json
  The file is not valid JSON. Problem near line 3, column 1: Expecting ',' delimiter.
  Help: Usual causes: a missing or extra comma, a missing quote, or a bracket that was not closed.

1 problem(s) found. Fix them and try again. The site was NOT updated.
```

- A valid change (the check-order fix) was blocked while its check was still running, then merged once it was green.

## Left for step 4

- The wording uses Python's "Expecting ',' delimiter", which means little to most people.
- The line number can point one line after the real mistake, because Python reports where it noticed the problem.
- Explain each message in the common-mistakes list of the editors' guide.
