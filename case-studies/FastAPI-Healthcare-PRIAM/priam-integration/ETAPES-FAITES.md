# Étapes faites — Intégration PRIAM dans FastAPI-Healthcare-PRIAM

Suivi précis des modifications apportées à ce clone (dépôt d'origine :
https://github.com/devalentineomonya/distributed-healthcare-system), dans le cadre de l'intégration
PRIAM (RGPD : droits d'accès/rectification/suppression, gestion du consentement pour un traitement
optionnel).

Convention : chaque ligne = un fichier touché lors d'une étape. LOC = lignes de code effectivement
ajoutées/supprimées (hors lignes de contexte inchangées).

---

## Étape 1 — Seed de données de test (2026-07-09)

**But** : la base Postgres de ce clone est vierge (aucune donnée). Sans enregistrement réel, le test
des droits (accès/rectification/suppression) ne peut rien trouver côté application.

| Action | Fichier | Lignes totales | LOC ajoutées | LOC supprimées |
|---|---|---|---|---|
| Ajout (nouveau fichier) | `app/db/seed.py` | 68 | 68 | 0 |
| Modification | `app/main.py` | 104 → 114 | 11 | 1 |

**Détail de `app/db/seed.py` (nouveau fichier, 68 lignes)** :
Fonction `seed_demo_data(db: Session)` qui, si la table `patients` est vide, crée :
- 1 patient (`Jane Doe`, id attendu = 1 sur une base fraîche)
- 1 médecin (`Alice Martin`)
- 1 rendez-vous reliant les deux
- 1 dossier médical relié au patient et au rendez-vous

No-op si un patient existe déjà (sûr à chaque redémarrage du conteneur).

**Détail de `app/main.py` (modifié, +11/-1 lignes)** :
- Ligne 1 : ajout de `import os`.
- Ligne ~10 : `from app.db.session import engine, get_db` → `from app.db.session import engine, get_db, SessionLocal` (import de `SessionLocal` en plus).
- Ligne ~11 : ajout de `from app.db.seed import seed_demo_data`.
- Lignes ~17-22 (après `models.Base.metadata.create_all(bind=engine)`) : ajout d'un bloc qui appelle
  `seed_demo_data()` via une session ouverte/fermée proprement, conditionné par la variable d'env
  `SEED_DEMO_DATA` (`"true"` par défaut, permet de désactiver le seed si besoin).

**Résultat** : sur une base fraîche, le patient créé aura `id=1` (auto-increment Postgres) — utilisé
comme `idRef` dans `Databases/db_insertion_script.sql` (racine du dépôt PRIAM) pour le data_subject
FastAPI-Healthcare.

**Pas encore fait** (prochaines étapes à tracer ici) :
- Les 3 endpoints Provider PRIAM (`/api/priam/dataAccessRight`, `/rectification`, `/erasure`) —
  n'existent pas encore dans ce clone (aucun dossier `app/priam/` pour l'instant).
- Le garde de consentement (`appointment-notifications`) sur les endpoints de rendez-vous.
- Le raccordement réseau Docker (`common_network`) pour joindre PRIAM.

---

## Étape 1bis — Correctif générique PRIAM : résolution de traitement par nom (2026-07-09)

**But** : contrainte de l'utilisatrice — ne jamais modifier le code de PRIAM, sauf si la modification
garde PRIAM générique (le but étant justement de prouver sa généricité, pas de la casser avec du
code spécifique à une application). Le mécanisme existant
(`PRIAM-Consent-Service/.../ContractServiceImpl.getConsentByDataSubject`) traduisait `processingId`
via 2 lignes codées en dur, pour seulement 2 anciens noms de traitement (commentées `// A supprimer`
par l'auteur d'origine lui-même) — inutilisable pour un nouveau nom de traitement comme
`appointment-notifications`.

