# The dashboard-centered tour (`/tour`)

`/tour` walks the same 22 steps as `/storyline`, but instead of reading like a document it puts the
live JobRunr Pro dashboard on stage. Each step pins a pulsing beacon to a real dashboard element and
narrates it in a popup. Visitors can click anywhere they like; the tour notices and offers the way back.

It exists because a year of Clarity data on `/storyline` showed the content works and the delivery
leaks: the hub page lost ~81% of visitors before step 1, `/login` was the number one exit, and the
whole story produced 5 form submits and 3 sign-ups in twelve months. So the tour removes the hub, keeps
sign-in inside the shell, and ends by asking.

`/storyline` is untouched and stays the default entry point for now.

## Where things live

| Piece | File |
|---|---|
| Page shell (intro, pill, jump list, panels) | `storyline-viewer/src/main/resources/templates/tour.peb` |
| Overlay engine | `storyline-viewer/src/main/resources/static/js/tour.js` |
| Styling | `storyline-viewer/src/main/resources/static/css/tour.css` |
| Routes + JSON blob | `storyline-viewer/.../api/TourController.java`, `TourData.java` |
| Step model | `storyline-viewer/.../model/TourInfo.java`, `TourIntro.java` |
| YAML parsing and defaults | `storyline-viewer/.../StorylineReader.java` |
| Sign-in loop | `storyline-viewer/.../security/TourAuthController.java`, `StorylineAuthSuccessHandler.java` |
| Anonymous throttle | `storyline-viewer/.../security/AnonymousRateLimitFilter.java` |
| Anonymous act-1 trigger | `demo-solution/.../TourSimulationController.java` |
| Gated one-click triggers | `demo-solution/.../TourDemoController.java` |
| Step content | `demo-solution/src/main/resources/storyline/steps/step-NN.yaml` (`tour:` block) |
| Intro copy | `demo-solution/src/main/resources/storyline/index.yaml` (`tour:` block) |
| Info-step stills | `demo-solution/src/main/resources/static/images/tour/` |

## Running it

Beyond the normal `docker compose up` and `./gradlew :demo-solution:bootRun`:

- Export a Java 25 `JAVA_HOME` (see CLAUDE.md) and a valid `JOBRUNR_PRO_LICENSE`.
- The magic-link sign-in needs an SMTP sink on 1025:
  `docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit`, inbox at http://localhost:8025.
- **Editing `tour.peb`, `tour.css` or `tour.js` does not hot-reload.** `bootRun` puts `storyline-viewer`
  on the classpath as a jar and devtools only watches directories. Run `./gradlew :storyline-viewer:jar`
  and restart the app.

Add `?tourDebug=1` to any tour URL to log every navigation and anchor decision to the console and to
expose `window.jobrunrTour` (`open(n)`, `resolved()`, `data`).

For pure template, CSS or JS work there is `./gradlew :ui-runner:bootRun`: the viewer UI alone, with
security off and no license, database or Docker needed. It copies the storyline content and tour
stills out of demo-solution at build time (never symlink them: CI checkouts and Windows clones break).
The tour shell renders there too, but with no live dashboard behind it the beacons never anchor.

## Authoring a step

Everything is one `tour:` block appended to the step's existing YAML:

