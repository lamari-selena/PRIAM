# Guide d'intégration PRIAM — brancher une nouvelle application cible

> Destiné à être suivi par n'importe quel LLM/agent pour reproduire une intégration PRIAM
> rapidement, sans redécouvrir les pièges déjà rencontrés. Générique : ne nomme aucune
> application cible spécifique, seulement des patrons à reproduire. Issu des sessions
> d'intégration FastAPI-Healthcare-PRIAM, TeaStore, SportTracker, Ghostfolio.
>
> **Statut de validation** : les 4 workflows RGPD (accès, rectification, suppression,
> consentement accordé/retiré/ré-accordé) ont été exécutés bout-en-bout contre la stack PRIAM
> complète (mysqldb, eureka, actor, data, consent, right, provider, gateway) + une vraie
> application cible, avec preuve d'état réel (base de données, file de messages) à chaque
> étape — pas seulement relus ou documentés. Tous les pièges des §6 à §9bis ci-dessous ont été
> rencontrés et corrigés au cours de ce test réel, pas anticipés en théorie. Si vous retrouvez
> l'un de ces bugs sur une copie du dépôt, c'est que vous êtes sur une copie non à jour — ne
> les corrigez pas une seconde fois sans vérifier d'abord si le fichier concerné les contient
> déjà.

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
10. `processing.processing_type` : la valeur doit correspondre **exactement** (casse
    incluse) à une constante de l'enum Java `priam.data.priamdataservice.enums.ProcessingType`
    — `NECESSARY`, `OPTIONAL`, `MANDATORY` ou `DEFAULT`, tout en majuscules. `'Necessary'`
    ou `'Optional'` (casse mixte) compilent en SQL mais font planter Hibernate
    (`IllegalArgumentException: No enum constant ...`) dès qu'un `processing` est lu ou
    manipulé — voir §7quater.
11. Si un `consent` est pré-accordé directement par ce script (point 9) pour un
    `processing` `OPTIONAL`, ajouter aussi la ligne `processed_data` correspondante
    (schéma `priam-data`, une ligne par `data_id` que `data_usage` relie à ce
    `processing`, `nb_occurrences = 1`) — sinon le premier retrait de consentement via
    l'API échoue. Voir §7quinquies.

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
- `attributes` (sur `GET /dataAccessRight`) arrive côté appli cible comme **un seul
  paramètre de requête, valeurs séparées par des virgules** (`attributes=a,b,c`), pas en
  paramètres répétés (`attributes=a&attributes=b`). Si l'appli cible parse `attributes`
  comme une valeur unique (`str` en Python/FastAPI, `String` en Java `@RequestParam`),
  des paramètres répétés font qu'elle ne garde que le dernier, perdant silencieusement les
  autres attributs demandés — voir §7ter pour le correctif déjà en place côté PRIAM.
- `CUSTOM_PROVIDER_URL` doit pointer **directement sur l'appli cible** (ex.
  `http://<service-appli-cible>:<port>`) si celle-ci implémente elle-même ces 3 endpoints
  (le cas courant, décrit dans ce guide). Ne pas le laisser sur sa valeur par défaut
  (`http://provider:8086`, le `Provider-microservice` générique de PRIAM) sauf si c'est
  spécifiquement ce composant générique qui sert de pont pour cette appli cible — sinon
  toute demande de droit approuvée échoue silencieusement ou touche la mauvaise base.

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

