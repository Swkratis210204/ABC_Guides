// Real-browser test of the whole site: 61 checks, driven through Microsoft Edge (headless) with the Chrome DevTools Protocol.
// It needs no packages, only Node 22+ and Edge. It clicks like a user: ticks courses, types ECTS, reloads, follows the sidebar.
// Run it against the Docker stack (docker compose up) or the live site. It is written for the AUEB CS data (course codes, counts).
//
//   1. Start Edge with a debugging port (PowerShell):
//        $edge = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
//        Start-Process $edge -ArgumentList '--headless=new','--disable-gpu','--remote-debugging-port=9333',"--user-data-dir=$env:TEMP\edge-test",'--no-first-run','about:blank'
//   2. Run the test (the last argument is a folder where screenshots are written):
//        node tools/browser-test/cdp-test.mjs 9333 http://localhost:3000 $env:TEMP
//        node tools/browser-test/cdp-test.mjs 9333 https://deptinfo-aueb-web.fly.dev $env:TEMP
//   3. Close that Edge process afterwards.
//
// The test clears the browser's saved calculator choices first, so it always starts clean.

import { writeFileSync } from "node:fs";

const [port, base, shotDir] = process.argv.slice(2);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const targets = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
const page = targets.find((t) => t.type === "page");
const ws = new WebSocket(page.webSocketDebuggerUrl);
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });

let nextId = 1;
const pending = new Map();
const consoleErrors = [];
ws.onmessage = (ev) => {
  const msg = JSON.parse(ev.data);
  if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); }
  if (msg.method === "Runtime.exceptionThrown") consoleErrors.push(msg.params.exceptionDetails?.exception?.description ?? "exception");
  if (msg.method === "Runtime.consoleAPICalled" && msg.params.type === "error") consoleErrors.push(msg.params.args.map((a) => a.value ?? a.description).join(" "));
};
const send = (method, params = {}) => new Promise((res) => { const id = nextId++; pending.set(id, res); ws.send(JSON.stringify({ id, method, params })); });
const js = async (expression) => {
  const r = await send("Runtime.evaluate", { expression, awaitPromise: true, returnByValue: true });
  if (r.result?.exceptionDetails) throw new Error("JS error: " + (r.result.exceptionDetails.exception?.description ?? r.result.exceptionDetails.text));
  return r.result?.result?.value;
};
const waitFor = async (expression, what, ms = 8000) => {
  const end = Date.now() + ms;
  while (Date.now() < end) { try { if (await js(expression)) return true; } catch { /* page still loading */ } await sleep(150); }
  throw new Error("timed out waiting for: " + what);
};
const shot = async (name) => {
  const r = await send("Page.captureScreenshot", { format: "png" });
  writeFileSync(`${shotDir}/${name}.png`, Buffer.from(r.result.data, "base64"));
};

let passed = 0, failed = 0;
const check = (name, ok, detail = "") => { ok ? passed++ : failed++; console.log(`${ok ? "PASS" : "FAIL"}  ${name}${ok ? "" : "   -> " + detail}`); };
const fold = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

const text = () => js("document.body.innerText");
const clickButton = (txt) => js(`(() => { const b = [...document.querySelectorAll('button')].find(b => b.textContent.includes(${JSON.stringify(txt)})); if (!b) return false; b.click(); return true; })()`);
const checkedCount = () => js("document.querySelectorAll('.calc-picker input[type=checkbox]:checked').length");
const tick = (code) => js(`(() => { const l = [...document.querySelectorAll('label.course-pick')].find(l => l.querySelector('.code')?.textContent.trim() === ${JSON.stringify(code)}); if (!l) return false; l.querySelector('input').click(); return true; })()`);
const setText = (selector, value) => js(`(() => { const i = document.querySelector(${JSON.stringify(selector)}); if (!i) return false; const set = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; set.call(i, ${JSON.stringify(value)}); i.dispatchEvent(new Event('input', { bubbles: true })); return true; })()`);
const goto = async (path) => {
  await send("Page.navigate", { url: base + path });
  await waitFor("document.readyState === 'complete'", "page load");
  await sleep(1200); // let React hydrate
};
const CALC = "/aueb/cs/requirements/calculator";

