# Step 4: instructions for people editing JSON

Goal: a short guide, no jargon, that tells people how to change the content. Done as a first draft. It is the template for the full guide.

The guide is [instructions.md](../instructions.md).

## Decisions

| # | Issue | Decision | Status |
|---|---|---|---|
| 1 | Who the guide is for | Two modules in one file. Module 1 is for people who only change data (browser only, no Git, no terminal). Module 2 is for people who use Git and code. | done |
| 2 | How non-technical people save a change | The GitHub web editor, then "Create a new branch and start a pull request", then Merge when the check is green. This follows the rule from step 3. | done |
| 3 | How to check a change before saving, without a terminal | No separate tool. The GitHub check runs on every pull request and a red result shows the plain-language message. Nothing goes live while it is red. | done |
| 4 | Local checks for developers | The pre-push hook runs the content check and the tests. The guide does not ask for a separate manual check. Commits run nothing. | done |
| 5 | Common mistakes | A short table for commas, quotes, brackets, wrong field names and values outside the allowed list. | done (short version) |

## What was built

- `docs/instructions.md`:
  - Module 1: edit a file in the browser, wait for the check, merge, what to do when the check is red, common mistakes.
  - Module 2: one-time setup, the protection rules, the full flow (temporary branch, push, pull request, merge, delete the branch, pull and clean up locally) and a troubleshooting table.

## Not done, moved on

- The full list of checker messages with an explanation of each. The short table of common mistakes covers the usual ones.
- The wording of the JSON syntax message ("Expecting ',' delimiter") and the line number that can point one line after the real mistake. The guide explains the line number. The checker itself is unchanged.
- Copy-and-fill templates (add a course, add a teacher, add a new year). The guide has a placeholder. Do them with step 5, when real data shows which edits are common.
- The check "someone follows it without help". It has not been tested. It is part of step 10, and what people stumble on goes back into this guide.
- The commands and GitHub screens in the guide were written from the repository setup and have not been run step by step by someone else.