```yaml
tour:
  skip: true              # optional: this step sits the tour out (the written guide keeps it);
                          # the tour renumbers itself 1..n so there is never a hole
  narration: |            # 2 to 4 sentences, spoken voice, inline <code> is fine, no em-dashes.
                          # Voice: light and dry, one joke per step at most, never at the reader's
                          # expense. The middle sentence stays instructional (what to look at, what
                          # to press). Facts and numbers are never bent for a punchline.
    ...
  anchor:
    page: /dashboard/jobs?state=FAILED   # where the stage must be
    selector: "#failed-menu-btn"         # what the beacon points at
    fallbackSelector: "#jobs-table-container"
    reveal: "#job-filter-panel-header"   # optional: clicked first if the target is collapsed
    match: path                          # path (default) or prefix, for off-track detection
  placement: right        # auto (default), top, right, bottom, left
  mode: dashboard         # dashboard (default when dashboardUrl is a /dashboard path), app, info
  screenshot: /images/tour/step-19-prometheus.png   # info mode only
  historyView: chart      # optional: job pages open on the history Chart while this step is open
  liveNotice: "..."       # optional: replaces the guide's liveNotice on the live demo, "" drops it
  actions:
    - label: Generate 20 reports that sometimes fail
      url: /bulk-generate-summary-reports?count=20
      method: GET         # GET (default) or POST
      anonymous: false    # true only for the /simulate/* endpoints
      focus: "#job-history-chart-panel [role='group']"   # optional, see "Following a trigger" below
      placement: left     # optional popup side once the stage followed the trigger
```

Defaults fill in the rest, so a step with no `tour:` block still narrates (its `challenge`) and still
knows where to stand (its `dashboardUrl`). Bad values fail at startup with the step path in the message.

- `mode: app` adds an "Open JobRunr Finance" button that slides `tryItUrl` in from the right.
- `mode: info` drops the beacon entirely and shows a wide centered card. No step uses it since steps 19
  and 20 moved onto the v9 Job Analytics; their Prometheus and Jaeger stills are no longer referenced.
