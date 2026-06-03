# PRIAM — PRIvacy Assessment Method

PRIAM is a metadata-driven microservice architecture that enforces GDPR privacy obligations — consent management, data subject rights, and breach handling — as a standalone layer that integrates non-intrusively with existing applications.

---

## Repository Structure

```
priam-microservice/
│
├── case-studies/                    # Target applications used to evaluate PRIAM
│   ├── TeaStore/                    #   Java · Spring Boot · e-commerce (reference)
│   ├── FastAPI-Healthcare/          #   Python · FastAPI · healthcare
│   ├── SportTracker/                #   Python · fitness & biometrics
│   └── Ghostfolio/                  #   TypeScript · NestJS · personal finance
│
├── PRIAM-Actor-service/             # Actor Management microservice (AM)
├── PRIAM-Consent-Service/           # Consent Management microservice (CM)
├── PRIAM-Data-service/              # Data & Processing Management microservice (DM)
├── PRIAM-Right-service/             # Right Management microservice (RM)
├── PRIAM-Breaches-Service/          # Breach Management microservice (BM)
├── PRIAM-Notification-Service/      # Notification microservice (N)
├── PRIAM-Gateway/                   # API Gateway
├── PRIAM-Eureka/                    # Service registry
├── Provider-microservice/           # Bridge to target application database
├── PRIAM-Frontend/                  # Angular UI for data subjects
├── PRIAM-Frontend-Provider/         # Angular UI for application owners
│
├── GatlingPRIAM/                    # Gatling load-test scenarios
├── monitoring/                      # Prometheus · Grafana · Jaeger
│
├── docker-compose.yml               # PRIAM microservices deployment
├── dot.env                          # Environment template (copy to .env)
└── build_images.sh                  # Build & push Docker images
```

---

## Architecture

### Overview

PRIAM is structured around three conceptual layers:

- **Core Engine** — the Data & Processing Management microservice, which holds the authoritative knowledge base (data metadata, processing metadata, actors) used by all other services.
- **Runtime Layer** — the operational microservices (Consent, Rights, Breach) that translate metadata declarations into real-time privacy enforcement.
- **Presentation Layer** — two Angular frontends, one for data subjects (end-users) and one for application owners (providers).

All microservices communicate through a central **API Gateway** and register with a **Eureka** service-discovery server. Asynchronous events (rights-request notifications, breach alerts) travel over a **Kafka** message bus.

```
                        ┌─────────────────────────────────────────────────┐
                        │                  PRIAM-MSA                      │
                        │                                                 │
  Data Subject  ──────► │  PRIAM Frontend (Angular)   :4200               │
  App Owner     ──────► │  Provider Frontend (Angular) :4000               │
                        │           │                                     │
                        │           ▼                                     │
                        │    API Gateway  :8090   ◄── Keycloak (OAuth2)   │
                        │           │                                     │
                        │    ┌──────┴──────────────────────────┐          │
                        │    │         Eureka Registry :8761   │          │
                        │    └──────┬──────────────────────────┘          │
                        │           │                                     │
                        │   ┌───────┼──────────────────────────────────┐  │
                        │   │       ▼  Core Engine                     │  │
                        │   │  Data & Processing (DM) :8081            │  │
                        │   │  Actor Management (AM)  :8082            │  │
                        │   └───────┬──────────────────────────────────┘  │
                        │           │                                     │
                        │   ┌───────┼──────────────────────────────────┐  │
                        │   │       ▼  Runtime Layer                   │  │
                        │   │  Consent Management (CM) :8089           │  │
                        │   │  Right Management (RM)   :8083           │  │
                        │   │  Breach Management (BM)  :8087           │  │
                        │   │  Notification Service (N) :8084          │  │
                        │   └───────┬──────────────────────────────────┘  │
                        │           │ Kafka :9092                         │
                        │           ▼                                     │
                        │  Provider Microservice :8086                    │
                        │  (bridge to the target application's DB)        │
                        └─────────────────────────────────────────────────┘
                                        │
                              ┌─────────▼──────────────────────────────────┐
                              │          Target Application (SUT)          │
                              │  any existing app with a REST API          │
                              │  e.g. TeaStore · SportTracker ·            │
                              │       FastAPI-Healthcare · Ghostfolio      │
                              │                   App DB                   │
                              └────────────────────────────────────────────┘
```

### Microservices

