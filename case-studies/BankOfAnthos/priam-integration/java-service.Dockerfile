# Local-dev-only Dockerfile for the 3 Java ledger services (ledgerwriter,
# balancereader, transactionhistory). BankOfAnthos ships no Dockerfile for
# these upstream (built via Jib/skaffold against a real GKE cluster instead,
# see docs/development.md) - this is a plain multi-stage Maven build added
# for this PRIAM integration session so the services can run under
# docker-compose. Not part of upstream BankOfAnthos.
#
# Build context must be the repo root (case-studies/BankOfAnthos/), because
# the root pom.xml is a multi-module reactor aggregating all 3 services.
# Select the module to build with --build-arg MODULE=src/ledger/<service>.

FROM maven:3.9.9-eclipse-temurin-21 AS builder
ARG MODULE
WORKDIR /build
COPY pom.xml .
COPY src/ledger/ledgerwriter src/ledger/ledgerwriter
COPY src/ledger/balancereader src/ledger/balancereader
COPY src/ledger/transactionhistory src/ledger/transactionhistory
COPY src/ledgermonolith src/ledgermonolith
RUN mvn -pl ${MODULE} -am package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
ARG MODULE
WORKDIR /app
COPY --from=builder /build/${MODULE}/target/*.jar app.jar
CMD ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