| Action | Fichier | Quoi | Pourquoi |
|---|---|---|---|
| Modification | `PRIAM-Data-service/.../repositories/ProcessingRepository.java` | `+ findByProcessingName(String)` | Requête dérivée Spring Data pour chercher un Processing par son nom |
| Modification | `PRIAM-Data-service/.../services/ProcessingServiceInterface.java` | `+ getProcessingIdByName(String)` (déclaration) | Exposer la résolution nom → id au niveau service |
| Modification | `PRIAM-Data-service/.../services/ProcessingService.java` | Implémentation de `getProcessingIdByName` | Utilise le repository, lève une exception claire si nom introuvable |
| Modification | `PRIAM-Data-service/.../web/ProcessingController.java` | `+ GET /api/processing/byName/{name}` | Nouvel endpoint REST, générique pour toute appli |
| Modification | `PRIAM-Consent-Service/.../openfeign/DataRestClient.java` | `+ getProcessingIdByName` (Feign) | Le service Consent peut appeler ce nouvel endpoint |
| Modification | `PRIAM-Consent-Service/.../services/ContractServiceImpl.java` | Remplace les 2 lignes codées en dur par : si `processingId` n'est pas numérique, le résoudre via l'appel Feign ci-dessus | Résolution générique, plus de hardcoding par cas d'étude |

**Résultat** : le code de consentement de FastAPI-Healthcare-PRIAM (à écrire) pourra appeler PRIAM
avec le nom lisible du traitement (`"appointment-notifications"`) directement, sans connaître d'id
numérique — comme les 3 autres cas d'étude. Pas encore committé.

---

## Étape 2 — Découverte : le vrai contrat des endpoints Provider (2026-07-09)

**Question posée** : comment PRIAM appelle-t-il réellement les 3 endpoints Provider ? Faut-il
modifier PRIAM pour ça ?

**Vérifié dans le code** (`PRIAM-Right-service/.../openfeign/ProviderRestClient.java:15-22`) :
PRIAM-Right-service appelle un client Feign nommé `"gateway"` sur :
- `GET /provider/api/dataAccessRight?idRef=...&dataTypeName=...&attributes=...`
- `POST /provider/api/rectification` (body: `RectificationRequestDTO` — champs `idRef`, `dataName`,
  `dataTypeName`, `newValue`, `primaryKeys`)
- `POST /provider/api/erasure` (body: `ErasureRequestDTO` — champs `idRef`, `dataName`,
  `dataTypeName`, `primaryKeys`)

**Vérifié dans la gateway** (`PRIAM-Gateway/.../GatewayApplication.java:82-85`) : la route
`/provider/**` **retire le préfixe `/provider`** puis transmet le reste tel quel à l'URL configurée
dans `CUSTOM_PROVIDER_URL` (défaut `http://provider:8086`, le `Provider-microservice` générique,
câblé sur la base TeaStore). Donc le chemin final réellement appelé est
**`{CUSTOM_PROVIDER_URL}/api/dataAccessRight`** (sans `/priam`).

**Conclusion — aucune modification PRIAM nécessaire pour brancher un cas d'étude** : il suffit de
changer la variable d'env `CUSTOM_PROVIDER_URL` pour qu'elle pointe vers l'appli ciblée (cohérent
avec le modèle "un cas d'étude à la fois").

**Erreur trouvée dans ma version précédente** (`case-studies/FastAPI-Healthcare/app/priam/router.py`,
et par extension probablement dans les 3 autres cas d'étude déjà "intégrés") : les routes étaient
montées sur le préfixe `/api/priam/...` au lieu de `/api/...` (bare) — **incompatible** avec le
chemin réellement appelé par PRIAM. Jamais détecté avant car le câblage bout-en-bout n'avait jamais
été testé réellement (seulement documenté). Correction pour FastAPI-Healthcare-PRIAM : monter le
router sur le préfixe **`/api`** directement, pas `/api/priam`.

**Vérifié conforme dans l'ancienne version** : noms des champs des DTOs (`idRef`, `dataName`,
`dataTypeName`, `newValue`, `primaryKeys`) et noms des query params (`idRef`, `dataTypeName`,
`attributes`) — corrects, à conserver tels quels.

---

