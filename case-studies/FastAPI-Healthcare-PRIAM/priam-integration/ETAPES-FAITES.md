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

**Pas encore fait** :
- Test bout-en-bout complet du workflow des droits (`answer=false` puis `answer=true`), avec
  vérification Postgres à chaque étape.
- Test bout-en-bout du consentement retiré (`POST /api/consent/create/1` `{"processingId":"3"}`
  pour basculer, puis vérifier via RabbitMQ — comptage de messages dans la queue `notifications`
  avant/après — qu'aucune notification n'est envoyée), et re-basculer pour restaurer l'état initial.