| Service | Module | Port | Role |
|---------|--------|------|------|
| **Eureka** | `PRIAM-Eureka` | 8761 | Service discovery — all microservices register here |
| **API Gateway** | `PRIAM-Gateway` | 8090 | Single entry point; validates Keycloak auth tokens and routes requests |
| **Actor Management (AM)** | `PRIAM-Actor-service` | 8082 | CRUD on actors: Data Subjects, DPO, Secondary Actors (processors, sub-processors) |
| **Data & Processing Mgmt (DM)** | `PRIAM-Data-service` | 8081 | Manages data and processing metadata; generates the ROPA (`GET /api/ropa`) and DPIA (`GET /api/dpia`) |
| **Consent Management (CM)** | `PRIAM-Consent-Service` | 8089 | Implements ABAC consent enforcement (CAP / CIP / CDP); stores consent history as an event-sourced audit trail |
| **Right Management (RM)** | `PRIAM-Right-service` | 8083 | Handles access, rectification and erasure requests; enforces the 1-month GDPR deadline with daily alerts |
| **Breach Management (BM)** | `PRIAM-Breaches-Service` | 8087 | Records and tracks personal data breaches, consequences and mitigation measures |
| **Notification (N)** | `PRIAM-Notification-Service` | 8084 | Kafka-based async notifications (email) triggered by RM and BM events |
| **Provider Microservice** | `Provider-microservice` | 8086 | Bridge between PRIAM and the target application; exposes `/api/rectification`, `/api/erasure`, `/api/dataAccessRight` |

### Consent Enforcement — ABAC Pattern

Consent enforcement follows the ABAC (Attribute-Based Access Control) architecture pattern, implemented across the Consent microservice and the API Gateway:

```
Data Subject ──► CAP (Consent Administration Point)   records preferences in CDB
Provider App ──► CEP (Consent Enforcement Point)       intercepts processing calls (via Gateway)
                     │
                     ▼
                 CDP (Consent Decision Point)           evaluates consent attributes
                     │
                     ▼
                 CIP (Consent Information Point)        returns up-to-date consent attributes from CDB
```

- **CAP** — end-users give or withdraw consent through the PRIAM frontend; preferences are stored in the Consent DB.
- **CIP** — provides the CDP with the current consent attributes (referenceId, processingId, start/end dates).
- **CDP** — decides whether a processing is authorised for a given user. Exposed at `GET /cdp/...` through the Gateway.
- **CEP** — integrated in the target application (and the Gateway): before executing an optional processing, the application calls the CDP and skips execution if consent is absent or revoked.

### Deadline Enforcement — GDPR Art. 12.3

The Right Management service runs a daily scheduled job (`DeadlineScheduler`) that scans all pending rights requests. Any request that has been unanswered for more than **23 days** (7 days before the 1-month legal deadline) triggers an alert notification to the application owner via the Notification service.

### Database-per-Service

Each microservice owns its dedicated MySQL schema, ensuring strict bounded-context isolation:

| Service | Schema |
|---------|--------|
| Actor | `priam-actor` |
| Data | `priam-data` |
| Consent | `priam-consent` |
| Right + Provider | `priam-right` |
| Breach | `priam-breach` |

All schemas are pre-initialised by the `priam-databases` Docker image.

### Observability Stack

| Tool | Port | Purpose |
|------|------|---------|
| Prometheus | 9090 | Metrics scraping (Spring Actuator `/actuator/prometheus`) |
| Grafana | 3000 | Dashboards (cAdvisor container metrics) |
| cAdvisor | 8088 | Container-level CPU / memory / network telemetry |
| Jaeger | 16686 | Distributed tracing via OpenTelemetry (OTLP → gRPC) |

---

## Case Studies

The `case-studies/` folder contains four target applications used to evaluate PRIAM. They were chosen to cover different domains, technology stacks, and architectural styles, demonstrating that PRIAM integrates with minimal effort regardless of the host application.

| Application | Domain | Stack | Architecture | Personal data | Art. 9? |
|-------------|--------|-------|--------------|---------------|---------|
| **TeaStore** | E-commerce | Java · Spring Boot · MySQL | Microservices | User accounts, orders, browsing history | No |
| **SportTracker** | Fitness & activity tracking | Python · PostgreSQL | Monolithic web app | User profiles, geolocation (GPX), heart-rate biometrics | Yes — biometrics |
| **FastAPI-Healthcare** | Healthcare management | Python · FastAPI · PostgreSQL | Microservices-adjacent | Patient records, diagnoses, appointments, credentials | Yes — health data |
| **Ghostfolio** | Personal finance | TypeScript · NestJS · PostgreSQL | Modular monolith (Nx) | Investment portfolio, full transaction history, net worth, account credentials | No (financially sensitive) |

