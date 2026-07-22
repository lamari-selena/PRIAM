# Bugs internes PRIAM déjà corrigés — historique, pas une lecture requise

> **Vous n'avez pas besoin de lire ce fichier pour intégrer une nouvelle application
> cible.** PRIAM est générique (`Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §0) — les entrées
> ci-dessous sont des bugs qui vivaient dans le code de PRIAM lui-même (`PRIAM-Right-
> service`, `PRIAM-Data-service`, `PRIAM-Consent-Service`, `PRIAM-Actor-service`,
> `PRIAM-Gateway`, `PRIAM-Frontend`, `PRIAM-Frontend-Provider`), pas dans le code d'une
> application cible — ils ont tous été corrigés une fois pour toutes dans ce dépôt, et
> une nouvelle intégration n'a plus jamais à les reproduire ni à les redécouvrir.
>
> Ce fichier existe pour deux raisons seulement :
> 1. **Provenance** : comprendre pourquoi telle ligne de code de PRIAM est écrite comme
>    elle l'est, si vous touchez un jour au code de PRIAM lui-même (pas au code d'une
>    application cible).
> 2. **Diagnostic d'une régression apparente** : si l'un de ces symptômes réapparaît
>    malgré ce catalogue, ce n'est presque jamais le même bug qui revient — c'est très
>    probablement un conteneur qui tourne avec du code compilé avant le correctif
>    (image pas reconstruite depuis le dépôt local, voir §8.9 du playbook principal),
>    pas une régression de logique à re-corriger ici.
>
> Le catalogue des pièges **encore pertinents pour un développeur qui intègre une
> nouvelle application** reste dans `Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §8 (annotation
> SQL, et les deux pièges qui demandent une action côté application cible en plus du
> correctif déjà en place côté PRIAM).

### Index rapide

| # | Groupe | Piège | Symptôme observable |
|---|---|---|---|
| 8.2.a | Pont Provider (côté PRIAM) | `dataAccessRight` renvoie un objet nu au lieu d'un tableau | `[]` silencieux, `200 OK` |
| 8.2.b | Pont Provider (côté PRIAM) | `idRef` interne PRIAM envoyé au lieu de l'externe | Donnée introuvable côté appli malgré un appel manuel réussi |
| 8.2.c | Pont Provider (côté PRIAM) | Contrat `{attribute,value}` vs `{champ: valeur}` incohérent entre Right/Data-service | `NullPointerException` sur Access Request |
| 8.2.d | Pont Provider (côté PRIAM) | `attributes` encodé en paramètres répétés au lieu de `a,b,c` | Attributs demandés en trop perdus silencieusement |
| 8.2.e | Pont Provider (côté PRIAM) | Un appel Provider par colonne, résultats recollés par position | Colonnes d'un même enregistrement désalignées si le tri appli cible n'était pas stable |
| 8.3.a | PRIAM-Right-service | Double réponse à une même demande | `HibernateException` fait tomber toute `requestList` |
| 8.3.b | PRIAM-Right-service | Filtre `requestList` par type cassé (ordinal vs string) | Un type de filtre masque tout, un autre montre tout |
| 8.3.c | PRIAM-Right-service | `dataSubjectCategory` toujours `null` (id sujet confondu avec id catégorie) — détail en §8.8.a | Dashboard Provider vide malgré des demandes réelles en base |
| 8.4 | PRIAM-Data-service | `DataSubject.age` en `int` primitif | `500 PropertyAccessException` dès qu'un sujet a `age = NULL` |
| 8.4bis | PRIAM-Data-service | `processed_data.nb_occurrences` absent de `db_creation_script.sql` | `400`/`500` sur tout `/api/processed-data/add` ou `/remove`, dès le premier octroi de consentement OPTIONAL réel (pas seed SQL) |
| 8.5 | PRIAM-Consent-Service | `ConsentServiceImpl.create()` ne résout pas `processingName` | `500` en envoyant un nom lisible au lieu d'un id numérique |
| 8.5bis | PRIAM-Consent-Service | **Non corrigé** — `DataRestClient.removeProcessedData` (`@DeleteMapping` + `@RequestBody`) perd silencieusement son corps via le client Feign par défaut | Retrait de consentement "réussit" (`end_date` posé, `200`) mais `processed_data` reste inchangé — pas d'erreur visible |
| 8.7.a | Gateway / Auth | Gateway plante au boot sans `CUSTOM_OIDC_ISSUER_URI` | `jwkSetUri cannot be empty` au démarrage |
| 8.7.b | Gateway / Auth | CORS inefficace sur les routes proxifiées | Préflight `OPTIONS` en `401`, invisible en curl |
| 8.7.c | Gateway / Auth | Pas de rafraîchissement automatique du token OIDC | "No data available" après ~5 min de session |
| 8.7.d | Gateway / Auth | Route M2M interne bloquée par l'auth JWT humaine | `500` sur Access Request dès qu'une donnée `INDIRECT`/`PRODUCED` est annotée |
| 8.7.e | Gateway / Auth | `CUSTOM_PROVIDER_URL` avec un chemin (pas juste `host:port`) silencieusement ignoré par le routage | `404` sur tout `/provider/**` malgré une variable d'env correcte dans le conteneur |
| 8.8.a | PRIAM-Frontend | `dataSubjectId`/id codé en dur (`ar-selection`, puis retrouvé dans `rectification`/`suppression`) | Page vide, ou demande réussie mais sur le mauvais sujet |
| 8.8.b | PRIAM-Frontend | `getIdReference()` cassé pour `idRef` non numérique | Cases décochées, rien ne persiste, pages vides |
| 8.8.c | PRIAM-Frontend | Page "My Requests" jamais rafraîchie | Statut figé sur "Pending" après approbation |
| 8.8.d | PRIAM-Frontend | Bouton d'approbation `(click)` + `routerLink` sur le même élément | "La demande n'est pas traitée" alors qu'elle a réussi |
| 8.8.e | PRIAM-Frontend | `requireHttps: environment.production` bloque la redirection OIDC en local | Page blanche permanente, aucune erreur console |
| 8.8.f | PRIAM-Frontend | Toggle `NECESSARY`/`MANDATORY` affiché décoché sur la page Consent | Laisse croire qu'aucun traitement nécessaire n'est actif |
| 8.8.g | PRIAM-Frontend-Provider | `/api` dupliqué dans l'appel `DataSubjectCategories` (déjà inclus dans `environment.api_actor`) | `404`, filtre "Data Subject Categories" du dashboard toujours vide |
| 8.9-P | Environnement (côté PRIAM) | CRLF / `gradle build` vs `assemble` / build du Gateway depuis le code source / heredoc BuildKit | Voir groupe détaillé |

### 8.2 Pont Provider — contrat de réponse (côté PRIAM)

**a. `dataAccessRight` renvoie un objet nu au lieu d'un tableau.** Symptôme le plus
trompeur du catalogue : `GET /right/api/personalDataValues/accessRight` renvoie `[]` alors
que la donnée existe bel et bien côté appli cible, sans aucune erreur ni côté
`PRIAM-Right-service` ni côté appli cible — le pont Provider répond `200 OK` avec un corps
valide. Cause : `ProviderRestClient.getPersonalDataValues` est typé `List<Map<String,
String>>` côté Feign — une implémentation qui renvoie l'objet directement (`{"name":
"..."}`) au lieu d'un tableau (`[{"name": "..."}]`) passe la désérialisation JSON basique
mais Feign/Jackson mappe silencieusement le résultat vers une liste vide plutôt que de
lever une erreur explicite. Correctif : toujours répondre avec un tableau, même à un seul
élément ; `[]` est la réponse correcte quand aucun enregistrement ne correspond à `idRef`.

**b. `idRef` interne PRIAM envoyé au lieu de l'identifiant externe.** Symptôme : même
après avoir corrigé l'enveloppe tableau (8.2.a), la valeur reste introuvable, alors qu'une
requête manuelle directement sur le pont Provider avec le bon `idRef` fonctionne. Cause :
`DataRequestServiceImpl.DataAccess(int dataSubjectId, ...)` transmettait directement
`dataSubjectId` (l'id numérique **interne** PRIAM) comme paramètre `idRef` du Feign client,
sans jamais le résoudre via `ActorRestClient.getDataSubject(dataSubjectId).getIdRef()`. Ce
bug est invisible sur tout cas d'étude dont l'`id_ref` est une chaîne numérique qui
coïncide par hasard avec le `dataSubjectId` interne (ex. `id_ref='1'`) — un `idRef` non
numérique (ou simplement différent) l'expose immédiatement. Corrigé (générique,
`PRIAM-Right-service`) : `ProviderRestClient.getPersonalDataValues` retypé `idRef` en
`String`, `DataRequestServiceImpl.DataAccess` résout `idRef` avant l'appel Feign.

**c. Contrat `{"attribute":..., "value":...}` vs `{champ: valeur}` incohérent entre
`PRIAM-Right-service` et `PRIAM-Data-service`.** Les deux microservices possèdent chacun
leur propre copie du client Feign `ProviderRestClient` — pas de code partagé.
`DataService.java` (`getProcessedPersonalDataList`,
`getProcessedIndirectAndProducedPersonalDataList`) attendait le format
`{"attribute": "...", "value": "..."}` de l'ancien `Provider-microservice` générique
(port 8086), alors que le contrat réellement documenté (§2 du playbook) et implémenté par
toute appli cible correcte est `{champ: valeur}` directement — `NullPointerException` dès
qu'on demandait un Access Request, silencieux jusqu'à inspection des logs `priam-data-ms`.
Si vous retrouvez ce bug ailleurs : chercher tout endroit qui lit une réponse Provider avec
`.get("attribute")`/`.get("value")` plutôt que `.get(<nomDuChamp>)` directement.

**d. `attributes` encodé en paramètres répétés.** `ProviderRestClient
.getPersonalDataValues` déclarait `attributes` en `List<String>` — OpenFeign encode par
défaut une collection en paramètres de requête **répétés**
(`attributes=a&attributes=b`), alors que le contrat (§2 du playbook) et toute appli cible
qui parse `attributes` comme une valeur simple attend un seul paramètre `attributes=a,b`.
Corrigé : paramètre Feign retypé `String`, la liste est jointe (`String.join(",",
attributes)`) avant l'appel — présent aussi bien dans `PRIAM-Right-service` que
`PRIAM-Data-service` (deux copies distinctes du client, voir 8.2.c — vérifier les deux si
vous retrouvez ce bug).

**e. Piège déjà corrigé — alignement par index entre colonnes.**
`DataService.getProcessedPersonalDataList`/`getProcessedIndirectAndProducedPersonalDataList`
(`PRIAM-Data-service`) appelaient le pont Provider **une fois par colonne annotée** (pas
une fois par `dataType` avec tous les attributs groupés, malgré le contrat §2), puis
recollaient les résultats par position : `PRIAM-Frontend` (tableau "Data List" de la page
Access Request) itère sur `dataType.data[0].dataValue` comme source de longueur puis lit
`dataType.data[N].dataValue[i]` pour chaque colonne au même index `i`, en supposant que
l'élément `i` de chaque colonne appartient au même enregistrement réel **dans tous les
appels séparés**. Ça n'était vrai que si le pont Provider de l'appli cible renvoyait ses
enregistrements dans un ordre stable et déterministe entre appels distincts — un ordre
non garanti pouvait désaligner silencieusement les colonnes d'un même enregistrement
affiché (ex. le solde d'un compte affiché à côté du nom d'un autre), sans aucune erreur.
Identifié en lisant le code, pas encore rencontré en échec réel.

Corrigé, à la racine plutôt qu'en reportant la charge sur chaque appli cible : `DataService`
regroupe désormais les colonnes par `dataType` (`Collectors.groupingBy`) et fait **un
seul** appel Provider par `dataType`, avec toutes les colonnes demandées ensemble
(`addDataValuesForDataType`, méthode extraite, utilisée pour les données directes et
indirectes/produites). Toutes les colonnes d'un même `dataType` sont ensuite extraites de
la **même** réponse — le désalignement devient structurellement impossible, pas seulement
évité par une consigne de tri côté appli cible. Bénéfice secondaire : moins d'appels HTTP
au pont Provider (un par `dataType` au lieu d'un par colonne).

### 8.3 PRIAM-Right-service

**a. Double réponse à une même demande.** Rien n'empêchait `POST /api/right/answer`
d'être appelé deux fois pour le même `dataRequestId` (pas de contrainte d'unicité SQL, pas
de vérification applicative). Conséquence, pas juste un doublon inoffensif :
`requestAnswerRepository.findDataRequestAnswerByDataRequest_DataRequestId` retourne un
`Optional<DataRequestAnswer>` — dès qu'un doublon existe, **toute lecture qui passe par
cette méthode plante** en `HibernateException: More than one row...`, y compris `GET
/right/api/right/requestList`, qui tombe entièrement. Symptôme observable : "No requests
available" malgré des données par ailleurs correctes en base. Corrigé : garde en tête de
`DataRequestServiceImpl.saveRequestAnswer`, qui vérifie si une réponse existe déjà et lève
une exception explicite (remontée en `409 Conflict`, pas un `500` générique) avant toute
tentative d'insertion. Si vous rencontrez l'erreur Hibernate malgré ce correctif, chercher
un doublon préexistant (`SELECT data_request_id, COUNT(*) FROM data_request_answer GROUP
BY data_request_id HAVING COUNT(*) > 1`) et le nettoyer manuellement — le correctif
empêche les *nouveaux* doublons, pas ceux déjà présents avant son déploiement.

Piège adjacent, source fréquente de doublons côté frontend : un bouton d'approbation avec
**à la fois** `(click)="postCompletedRectificationRequest()"` **et** `routerLink` sur le
même élément déclenche les deux — la requête HTTP part, et la navigation a lieu
immédiatement sans attendre la réponse, poussant l'utilisateur à re-cliquer depuis le
dashboard en pensant que ça a échoué. Voir §8.8.d pour le correctif frontend complet.

**b. Filtre `requestList` par type cassé (ordinal vs string).** Dashboard Provider :
cocher **uniquement** "Access" dans le filtre "Request Types" n'affiche **aucune**
demande, alors que cocher "Rectification" ou "Erasure" affiche **toutes** les demandes.
Cause : `DataRequestServiceImpl.getDataRequestByFilters` convertissait les types
sélectionnés en **ordinal** de l'enum Java `DataRequestType { RECTIFICATION, ERASURE,
ACCESS }` (0/1/2), puis passait ces entiers à une requête SQL **native**
(`WHERE dr.data_request_type IN :types`) sur une colonne `varchar(25)`
(`@Enumerated(EnumType.STRING)`) contenant les vraies chaînes. MySQL convertit
implicitement une chaîne non numérique en `0` lors d'une comparaison avec un entier :
`IN (0)` (RECTIFICATION) matche donc toutes les lignes, tandis que `IN (1)`/`IN (2)` ne
matchent jamais rien. Corrigé : `findByTypes` accepte `List<String>`, transmis tel quel.
Si vous ajoutez une nouvelle requête native comparant un champ `@Enumerated(EnumType
.STRING)` à des valeurs fournies par l'appelant, vérifiez que ce sont bien des chaînes et
pas des ordinaux.

**c.** Voir §8.8.a — le vrai correctif (champs plats `dataSubjectCategoryId`/
`dataSubjectCategoryName` sur l'entité `DataSubject` de `PRIAM-Right-service`) est décrit
en détail dans le groupe 8.8 ci-dessous, avec le symptôme frontend qui l'a fait découvrir.

### 8.4 PRIAM-Data-service — `DataSubject.age` en `int` primitif

Les entités `DataSubject` de `PRIAM-Actor-service`, `PRIAM-Data-service`,
`PRIAM-Right-service`, `PRIAM-Consent-Service` (et les DTOs Actor) déclaraient `age` en
`int` primitif. La colonne SQL `data_subject.age` est nullable — un sujet dont l'âge n'est
pas connu ou pertinent (cas courant : un compte utilisateur générique) a `age = NULL`.
Hibernate ne peut pas assigner `null` à un primitif : toute lecture d'un tel `DataSubject`
(y compris via Feign, où Jackson a le même problème) plante en `500
(PropertyAccessException)`, bloquant silencieusement droits, résolution d'idRef, décision
de consentement. Corrigé dans les 5 fichiers concernés (`age` → `Integer`). Si vous
retrouvez `private int age;` sur une copie du dépôt, appliquez le même correctif partout
où le champ apparaît (entités **et** DTOs — un seul endroit oublié suffit à faire
replanter la chaîne).

### 8.4bis PRIAM-Data-service — colonne `nb_occurrences` manquante dans le schéma

`ProcessedData.java` (`priam.data.priamdataservice.entities`) déclare un champ
`nbOccurrences` (mappé par Hibernate en `nb_occurrences`), utilisé comme compteur de
références par `ProcessedDataService.addProcessedData`/`removeProcessedData` :
incrémenté à chaque nouveau rapport du même `data_id` pour un sujet (plusieurs lignes
d'un type un-à-plusieurs qui partagent les mêmes colonnes annotées), décrémenté à la
suppression, la ligne n'étant réellement supprimée qu'une fois le compteur à 0 — un
mécanisme nécessaire dès qu'un `data_type` a plusieurs lignes par sujet. Mais
`Databases/db_creation_script.sql` créait `processed_data` sans cette colonne :

```sql
create table processed_data(
data_id int,
data_subject_id int,
primary key (data_id, data_subject_id), ...);
```

Symptôme : invisible sur les données seedées directement en SQL (le script d'annotation
insère les lignes `processed_data` sans jamais passer par Hibernate). Se déclenche au
premier appel réel de `POST /api/processed-data/add` (Consent Decision Point,
`ConsentServiceImpl.create()`, à l'octroi d'un consentement OPTIONAL réel via l'API) ou
`/remove` : `SQLSyntaxErrorException: Unknown column 'processedd0_.nb_occurrences' in
'field list'`, remonté comme `400 Bad Request` (`addProcessedData`) ou `500` (le CDP,
via son propre appel Feign vers Actor puis Data-service). Rencontré et reproduit en
conditions réelles pendant l'intégration Bank of Anthos : le toggle "Contact Management"
de la page Consent (PRIAM-Frontend) affichait un état coché côté UI (mise à jour
optimiste avant la réponse serveur) alors que l'appel réel avait échoué en `500` —
symptôme trompeur, confirmé uniquement en lisant les logs `priam-consent-ms`/
`priam-data-ms` et en relisant l'état réel de `processed_data` en base (playbook §7 :
un état visuel côté UI ne prouve rien de plus qu'un `200` HTTP). Corrigé en ajoutant la
colonne (`nb_occurrences int not null default 1`) à `db_creation_script.sql`. Si vous
retrouvez ce symptôme sur une copie du dépôt, la table a probablement été initialisée
avant ce correctif — un volume MySQL vierge (rebuild de `mysqldb` + volume de données
effacé) suffit, `db_creation_script.sql` n'étant exécuté que sur un volume vierge comme
`db_insertion_script.sql` (playbook §1).

### 8.5 PRIAM-Consent-Service — résolution de `processingName`

**Mécanisme générique déjà en place** (à connaître avant de chercher à le réimplémenter) :
`ContractServiceImpl.getConsentByDataSubject` (le CDP, lecture) résout un `processingId`
non-numérique via un appel Feign vers `Data-service` : `GET /api/processing/byName/{name}`.
Aucun hardcoding par application.

**Piège déjà corrigé — `ConsentServiceImpl.create()` (la bascule effective
accorder/retirer) ne faisait pas la même résolution.** `POST
/cdp/api/consent/create/{idRef}` avec `{"processingId": "portfolio-sharing"}` (nom
lisible) renvoyait `500`, alors que la lecture (CDP) acceptait déjà un nom générique et
qu'un id numérique fonctionnait. Cause : `ConsentServiceImpl.create` transmettait
`processingId` tel quel à `getDataIds`/`addProcessedData`/`removeProcessedData`, qui
attendent un `int` côté `Data-service` — d'où un `400` remonté et enveloppé en `500`
("Error saving new consent"). Corrigé : même résolution que le CDP ajoutée en tête de
`create()`, avec la forme numérique résolue réécrite sur l'objet `Consent` avant sauvegarde
pour que les lectures ultérieures restent cohérentes.

**Piège déjà corrigé — le CIP (`getListConsentByDataSubject`) ne résolvait pas non plus
`processingName`, contrairement au CDP qui l'appelle.** Symptôme : après avoir accordé un
consentement, `has_pending_consent_decision()` (§4bis du playbook, endpoint CIP
`/api/contract/list/consents/{idRef}/{processingId}`) continuait à répondre "aucune
décision" indéfiniment si on lui passait un nom lisible — le flag `priamConsentRequired`
restait bloqué à `true` pour toujours, malgré un consentement réellement enregistré. Cause :
`getListConsentByDataSubject` comparait `processingId` tel quel
(`c.getProcessingId().equals(processingId)`), une comparaison de chaîne contre l'id
numérique stocké — ne matchait jamais si on passait un nom. Le CDP
(`getConsentByDataSubject`) résolvait bien le nom avant de l'appeler, ce qui masquait le
bug pour les appelants du CDP, mais pas pour un appel direct au CIP. Corrigé : résolution
déplacée dans `getListConsentByDataSubject` elle-même (source unique), la duplication dans
le CDP retirée.

**d. Précision `date` insuffisante sur `consent.start_date`/`end_date` — bascules muettes
après la première du jour.** Symptôme (trouvé lors de l'intégration Ghostfolio, en testant
plusieurs bascules accorder/retirer le même jour depuis un vrai navigateur) : décocher un
processing `OPTIONAL` sur la page Consent semble ne rien faire après le tout premier
grant/retrait de la journée - le toggle visuel se remet dans le bon état après un
rafraîchissement manuel (§8.8.c), mais l'état réel côté base ne change plus. Cause :
`ConsentServiceImpl.create()` retrouve "le consentement courant à basculer" via
`ORDER BY start_date DESC LIMIT 1` (`ConsentRepository`) ; la colonne était en `date`
(précision jour), donc tous les consentements créés le même jour calendaire sont à égalité
sur `start_date`, et ce `ORDER BY` ne peut plus les départager - MySQL retombe sur une ligne
arbitraire. La première bascule du jour fonctionne (une seule ligne, pas d'ambiguïté) ;
chaque bascule suivante retrouve la même ligne déjà fermée et ne fait qu'ajouter des lignes
"accordé", sans jamais en refermer. Corrigé : `start_date`/`end_date` élargis de `date` à
`datetime` dans `db_creation_script.sql`, et `@Temporal(TemporalType.TIMESTAMP)` ajouté sur
les deux champs `Date` de `Consent.java` (sans l'annotation, Hibernate persiste quand même en
précision jour malgré le type SQL `datetime` de la colonne). Vérifié avec 4 bascules
successives le même jour, alternant correctement accordé/retiré à chaque fois.

### 8.5bis PRIAM-Consent-Service — `removeProcessedData` perd son corps DELETE (NON CORRIGÉ)

**Statut : trouvé pendant l'intégration Bank of Anthos, documenté mais pas corrigé dans
cette session** (voir le compromis expliqué plus bas) — contrairement à toutes les
autres entrées de ce fichier, qui sont des bugs déjà résolus.

`ConsentServiceImpl.create()` (le toggle grant/revoke, `POST /api/consent/create/{idRef}`)
appelle, sur retrait (`existingConsent.getEndDate() == null` → cas 1a) :
```java
processingRestClient.removeProcessedData(dataSubjectId, processingRestClient.getDataIds(processingId));
```
`DataRestClient.removeProcessedData` (`PRIAM-Consent-Service/.../openfeign/DataRestClient.java`) :
```java
@DeleteMapping("/data/api/processed-data/remove")
public ResponseEntity<String> removeProcessedData(@RequestParam int subjectId, @RequestBody List<Integer> dataIds);
```

Reproduit en conditions réelles (Bank of Anthos, sujet `priamqa5`, `data_subject_id=5`) :
1. Consentement OPTIONAL accordé puis un enregistrement réel créé (une ligne
   `processed_data(data_id=11..14, data_subject_id=5, nb_occurrences=1)` par le mécanisme
   `report_processed_data` côté application cible, playbook §4bis).
2. Retrait du consentement via un vrai clic sur le toggle PRIAM-Frontend : `200`, le
   `consent.end_date` est bien posé en base (confirmé) — **mais** `processed_data`
   reste identique (`nb_occurrences=1`, ligne toujours présente) — vérifié par lecture
   directe de la table (playbook §7), pas seulement par le code `200`.
3. Isolation : appeler directement `DELETE /api/processed-data/remove?subjectId=5` avec
   le même corps JSON (`[11,12,13,14]`) **contre `PRIAM-Data-service` directement**
   (`ProcessedDataService.removeProcessedData`, sans passer par le Feign client de
   Consent-Service) fonctionne parfaitement : les 4 lignes sont supprimées. Le bug n'est
   donc pas dans `ProcessedDataService` mais dans la façon dont `ConsentServiceImpl`
   l'appelle.

**Cause probable** (non confirmée par un test isolé du client Feign faute de temps dans
cette session) : Feign utilise par défaut `java.net.HttpURLConnection` comme client HTTP
(aucune dépendance `feign-httpclient`/`feign-hc5`/OkHttp dans
`PRIAM-Consent-Service/build.gradle`), un client historiquement connu pour ignorer ou mal
transmettre le corps d'une requête `DELETE` — contrairement à `addProcessedData`
(`@PostMapping` + `@RequestBody`, fonctionne correctement, confirmé par le test de grant
qui a réussi dans la même session). `getDataIds` (`GET
/data/processing/data-usage/DataIds/{processingId}`, sans corps) fonctionne aussi
correctement, confirmé par appel direct — ce n'est donc pas un problème de chemin
Gateway, seulement de corps sur `DELETE`.

**Piste de correctif, non appliquée ni testée dans cette session** : ajouter un client
Feign supportant réellement `DELETE` + corps (`implementation
'io.github.openfeign:feign-httpclient'` dans `build.gradle` + `feign.httpclient.enabled=
true`), ou changer `removeProcessedData` en `@PostMapping` avec un chemin dédié (`/api/
processed-data/remove`, un verbe HTTP moins strict sur le corps qu'un `DELETE`) côté
`PRIAM-Data-service` + `DataRestClient`.

**Pourquoi non corrigé dans cette session** : la correction nécessite soit une
reconfiguration du client HTTP Feign (dépendance + propriété + rebuild Gradle +
re-test), soit un changement de contrat d'API touchant `ProcessedDataController`
(`PRIAM-Data-service`) en plus de `DataRestClient` (`PRIAM-Consent-Service`) — un
changement à deux services qu'il n'était pas raisonnable de livrer sans le temps de le
valider correctement dans le budget de cette session d'intégration. **Impact réel
limité** : la décision de consentement elle-même (`consent.end_date`, ce que
`get_consent()`/CDP lisent réellement, playbook §4) est correcte et déjà testée
end-to-end (§7) — seul le compteur de bookkeeping auxiliaire `processed_data` reste
périmé après un retrait, ce qui peut laisser une colonne visible sur la page Access
Request d'un sujet après qu'il a retiré son consentement pour le traitement
correspondant, jusqu'à un appel manuel équivalent à l'étape d'isolation ci-dessus.

### 8.7 Gateway / CORS / Auth

**a. Piège déjà corrigé — la Gateway plante au démarrage sans `CUSTOM_OIDC_ISSUER_URI`,
malgré le fail-open documenté (§6 du playbook).** Symptôme : `jwkSetUri cannot be empty` au
boot, alors que `SecurityConfig.java` est censé se rabattre sur `permitAll()` quand aucun
émetteur OIDC n'est configuré. Cause : Spring Boot active
`ReactiveOAuth2ResourceServerAutoConfiguration` dès que la propriété
`spring.security.oauth2.resourceserver.jwt.issuer-uri` est une clé **présente** dans
l'environnement, même résolue à `""` — indépendamment du bean `SecurityWebFilterChain`
manuel de `SecurityConfig.java`, qui ne s'appuie jamais sur ce bean auto-configuré.
Corrigé : `ReactiveOAuth2ResourceServerAutoConfiguration.class` exclue explicitement dans
`@SpringBootApplication(exclude = {...})` de `GatewayApplication.java`, aux côtés de
`R2dbcAutoConfiguration.class` déjà exclue.

**b. CORS inefficace sur les routes proxifiées de la Gateway.** Une annotation
`@CrossOrigin` posée sur `GatewayApplication` (patron Spring MVC classique) n'a **aucun**
effet sur les routes que `RouteLocator` proxifie (Spring Cloud Gateway reactive) : ces
routes ne passent jamais par le dispatch annoté que `@CrossOrigin` intercepte. Symptôme
côté navigateur : le preflight `OPTIONS` reçoit `401` sans header `Access-Control-Allow-*`,
donc **tout** appel cross-origin est bloqué silencieusement côté navigateur — alors que les
mêmes appels réussissent en curl (pas de préflight en curl). Corrigé :
`SecurityConfig.java` configure CORS explicitement dans la `SecurityWebFilterChain`,
origines pilotées par `CUSTOM_FRONTEND_ORIGINS` (liste séparée par virgules) — la variable
elle-même reste à régler par cas d'étude (voir playbook, checklist point 13), mais le
mécanisme qui la lit est déjà en place et n'a plus besoin d'être touché.

**c. Pas de rafraîchissement automatique du token OIDC.** Les tokens d'accès Keycloak
expirent par défaut après 5 min. Ni `PRIAM-Frontend` ni `PRIAM-Frontend-Provider`
n'appelaient `oauthService.setupAutomaticSilentRefresh()` après connexion — passé ce délai,
**tous** les appels API échouent silencieusement en `401` (pas de redirection vers le
login, juste des données qui n'arrivent plus), symptôme du type "No data
available"/"No requests available" alors que les filtres et les données en base sont
corrects. Corrigé : `setupAutomaticSilentRefresh()` appelé à la fin de `oidcInitializer`
dans les deux `app.module.ts` — déjà en place dans les deux frontends PRIAM existants,
rien à ajouter côté application cible.

**d. Route M2M interne bloquée par l'auth JWT humaine.** `GET
/data/processedPersonalDataList/{idRef}` et `.../purposes/{idRef}` plantaient en `500` dès
qu'un item `INDIRECT`/`PRODUCED` avait une ligne `processed_data`. Cause :
`DataService.getProcessedPersonalDataList` appelle
`rightRestClient.isDataRequestAcceptedForDataId(...)` pour chaque donnée
`INDIRECT`/`PRODUCED` — un appel Feign machine-à-machine vers `PRIAM-Right-service GET
/api/isAccepted`. Mais `/right/**` est classé route **humaine** (JWT requis) dans
`SecurityConfig.java` — `isAccepted` n'avait jamais été distingué du reste de `/right/**`,
donc cet appel M2M interne se fait rejeter en `401`, non rattrapé côté `DataService`
(`FeignException$Unauthorized` remonte tel quel, `500`). Corrigé : `/right/api/isAccepted`
ajouté aux routes M2M explicitement listées dans `SecurityConfig.java`, sans toucher au
reste de `/right/**`.

**e. `CUSTOM_PROVIDER_URL` avec un chemin non racine silencieusement ignoré par le
routage.** Rencontré lors de l'intégration TeaStore : le pont Provider y vit dans le même
WAR Tomcat que le reste de la persistence, déployé sous un chemin de contexte non-racine
(`/tools.descartes.teastore.persistence`, dérivé du nom du WAR) plutôt qu'à la racine du
serveur — `CUSTOM_PROVIDER_URL=http://persistence:8080/tools.descartes.teastore.persistence`
était donc nécessaire pour que `{CUSTOM_PROVIDER_URL}/api/...` (playbook §2) résolve
correctement. Symptôme : `404 Not Found` sur **tout** appel `/provider/**` passant par la
Gateway (confirmé en curlant la Gateway directement, `/actuator/gateway/routes` en DEBUG
logs), alors que le même appel direct au conteneur cible (`curl
http://persistence:8080/tools.descartes.teastore.persistence/api/...`) réussissait, et que
la variable d'environnement `CUSTOM_PROVIDER_URL` était bien correcte dans le conteneur
Gateway (`docker exec priam-api-gateway env`) — un piège trompeur qui pointe d'abord vers
un conteneur périmé (§8.9 du playbook) avant de pointer vers la vraie cause. Cause réelle,
confirmée en lisant les logs `DEBUG` de `RoutePredicateHandlerMapping`/
`ObservedRequestHttpHeadersFilter` de Spring Cloud Gateway : `RouteToRequestUrlFilter` ne
reprend que le schéma/hôte/port de `Route.getUri()` pour construire l'URL sortante — tout
segment de chemin présent dans cette URI est silencieusement ignoré, le chemin réellement
transmis restant celui déjà réécrit par le filtre `rewritePath` de la route (`/api/...`,
sans le préfixe de contexte). `GatewayApplication.java` passait pourtant
`providerServiceURL` tel quel (avec son chemin) à `.uri(...)` sur la route `/provider/**` —
correct pour toute appli cible dont le pont vit à la racine (tous les cas d'étude
précédents), mais silencieusement cassé dès qu'un chemin non vide y figure. Corrigé :
`.uri(...)` de la route `/provider/**` ne reçoit plus que le schéma+autorité
(`providerBaseUri()`, nouvelle méthode) ; si `providerServiceURL` porte un chemin, il est
réinjecté explicitement via `.prefixPath(path)`, chaîné après `rewritePath` dans le filtre
de la route — sans effet sur les autres routes ni sur un `CUSTOM_PROVIDER_URL` sans chemin
(comportement inchangé pour tout cas d'étude existant, chemin vide → branche identique à
avant). Vérifié par le vrai workflow PRIAM-Right-service (`POST
/api/right/rectificationRequest` puis `/api/right/answer` avec `answer:true`) : `404`
reproduit avant le correctif, `200` + changement réel en base TeaStore après reconstruction
de la Gateway.

### 8.8 PRIAM-Frontend (Angular)

**a. `ar-selection` toujours vide (id codé en dur).**
`pages/ar-selection/ar-selection.component.ts` (page "Do an access Request", données
indirectes/produites) avait `referenceId: number = 507` codé en dur au lieu de
`this.securityService.getIdReference()` — la page interrogeait donc toujours un `id_ref`
qui n'existe dans aucun cas d'étude réel. `postAccessRequest()` avait le même problème
dans l'autre sens (`dataSubjectId: 1` codé en dur). Corrigé : les deux utilisent
`this.referenceId` dérivé de `securityService.getIdReference()`, avec garde `null`.

**b. `getIdReference()` cassé pour tout `idRef` non numérique.** Symptômes observés (cas
d'étude avec `idRef` non numérique, ex. un UUID) : (1) les traitements `NECESSARY`
s'affichent décochés alors qu'ils devraient toujours l'être ; (2) cocher un traitement
`OPTIONAL` puis recharger la page — le choix n'est jamais enregistré ; (3) la page Access
Request n'affiche ni données ni traitements. Aucune erreur réseau visible. Cause commune :
`SecurityService.getIdReference()` faisait `parseInt(String(value), 10)` sur la claim
`idReference` du token puis renvoyait `null` si le résultat était `NaN`. Chaque composant
consommateur garde ses appels réseau derrière `if (this.referenceId != null)` : avec
`referenceId === null`, ces gardes **suppriment silencieusement l'appel réseau
lui-même**, en lecture (pages vides) comme en écriture (le clic ne déclenche littéralement
aucune requête HTTP). Corrigé : `getIdReference()` renvoie désormais la chaîne brute
(`string | null`) — cohérent avec le reste de PRIAM, qui traite `idRef` comme une chaîne
partout ; tous les services/composants consommateurs retypés en conséquence.

**Piège composé, corrigé en même temps** : certains appels ont réellement besoin du
`dataSubjectId` **interne** numérique de PRIAM, pas de `idRef` — notamment la création
d'une demande d'accès et la page "My Requests". Nouveau service générique
`ActorService.getDataSubjectId(idRef)` (`GET /actor/api/DataSubjectId/{idRef}`) ajouté
pour résoudre `idRef → dataSubjectId` interne juste avant ces deux appels — voir §8.6 du
playbook principal pour la race condition possible sur ce même endpoint juste après une
inscription (ce point-là reste pertinent pour une application cible).

**Même bug retrouvé ailleurs, corrigé aussi — `dataSubjectId: 1` codé en dur dans
`RectificationComponent.postRectification()` et `SuppressionComponent.postSuppression()`.**
Le correctif initial n'avait été appliqué qu'à `ArSelectionComponent` (demandes d'accès) ;
les pages de soumission de rectification et suppression avaient le même patron fautif.
Symptôme à deux visages : (1) la demande soumise **réussit** et modifie réellement des
données — mais toujours celles du sujet `1`, jamais celles de l'utilisateur réellement
connecté ; (2) la page "My Requests" de ce même utilisateur reste vide. Corrigé en
appliquant le même patron que `ArSelectionComponent` aux deux composants :
`SecurityService.getIdReference()` puis `ActorService.getDataSubjectId(idRef)` avant de
construire le corps de la requête.

**Bug distinct, côté `PRIAM-Right-service` cette fois, trouvé en creusant le symptôme
ci-dessus — `DataRequestServiceImpl.getDataRequestByFilters()` et
`.getRequestDataDetail()` appelaient `actorRestClient.getDataSubjectCategoryById(dataRequest.getDataSubjectId())`,
traitant l'id du **sujet** comme si c'était l'id d'une **catégorie**.** Symptôme : le
tableau de bord Provider (`PRIAM-Frontend-Provider`) n'affichait aucune des demandes
récentes malgré leur présence réelle en base (`dataSubjectCategory` revenait `null`, et le
template affichait `request.dataSubjectCategory.dataSubjectCategoryName` sans garde `?.`,
ce qui fait planter le rendu de toute la liste). Corrigé à la racine : l'entité
`DataSubject` de `PRIAM-Right-service` ne déclarait qu'un champ imbriqué
`dataSubjectCategory` (`@ManyToOne`) jamais réellement peuplé par Jackson — le JSON réel
renvoyé par `Actor-service` (`DataSubjectResponseDTO`) porte `dataSubjectCategoryId`/
`dataSubjectCategoryName` en champs plats, pas imbriqués. Ajout de ces deux champs plats à
l'entité, suppression du second appel Feign fautif. Garde `?.` ajoutée en plus côté
template (`dashboard.component.html`) en défense en profondeur.

**c. Page "My Requests" jamais rafraîchie après approbation.**
`pages/requests/requests.component.ts` ne récupérait la liste des demandes et leur statut
qu'une seule fois, dans `ngOnInit()`. Un data subject qui soumet une demande, consulte "My
Requests" (statut "Pending", correct), puis attend l'approbation sans recharger
complètement la page ne voit jamais le statut passer à `FULL`/`PARTIAL`/`REFUSED`. Corrigé :
logique extraite dans une méthode `refresh()` réutilisable, appelée par `ngOnInit()` **et**
par un nouveau bouton "Refresh". **Même bug, même correctif** retrouvé sur
`access-request.component.ts` (page "Do an Access Request") lors de l'intégration
Ghostfolio.

**d. Bouton d'approbation `(click)` + `routerLink` sur le même élément.** Symptôme :
approuver une demande depuis `PRIAM-Frontend-Provider` affiche une erreur, "la demande
n'est pas traitée" — alors que la première tentative avait en réalité réussi. Cause :
`rectification.component.html`/`suppression.component.html` avaient le bouton
d'approbation avec **à la fois** `(click)="postCompletedRectificationRequest()"` **et**
`routerLink="/dashboard"` sur le même élément `<button>`. Angular déclenche les deux : la
requête HTTP part, **et** la navigation a lieu immédiatement, sans attendre la réponse —
l'utilisateur est redirigé avant de voir le résultat, ce qui pousse à re-cliquer depuis le
dashboard, ce qui déclenche le garde anti-double-réponse (§8.3.a), remonté en erreur.
Corrigé des deux côtés : `routerLink` retiré du bouton de soumission (laissé uniquement sur
"annuler"), navigation déplacée dans le callback de succès du `.subscribe()`, garde
`submitting` ajoutée pour bloquer un second clic pendant que la première requête est en
vol ; côté backend, l'exception de double-réponse remonte maintenant en `409` propre
(§8.3.a) plutôt qu'un `500` générique.

**e. `requireHttps: environment.production` bloque silencieusement toute redirection OIDC
en local.** Bug déjà présent dans `PRIAM-Frontend` **avant toute modification de ce projet**
(vérifié en comparant avec un clone vierge de PRIAM non touché) — pas une régression
d'intégration. Symptôme : page blanche permanente à l'ouverture de `PRIAM-Frontend`, aucune
erreur console, aucun appel réseau vers l'issuer OIDC, aucune redirection. Cause :
`angular-oauth2-oidc` (`OAuthService.validateUrlForHttps()`) traite `requireHttps: true`
comme "rejeter toute URL `http://`, sans exception" — seule la chaîne littérale
`'remoteOnly'` bénéficie de la dérogation `localhost`. Comme le `authorization_endpoint`
résolu depuis le document de découverte de Keycloak est `http://localhost:8080/...`
(Keycloak en HTTP en local), et que le build `production: true` de `PRIAM-Frontend`
transmet `requireHttps: true` via `authConfigFactory`, `initLoginFlow()` lève une exception
**de façon synchrone**, à l'intérieur d'un appel non attendu — la promesse de
l'`APP_INITIALIZER` est rejetée silencieusement, sans jamais atteindre le `.catch()` de
`main.ts`, et le bootstrap Angular reste bloqué indéfiniment. Corrigé : `requireHttps:
'remoteOnly'` dans les deux `app.module.ts` (`PRIAM-Frontend` et `PRIAM-Frontend-Provider`,
ce dernier par précaution).

**f. Toggle `NECESSARY`/`MANDATORY` affiché décoché sur la page Consent, alors qu'il
devrait être précoché et non révocable (§1 point 6 du playbook).** Symptôme : la section
"Necessary processing" de `consent.component.html` affiche chaque toggle décoché par défaut
(bien que désactivé, non cliquable), laissant croire qu'aucun traitement nécessaire n'est
actif. Cause : contrairement à la section "Optional processing" juste au-dessus
(`[checked]="isDisable(data) || isActivate(data)"`), le `<mat-slide-toggle>` de la section
"Necessary processing" n'avait **aucune** liaison `[checked]` du tout (oubli lors d'un
copier-coller, seul `[disabled]` était présent). Corrigé : `[checked]="isDisable(data)"`
ajouté.

**g. `/api` dupliqué dans l'appel `GET .../DataSubjectCategories` de
`PRIAM-Frontend-Provider`.** Rencontré lors d'un vrai test navigateur sur l'intégration
TeaStore (Playwright, dashboard Provider) : `GetDashboardService.getDataSubjectCategory()`
appelle `${environment.api_actor}/api/actor/DataSubjectCategories` — mais
`environment.api_actor` (build de production, `PRIAM-Frontend-Provider/Dockerfile`) vaut
déjà `http://<GATEWAY_ORIGIN>/actor/api` (préfixe `/actor` de la route Gateway + préfixe
`/api` propre à `PRIAM-Actor-service`), donc l'URL réellement appelée devient
`.../actor/api/api/actor/DataSubjectCategories` — `api` en double, `404`. Symptôme discret :
la page dashboard elle-même fonctionne (la liste des demandes charge normalement via un
autre appel), seul le filtre "Data Subject Categories" de la barre latérale reste vide en
permanence, facile à prendre pour une simple absence de catégories plutôt qu'un bug
d'URL — confirmé par lecture des logs réseau du navigateur (`404` sur cette seule requête)
et de la console (`[Error] getDataSubjectCategory(): HttpErrorResponse`), le reste du
dashboard (liste des demandes, page détail rectification avec l'appel `dataValue`)
fonctionnant normalement en parallèle. Corrigé : préfixe `/api` retiré de l'appel
(`${environment.api_actor}/actor/DataSubjectCategories`), cohérent avec le seul autre appel
du même service (`getFilteredRequests()`, qui n'a jamais eu ce préfixe en trop).

### 8.9-P Environnement Docker/Windows (côté PRIAM)

Sous-ensemble des pièges d'environnement qui concernent spécifiquement le code/les
Dockerfiles de PRIAM lui-même — déjà réglés dans ce dépôt, rien à reproduire côté
application cible. (Les pièges d'environnement qui touchent n'importe quel conteneur,
y compris ceux d'une application cible, restent dans `Docs/PRIAM-INTEGRATION-PLAYBOOK.md`
§8.9.)

- **Fins de ligne CRLF** : un `.gitattributes` racine force le LF sur `*.sh` et `gradlew`,
  donc un checkout frais ne devrait plus reproduire ce problème. Si un script casse au
  démarrage du conteneur (`exec ...: no such file or directory`, `gradlew: not found`) à
  cause du `\r` en fin de shebang, c'est que `.gitattributes` ne couvre pas ce fichier ou
  que le checkout est antérieur à son ajout. Vérifier avec `file <script>` ; corriger avec
  `sed -i 's/\r$//' <script>` si besoin, ou étendre `.gitattributes`.
- **`gradle build` vs `gradle assemble`** : tous les `Dockerfile` des microservices PRIAM
  doivent utiliser `RUN gradle assemble` (pas `gradle build`, qui exécute aussi les tests —
  un test qui échoue pour une raison propre à l'environnement du conteneur de build, sans
  rapport avec le code, fait planter tout le build de l'image).
- **`./gradlew` vs `gradle`** : préférer `RUN gradle assemble` (Gradle déjà présent dans
  l'image `gradle:<version>` de base) à `RUN ./gradlew assemble` (le wrapper télécharge sa
  propre distribution à chaque build où le cache Docker est invalidé — lent, premier point
  de défaillance sur un réseau instable).
- **Le service `gateway` du `docker-compose.yml` racine doit builder depuis
  `./PRIAM-Services/PRIAM-Gateway`** (code source local), pas consommer une image Docker
  distante préconstruite — sinon les correctifs déjà commités dans ce fichier ne sont pas
  forcément dans l'image réellement exécutée, et on redécouvre des bugs déjà corrigés. Déjà
  réglé dans le `docker-compose.yml` racine actuel ; à revérifier seulement si vous
  retrouvez une variante qui pointe vers une image `registry.gitlab.com/...` décommentée.
- **Un `RUN <<EOF ... EOF` heredoc BuildKit peut résoudre une substitution d'`ARG`/`ENV` en
  chaîne vide, silencieusement, sans erreur de build.** Rencontré sur
  `PRIAM-Frontend/Dockerfile` en ajoutant `TARGET_APP_URL` (§4ter du playbook) au bloc
  heredoc existant qui génère `environment.ts` : toutes les autres variables du même
  heredoc (`API_DATA`, `OIDC_ISSUER`, etc.) se substituaient correctement, seule la
  nouvelle `TARGET_APP_URL` ressortait vide dans le fichier généré. Cause racine non
  identifiée avec certitude. Contournement appliqué, déjà en place : le bloc heredoc
  remplacé par une séquence de `RUN echo "..." > fichier && echo "..." >> fichier && ...`
  individuels. **Si un futur build arg ressort vide dans un fichier généré par ce
  Dockerfile alors que sa valeur est bien passée en build arg, suspecter ce même piège
  heredoc avant toute autre hypothèse** — et éviter de réintroduire un bloc heredoc sur ce
  fichier précis.
