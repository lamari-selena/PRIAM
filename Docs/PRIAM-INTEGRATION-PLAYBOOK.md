# Guide d'intégration PRIAM — brancher une nouvelle application cible

> Destiné à être suivi par n'importe quel LLM/agent pour reproduire une intégration PRIAM
> rapidement, sans redécouvrir les pièges déjà rencontrés. Générique : ne nomme aucune
> application cible spécifique, seulement des patrons à reproduire. Issu des sessions
> d'intégration FastAPI-Healthcare-PRIAM, TeaStore, SportTracker, Ghostfolio.

## 0. Vue d'ensemble de l'architecture

PRIAM est un ensemble de microservices Spring Boot (`actor`, `data`, `consent`, `right`,
`provider`, `gateway`, `eureka`) + une base MySQL (`mysqldb`), branchés sur **une seule**
application cible à la fois (mono-tenant dans l'état actuel du code). L'application cible
n'a besoin de connaître PRIAM que par 2 canaux :

- **Sortant** (l'appli cible appelle PRIAM) : vérifier le consentement avant un traitement
  optionnel — voir §4.
- **Entrant** (PRIAM appelle l'appli cible) : exécuter un droit RGPD (accès/rectification/
  suppression) via 3 endpoints Provider que l'appli cible doit exposer — voir §2-3.

L'appli cible ne voit jamais Keycloak, Eureka, ou les autres microservices PRIAM
directement — seulement `PRIAM_CDP_URL` (pour le consentement) et le fait d'exposer les
3 endpoints Provider (pour les droits).

## 1. Annotation (SQL) — modéliser l'appli cible dans PRIAM

Fichier : `Databases/db_insertion_script.sql`, copié dans l'image Docker `Databases` et
exécuté automatiquement par MySQL au premier démarrage (`/docker-entrypoint-initdb.d/`) —
**sur un volume MySQL vierge uniquement**. Ce n'est PAS un fichier `annotations.json`
appelé via API REST — c'est du SQL direct.

Étapes, dans l'ordre :

1. **Identifier le vrai schéma** de l'appli cible : ouvrir son code (modèles ORM), pas
   supposer. Chaque `source_details` doit citer la vraie table/colonne physique.
2. **Ordre cross-schema obligatoire** : `INSERT INTO data_subject_category` (schéma
   `priam-actor`) **AVANT** `INSERT INTO data` (schéma `priam-data`), car
   `data.data_subject_category_id` référence `priam-actor.data_subject_category` (FK
   inter-schéma).
3. `personal_data_category` ne contient que 10 lignes par défaut (voir
   `Databases/db_creation_script.sql`) — si l'appli cible a des catégories manquantes
   (ex. "contact", "financial"), les ajouter (lignes de données simples, pas de contrainte
   CHECK dessus).
4. `data_type.data_type_name` doit correspondre **littéralement** au nom que le code du
   pont Provider de l'appli cible compare (ex. `"Patient"`, pas `"patients"` le nom de
   table SQL) — vérifier dans le code du pont, pas deviner.
5. `data` : une ligne par colonne personnelle réellement rectifiable/effaçable, avec
   `source_details` citant la vraie table.colonne.
6. `processing` : `Necessary` (base légale = contrat/nécessité) vs `Optional` (base légale
   = consentement, article 6.1.a). `processing_name` est le nom lisible — la résolution
   par nom est déjà générique côté PRIAM (voir §6), pas besoin de connaître un id
   numérique à l'avance.
7. `data_usage` / `purpose` : relient `data` à `processing`.
8. `data_subject.id_ref` : doit être un id **réel et stable** de l'appli cible (pas un
   placeholder inventé). Si l'appli cible ne seed aucune donnée par défaut, ajouter un
   script de seed côté appli cible d'abord (voir §9), pour que l'`id_ref` soit fiable
   (ex. `id=1` sur une base auto-increment fraîche).
9. `contract` / `consent` : seed un consentement **pré-accordé** (`end_date = NULL`) pour
   chaque `processing` de type `Optional`, pour que les tests de droits démarrent d'un
   état propre (le test de retrait/octroi de consentement se fait ensuite via le vrai
   endpoint PRIAM, pas en éditant ce script — voir §4).

Si plusieurs cas d'étude doivent coexister dans la même base PRIAM, namespacer les ids
(plages `1xx`/`2xx`/`3xx`/`4xx`) ; sinon un script autonome avec des ids simples
(`1, 2, 3...`) est plus lisible.

## 2. Le pont Provider — 3 endpoints à écrire côté appli cible

Contrat vérifié dans `PRIAM-Right-service/.../openfeign/ProviderRestClient.java` et la
route `/provider/**` de `PRIAM-Gateway` (qui **retire uniquement le préfixe `/provider`**
puis transmet vers `CUSTOM_PROVIDER_URL`) :

```
GET  {CUSTOM_PROVIDER_URL}/api/dataAccessRight?idRef=...&dataTypeName=...&attributes=a,b,c
POST {CUSTOM_PROVIDER_URL}/api/rectification   body: {idRef, dataTypeName, dataName, newValue, primaryKeys}
POST {CUSTOM_PROVIDER_URL}/api/erasure         body: {idRef, dataTypeName, dataName, primaryKeys}
```

