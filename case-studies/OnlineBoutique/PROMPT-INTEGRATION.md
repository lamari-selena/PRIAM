You are an experienced developer: on this project, that means reading the real code
before acting, never assuming something works without testing it against real state, and
honestly documenting the limitations and trade-offs you encounter rather than glossing
over them.

You are going to integrate PRIAM (a GDPR rights/consent platform, Spring Boot
microservices + 2 Angular frontends) into **OnlineBoutique**, whose code lives in
`case-studies/OnlineBoutique/src`. PRIAM itself lives at the root of this repository/folder.

**Before writing a single line of code:**

1. Read `Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §0 through §7 in full (the
   contract: SQL annotation, Provider bridge, rights workflow, consent,
   registration/forced consent, Docker network, auth, test methodology). This
   is the stable specification, it does not change from one case study to the
   next.
2. §8 of that same file is a catalog of pitfalls still actionable on the
   target-application side (SQL annotation, and the two cases that need action
   on both sides) — organized by group with an index at the top of the section,
   **not meant to be read linearly**. If a symptom you observe matches an
   existing entry, apply the already-documented fix instead of rediscovering
   it through debugging.
3. Open `OnlineBoutique`'s real code (ORM models, existing routes) — never invent
   a schema or an endpoint, verify it in the source code.
4. If you see `Docs/SESSION-HANDOFF-*.md` or any file in `Docs/` documenting a
   session for a case study **other than** yours, ignore it — it is not
   generic, and it should not be in `Docs/` in the first place (flag it if you
   find it there).

**What you must deliver**, in this order:

1. `Databases/db_insertion_script.sql` — annotation of `OnlineBoutique`'s real
   schema (§1). Pay special attention to: `processing_type`/`purpose_type` in
   exact UPPERCASE (§8.1.a), `is_primary_key=1` on every table with several
   rows per subject, a `processed_data` row for every pre-granted consent
   (§8.1.b).
2. The 4 Provider bridge endpoints on the `OnlineBoutique` side (§2) —
   `dataAccessRight`, `rectification`, `erasure`, `dataValue` (the latter easy
   to forget: absent from the Right-service DTOs, called only by
   `PRIAM-Frontend-Provider`, with no `dataTypeName` in its body). Bare `/api`,
   no auth, `dataAccessRight` always returning a JSON array, `attributes` as a
   single comma-separated string.
3. The CEP `get_consent()` (§4) on at least one real `OPTIONAL` processing
   activity of the application — fail-open if `PRIAM_CDP_URL` is absent,
   fail-closed otherwise.
4. `register_data_subject()` wired at **every** user-creation point of the
   application, and `has_pending_consent_decision()` + a client-side redirect
   to PRIAM's consent page (§4bis). **If the application also reports
   "processed" data right at sign-up (e.g. a default account created on
   signup), explicitly wait for `register_data_subject` to finish before any
   call that resolves `idRef → dataSubjectId` internally — otherwise a race
   condition, see §4bis and §8.6.**
5. **Bidirectional, persistent navigation between `OnlineBoutique` and PRIAM** —
   not just the forced sign-up redirect (point 4): a visible button/link in
   `OnlineBoutique` (e.g. in account settings) letting an already-registered user
   go back to PRIAM at any time to manage their data ("Manage on PRIAM",
   §4ter); and, in the other direction, PRIAM-Frontend's "back to the app"
   link (driven by `TARGET_APP_URL`, §4ter) must point to a real, working page
   of `OnlineBoutique`, not just its default root URL.
6. **`report_processed_data()` wired at every point where a personal record is
   created** (§4bis) — not just sign-up: any later creation of a record of a
   `data_type` with several rows per subject (a task, an order, an
   appointment...) must also report its `data_id` values. **This is the most
   frequently forgotten point of the entire integration** (§4bis): without it,
   the Access Request page stays empty for any dynamically registered subject,
   no matter how careful the rest of the work is.
7. A one-off backfill script for already-existing users (§4bis, last point),
   if there are any.
8. Docker wiring (`common_network`, `PRIAM_CDP_URL`/`PRIAM_ACTOR_URL`,
   `CUSTOM_PROVIDER_URL` pointing directly at `OnlineBoutique`) — §5. **If another
   PRIAM checkout is already running on this machine, check that there is no
   `name:`/`container_name:` Docker Compose collision before starting
   anything** (§5, a documented pitfall — two checkouts with the same project
   identity silently step on each other).
9. If a Keycloak IdP is wired up (§6) **and** `OnlineBoutique` has its own local
   sign-up (email/password): automatic Keycloak provisioning at sign-up
   (§4bis, "Automatic Keycloak identity provisioning") — otherwise the "Manage
   on PRIAM" link (point 5, §4ter) leads to a Keycloak identity unrelated to
   the user's real account. Covers local sign-up only (no password to
   synchronize for a sign-up through a social provider) — document it as a
   known limitation in the final report rather than silently ignoring it.

**Non-negotiable constraints:**

- **PRIAM stays generic: 0 lines of code changed in the PRIAM
  microservices/frontends for this case study**, unless you discover a real
  generic PRIAM bug (not specific to `OnlineBoutique`) through a real test — only
  in that case, fix it and add an entry to the matching existing thematic
  group, in `Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §8 — do not invent a new
  numbered section of your own.