- `historyView` is written to the dashboard's `jobHistoryDisplayMode` on every step change: `chart` where
  the step sets it (steps 5 and 6), `timeline` everywhere else, because the other narrations were written
  for it (step 16's log lines live there). The Detailed analytics tab (`analyticsDataGridDetailedSelectedTab`)
  is reset to Job Signatures at the same moment, and before a trigger reloads the stage, because the step 20
  anchor is missing from the Exceptions tab.

### Following a trigger

A trigger may answer `{"ok": true, "message": "...", "stageUrl": "/dashboard/jobs/<id>"}`. The stage then
soft-navigates there (no history entry) and that page becomes the step's anchor until the visitor changes
step: off-track detection, "take me back" and reanchoring all use it. The beacon points at the action's
`focus`, the popup moves to its `placement`. Step 6 uses this to open the payment it just created on its
Chart. That payment carries the `transfer:times-out-once` label, so `HandleTransferToJobRunrFinance`
throws a `TransferTimeoutException` on its first attempt, after the card was charged; the retry then shows
`payment-processing` and `charge-card` as skipped.

## The mobile deck (`/m`)

`/m` renders the guide's fields, but a step can override them for phones with a `mobile:` block, without
touching `/storyline`:

```yaml
mobile:
  scenario: ...           # optional, like challenge, solution and tryIt: falls back to the guide
  dashboardUrl: /dashboard
  latestJob:              # optional dashboard API queries, tried in order; the card embeds the newest
    - states=SUCCEEDED&label=transfer%3Atimes-out-once     # job the first one finds, else dashboardUrl
  historyView: chart
```

`mobile.js` writes the same `jobHistoryDisplayMode`, analytics tab and analytics period presets as the tour
before it loads a card's iframe.
- Anything that writes to the bank is gated. Only `/simulate/*` may be `anonymous: true`; the gated
  one-click triggers live on `/demo/*` and are GETs, because the tour fetches without a CSRF token.

## Invariants worth not breaking

These are the non-obvious ones. Each was a bug before it was a rule.

1. **Never let the iframe push history.** React-router `Link` clicks `pushState`, and history entries
   made inside an iframe join the parent's session history, so the back button would walk every
   dashboard page the tour ever opened. `Stage.softNavigate()` uses `replaceState` plus a synthetic
   `popstate` instead. Every tour step must produce exactly one history entry, no more (21 while
   step 4 sits out). The post-trigger stage refresh uses `location.reload()` for the same reason.
2. **Being in the DOM is not being visible.** MUI leaves the contents of a collapsed accordion fully
   laid out inside a zero-height clip, so a bare `querySelector` anchors the beacon to thin air.
   `Stage.visible()` walks the ancestors and rejects anything inside such a clip. This is also what
   makes `reveal` fire.
3. **The popup never enters the pill's lane** (`PILL_LANE`), and it re-places itself whenever its
   content changes height (`Popup.reflow()`), including when an info-step screenshot finishes loading.
4. **Nothing sends the visitor to `/login`.** Gated triggers, the app panel and a 401/403/login-redirect
   response all morph the popup into the inline email form. That is the single biggest thing the tour
   fixes over `/storyline`.
5. **The embedded JSON escapes every `<`** so the blob cannot close its own `<script>` tag.
6. **Step 1 only works because jobs are slower than the queue drains.** `jobrunr.background-job-server.worker-count=8`
   plus the six seconds `CreditCardService.createNewCreditCard()` spends on its checks is what makes 25
   applications visibly wait in Enqueued. Raise the worker count or drop the delay and step 1 shows an
   empty list again, which is exactly what it did before.
7. **The dashboard hides the Labels column by default** and remembers that per job state in localStorage,
   which the tour shares with the iframe. `DashboardColumns.showLabels()` in `tour.js` forces `labels: true`
   at boot, keeping any other column the visitor chose. Step 9 is unreadable without it.
8. **The closing card asks inside the tour.** `POST /tour/trial` forwards to the same n8n webhook as the
   mobile demo with `form: trial-demo-tour`, so nobody has to leave for jobrunr.io to request a trial.

## After upgrading JobRunr Pro

The anchors are JobRunr Pro dashboard element ids. They survive minification but not necessarily a
version bump, so sweep them:

1. Start the app, open `http://localhost:8080/tour/step/1?tourDebug=1`.
2. In the console:
   ```js
   for (const s of jobrunrTour.data.steps) {
     await jobrunrTour.open(s.number);
     await new Promise(r => setTimeout(r, 1200));
     console.log(s.number, s.anchor?.selector ?? 'info', '->', jobrunrTour.resolved());
   }
   ```
3. Every step with an anchor must resolve to its own `selector`, never to `fallbackSelector` and
   never to nothing. `resolved()` prints the element id, or its tag name when it has none: step 19's
   Failure rate column header shows up as `div`, while its fallback would print its own id.

Note that a CSS id selector may not start with a digit: the High queue is `[id='0-queue-menu-btn']`.

## State of play

Built and verified (Aug 2026): all four phases of the original plan, all 22 steps, the hybrid auth
loop end to end against mailpit, the rate limiter, off-track snap-back, deep links, mobile redirect,
and real Prometheus and Jaeger screenshots for steps 19 and 20. `./gradlew build` is green.

Second pass (Aug 2026), after walking it with fresh eyes: the intro says JobRunr Pro before it says
neobank, step 1 makes its jobs slow enough to watch, step 2 asks you to open a scheduled job instead of
cancelling it, steps 10, 18 and 21 trigger their story with one click instead of "Open JobRunr Finance",
external jobs became step 18 (which pushed metrics, tracing, replacement and filters to 19 to 22), and
the closing card asks for the trial without leaving the tour.

Third pass (25 Aug 2026): a copy pass over every visitor-facing string, intro card, all
narrations, action labels, finale, sign-in and trial forms, toasts and the rate-limit message. Same
facts, lighter voice: the tour should feel like a fun walk through a bank, aimed at developers and
CTOs. The voice rules now sit next to the narration spec above.

Fourth pass (25 Aug 2026), after Nicholas walked it: the intro card lost a sentence, step 4 sits the
tour out via `tour: skip: true` (the guide keeps it, the tour renumbers to 21), every successful
trigger now reloads the stage on the step's own page so fresh jobs appear without a manual refresh,
step 6 talks durable execution and got a real one-click payment (`/demo/payment`, four `runStepOnce`
steps) instead of the vague app panel, step 21's code chip now focuses the `enqueueOrReplace` lines,
and the "Sign in here" escape hatch left the inline email form. Tracking was audited too: the tour
inherits Clarity, GA, LinkedIn Insight, HubSpot and the jobrunr-ping pixel from `base.peb`, and the
storyline never carried Reddit or LinkedIn conversion events, so there was nothing extra to port.

Three changes went slightly beyond the tour itself and are worth a second opinion:

- `SecurityModelEnricher` now hands templates a plain `Csrf` record. Pebble cannot reflect into Spring
  Security's package-private `CsrfToken`, so **`/login` threw for every real browser** before this. It
  is a pre-existing bug, not a tour bug, but the tour's sign-in depends on it.
- `server.error.include-message=always` in `demo-solution`, so the tour can surface "No active credit
  cards found…" verbatim instead of a generic failure. Exception messages here are business-level, but
  it is a global change.
- `GET /actuator/prometheus` and `/actuator/health` are permitted. Without them Prometheus could not
  scrape at all, so step 18's "run the demo locally to explore the metrics" was untrue. The rest of
  `/actuator` stays authenticated.

Fifth pass (28 Oct 2026), for JobRunr Pro v9: steps 19 and 20 left their screenshots for the live Job
Analytics (19 on the KPIs, 20 on Detailed analytics with a trigger for failing reports), steps 5 and 6
open job pages on the new history Chart, and step 6's payment times out once so the Chart shows durable
steps being skipped. The trigger opens that exact job. The mobile deck got `mobile:` overrides for steps
4, 5, 6, 19 and 20. The analytics period preset moved from the intro card to boot, so deep links get it
too. PaymentService gained a returned job id, extra labels and a cacheable job lambda (no more "Job details
not cacheable" banner), which shifted the code-chip line ranges of steps 6, 10, 11, 13, 14 and 15.

Open, deliberately not done:

- **Deep links to `/dashboard/analytics/<signature>` return 404** from the embedded dashboard: the dots and
  parentheses make Spring treat the path as a static resource. Client-side navigation works, so the tour
  never loads that URL directly. Worth reporting to JobRunr Pro.
- **The guide's step 6 `dashboardUrl` filters on `...payment.PaymentService`**, a package that does not
  exist (it is `payments`), so `/storyline` shows an empty list there. The mobile deck has its own URL.

- **The landing page and `/storyline` still do not link to `/tour`.** Pointing the CTA at it, or running
  a soft A/B banner, is a separate call.
- **Clarity comparison.** The tour fires `tour-started`, `tour-step-view`, `tour-action-run`,
  `tour-login-prompt`, `tour-login-sent`, `tour-offtrack`, `tour-offtrack-snapback`, `tour-code-opened`,
  `tour-app-opened`, `tour-finale-click` and `tour-completed`, and its `/tour/step/N` page views are
  directly comparable to the year of `/storyline/step/N` data. Nothing has been read back yet.
- **Production smoke test** on finance.demo.jobrunr.io (the `prd` profile flips steps 18 and 19 from
  "open it" links to their `liveNotice`).

## Verification checklist

Worth re-running after any substantial change:

```bash
# routes reachable anonymously
for u in / /storyline /storyline/step/12 /m /tour /tour/step/7; do curl -s -o /dev/null -w "$u %{http_code}\n" localhost:8080$u; done
# writes still gated
curl -s -o /dev/null -w "%{http_code} %{redirect_url}\n" localhost:8080/bulk-add-cards      # 302 -> /login
# throttle
for i in $(seq 1 8); do curl -s -o /dev/null -w "%{http_code} " -X POST localhost:8080/tour/trial -H 'Content-Type: application/json' -d '{"email":"x"}'; done   # six 400s then 429
# mobile
curl -s -o /dev/null -H 'User-Agent: iPhone Mobile' -w "%{http_code} %{redirect_url}\n" localhost:8080/tour               # 302 -> /m
```

In the browser: run the anchor sweep above, confirm every step adds exactly one history entry (21
today) and that Back walks them one by one, and check that no popup overlaps the pill at 1440x900.