Points importants :
- Montés sur le préfixe bare **`/api`**, PAS `/api/priam` (erreur fréquente — jamais
  détectée sans test bout-en-bout réel, car documentée mais jamais câblée).
- **Aucune authentification** — appelés uniquement en machine-à-machine par PRIAM.
- `idRef` = id primaire réel du sujet dans l'appli cible (forme string).
- `primaryKeys` sert à désambiguïser un enregistrement qui n'est pas la table "sujet"
  elle-même (ex. un dossier médical, une commande) — `idRef` seul ne suffit pas.
- `attributes`/`dataName` doivent être limités à une liste blanche de champs autorisés
  par `dataTypeName`, valider côté appli cible (400 si champ non listé).

## 3. Le vrai workflow des droits (PRIAM-Right-service)

**Piège fréquent** : appeler directement les 3 endpoints Provider pour "tester les
droits" court-circuite le vrai mécanisme métier de PRIAM. Le vrai flux passe par
`PRIAM-Right-service` :

1. **Demande** — `POST /api/right/accessRequest` (ou `/rectificationRequest`,
   `/erasureRequest`) avec `{dataSubjectId, dataTypeName, data: {dataId}, newValue, claim,
   primaryKeys: []}` → crée un `DataRequest` (non répondu), notifie le "responsable de
   traitement" (app owner).
2. **Réponse** — `POST /api/right/answer` avec `{dataRequestId, answer: bool,
   providerClaim, data: []}` :
   - `answer=false` → enregistre uniquement `AnswerType.REFUSED`. **Rien d'autre ne se
     passe** — aucun appel Provider.
   - `answer=true` (rectification/erasure) → enregistre `AnswerType.FULL` **ET** appelle
     automatiquement le endpoint Provider correspondant (`ProviderRestClient`, via le
     Gateway) — c'est PRIAM qui déclenche l'exécution, pas l'appelant du `/answer`.
3. Pour les demandes d'**accès**, la lecture réelle passe par un endpoint toujours ouvert
   (`DataAccess`/`personalDataValues/accessRight`), pas par le mécanisme d'auto-exécution
   ci-dessus — la réponse enregistre seulement quels champs sont `FULL`/`PARTIAL`/
   `REFUSED` (comptabilité `isAccepted`).

**Test complet à faire** (pas un raccourci) : cycle avec `answer=false` (vérifier qu'AUCUN
changement n'a lieu dans la base de l'appli cible) ET un second cycle avec `answer=true`
(vérifier que le changement a bien lieu automatiquement) — les deux, pas un seul.

## 4. Le pont Consentement (CEP) — traitement optionnel

Patron minimal (voir `FastAPI-Healthcare-PRIAM/app/priam/consent.py` comme référence) :

```python
def get_consent(id_ref, processing_id) -> bool:
    if not PRIAM_CDP_URL:
        return True  # PRIAM absent -> comportement pré-PRIAM préservé
    try:
        # GET {PRIAM_CDP_URL}/api/decision/{processing_id}?idRefList={id_ref}, timeout ~3s
        return decision.get(id_ref, False) is True
    except Exception:
        return False  # PRIAM injoignable/erreur -> refus par defaut (fail-closed)
```

- **Une seule fonction** suffit tant que rien n'appelle jamais avec plusieurs ids à la
  fois — ne pas ajouter de variante "batch" avant qu'un vrai appelant en ait besoin (le
  endpoint PRIAM le supporte déjà nativement via `idRefList` répété, donc l'ajouter plus
  tard ne coûtera qu'une fonction, pas une réécriture).
- `processing_id` peut être passé sous forme de **nom lisible** directement (résolution
  générique déjà en place côté PRIAM, voir §6) — pas besoin de connaître un id numérique.
- **Point d'insertion** : entourer **uniquement** l'effet de bord optionnel d'un `if
  get_consent(...): ...` — ne jamais gater le traitement obligatoire. Pas de `else`
  explicite nécessaire ("ne rien faire" = ne pas appeler la fonction).
- Ne pas utiliser un garde déclaratif au niveau route (décorateur/middleware qui bloque
  toute la requête avec 403) — ça bloquerait à tort le traitement obligatoire. Le `if`
  inline, au milieu de la logique métier, est le bon niveau de granularité.

## 5. Réseau Docker

- PRIAM expose un réseau externe partagé : `common_network` (déclaré `external: true`
  dans les `docker-compose.yml` qui le consomment ; créé/possédé par le
  `docker-compose.yml` racine de PRIAM au premier démarrage de sa stack).
- Dans le `docker-compose.yml` de l'appli cible : ajouter au service applicatif
  `PRIAM_CDP_URL: http://consent:8089` (nom de service + port réels de
  `PRIAM-Consent-Service`) et l'attacher à `common_network` en plus de son réseau propre.