### Repository layout

```
case-studies/
├── TeaStore/
│   ├── docker-compose.yml               # PRIAMed TeaStore (reference deployment)
│   ├── docker-compose_withoutPRIAM.yml  # Baseline (no PRIAM) for performance comparison
│   └── source/                          # Full TeaStore source with PRIAM modifications
├── FastAPI-Healthcare/
│   ├── docker-compose.yml               # Standalone deployment
│   └── app/                             # Python/FastAPI source
├── SportTracker/
│   ├── docker-compose.yaml              # Standalone deployment
│   └── sporttracker/                    # Python source
└── Ghostfolio/
    ├── docker/
    │   └── docker-compose.yml           # Standalone deployment
    └── apps/                            # NestJS + Angular source (Nx monorepo)
```

### Integration points common to all case studies

Integrating PRIAM into any of these applications requires only three targeted changes:

1. **Provider endpoints** — add three REST endpoints to the application's back-end (or as a thin sidecar) so PRIAM's Right Management service can propagate approved rights requests:

   | Endpoint | Method | Purpose |
   |----------|--------|---------|
   | `/api/dataAccessRight` | `GET` | Return the current value(s) of requested personal data attributes |
   | `/api/rectification` | `POST` | Update a personal data attribute with the approved new value |
   | `/api/erasure` | `POST` | Delete or nullify a personal data attribute |

2. **Consent check** — wrap any *optional* processing call with a `getConsent(userId, processingId)` guard that queries PRIAM's Consent Decision Point before execution (see TeaStore's `RecommenderSelector.java` for the reference implementation).

3. **Authentication bridge** — pass the application's existing session token to Keycloak so PRIAM's API Gateway can validate identity without requiring users to authenticate a second time.

### TeaStore — reference integration

TeaStore is the primary case study: it is fully integrated, dockerised, and the subject of all Gatling load tests in this repository. Its source code (including the PRIAM-specific modifications) is located in:

```
case-studies/TeaStore/source/
```

The consent enforcement modification is in [`RecommenderSelector.java`](case-studies/TeaStore/source/services/tools.descartes.teastore.recommender/src/main/java/tools/descartes/teastore/recommender/algorithm/RecommenderSelector.java) — the `getConsent()` guard added around the recommendation call is the minimal code change required to enforce consent for an optional processing.

### SportTracker

A self-hosted fitness tracking application. It processes biometric data (heart rate) and precise geolocation (GPX tracks), both of which are GDPR Art. 9 / Art. 4(1) sensitive categories. Its monolithic architecture demonstrates that PRIAM is not restricted to microservice hosts. Source: `case-studies/SportTracker/`.

### FastAPI-Healthcare

A healthcare management REST API. It handles patient medical records, diagnoses, and appointment history — the strongest possible GDPR use case (Art. 9 health data). Its FastAPI/Python stack demonstrates PRIAM's cross-language applicability. Source: `case-studies/FastAPI-Healthcare/`.

### Ghostfolio

An open-source personal wealth and portfolio management platform (8 600+ GitHub stars). It processes investment holdings, full transaction history, and net worth — financially sensitive personal data with strict retention and portability obligations under GDPR Art. 17 and Art. 20. Its NestJS/PostgreSQL back-end maps cleanly onto PRIAM's Provider microservice. Source: `case-studies/Ghostfolio/`.

---

## Prerequisites

- **Docker** ≥ 24 and **Docker Compose** ≥ 2
- **Maven** ≥ 3.8 (only for running Gatling load tests locally)
- A running **Keycloak** instance reachable at `http://localhost:8080` (or update `CUSTOM_KEYCLOAK_URL` in `.env`)
- A Docker network named `common_network` shared with the TeaStore stack — create it once with `docker network create common_network`

---

## Configuration

Two `.env` files must be present before starting any stack.

### 1 — Root `.env` (PRIAM services)

```bash
cp dot.env .env
```

Key variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | `priamu` | MySQL user for all PRIAM schemas |
| `DB_PASSWORD` | `MaiRP_pWd-UsEr` | MySQL password |
| `EUREKA_URL` | `http://eureka:8761` | Internal Eureka address |
| `CUSTOM_KEYCLOAK_URL` | `http://localhost:8080` | Keycloak base URL (external) |
| `CUSTOM_CDP_URL` | `http://consent:8089` | Consent Decision Point URL |
| `JAEGER_ENDPOINT` | `http://jaeger:4317` | OTLP collector gRPC endpoint |
| `NOTIFICATION_APP_OWNER_EMAIL` | `owner@example.com` | Email for rights-deadline alerts |

