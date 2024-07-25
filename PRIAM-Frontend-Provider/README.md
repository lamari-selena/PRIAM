# ProviderPriam

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 16.2.4.

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via a platform of your choice. To use this command, you need to first add a package that implements end-to-end testing capabilities.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.

--------------------------------------------------------------

# Rapport Technique du Projet PRIAM

Ce rapport technique vise à fournir une documentation complète pour les composantes front-end de l'application PRIAM, à savoir le front-end user et le front-end provider.

---

## 1. Description du Projet

Le projet PRIAM se divise en deux principales parties :

- **Front-end user** : Interface conçue pour permettre aux utilisateurs finaux d'interagir avec les fonctionnalités offertes par l'application.
- **Front-end provider** : Interface dédiée aux fournisseurs pour gérer et administrer leurs services au sein de l'application PRIAM.

Les sections suivantes détaillent davantage les aspects techniques, l'architecture, et les technologies sous-jacentes à ces composantes front-end.

## 2. Technologies Utilisées

Le front-end de l'application a été développé en utilisant les technologies suivantes :

- **Angular** : Framework JavaScript pour construire des applications front-end, facilitant la création de composants réutilisables et d'une structure modulaire.
- **TypeScript** : Langage de programmation qui apporte la typage statique à JavaScript, permettant une meilleure maintenabilité et une détection d'erreurs efficace.
- **HTML** : Langage de balisage standard utilisé pour structurer le contenu de l'application web.
- **CSS** : Langage de feuille de style permettant de styliser et de personnaliser l'apparence des éléments HTML.
- **Angular Material** : Bibliothèque UI pour Angular qui fournit une collection de composants UI prêts à l'emploi, contribuant à une expérience utilisateur cohérente et moderne.
- **Theming d'Angular Material** : Utilisation des capacités de theming intégrées à Angular Material pour garantir un aspect visuel uniforme et professionnel à travers toute l'application PRIAM.

## 3. Architecture de l'Application

### Structure Générale

Les projets front-end user et provider de PRIAM partagent une architecture commune, bien que les fonctionnalités spécifiques diffèrent.

### Organisation du Projet

- **Interfaces** : Définition des structures d'objets utilisées au sein des applications.
- **Pages** : Correspondent à chaque interface/page de l'application.
- **Shared** : Contient des composants et des services potentiellement partagés entre les différentes pages de l'application.

### Fichiers Clés

- **app-routing.module.ts** : Définit les différentes URL de chaque composant et établit des routes entre eux via `router-outlet`.
- **app.module.ts** : Centralise tous les imports nécessaires au fonctionnement de l'application, y compris Angular Material, les modules pour les requêtes HTTP, et les composants spécifiques du projet.
- **Styles et Thèmes** : Utilisation de fichiers SCSS pour définir des palettes de couleurs, la typographie, et des thèmes personnalisés pour l'application et Angular Material.

## Documentation du Code

### Front-end User

#### Access-Request:

- **`getPersonalDataList(referenceId)`**
  - **Entrée**: `referenceId`
  - **Sortie**: Liste des valeurs des données personnelles de l'utilisateur.

- **`getPersonalDataListByPurpose(referenceId)`**
  - **Entrée**: `referenceId`
  - **Sortie**: Liste des finalités pour chaque donnée personnelle par processing.

- **`getPersonalDataListTransfer(referenceId)`**
  - **Entrée**: `referenceId`
  - **Sortie**: Liste des acteurs auxquels les données personnelles traitées sont transférées.

- **`getPrimaryKeys(dataType, rowIndex)`**
  - **Entrée**: `dataType`, `rowIndex`
  - **Sortie**: Liste des clés primaires présentes dans un tableau.

- **`getNonPrimaryKeys(dataType, rowIndex)`**
  - **Entrée**: `dataType`, `rowIndex`
  - **Sortie**: Liste des clés non primaires présentes dans un tableau.

#### AR-Selection:

- **`getIndirectAndGeneratedDataList(referenceId)`**
  - **Entrée**: `referenceId`
  - **Sortie**: Liste des noms des données indirectes et générées par data type.

- **`postAccessRequest(accessRequest)`**
  - **Entrée**: `accessRequest` (de type AccessRequest)
  - **Sortie**: Message du back-end.

#### Rectification:

- **`postRectification(rectification)`**
  - **Entrée**: `rectification` (de type Rectification)
  - **Sortie**: Message du back-end.

#### Suppression:

- **`postSuppression(suppression)`**
  - **Entrée**: `suppression` (de type Suppression)
  - **Sortie**: Message du back-end.

### Front-end Provider

#### Dashboard:

- **`getDataSubjectCategory()`**
  - **Entrée**: Aucune
  - **Sortie**: Liste des catégories d'utilisateurs gérées par l'application.

- **`getFilteredRequests(requestFilter)`**
  - **Entrée**: `requestFilter` (de type RequestFilter)
  - **Sortie**: Liste des demandes correspondant aux filtres sélectionnés.

- **`getSelectedRequest(requestId, requestType, response)`**
  - **Entrée**: `requestId`, `requestType`, `response`
  - **Sortie**: Informations nécessaires pour récupérer la demande sélectionnée sur le tableau de bord.

#### Access-Request:

- **`getSelectedAccessRequest(requestId, requestType)`**
  - **Entrée**: `requestId`, `requestType`
  - **Sortie**: Les choix de donnée réalisés par un utilisateur.

- **`getSelectedAccessRequestAnswer(requestId)`**
  - **Entrée**: `requestId`
  - **Sortie**: La réponse du provider à une demande d'accès, si elle existe.

- **`postCompletedAccessRequest(completedAccessRequest)`**
  - **Entrée**: `completedAccessRequest` (de type CompletedAccessRequest)
  - **Sortie**: Message du backend.

#### Rectification:

- **`getSelectedRectificationRequest(requestId, requestType)`**
  - **Entrée**: `requestId`, `requestType`
  - **Sortie**: Le choix de la donnée à rectifier par l'utilisateur.

- **`getSelectedRectificationRequestAnswer(requestId)`**
  - **Entrée**: `requestId`
  - **Sortie**: La réponse du provider à une demande de rectification, si elle existe.

- **`postCompletedRectificationRequest(completedRectificationRequest)`**
  - **Entrée**: `completedRectificationRequest` (de type CompletedRectificationSuppressionRequest)
  - **Sortie**: Message du backend.

#### Suppression:

- **`getSelectedSuppressionRequest(requestId, requestType)`**
  - **Entrée**: `requestId`, `requestType`
  - **Sortie**: Le choix de la donnée à supprimer par l'utilisateur.

- **`getSelectedSuppressionRequestAnswer(requestId)`**
  - **Entrée**: `requestId`
  - **Sortie**: La réponse du provider à une demande de suppression, si elle existe.

- **`postCompletedSuppressionRequest(completedSuppressionRequest)`**
  - **Entrée**: `completedSuppressionRequest` (de type CompletedRectificationSuppressionRequest)
  - **Sortie**: Message du backend.
