# How to change the content

Draft. This is also the template for the full guide in step 4.

All the information on the site lives in JSON files in the [`content/`](../content/) folder. Nobody can change the live branch (`testing`) directly. Every change is a **proposal** (a pull request). It goes in only when the automatic check is green.

Pick the module that matches you:

- [Module 1: I only want to change the data](#module-1-i-only-want-to-change-the-data) (no Git, no terminal)
- [Module 2: I work with Git and code](#module-2-i-work-with-git-and-code)

---

## Module 1: I only want to change the data

You need a GitHub account and access to the repository. Everything is done in the browser.

### Change something that already exists

1. Open the repository on GitHub and go to the `content` folder.
2. Click through the folders to the file you want. Example: `content` > `example-university` > `computer-science` > `general` > `faculty.json`.
3. Click the **pencil icon** (Edit this file), top right of the file.
4. Change the text. Change only what is between the quotes, for example a name or a date. Leave the quotes, commas and brackets where they are.
5. Click **Commit changes...** (green button, top right).
6. In the window, write a short note of what you changed, for example `Update phone number of the secretary`.
7. Choose **Create a new branch for this commit and start a pull request**. Do not choose "Commit directly", it is not allowed.
8. Click **Propose changes**, then **Create pull request**.

### Wait for the check

At the bottom of the page you will see a yellow circle while the check runs. It takes about a minute.

| You see | It means | What to do |
|---|---|---|
| 🟡 Yellow circle | Still checking | Wait |
| ✅ Green tick, "All checks have passed" | The change is valid | Click **Merge pull request**, then **Confirm merge**, then **Delete branch** |
| ❌ Red cross | The change has a mistake | See below |

After you merge, the change is part of the content. (Later, once the website is built, it will appear on the site by itself.)

### If the check is red

1. Click **Details** next to the red check, then open **Check the content files**.
2. Read the message. It names the file and the place of the mistake. Example:

   ```
   PROBLEM in content/index.json
     The file is not valid JSON. Problem near line 3, column 1.
   ```
3. Go back to the pull request, click the **Files changed** tab, open the file menu (three dots) and choose **Edit file**.
4. Fix the mistake and click **Commit changes** again. This time keep the same branch. The check runs again by itself.

Nothing breaks while the check is red. The live content stays as it was until you merge.

### The most common mistakes

| Mistake | How it looks | Fix |
|---|---|---|
| Missing comma | `"name": "Anna"` followed by another line without a comma | Every line inside `{ }` or `[ ]` ends with a comma, except the last one |
| Extra comma at the end | `"name": "Anna",` as the last line before `}` | Remove the comma on the last line |
| Missing quote | `"name": "Anna` | Text always has a quote at the start and at the end |
| Missing bracket | A `{` or `[` that is never closed | Every `{` needs a `}` and every `[` needs a `]` |
| Misspelled field name | `"semester"` instead of `"semesters"` | Use the exact name. The message suggests the right one |
| Value not in the allowed list | `"rank": "Prof"` | The message lists the allowed values. Use one of them |

Tip: the line number in the message can point one line **after** the real mistake. Look at the line above it too.

The full list of messages, with an explanation of each, comes in step 4.

### Add something new

Copy-and-fill templates (add a course, add a teacher, add a new year) will be added here in step 4.

---

## Module 2: I work with Git and code

### One-time setup

```
git clone https://github.com/Swkratis210204/ABC_Guides.git
cd ABC_Guides
git checkout testing
pip install -r requirements.txt
git config core.hooksPath .githooks
```

The last line turns on the pre-push hook. It runs the tests and the content check before every push and cancels the push if something fails.

### The rules

- `testing` is protected. Direct pushes are blocked for everyone, including the owner.
- A change needs a pull request and a green `tests` check (content check, then pytest).
- The branch must be up to date with `testing` before it can merge.
- No approvals are needed. Anyone with access can merge a green pull request.
- `main` is the old project. Ignore it.

### The flow

**1. Start from an up-to-date `testing`**

```
git checkout testing
git pull
```

**2. Make a temporary branch**

```
git checkout -b my-change
```

Use a short name that says what it does, for example `add-math-courses-2027`.

**3. Make the change and commit**

```
git add -A
git commit -m "Add math courses for 2027-2028"
```

Committing runs no checks.

**4. Push the branch**

```
git push -u origin my-change
```

The pre-push hook runs the content check and the tests here, and cancels the push if anything fails. Fix the problem, commit and push again.

Optional: to check earlier, without pushing, run `python scripts/validate.py` (content) and `python -m pytest tests -q` (tests). `All good` means the content is valid.

**5. Open the pull request**

Open the link that `git push` prints, or go to the repository on GitHub and click **Compare & pull request**. Make sure the base branch is `testing`. Then click **Create pull request**.

**6. Wait for the check**

Wait until `tests` is green. If it is red, open **Details**, read the message, fix it locally, commit and push to the same branch (the hook runs again). The check runs again.

If GitHub says the branch is out of date, click **Update branch**, or run:

```
git pull origin testing
git push
```

**7. Merge**

Click **Merge pull request**, then **Confirm merge**. Then click **Delete branch** on the pull request page. This removes the temporary branch on GitHub.

**8. Clean up locally**

```
git checkout testing
git pull
git branch -d my-change
git fetch --prune
```

- `git pull` brings the merged change into your `testing`.
- `git branch -d` deletes the temporary branch on your computer. Git refuses if the branch was not merged, which protects you from losing work. Use `-D` only if you are sure you want to throw the work away.
- `git fetch --prune` forgets branches that were deleted on GitHub.

### If something goes wrong

| Problem | What to do |
|---|---|
| `push` rejected, "protected branch" | You tried to push to `testing`. Make a branch (step 2) and push that |
| Pre-push hook cancelled the push | Read the message above, fix it, push again. Do not skip the hook |
| Merge button is grey | The check is still running, failed, or the branch is out of date |
| Merge conflict | Run `git pull origin testing` on your branch, fix the conflicts in the files, commit, push |
| Deleted the wrong branch locally | `git reflog` shows the last commit. `git checkout -b my-change <commit>` brings it back |

### Changing the checker or the workflow

- The workflow job must stay named `tests`. The ruleset (`.github/rulesets/protect-testing.json`) requires that exact name. If you rename it, change the ruleset too.
- Changing a schema is a bigger step. Update the sample data and the tests in the same pull request.