- **Minimize the number of lines added on the `OnlineBoutique` side** — reuse the
  patterns already documented (§4bis, a global singleton/module to avoid
  repeated wiring), do not build an abstraction or config that was not asked
  for.
- **Never test only by reading code or calling the Provider endpoints
  directly.** The real workflow goes through `PRIAM-Right-service` (§3) — a
  cycle with `answer=false` AND `answer=true`, proof of real database state at
  every step (§7), not just an HTTP 200.
- **Test at least once from a real browser**, with a **non-numeric** `idRef`
  (a UUID or a free-form string, not an auto-incremented integer) — several
  bugs in §8 only show up in this exact case (a numeric coincidence that
  masks the bug on a simple `idRef`).
- **If you hit unexpected behavior and Docker is running**, look at the real
  logs (`docker logs <service>`) and the real database state **before**
  concluding anything from reading the code alone — several bugs in this
  project are only visible under real traffic (CORS in the browser but not in
  curl, token expiry after 5 minutes, races between two fire-and-forget
  calls).

**Final deliverable — two files, not one:**

1. `case-studies/OnlineBoutique/priam-integration/INTEGRATION-REPORT.md` — the
   mechanism explained in one page, a table of bugs found during this session
   (root cause → fix → proof of verification), a table of workflows verified
   against real state, and the LOC breakdown below. If you fixed a generic
   PRIAM bug, make sure you also documented it in
   `Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §8 (see the constraints above).
2. `case-studies/OnlineBoutique/priam-integration/ETAPES-FAITES.md` — the raw
   detail of **every test actually run** (access, rectification, erasure,
   consent granted/withdrawn/re-granted), not just the summary already in the
   report above. Start with a short reference of the URLs/ports actually used
   in this integration (Gateway `:8090` and its `/right`, `/cdp`, `/actor`,
   `/data`, `/provider` prefixes; the Provider bridge port on the `OnlineBoutique`
   side; PRIAM frontend ports if wired up) — so "where is such-and-such API"
   does not have to be reconstructed by re-reading the whole file. Then, for
   each test: the endpoint/URL called (method, request body), the HTTP
   response obtained, and above all **the real state observed in the database
   afterward** (§7 — the real proof, not the 200 code). Must be detailed
   enough that a human or an AI with no prior context could reproduce each
   test identically (e.g. complete `curl` commands, not just "I tested
   rectification and it worked"). This is the file `Docs/PRIAM-AUTH-OIDC.md`
   already cites as an example (an appendix of curl commands for
   authentication tests) — write it for the auth part too if you wire one up,
   not just for rights/consent.

**LOC breakdown — not a single aggregated total.** A `File | Status
(new/modified) | +lines | -lines` table per file (as usual), **plus** a
summary table crossing two axes:

| Axis | Detail |
|---|---|
| **By functional category** | Annotation (the SQL script) / Rights-API (the 4 Provider bridge endpoints + any code tied to the §3 workflow) / Consent (CEP + `register_data_subject`/`has_pending_consent_decision`/`report_processed_data` + backfill + bidirectional app↔PRIAM navigation) / OAuth2 (Keycloak provisioning, auth-related `docker-compose.yml`/`.env` wiring) / Docker-network (the rest of the `docker-compose.yml`/`.env` wiring) |
| **By line nature** | Executable code vs. comments vs. blank lines — for **each** category above, not just one overall total |

One line = counted in a single functional category (wherever it physically
lives, not wherever it is "conceptually related" if a file mixes several
roles). State the method used to distinguish code/comment/blank (manual
count, a script, etc.) — no need for a sophisticated tool, just be honest
about the method, and do not black-box a plain `git diff --numstat` as if it
already gave you this breakdown (it does not — it only counts raw +/- lines
per file).

Do the same for PRIAM itself (for its own LOC).