- **`common_network` doit déjà exister** avant qu'une référence `external: true`
  fonctionne — démarrer au moins une fois la stack PRIAM racine (ou celle qui la
  possède) avant celle de l'appli cible.

## 6. Piège déjà corrigé — résolution du traitement par nom (ne pas re-découvrir)

`ContractServiceImpl.getConsentByDataSubject` (PRIAM-Consent-Service) résout maintenant un
`processingId` non-numérique via un appel Feign vers `Data-service` :
`GET /api/processing/byName/{name}`. Générique, aucun hardcoding par application. Déjà en
place dans 5 fichiers (`PRIAM-Data-service` : `ProcessingRepository`,
`ProcessingServiceInterface`, `ProcessingService`, `ProcessingController` ; et
`PRIAM-Consent-Service` : `DataRestClient` + `ContractServiceImpl`). Si vous retrouvez
l'ancien mécanisme à 2 lignes codées en dur, c'est que vous êtes sur une copie non à jour.

## 7. Piège Gateway — filtre Keycloak sur routes machine-à-machine

`KeycloakLoginCheckFilter` (PRIAM-Gateway) exige un header `X-Username` et rejette en 401
**avant même d'essayer Keycloak** si absent. Les appels internes PRIAM-à-PRIAM
(`Consent → Gateway → Data/Actor` pour résoudre un nom/idRef, `Right → Gateway → Provider`
pour l'auto-exécution d'un droit) n'ont pas d'utilisateur humain, donc pas de header —
bloqués par défaut, indépendamment du fait que Keycloak soit déployé ou non.

**Règle générique à appliquer** : une route du Gateway n'a besoin du filtre humain que si
un humain/frontend l'appelle réellement. Les routes purement machine-à-machine
(`/data/**`, `/actor/**`, `/provider/**` dans `GatewayApplication.java`) ne devraient
jamais l'exiger, quelle que soit l'application branchée. Correctif : commenter (pas
supprimer) `.filter(keycloakLoginCheckFilter)` sur ces routes, en laissant une note
expliquant pourquoi.

## 8. Méthodologie de test — preuve par état réel

- **Droits** : vérifier l'état réel en base de l'appli cible (`SELECT` direct après
  l'appel), pas seulement le code 200 de la réponse HTTP.
- **Consentement accordé** : idem, + vérifier qu'un effet de bord observable a bien eu
  lieu (ex. un message publié dans une queue de message, un email envoyé) — compter avant/
  après, pas juste l'absence d'erreur.
- **Consentement refusé** : la preuve d'ABSENCE est le point délicat. "Pas d'erreur" ne
  prouve rien. Compter l'état observable AVANT et APRÈS l'appel, confirmer qu'il n'a **pas
  changé** (même nombre de messages en queue, aucune nouvelle ligne de log d'envoi).
  Vérifier aussi que le traitement obligatoire, lui, a bien eu lieu quand même.

## 9. Limites de ressources Docker observées

- Faire tourner toute la stack PRIAM (7 microservices Java/Spring + MySQL + Eureka) EN
  PLUS de la stack de l'appli cible peut saturer une VM Docker Desktop/WSL2 modeste
  (symptômes : `HikariPool - Thread starvation or clock leap detected`, JVM figée en plein
  démarrage, Docker Desktop qui plante avec des 500 sur sa propre API). Démarrer les
  services un par un dans l'ordre des dépendances, laisser 50-90s à chaque service Java
  pour finir sa santé avant de considérer un échec de healthcheck comme réel (ce n'est pas
  toujours un vrai échec, souvent juste un démarrage JVM normal).
- `docker compose up --build <service>` peut déclencher une reconstruction de **tous**
  les services buildables du fichier d'un coup (bake) et planter sur une VM chargée.
  Préférer `COMPOSE_BAKE=false docker compose build <service>` seul, puis
  `docker compose up -d --no-build <service>`, pour cibler précisément un seul service.

## Check-list rapide pour une nouvelle application cible

1. [ ] Identifier le vrai schéma (tables/colonnes) de l'appli cible.
2. [ ] Écrire/adapter `db_insertion_script.sql` (§1) — ordre actor avant data, catégories
   manquantes, noms réels.
3. [ ] Ajouter un script de seed côté appli cible si elle n'a pas de données par défaut,
   pour un `idRef` fiable.
4. [ ] Écrire les 3 endpoints Provider (§2), montés sur `/api` bare, sans auth.
5. [ ] Écrire le CEP `get_consent()` (§4), une seule fonction, fail-open si
   `PRIAM_CDP_URL` absent, fail-closed sinon.
6. [ ] Câbler `PRIAM_CDP_URL` + `common_network` dans le `docker-compose.yml` de l'appli
   cible (§5).
7. [ ] Tester le vrai workflow des droits via `PRIAM-Right-service` (§3), pas un appel
   direct aux endpoints Provider — cycle `answer=false` ET `answer=true`.
8. [ ] Tester le consentement dans les deux sens (accordé/refusé) avec preuve d'état réel
   (§8), pas juste l'absence d'erreur HTTP.