## Étape 3 — Test réel des 3 endpoints Provider contre Postgres (2026-07-09)

**But** : vérifier que `app/priam/router.py` (écrit à l'étape précédente) fonctionne vraiment
bout-en-bout — appel HTTP → écriture/lecture Postgres réelle — pas seulement une relecture de code.

**Incident en cours de route** : Docker Desktop s'est bloqué (commandes `docker info` et `docker
compose` restées pendantes plusieurs minutes sans réponse). Résolu par redémarrage de Docker Desktop
+ `wsl --shutdown`.

**Démarrage de la stack** : `docker compose up -d db redis rabbitmq app` — les 4 conteneurs
(`db`, `redis`, `rabbitmq`, `app`) démarrent et passent `Healthy`. `GET /health` confirme
`{"status":"healthy","database":"connected"}`.

**Constat notable** : le volume `postgres_data` avait survécu au blocage/redémarrage de Docker —
une modification faite lors d'un test antérieur (`first_name`: `Jane` → `Janet`) était toujours
présente en base après redémarrage complet des conteneurs.

**Tests effectués (patient id=1, seedé à l'étape 1)**, avec vérification systématique de l'état réel
en base via `docker compose exec db psql -U healthcare_user -d healthcare_db` (pas seulement la
réponse HTTP) :

| Appel | Requête | Réponse HTTP | Vérifié en base Postgres |
|---|---|---|---|
| `GET /api/dataAccessRight` | `idRef=1&dataTypeName=Patient&attributes=first_name,last_name,email,phone` | `200` avec les 4 champs | — (lecture seule) |
| `POST /api/rectification` | `{"idRef":"1","dataTypeName":"Patient","dataName":"first_name","newValue":"Jeanne"}` | `{"status":"ok"}` | `first_name` = `Jeanne` confirmé |
| `POST /api/erasure` | `{"idRef":"1","dataTypeName":"Patient","dataName":"phone"}` | `{"status":"ok"}` | `phone` = `NULL` confirmé |

**Résultat** : les 3 endpoints Provider fonctionnent bout-en-bout avec une vraie base Postgres (pas
de mock, pas de SQLite de substitution — testé explicitement avec Postgres à la demande de
l'utilisatrice). Tâche 7 de la liste de suivi validée.

**Pas encore fait** :
- Le garde de consentement (`appointment-notifications`) — `app/priam/consent.py` n'existe pas
  encore dans ce clone. Voir le fichier original `case-studies/FastAPI-Healthcare/app/priam/consent.py`
  (déjà écrit, non encore répliqué ici) pour le CEP à copier, plus le câblage dans `appointment.py`
  et `docker-compose.yml` (`PRIAM_CDP_URL`, réseau commun vers `PRIAM-Consent-Service`).
- Test bout-en-bout du consentement (accordé / refusé / CDP injoignable).

---

## Étape 4 — Écriture du CEP `consent.py` (minimal) + câblage `appointment.py` (2026-07-10)

**Décision de l'utilisatrice** : ne pas copier la version `get_consent`/`get_consent_batch` de
l'ancien `FastAPI-Healthcare` telle quelle. Vérifié : `get_consent_batch` n'est appelée nulle part
(ni ici, ni dans SportTracker qui a le même patron) — capacité inutilisée. Écrit la version stricte
minimum : **une seule fonction**, qui construit `idRefList` avec un seul id à l'intérieur (l'API
PRIAM l'accepte de toute façon sous cette forme).

| Action | Fichier | Lignes | Détail |
|---|---|---|---|
| Ajout (nouveau fichier) | `app/priam/consent.py` | 30 | `get_consent(patient_id, processing_id) -> bool` : `GET {PRIAM_CDP_URL}/api/decision/{processing_id}?idRefList={id}`, timeout 3s ; retourne `True` si `PRIAM_CDP_URL` absent (comportement pré-PRIAM préservé) ; retourne `False` si erreur/injoignable (fail-closed) |
| Modification | `app/api/routes/appointment.py` | +7/-0 (x2) | Import `get_consent` ; dans `create_appointment` et `update_appointment`, la planification de `send_appointment_notification` (via `background_tasks.add_task`) est maintenant entourée d'un `if get_consent(patient_id, "appointment-notifications"):` — le traitement obligatoire (création/modification du rendez-vous) reste inconditionnel |

**Non gardés délibérément** (fidèle au modèle existant, pas une omission) : `delete_appointment`
et `update_appointment_status` notifient sans garde de consentement, comme dans l'original —
l'utilisatrice n'a pas demandé de changer ça.

---

## Étape 5 — Câblage réseau Docker + découverte du vrai contrat du Right-service (2026-07-10)

**Câblage** (`docker-compose.yml` du clone) : ajout de `PRIAM_CDP_URL: http://consent:8089` dans
`environment:` du service `app`, et attachement au réseau externe `common_network` (déclaré
`external: true`), en plus du réseau propre `healthcare-network`.

**Test direct de `get_consent()`** (exécuté dans le conteneur `app` via `python -c`, contre la
vraie stack PRIAM — `consent`, `actor`, `data`, `mysqldb`, `eureka` démarrés manuellement pour ce
test) : a révélé 2 bugs bloquants côté PRIAM, corrigés au niveau du `PRIAM-Gateway` (générique, pas
spécifique à ce cas d'étude — voir le journal hors-dépôt et `docs/PRIAM-INTEGRATION-PLAYBOOK.md`
pour le détail) :
1. `consent` doit passer par le `gateway` (résolution Eureka) pour joindre `data`/`actor` — le
   service `gateway` n'était pas démarré.
2. `KeycloakLoginCheckFilter` bloquait ces appels machine-à-machine (401, header `X-Username`
   absent) sur les routes `/data/**`, `/actor/**` — commenté (pas supprimé) dans
   `GatewayApplication.java`, avec une note explicative.

**Résultat après correction** : `get_consent(1, "appointment-notifications")` retourne bien un
booléen cohérent contre la vraie stack (plus d'erreur 401/503).

**Découverte majeure (remise en question du plan de test des droits)** : l'utilisatrice a demandé
comment PRIAM déclenche réellement l'exécution d'un droit — vérification dans
`PRIAM-Right-service` a révélé que le test de l'Étape 3 (appel direct des 3 endpoints Provider)
**court-circuite le vrai workflow métier**. Le vrai flux :
`POST /api/right/rectificationRequest` (enregistre une `DataRequest`) puis
`POST /api/right/answer` (`answer=false` → enregistre juste `REFUSED`, rien d'autre ;
`answer=true` → enregistre `FULL` **et** appelle automatiquement `POST /provider/api/rectification`
via `ProviderRestClient`, lui-même routé par le Gateway sur `/provider/**` — qui avait le même
problème de filtre Keycloak, corrigé en même temps (commenté, même raison).

**Bloqué en fin de session** : après un redémarrage de Docker Desktop (2 pannes dans la session),
relancer toute la chaîne de dépendances (`mysqldb`, `eureka`, `actor`, `data`, `consent`, `gateway`,
`right`) simultanément avec la stack du clone a saturé les ressources de la VM Docker Desktop/WSL2
(`HikariPool - Thread starvation or clock leap detected`, `gateway` figé 20+ min en plein
démarrage). Le test complet du workflow des droits (`rectificationRequest` → `answer=false` →
vérif Postgres inchangée → nouveau cycle `answer=true` → vérif Postgres modifiée automatiquement)
n'a pas pu être exécuté cette session — **reste à faire**, une fois les ressources Docker
stabilisées (voir `docs/PRIAM-INTEGRATION-PLAYBOOK.md` §9 pour la méthode : démarrer les services
un par un, laisser le temps aux JVM).

**Pas encore fait à l'issue de l'Étape 5** (tout résolu à l'Étape 6 ci-dessous) :
- Test bout-en-bout complet du workflow des droits (`answer=false` puis `answer=true`), avec
  vérification Postgres à chaque étape.
- Test bout-en-bout du consentement retiré (`POST /api/consent/create/1` `{"processingId":"3"}`
  pour basculer, puis vérifier via RabbitMQ — comptage de messages dans la queue `notifications`
  avant/après — qu'aucune notification n'est envoyée), et re-basculer pour restaurer l'état initial.

---

## Étape 6 — Test bout-en-bout complet de la stack (2026-07-11)

**But** : terminer ce que l'Étape 5 n'a pas pu finir — exécuter réellement les 4 workflows
(accès, rectification, suppression, consentement) contre la stack PRIAM complète (mysqldb,
eureka, actor, data, consent, right, provider, gateway) + FastAPI-Healthcare-PRIAM, avec
vérification d'état réel à chaque étape (pas seulement le code HTTP).

### Blocages d'environnement résolus avant de pouvoir tester

| Blocage | Cause | Résolution |
|---|---|---|
| `docker pull`/build échoue par intermittence (`no such host`) | Résolveur DNS par défaut de l'hôte (`192.168.100.1`, passerelle VPN) instable ; Docker Desktop (mode réseau "direct connection") en dépend directement | Mode réseau **mirrored** de WSL2 : `%USERPROFILE%\.wslconfig` avec `networkingMode=mirrored` + `dnsTunneling=true`, puis `wsl --shutdown`. Réduit la fréquence des échecs sans les éliminer à 100 % — prévoir des retries sur les builds. |
| `priam-databases` (MySQL) ne démarre pas : `exec /docker-entrypoint.sh: no such file or directory` | `Databases/docker-entrypoint.sh` et `init.sh` avaient des fins de ligne CRLF (checkout Windows, pas de `.gitattributes` dans le dépôt) | Converti en LF (`sed -i 's/\r$//'`). Voir §9bis du playbook — vérifier tout script `.sh` copié dans une image Docker. |
| Build `PRIAM-Eureka` échoue : test `contextLoads()` en échec (`NullPointerException` dans `CgroupV2Subsystem`) | `Dockerfile` utilise `gradle build` (exécute les tests) au lieu de `gradle assemble` comme tous les autres services PRIAM ; l'échec est un artefact d'environnement (détection cgroups v2 dans le conteneur de build), pas un vrai bug | `RUN gradle build` → `RUN gradle assemble` dans `PRIAM-Eureka/Dockerfile` |
| Build `PRIAM-Gateway` très lent (jusqu'à 36 min) puis échoue par timeout réseau | `Dockerfile` utilise `./gradlew assemble` (wrapper), qui télécharge une distribution Gradle 8.7 complète (~130 Mo) à **chaque** build dès qu'une ligne source change (invalide le cache Docker) | `RUN ./gradlew assemble` → `RUN gradle assemble` (utilise le Gradle 7.4 déjà présent dans l'image de base, comme les autres services) — build ramené à ~2-3 min |

### Bugs PRIAM (génériques, pas spécifiques à ce cas d'étude) trouvés et corrigés

Tous documentés en détail dans `docs/PRIAM-INTEGRATION-PLAYBOOK.md` (§7 à §7quater, §9bis) pour
éviter qu'un futur cas d'étude les redécouvre :

1. **`PRIAM-Actor-service/.../SecondaryActorServiceImpl.java`** — imports manquants
   (`SecondaryActorRequestDTO`, `SecondaryActorResponseDTO`), empêchait la compilation.
2. **`PRIAM-Gateway`** — `spring-boot-starter-security` présent sans aucun bean
   `SecurityWebFilterChain` : Spring Security refuse tout par défaut (401) sur **toutes** les
   routes, y compris celles où `KeycloakLoginCheckFilter` était déjà désactivé pour le
   machine-à-machine (§7 du playbook, déjà corrigé lors d'une session précédente). C'était le
   vrai bloquant, pas le filtre Keycloak. Ajout de `SecurityConfig.java` (permissive, délègue
   à `KeycloakLoginCheckFilter`).
3. **Entités `DataSubject.age`** (`PRIAM-Actor-service`, `PRIAM-Data-service`,
   `PRIAM-Right-service`, `PRIAM-Consent-Service`, + DTOs Actor) — champ `int` primitif alors
   que la colonne SQL est nullable (`age = NULL` pour un sujet sans âge connu) → 500 dès
   qu'on lit un `DataSubject`. Changé en `Integer` partout.
4. **`PRIAM-Right-service/.../ProviderRestClient.java`** — `getPersonalDataValues` envoyait
   `attributes` en paramètres de requête répétés (comportement par défaut d'OpenFeign pour
   `List<String>`) au lieu d'un seul paramètre `attributes=a,b,c` comme documenté dans le
   contrat (§2 du playbook) — un parseur à valeur unique côté appli cible (ex. FastAPI
   `str`) ne garde que la dernière valeur, perdant silencieusement les autres attributs.
   Changé en `String` (join côté appelant).
5. **Données de test — `processing.processing_type`** — insérées en `'Necessary'`/`'Optional'`
   au lieu de `NECESSARY`/`OPTIONAL` (l'enum Java `ProcessingType` n'a que des constantes
   majuscules) → `IllegalArgumentException` dès qu'un `processing` est manipulé.
6. **Données de test — `processed_data` (bookkeeping Data-service)** — un consentement
   pré-accordé directement en SQL (bypass de l'API) n'a pas de ligne `processed_data`
   correspondante, alors que `ConsentServiceImpl.create` (retrait de consentement) en a besoin
   pour fonctionner. Sans ça, le premier retrait de consentement échoue avec un message
   trompeur (`"Subject not found with ID"` — la vraie cause est le bookkeeping manquant, pas
   le sujet). Lignes ajoutées dans `Databases/db_insertion_script.sql` (+ copie
   `priam-integration/`).
7. **`docker-compose.yml` (racine PRIAM)** — le service `gateway` pointait vers une image
   Docker distante (`registry.gitlab.com/...`) au lieu de construire depuis
   `./PRIAM-Gateway` : le code réellement exécuté ne reflétait pas les correctifs déjà commités
   dans le dépôt (dont le §7 du playbook). Reconfiguré pour builder localement.
8. **`.env` (racine PRIAM)** — `CUSTOM_PROVIDER_URL` pointait vers le `Provider-microservice`
   générique (`:8086`, non connecté à la base de FastAPI-Healthcare-PRIAM) au lieu de l'appli
   cible elle-même (`http://app:8000`), qui implémente son propre pont Provider
   (`app/priam/router.py`, voir Étape 2-3). Généralisable : quand l'appli cible auto-héberge
   son pont Provider, `CUSTOM_PROVIDER_URL` doit pointer directement dessus.

### Bug de l'appli cible (pas PRIAM) trouvé et corrigé

**`app/core/notifications.py`** — fichier préexistant de l'appli de base (commit `58846ec2`,
antérieur à l'intégration PRIAM ; vérifié via `git log --follow`), donc sa correction ne viole
pas la contrainte "l'intégration PRIAM ne doit pas modifier le code de l'appli". Bug :
`send_appointment_notification` faisait `appointment_obj["patient"].email`, mais
`crud_appointment.get_with_details()` ne renvoie jamais de clé `"patient"` (seulement
`"patient_name"`, une chaîne). `KeyError` intercepté silencieusement par le `except Exception`
englobant → **aucune notification n'était jamais publiée dans RabbitMQ**, que le consentement
soit accordé ou non, rendant le test de consentement impossible à prouver. Corrigé en
récupérant l'objet `Patient` séparément via `patient.get(db, id=appointment_obj["patient_id"])`
(même patron déjà utilisé dans la branche `notification_type == "cancelled"` du même fichier).

### Données de test manquantes côté appli cible comblées

- **`availabilities`** (Postgres, `app/db/seed.py` ne les seed pas) — sans ça,
  `POST /api/appointments/` échoue systématiquement (`"Doctor is not available"`), bloquant tout
  test de consentement. Ligne insérée directement en base pour le médecin id=1 (7 jours,
  8h-18h) — **pas encore répercuté dans `seed.py`**, à faire si ce test doit être rejouable
  automatiquement sans intervention manuelle.
- **Compte utilisateur FastAPI** — `appointment_router` exige une authentification
  (`Depends(get_current_user)` au niveau `include_router`, `app/main.py`), non documentée dans
  ce fichier jusqu'ici. Créé via `POST /api/auth/register` + `POST /api/auth/login`.

### Résultats des 4 tests (preuve d'état réel, méthodologie §8 du playbook)

| Droit | Cycle refusé | Cycle accepté |
|---|---|---|
| **Accès** | `answer=false` → `data_request_answer.answer=REFUSED` | `answer=true` → `FULL` ; lecture réelle (`GET /personalDataValues/accessRight`) retourne les vraies valeurs Postgres (`first_name`, `email`) |
| **Rectification** | `last_name` reste `"Doe"` en base Postgres | PRIAM appelle automatiquement `POST /api/rectification` → `last_name` devient `"Smith"` en base réelle |
| **Suppression** | `insurance_id` reste `"INS-000001"` | PRIAM appelle automatiquement `POST /api/erasure` → `insurance_id` devient `NULL` en base réelle |

**Consentement** (`appointment-notifications`, queue RabbitMQ `notifications` comme preuve
observable) :
1. Consentement accordé (état initial) → création d'un RDV → queue `0 → 1` message.
2. Consentement retiré (`POST /api/consent/create/1` `{"processingId":"3"}`) → nouveau RDV créé
   normalement (traitement obligatoire non affecté) → queue reste à **1** (aucune notification).
3. Consentement ré-accordé (même endpoint, rappelé une seconde fois — bascule automatiquement)
   → nouveau RDV → queue `1 → 2`.

**Tous les scénarios demandés sont validés bout-en-bout, sur base de données réelle, sans mock.**

**Pas encore fait** :
- Répercuter le seed `availabilities` dans `app/db/seed.py` pour que le test soit rejouable
  sans intervention manuelle sur une base fraîche.
- Commit des correctifs (rien n'a été committé automatiquement cette session — décision
  explicite de l'utilisatrice à confirmer avant tout `git commit`).

### Annexe — commandes exactes utilisées (rejouables telles quelles)

Toutes exécutées en ligne de commande (`curl`), pas via un navigateur. Prérequis : la stack
PRIAM (mysqldb, eureka, actor, data, consent, right, provider, gateway) et
FastAPI-Healthcare-PRIAM (db, redis, rabbitmq, app) démarrées, `.env` racine avec
`CUSTOM_PROVIDER_URL=http://app:8000`, patient id=1 (`Jane Doe`) seedé.

**Droit d'accès — cycle accepté**
```bash
curl -X POST http://localhost:8083/api/right/accessRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataRequestClaim":"Test acces","data":[{"dataId":1},{"dataId":3}]}'
# -> {"dataRequestId": N, ...}

curl -X POST http://localhost:8083/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"requestAnswerId":0,"answer":true,"providerClaim":"Approuve","dataRequestId":N,"data":[{"dataId":1},{"dataId":3}]}'
# -> {"answer":"FULL", ...}

curl "http://localhost:8083/api/personalDataValues/accessRight?dataSubjectId=1&dataTypeName=Patient&attributes=first_name,email"
# -> [{"first_name":"Jane","email":"jane.doe@example.com"}]  (lecture reelle, endpoint toujours ouvert)
```

**Droit d'accès — cycle refusé** (identique, `answer=false` et `data:[]`) :
```bash
curl -X POST http://localhost:8083/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"requestAnswerId":0,"answer":false,"providerClaim":"Refuse","dataRequestId":N,"data":[]}'
# -> {"answer":"REFUSED", ...}
```

**Rectification — cycle refusé puis accepté** (`dataId":2` = `last_name`) :
```bash
# Etat avant
curl "http://localhost:8000/api/dataAccessRight?idRef=1&dataTypeName=Patient&attributes=last_name"

curl -X POST http://localhost:8083/api/right/rectificationRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Patient","data":{"dataId":2},"newValue":"Smith","claim":"Correction nom","primaryKeys":[]}'
# -> {"dataRequestId": N, ...}

curl -X POST http://localhost:8083/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"requestAnswerId":0,"answer":false,"providerClaim":"Refuse","dataRequestId":N,"data":[]}'
# -> refuse : re-verifier /api/dataAccessRight, doit etre inchange ("Doe")

# Refaire un rectificationRequest (nouveau N), puis :
curl -X POST http://localhost:8083/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"requestAnswerId":0,"answer":true,"providerClaim":"Approuve","dataRequestId":N,"data":[]}'
# -> accepte : PRIAM appelle POST /api/rectification sur Provider automatiquement
# re-verifier /api/dataAccessRight -> "Smith"
```

**Suppression — même patron** (`erasureRequest` au lieu de `rectificationRequest`, pas de
`newValue`, `dataId":8` = `insurance_id`) :
```bash
curl -X POST http://localhost:8083/api/right/erasureRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Patient","data":{"dataId":8},"newValue":null,"claim":"Suppression id assurance","primaryKeys":[]}'
# puis /api/right/answer comme ci-dessus (false puis true sur 2 cycles distincts)
# accepte -> attribute insurance_id devient "None" via GET /api/dataAccessRight
```

**Consentement — accordé / retiré / ré-accordé** (`processingId":"3"` = `appointment-notifications`) :
```bash
# Etat courant du consentement
curl "http://localhost:8089/api/decision/appointment-notifications?idRefList=1"
# -> {"1": true} si accorde

# Authentification FastAPI (JWT maison, sans rapport avec PRIAM/Keycloak)
curl -X POST http://localhost:8000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","username":"testuser","password":"TestPass123!","role":"admin"}'

TOKEN=$(curl -s -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","password":"TestPass123!"}' \
  | python -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

# Preuve d'etat reel : compteur de messages AVANT
docker exec fastapi-healthcare-priam-rabbitmq-1 rabbitmqctl list_queues name messages

# Creation d'un RDV (consentement accorde -> notification attendue)
curl -X POST http://localhost:8000/api/appointments/ \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"patient_id":1,"doctor_id":1,"start_time":"2026-08-01T10:00:00","end_time":"2026-08-01T11:00:00","status":"scheduled","notes":"test"}'

# Compteur APRES -> doit avoir augmente de 1
docker exec fastapi-healthcare-priam-rabbitmq-1 rabbitmqctl list_queues name messages

# Retrait du consentement (CAP : bascule automatiquement l'etat a chaque appel)
curl -X POST http://localhost:8089/api/consent/create/1 \
  -H "Content-Type: application/json" -d '{"processingId":"3"}'
curl "http://localhost:8089/api/decision/appointment-notifications?idRefList=1"
# -> {"1": false}

# Nouveau RDV (consentement retire) -> le RDV est cree (traitement obligatoire),
# mais le compteur RabbitMQ NE DOIT PAS augmenter
curl -X POST http://localhost:8000/api/appointments/ \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"patient_id":1,"doctor_id":1,"start_time":"2026-08-03T10:00:00","end_time":"2026-08-03T11:00:00","status":"scheduled","notes":"test 2"}'
docker exec fastapi-healthcare-priam-rabbitmq-1 rabbitmqctl list_queues name messages
# -> compteur inchange

# Re-octroi (rappeler le meme endpoint une 2e fois bascule dans l'autre sens)
curl -X POST http://localhost:8089/api/consent/create/1 \
  -H "Content-Type: application/json" -d '{"processingId":"3"}'
```

**Prérequis de données non liés à PRIAM, à créer une fois par base fraîche** (voir section
"Données de test manquantes" ci-dessus pour le pourquoi) :
```sql
-- Disponibilite du medecin id=1, sinon POST /api/appointments/ echoue toujours
INSERT INTO availabilities (doctor_id, day_of_week, start_time, end_time, is_available)
SELECT 1, d, '08:00', '18:00', true FROM generate_series(0,6) AS d;
```