await send("Runtime.enable");
await send("Page.enable");
await send("Emulation.setDeviceMetricsOverride", { width: 1280, height: 1100, deviceScaleFactor: 1, mobile: false });

// Start from a clean slate: clear anything remembered by a previous run.
await goto(CALC);
await js("localStorage.clear()");
await goto(CALC);

// 1. Page loaded with the picker
const total = await js("document.querySelectorAll('.calc-picker input[type=checkbox]').length");
check("picker lists all 88 courses as checkboxes", total === 88, `found ${total}`);
check("nothing is ticked at first", (await checkedCount()) === 0);
check("a course offered in two semesters shows both (6023 -> Ζ΄/Η΄)", (await text()).includes("Γραμμικά Μοντέλα") && (await text()).includes("(Ζ΄/Η΄)"));
check("there is no 'calculate' button: the result is live", !(await clickButton("Υπολογισμός")));

// 2. Select all mandatory: the result appears by itself
check("'select all mandatory' button exists", await clickButton("Επιλογή όλων των υποχρεωτικών"));
check("22 mandatory courses are ticked", (await checkedCount()) === 22, `ticked ${await checkedCount()}`);
check("counter says 22 selected", (await text()).includes("22 επιλεγμένα μαθήματα"));
await waitFor("document.body.innerText.includes('152 / 240 ECTS')", "result with 152 / 240 ECTS (no button pressed)");
let t = await text();
check("result appears on its own: 152 / 240 ECTS", t.includes("152 / 240 ECTS"));
check("result says 88 ECTS are missing", t.includes("Σας λείπουν 88 ECTS"));
check("mandatory rule is satisfied (✓ present)", t.includes("✓"));
check("the manual-check section is shown", t.includes("Δεν ελέγχονται από τον υπολογιστή"));
check("the estimate disclaimer is shown", t.includes("Ο υπολογισμός είναι ενδεικτικός"));
await shot("calc-after-mandatory");

// 2b. THE REPORTED BUG: with a result on screen, add a free elective and type its ECTS: the total must follow by itself
check("tick a free elective (8154)", await tick("8154"));
check("its ECTS box appears", await js(`!!document.querySelector('input.ects-input')`));
check("type 6 into it", await setText("input.ects-input", "6"));
await waitFor("document.body.innerText.includes('158 / 240 ECTS')", "total updates to 158 / 240 without pressing anything");
check("total updated by itself: 152 + 6 = 158 / 240", (await text()).includes("158 / 240 ECTS"));
await tick("8154");
await waitFor("document.body.innerText.includes('152 / 240 ECTS')", "back to 152 after un-ticking");
check("un-ticking it takes the 6 ECTS away again", (await text()).includes("152 / 240 ECTS"));

// 3. A complete degree plan: + all 10 core electives + three 6-ECTS electives = 240
for (const code of ["3515", "3155", "3541", "3513", "3662", "3672", "3642", "3511", "3745", "3543", "3612", "3814", "3584"]) {
  if (!(await tick(code))) check(`could tick ${code}`, false, "course row not found");
}
check("35 courses are ticked now", (await checkedCount()) === 35, `ticked ${await checkedCount()}`);
await waitFor("document.body.innerText.includes('240 / 240 ECTS')", "result with 240 / 240 ECTS");
t = await text();
check("result shows 240 / 240 ECTS", t.includes("240 / 240 ECTS"));
check("result says the total is covered", t.includes("Το σύνολο ECTS καλύπτεται"));
check("result says all calculable rules are met", t.includes("πληρούνται όλες οι προϋποθέσεις που μπορεί να ελέγξει ο υπολογιστής"));
check("a consistent plan produces no warnings section", !t.includes("Παρατηρήσεις"));
await shot("calc-full-plan");

// 4. Selection survives a reload (localStorage)
await goto(CALC);
await waitFor("document.querySelectorAll('.calc-picker input[type=checkbox]:checked').length > 0", "selection restored", 4000).catch(() => {});
check("after a reload the 35 ticks are restored", (await checkedCount()) === 35, `ticked ${await checkedCount()}`);
await waitFor("document.body.innerText.includes('240 / 240 ECTS')", "result restored after reload").catch(() => {});
check("and the result is shown again without pressing anything", (await text()).includes("240 / 240 ECTS"));

