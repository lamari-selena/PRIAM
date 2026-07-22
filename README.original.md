# PRIAM + Gatling + TeaStore + Monitoring Integration Guide

## Step 1 — Add `.env` Files

Three `.env` files are required:

- `./GatlingPRIAM/.env`
- `./TeaStore app/Example-PRIAM-Teastore/examples/docker/.env`
- `./.env` (at the root of the PRIAM project -- rename the file dot.env which is in the repo to .env)

---

## Step 2 — Start Required Services

### 2.1 — Start Monitoring

```bash
cd monitoring
docker compose up -d 
```

### 2.2 — Start TeaStore with PRIAM

```bash
cd ..
```

```bash
cd teastore
```


```bash
docker compose -f "TeaStore app/Example-PRIAM-Teastore/examples/docker/docker-compose.yml" up -d
```

### 2.3 — Start PRIAM Microservices

```bash
docker compose up -d
```

---

## Step 3 — Run Gatling Tests

From the `GatlingPRIAM` folder:

### 3.1 — "Rights" Tests with 1000 Users

```bash
mvn gatling:test -Dgatling.simulationClass=AccessRequestSimulation
```

```bash
mvn gatling:test -Dgatling.simulationClass=ErasureRequestSimulation
```

```bash
mvn gatling:test -Dgatling.simulationClass=RectificationRequestSimulation
```
### 3.2 — Consent Management Test (1000 users)

```bash
mvn gatling:test -Dgatling.simulationClass=TeaStoreCartPagePerformance
```

### 3.3 — Load Scalability Test

Phases:
- Ramp-up
- Plateau (high-load stability)
- Ramp-down

```bash
mvn gatling:test -Dgatling.simulationClass=TeaStoreCartPageScalability
```

### 3.4 — Withdraw Consent for a User

```bash
mvn gatling:test -Dgatling.simulationClass=WithdrawalOfConsent
```

### 3.5 — Re-execute Load Test

```bash
mvn gatling:test -Dgatling.simulationClass=TeaStoreCartPageScalability
```

---

## Step 4 — Clean Docker Environment

```bash
docker stop $(docker ps -aq)
docker rm $(docker ps -aq)
```

---

## Step 5 — Restart Monitoring and TeaStore without PRIAM

```bash
docker compose -f ./examples/docker/docker-compose_withoutPRIAM.yml up -d
```

---

## Step 6 — Monitoring Tools Access

| Tool       | Address                        |
|------------|--------------------------------|
| Grafana    | http://localhost:3000          |
| Prometheus | http://localhost:9090          |
| cAdvisor   | http://localhost:8088          |
| Jaeger     | http://localhost:16686         |
-----------------------------------------------

### Accessing Grafana

Once logged into Grafana (default credentials: `admin` / `admin`), navigate to the **Cadvisor Dashboard** to monitor container metrics and performance.

