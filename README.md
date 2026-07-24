# Generic PRIAM

A standalone, self-contained copy of PRIAM (a GDPR rights/consent platform:
Spring Boot microservices + 2 Angular frontends) with **zero dependency on
any specific target application or case study**. Extracted from a larger
multi-case-study repository so it can be run, read, and reasoned about
entirely on its own.

Everything here is generic by construction: the microservices, the database
schema, the Keycloak realm, and both documentation files describe patterns
and pitfalls in the abstract (e.g. "a target application with its own local
sign-up"), never a named product. If you find a sentence that names a real
application anywhere in this folder, that is a bug in the extraction —
please flag/fix it.

## What's in here

| Path | What it is |
|---|---|
| `PRIAM-Services/` | The 9 Spring Boot microservices (`PRIAM-Actor-service`, `PRIAM-Consent-Service`, `PRIAM-Data-service`, `PRIAM-Right-service`, `Provider-microservice`, `PRIAM-Gateway`, `PRIAM-Eureka`, `PRIAM-Notification-Service`, `PRIAM-Breaches-Service`) plus the 2 Angular frontends (`PRIAM-Frontend`, `PRIAM-Frontend-Provider`). Unmodified source. |
| `Databases/` | `db_creation_script.sql` (the generic schema — actor/data/right/consent/breach) + Docker build for the `mysqldb` service. `db_insertion_script.sql` is an **intentionally empty template** — see below. |
| `Keycloak/priam-realm-realm.json` | A minimal, generic realm: the `Data-client`/`Provider-client` OIDC clients PRIAM's own two frontends use, one demo user per client, no application-specific accounts. |
| `docker-compose.yml` | The full stack, project name `priam`. |
| `.env.example` | Every environment variable PRIAM reads, with generic/safe defaults and empty placeholders for anything that must be filled in once you integrate a real target application. |
| `Docs/PRIAM-INTEGRATION-PLAYBOOK.md` | The full integration contract (SQL annotation, Provider bridge, rights workflow, consent, registration/forced consent, Docker network, auth, test methodology) and a catalog of known pitfalls — every pitfall's *rule* and *fix* are kept, only the "which case study found this" attribution was stripped. |
| `Docs/PRIAM-AUTH-OIDC.md` | The generic OIDC wiring guide (Gateway resource server + both frontends as OIDC clients), same treatment. |
| `Docs/PROMPT-NOUVELLE-INTEGRATION.md` | A copy-paste-ready prompt template (placeholders `<APP_NAME>`/`<APP_PATH>`) for briefing an LLM/agent to integrate a new target application, reusing this playbook's contract. |

## Running it standalone

```bash
cp .env.example .env
docker network create common_network   # one-time prerequisite, see Docs/PRIAM-INTEGRATION-PLAYBOOK.md §5
docker compose up -d
```

This brings up the full stack with **zero annotated data** — every service
starts healthy, but `data_subject`/`data`/`processing` are all empty (see
`Databases/db_insertion_script.sql`'s header comment). This is a valid,
safe starting point to verify the stack itself works, or as the base to
annotate a real target application on top of.

- Gateway: `http://localhost:8090` (`/right`, `/cdp`, `/actor`, `/data`, `/provider` prefixes)
- PRIAM-Frontend (data subject UI): `http://localhost:4200`
- PRIAM-Frontend-Provider (data controller UI): `http://localhost:4000`
- Keycloak: `http://localhost:8080` (admin/admin), demo users `priam-demo-subject`/`PriamTest123!` and `priam-demo-controller`/`OwnerPass123!`

**⚠️ If another PRIAM checkout (this one or the source repository) is
already running on the same machine**, check for a Docker Compose project
name / `container_name:` collision before starting this one — see
`Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §5. Every service here uses the same
fixed `container_name:` values as the source repository's own root
`docker-compose.yml` (`priam-actor-ms`, `priam-databases`, etc.) — the two
cannot run at the same time without renaming one of them.

## Integrating a real target application

1. Read `Docs/PRIAM-INTEGRATION-PLAYBOOK.md` in full (§0-§7 is the stable
   contract; §8 is a pitfall catalog, read its index first, not linearly).
2. Read `Docs/PRIAM-AUTH-OIDC.md` if you're wiring up authentication.
3. Use `Docs/PROMPT-NOUVELLE-INTEGRATION.md` as a ready-made briefing for an
   LLM/agent doing the integration, or follow it yourself step by step.
4. Replace `Databases/db_insertion_script.sql` with a real annotation of
   your target application's schema (§1).
5. Point `CUSTOM_PROVIDER_URL` (`.env`) directly at your target
   application's own Provider bridge implementation (§2), and attach its
   own `docker-compose.yml` to `common_network` (§5).

## What was deliberately left out of this extraction

- Any `case-studies/` content, or code belonging to a target application.
- `db_insertion_script.sql`'s real content (per-target-application, see
  above) — replaced with an empty, documented template.
- Keycloak realm users/clients specific to a particular target application
  — only the two generic PRIAM-frontend clients and one demo user each
  remain.
- Any `Docs/SESSION-HANDOFF-*.md`-style file documenting a specific past
  session — those are explicitly out of scope for a generic package (see
  `Docs/PROMPT-NOUVELLE-INTEGRATION.md` point 4).

## Provenance

Extracted from a larger repository containing this same PRIAM codebase
integrated against several target applications, each in its own
`case-studies/<name>/` folder. Files were copied via `git ls-files` (source
only, no build artifacts/`node_modules`/`target`), then the two Docs files
and the Keycloak realm were edited by hand to remove every reference to a
specific target application while preserving the underlying technical
content (the rule, the symptom, the fix). This folder is a snapshot, not a
live subtree — it does not stay in sync with the source repository
automatically.