### 2 — Gatling `.env`

```bash
cp GatlingPRIAM/dot.env GatlingPRIAM/.env
```

---

## Starting PRIAM

Services must be started in order: monitoring first, then the target application, then PRIAM.

### Step 1 — Create the shared Docker network

```bash
docker network create common_network
```

> Skip if it already exists.

### Step 2 — Start the observability stack

```bash
cd monitoring && docker compose up -d && cd ..
```

Grafana: http://localhost:3000 (default credentials: `admin` / `admin`) — open the **cAdvisor Dashboard** to monitor container metrics.

### Step 3 — Start the target application

The examples below use TeaStore (the reference case study). Substitute the appropriate `docker-compose` path for any other case study.

```bash
# TeaStore (reference)
docker compose -f case-studies/TeaStore/docker-compose.yml up -d

# SportTracker
docker compose -f case-studies/SportTracker/docker-compose.yaml up -d

# FastAPI-Healthcare
docker compose -f case-studies/FastAPI-Healthcare/docker-compose.yml up -d

# Ghostfolio
docker compose -f case-studies/Ghostfolio/docker/docker-compose.yml up -d
```

TeaStore WebUI: http://localhost:8180

> **Distributed tracing** — the TeaStore and PRIAM compose files read `JAEGER_ENDPOINT` from the environment. Set it before starting if Jaeger is running on a non-default host:
> ```bash
> export JAEGER_ENDPOINT=http://<jaeger-host>:4317
> ```

### Step 4 — Start PRIAM microservices

```bash
docker compose up -d
```

This starts (in dependency order):

1. MySQL (`priam-databases`) — waits for healthy state
2. Eureka (`priam-eureka`) — service registry
3. Actor, Data, Consent, Right, Provider — core and runtime microservices
4. Breach (`priam-breach-ms`) — breach management
5. Notification (`priam-notification-ms`) — Kafka + Zookeeper
6. API Gateway (`priam-api-gateway`) — depends on all microservices

### Step 5 — Verify the deployment

| Service | Health check URL |
|---------|-----------------|
| Eureka dashboard | http://localhost:8761 |
| API Gateway | http://localhost:8090/health |
| Actor MS | http://localhost:8082 |
| Data MS | http://localhost:8081 |
| Consent MS | http://localhost:8089 |
| Right MS | http://localhost:8083 |
| Provider MS | http://localhost:8086 |
| Breach MS | http://localhost:8087 |
| Notification MS | http://localhost:8084 |
| ROPA report | http://localhost:8090/data/api/ropa |
| DPIA report | http://localhost:8090/data/api/dpia |

All microservices should appear as **UP** in the Eureka dashboard before proceeding.

---

## Running the Gatling Load Tests

From the `GatlingPRIAM` directory:

```bash
cd GatlingPRIAM
```

### Rights scenarios (functional correctness)

```bash
# Right of access
mvn gatling:test -Dgatling.simulationClass=AccessRequestSimulation

# Right to rectification
mvn gatling:test -Dgatling.simulationClass=RectificationRequestSimulation

# Right to erasure
mvn gatling:test -Dgatling.simulationClass=ErasureRequestSimulation
```

### Consent management scenarios (performance)

```bash
# Flash-crowd performance test (non-PRIAMed vs PRIAMed TeaStore)
mvn gatling:test -Dgatling.simulationClass=TeaStoreCartPagePerformance

# Scalability test (ramp-up → plateau → ramp-down)
mvn gatling:test -Dgatling.simulationClass=TeaStoreCartPageScalability

# Withdraw consent mid-session and verify enforcement
mvn gatling:test -Dgatling.simulationClass=WithdrawalOfConsent
```

---

## Stopping and Cleaning Up

```bash
# Stop PRIAM services
docker compose down

# Stop the target application (example: TeaStore)
docker compose -f case-studies/TeaStore/docker-compose.yml down

# Stop monitoring
cd monitoring && docker compose down && cd ..
```

To wipe all stopped containers in one shot:

```bash
docker stop $(docker ps -aq) && docker rm $(docker ps -aq)
```

---

## Building Images Locally

To build and push all PRIAM images from source (requires Docker login to the GitLab registry):

```bash
./build_images.sh
```

To build only Docker images without pushing:

```bash
./build_docker_images.sh
```