**Piège emboîté (le vrai bloquant si vous voyez encore un 401 après ce correctif)** :
`PRIAM-Gateway/build.gradle` déclare `spring-boot-starter-security` (+ oauth2-client,
oauth2-resource-server). Sans **aucun** bean `SecurityWebFilterChain` explicite, Spring
Security applique son comportement par défaut pour WebFlux : refuser (401) **toute**
requête sur **toutes** les routes, avant même que le `RouteLocator` (et donc
`KeycloakLoginCheckFilter`, activé ou non) ne soit atteint. Désactiver le filtre custom
sur une route ne suffit donc pas — il faut aussi neutraliser le deny-all par défaut de
Spring Security, en lui déléguant explicitement l'autorisation :

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }
}
```

Ce bean ne réintroduit aucune faille par rapport à l'état actuel : l'autorisation réelle
reste portée par `KeycloakLoginCheckFilter` (routes humaines) et par l'absence de filtre
(routes machine-à-machine), comme avant — ce correctif retire seulement la couche
Spring Security qui empêchait les deux d'être atteintes.

## 7bis. Piège déjà corrigé — `DataSubject.age` en `int` primitif

Les entités `DataSubject` de `PRIAM-Actor-service`, `PRIAM-Data-service`,
`PRIAM-Right-service`, `PRIAM-Consent-Service` (et les DTOs Actor `DataSubjectRequestDTO`
/ `DataSubjectResponseDTO`) déclaraient `age` en `int` primitif. La colonne SQL
`data_subject.age` est nullable — un sujet dont l'âge n'est pas connu ou pertinent pour
l'appli cible (cas courant : un patient, un compte utilisateur) a `age = NULL`. Hibernate
ne peut pas assigner `null` à un primitif : toute lecture d'un tel `DataSubject`
(y compris via Feign, où Jackson a le même problème de désérialisation) plante en 500
(`PropertyAccessException`), bloquant silencieusement tout : droits, résolution d'idRef,
décision de consentement. Déjà corrigé dans ces 5 fichiers : `age` en `Integer`. Si vous
retrouvez `private int age;` sur une copie du dépôt, appliquez le même correctif partout
où le champ apparaît (entités **et** DTOs — un seul endroit oublié suffit à faire
replanter la chaîne).

## 7ter. Piège déjà corrigé — encodage de `attributes` dans `ProviderRestClient`

`PRIAM-Right-service/.../openfeign/ProviderRestClient.getPersonalDataValues` déclarait
`attributes` en `List<String>`. OpenFeign encode par défaut une collection en paramètres
de requête **répétés** (`attributes=a&attributes=b`), alors que le contrat documenté au
§2 (et toute appli cible qui parse `attributes` comme une valeur simple) attend un seul
paramètre `attributes=a,b`. Déjà corrigé : le paramètre Feign est maintenant `String`, et
`DataRequestServiceImpl.DataAccess` joint la liste (`String.join(",", attributes)`) avant
l'appel — la capacité de demander plusieurs attributs à la fois est intacte, seul le
format de transport a changé.

## 7quater. Piège déjà corrigé — casse de `processing_type`

Voir §1 point 10. L'enum Java `ProcessingType` (`PRIAM-Data-service`) n'a que des
constantes majuscules. Toute donnée de seed avec `'Necessary'`/`'Optional'` (casse mixte)
fait planter Hibernate dès qu'un `processing` est manipulé (résolution de consentement,
listing, etc.), avec un message d'erreur qui pointe vers l'enum et non vers le SQL —
facile à mal diagnostiquer côté application cible alors que la cause est la donnée de
seed PRIAM.

## 7quinquies. Piège déjà corrigé — bookkeeping `processed_data` manquant

Voir §1 point 11. `ConsentServiceImpl.create` (le CAP — l'endpoint qui bascule un
consentement accordé/retiré, `POST /api/consent/create/{idRef}`) appelle
`Data-service.removeProcessedData` lors d'un retrait, qui s'attend à trouver une ligne
`processed_data` par `data_id` concerné (normalement créée par `addProcessedData` quand
le consentement a été accordé **via l'API**). Si le consentement initial a été pré-seedé
directement en SQL (point 9 du §1, le cas courant pour démarrer les tests dans un état
"accordé" propre), cette ligne n'existe pas, et le premier retrait échoue avec
`IllegalArgumentException: Subject not found with ID: ...` — message trompeur, la vraie
cause est le bookkeeping manquant, pas une histoire de sujet introuvable.

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
- **Le mécanisme d'observation lui-même doit être vérifié en premier**, avant d'accuser
  PRIAM d'un consentement mal appliqué. Si l'effet de bord observable (queue de message,
  email, log) est produit par du code de l'appli cible **indépendant** de PRIAM, ce code
  peut avoir son propre bug qui l'empêche de s'exécuter dans tous les cas (accordé ou
  refusé) — auquel cas le compteur ne bougera jamais, quel que soit l'état du
  consentement, et on croira à tort que PRIAM bloque tout. Tester d'abord que l'effet de
  bord se produit bien en consentement accordé (état de départ habituel après le seed du
  §1 point 9) avant de tester le retrait.
- **Prérequis côté appli cible à vérifier avant de lancer les tests** (généralement
  indépendants de PRIAM, mais bloquants pour observer quoi que ce soit) :
  - Authentification : si les routes de l'appli cible exigent un compte/token (cas
    fréquent), créer un compte de test et l'utiliser pour tous les appels, avant même de
    commencer les tests PRIAM.
  - Données de référence : toute contrainte métier de l'appli cible sur le chemin testé
    (ex. disponibilité d'une ressource, statut d'un compte, quota) doit être satisfaite
    par les données de seed — sinon l'appli cible rejette la requête avant même que le
    code PRIAM (garde de consentement, etc.) ne soit atteint, et l'échec n'a rien à voir
    avec PRIAM.

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

## 9bis. Fiabilité du build et de l'environnement Docker (Windows notamment)

- **Fins de ligne CRLF** : ce dépôt n'a pas de `.gitattributes` forçant le LF. Sur un
  checkout Windows, tout script `.sh` copié tel quel dans une image Docker Linux
  (`Databases/docker-entrypoint.sh`, `init.sh`, `PRIAM-Gateway/gradlew` si utilisé) casse
  au démarrage du conteneur (`exec ...: no such file or directory`, ou `gradlew: not
  found`) à cause du `\r` en fin de shebang. Symptôme facile à mal diagnostiquer (ressemble
  à un fichier manquant). Vérifier avec `file <script>` (doit dire juste "... executable",
  pas "with CRLF line terminators") ; corriger avec `sed -i 's/\r$//' <script>` si besoin.
- **`gradle build` vs `gradle assemble`** : tous les `Dockerfile` des microservices PRIAM
  doivent utiliser `RUN gradle assemble` (pas `gradle build`, qui exécute aussi les
  tests). Un test qui échoue pour une raison propre à l'environnement du conteneur de
  build (ex. détection cgroups v2 par Micrometer, sans rapport avec le code) fait planter
  tout le build de l'image. Si un `Dockerfile` utilise encore `gradle build`, c'est une
  incohérence avec le reste du dépôt à corriger.
- **`./gradlew` vs `gradle`** : préférer aussi `RUN gradle assemble` (Gradle déjà présent
  dans l'image `gradle:<version>` de base) à `RUN ./gradlew assemble` (le wrapper). Le
  wrapper télécharge sa propre distribution Gradle (souvent plus récente, ~130 Mo) à
  chaque build où le cache Docker est invalidé par un changement de source — lent, et
  premier point de défaillance sur un réseau instable (timeouts après 20-40 min observés).
- **Le service `gateway` du `docker-compose.yml` racine doit builder depuis
  `./PRIAM-Gateway`** (code source local), pas consommer une image Docker distante
  préconstruite (`image: registry.gitlab.com/...`) — sinon les correctifs déjà commités
  dans le dépôt (§7 et suivants) ne sont pas forcément dans l'image réellement exécutée,
  et on redécouvre des bugs déjà corrigés dans le code.
- **DNS Docker Desktop instable derrière un VPN (Windows/WSL2)** : si `docker pull`/build
  échoue par intermittence avec `dial tcp: lookup <host>: no such host` alors que la
  machine a par ailleurs accès à internet, c'est généralement le résolveur DNS par défaut
  de l'hôte (souvent une passerelle VPN) qui time-out de façon intermittente, et dont
  dépend directement le mode réseau "direct connection" de Docker Desktop récent (pas de
  daemon.json ni de champ DNS manuel dans les Settings de ce mode). Correctif recommandé
  par Microsoft pour ce cas VPN+WSL2 : créer/éditer `%USERPROFILE%\.wslconfig` :
  ```ini
  [wsl2]
  networkingMode=mirrored
  dnsTunneling=true
  ```
  puis `wsl --shutdown` (redémarre tout WSL2, pas seulement Docker — prévenir si d'autres
  distros tournent) et relancer Docker Desktop. Réduit fortement la fréquence des échecs
  sans forcément les éliminer à 100 % — prévoir des retries sur les étapes de build/pull
  plutôt que d'abandonner au premier échec réseau.
- **Builds parallèles moins fiables que séquentiels sur réseau instable** : `docker
  compose build <svc1> <svc2> <svc3>` en une seule commande échoue plus souvent (plusieurs
  téléchargements Maven/Gradle concurrents, un seul qui timeout fait échouer tout le lot)
  que le même build lancé service par service. Sur un réseau flaky, préférer des builds
  séquentiels avec retry individuel.

## Check-list rapide pour une nouvelle application cible

1. [ ] Identifier le vrai schéma (tables/colonnes) de l'appli cible.
2. [ ] Écrire/adapter `db_insertion_script.sql` (§1) — ordre actor avant data, catégories
   manquantes, noms réels, `processing_type` en MAJUSCULES (§1.10), ligne
   `processed_data` pour tout consentement pré-accordé (§1.11).
3. [ ] Ajouter un script de seed côté appli cible si elle n'a pas de données par défaut,
   pour un `idRef` fiable — inclure toute donnée de référence requise par les contraintes
   métier de l'appli cible sur le chemin testé (§8), pas seulement le sujet lui-même.
4. [ ] Écrire les 3 endpoints Provider (§2), montés sur `/api` bare, sans auth,
   `attributes` parsé comme une chaîne unique séparée par des virgules.
5. [ ] Écrire le CEP `get_consent()` (§4), une seule fonction, fail-open si
   `PRIAM_CDP_URL` absent, fail-closed sinon.
6. [ ] Câbler `PRIAM_CDP_URL` + `common_network` dans le `docker-compose.yml` de l'appli
   cible (§5), et pointer `CUSTOM_PROVIDER_URL` (`.env` racine PRIAM) directement sur
   l'appli cible (§2).
7. [ ] Démarrer la stack PRIAM en s'assurant que `gateway` build depuis le code source
   local (§9bis), pas une image distante — sinon les correctifs des §6-7quinquies
   pourraient ne pas être dans l'image exécutée.
8. [ ] Créer un compte/token de test si l'appli cible exige une authentification sur les
   routes testées (§8).
9. [ ] Tester le vrai workflow des droits via `PRIAM-Right-service` (§3), pas un appel
   direct aux endpoints Provider — cycle `answer=false` ET `answer=true`, avec
   vérification d'état réel en base à chaque étape.
10. [ ] Tester le consentement dans les deux sens (accordé/refusé/ré-accordé) avec preuve
    d'état réel observable (§8) — en vérifiant d'abord que l'effet de bord se produit bien
    en consentement accordé avant de tester le retrait.