// 5. Un-tick one course: the total drops by its ECTS (3584 = 6)
await tick("3584");
await waitFor("document.body.innerText.includes('234 / 240 ECTS')", "result with 234 / 240 ECTS");
t = await text();
check("un-ticking a 6-ECTS course gives 234 / 240", t.includes("234 / 240 ECTS") && t.includes("Σας λείπουν 6 ECTS"));

// 6. Cycles: 5 cycle-1 courses -> one cycle completable
await clickButton("Καθαρισμός");
check("'clear' empties the selection", (await checkedCount()) === 0);
for (const code of ["3155", "3513", "3745", "3543", "6127"]) await tick(code);
await waitFor("document.body.innerText.includes('Κύκλοι σπουδών')", "cycles section");
t = await text();
check("5 cycle-1 courses -> 'up to 1 cycles'", /έως\s*1\s*κύκλους/.test(t.replace(/\n/g, " ")), t.slice(t.indexOf("Κύκλοι σπουδών"), t.indexOf("Κύκλοι σπουδών") + 200));
check("cycle 1 is marked as completing", t.includes("Ολοκληρώνεται"));
await shot("calc-cycles");

// 7. Search box folds accents and case
await clickButton("Καθαρισμός");
await setText("#calc-filter", "αλγοριθμοι");
await sleep(400);
const visible = await js("[...document.querySelectorAll('label.course-pick')].map(l => l.textContent)");
check("typing 'αλγοριθμοι' finds Αλγόριθμοι (accent/case-insensitive)", visible.some((x) => x.includes("Αλγόριθμοι")) && visible.length < 15, `${visible.length} rows`);
await setText("#calc-filter", "");

// 8. Nothing selected: no result, no error, just the hint
await sleep(600);
t = await text();
check("with nothing ticked there is no stale result and no error", !t.includes("/ 240 ECTS") && !t.includes("δεν ήταν δυνατός") && t.includes("ενημερώνεται αυτόματα"));

// 9. Free electives and "other ECTS"
await goto(CALC);
await clickButton("Καθαρισμός");
check("ticking a free elective (8154) works", await tick("8154"));
check("an ECTS box appears next to it", await js(`!!document.querySelector('input.ects-input[aria-label*="Επιχειρηματικότητα"]')`));
check("typing 6 into it works", await setText("input.ects-input", "6"));
check("typing '4,5' (decimal comma) into 'other ECTS' works", await setText("#other-ects", "4,5"));
await waitFor("document.body.innerText.includes('10,5 / 240 ECTS')", "free-elective ECTS added (10,5 / 240)");
t = await text();
check("free elective ECTS are added: 6 typed + 4,5 other = 10,5 / 240", t.includes("10,5 / 240 ECTS"));
check("result says 10,5 ECTS were entered by the user", t.includes("Από αυτά, 10,5 ECTS τα δηλώσατε εσείς"));
check("no 'ECTS unknown' warning once ECTS are typed", !t.includes("δεν αναφέρει ECTS γι' αυτό"));
await shot("calc-free-elective");

await goto(CALC);
check("typed free-elective ECTS survive a reload", (await js(`document.querySelector('input.ects-input')?.value`)) === "6");
check("'other ECTS' survives a reload", (await js(`document.getElementById('other-ects')?.value`)) === "4,5");

await setText("input.ects-input", "abc");
await waitFor("document.body.innerText.includes('Ελέγξτε τα ECTS')", "validation message");
check("a non-number is rejected with a clear message (no server error)", (await text()).includes("Ελέγξτε τα ECTS που πληκτρολογήσατε"));
await clickButton("Καθαρισμός");
check("'clear' also resets typed ECTS and other ECTS", (await js(`document.getElementById('other-ects').value`)) === "" && (await checkedCount()) === 0);

// 10. The course views are genuinely different, and each level page states its requirement
await goto("/aueb/cs/courses/semester/6");
t = await text();
check("semester view: grouped by level, with ECTS totals and a cycles column", t.includes("Υποχρεωτικά μαθήματα πυρήνα") && t.includes("Μαθήματα επιλογής πυρήνα") && /\d+ μαθήματα? · \d+ ECTS/.test(t) && fold(t).includes("κυκλοι"));
const semesterHeaders = await js(`[...document.querySelectorAll('th')].map(th => th.innerText)`);
check("semester view has no redundant semester column", !semesterHeaders.some((h) => fold(h) === "εξαμηνο"), JSON.stringify(semesterHeaders));
await goto("/aueb/cs/courses/level/CORE_MANDATORY");
t = await text();
check("level ΥΠ states 'all 22 must be passed'", t.includes("Τι απαιτείται από αυτό το επίπεδο") && t.includes("22 υποχρεωτικά"));
check("level view is grouped by semester", t.includes("Α΄ εξάμηνο") && t.includes("ΣΤ΄ εξάμηνο"));
await goto("/aueb/cs/courses/level/CORE_ELECTIVE");
check("level ΕΠ states the minimum of 4 courses", (await text()).includes("τουλάχιστον 4 μαθήματα επιλογής πυρήνα"));
await goto("/aueb/cs/courses/level/ELECTIVE");
check("level Ε states the 64 ECTS rule", (await text()).includes("64 ECTS"));
await goto("/aueb/cs/courses/level/FREE");
t = await text();
check("level ΕΛ states the 240 ECTS total", t.includes("240 ECTS"), t.slice(t.indexOf("Τι απαιτείται"), t.indexOf("Τι απαιτείται") + 150));
await goto("/aueb/cs/courses/cycle/applied-math");
t = await text();
check("cycle view shows the rule and the 'also counts for other cycles' column", fold(t).includes(fold("Μετρά και στους κύκλους")) && fold(t).includes("τουλαχιστον 5"));
await shot("view-cycle");

// 11. THE OTHER REPORTED BUG: the three sidebar entries must lead to three different pages
await goto("/aueb/cs");
const hrefs = await js(`[...document.querySelectorAll('.sidenav a')].map(a => a.getAttribute('href'))`);
check("sidebar 'by semester' leads to a real semester page", hrefs.some((h) => h.endsWith("/courses/semester/1")), JSON.stringify(hrefs));
check("sidebar 'by level' leads to a real level page", hrefs.some((h) => h.endsWith("/courses/level/CORE_MANDATORY")));
check("sidebar 'by cycle' leads to a real cycle page", hrefs.some((h) => h.endsWith("/courses/cycle/data-science-ml")));
check("no sidebar entry is just a #anchor on the same hub page", !hrefs.some((h) => h.includes("/courses#")));
const seen = {};
for (const [label, path] of [["Ανά εξάμηνο", "/courses/semester/1"], ["Ανά επίπεδο", "/courses/level/CORE_MANDATORY"], ["Ανά κύκλο σπουδών", "/courses/cycle/data-science-ml"]]) {
  await js(`[...document.querySelectorAll('.sidenav a')].find(a => a.textContent.trim() === ${JSON.stringify(label)}).click()`);
  await waitFor(`location.pathname.endsWith(${JSON.stringify(path)})`, `navigation to ${path}`);
  await sleep(700);
  seen[label] = { path: await js("location.pathname"), text: await text(), current: await js(`[...document.querySelectorAll('.sidenav a[aria-current="page"]')].map(a => a.textContent.trim())`) };
  check(`clicking '${label}' opens ${path} and highlights that entry`, seen[label].path.endsWith(path) && seen[label].current.includes(label), JSON.stringify(seen[label].current));
}
const [s1, s2, s3] = Object.values(seen).map((x) => x.text);
check("the three pages are different from each other", s1 !== s2 && s2 !== s3 && s1 !== s3);
check("semester page: grouped by level", s1.includes("Υποχρεωτικά μαθήματα πυρήνα"));
check("level page: shows the requirement", s2.includes("Τι απαιτείται από αυτό το επίπεδο"));
check("cycle page: shows the cycle rule", s3.includes("Ολοκλήρωση κύκλου"));

check("no browser console errors", consoleErrors.length === 0, consoleErrors.slice(0, 3).join(" | "));
console.log(`\n${passed} passed, ${failed} failed`);
ws.close();
process.exit(failed ? 1 : 0);
