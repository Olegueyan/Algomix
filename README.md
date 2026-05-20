# Algomix - Gaetan Rousselin

Algomix est une application Android complète autour du Rubik's Cube. Elle combine un cube 3D interactif, un moteur de notation Rubik, des modes d'entraînement, une bibliothèque d'algorithmes, un timer, un scan caméra, un export PDF et une synchronisation cloud Supabase. L'application est pensée offline-first: les données sont utilisables localement, puis synchronisées quand le cloud est configuré.

## Sommaire

- [Présentation](#présentation)
- [Architecture DDD](#architecture-ddd)
- [Flux applicatif](#flux-applicatif)
- [Dépendances](#dépendances)
- [Fonctionnalités principales](#fonctionnalités-principales)
- [Build et qualité](#build-et-qualité)
- [Supabase](#supabase)
- [Moteur OpenGL](#moteur-opengl)
- [Tests et documentation](#tests-et-documentation)

## Présentation

Algomix est une application Android native Kotlin dédiée à la manipulation et à l'apprentissage du Rubik's Cube. Elle permet de visualiser un cube en 3D, d'exécuter des mouvements en notation Rubik, de jouer des séquences pas à pas, d'éditer des algorithmes, de gérer une bibliothèque locale, de chronométrer des solves et de synchroniser les données durables dans Supabase.

L'interface est construite avec ViewBinding et des layouts XML Material3, avec un thème orange (#E65100) compact dérivé des maquettes du projet. La navigation entre Accueil, Bibliothèque, Timer et Paramètres se fait via un `MaterialBottomNavigationView` combiné à un `ViewPager2`. Les fragments gèrent les insets système via `WindowInsetsCompat` pour rester lisibles sans scroll inutile.

Le cube affiché à l'écran est rendu avec OpenGL ES. Les règles Rubik ne sont pas écrites dans le renderer: elles vivent dans le domaine métier. Ces règles couvrent le parsing des notations (`R`, `U'`, `Rw`, `M`, `x`), l'application des mouvements, les rotations globales, les wide moves, les slice moves, les scrambles, la validation du cube, le playback et l'édition de séquences.

Le domaine désigne ici la couche métier pure du projet. Elle contient les modèles et règles Rubik sans dépendre d'Android, d'OpenGL, de Room, de Supabase ou de CameraX. Le renderer reçoit seulement un état de cube déjà calculé et se charge de l'afficher.

## Architecture DDD

DDD signifie Domain-Driven Design. Dans Algomix, cette approche sert à placer les règles importantes du Rubik et les données métier au centre du code, puis à brancher Android, Room, Supabase, CameraX ou OpenGL autour de ce noyau.

Cette structure permet de remplacer Supabase par Firebase, Appwrite ou un backend maison, et CameraX par une autre solution de scan, sans réécrire le domaine ni les écrans. Les points de remplacement sont les ports applicatifs: ils décrivent ce que l'application attend, sans imposer une technologie précise.

| Couche | Rôle dans Algomix | Exemples de classes |
| --- | --- | --- |
| `domain` | Contient les règles métier pures: cube, notation, validation, bibliothèque, timer, préférences, scan et session locale. | `CubeState`, `FaceletCube`, `MoveParser`, `MoveExecutor`, `CubeValidator`, `EditingSession`, `PlaybackState`, `LibraryModels`, `UserPreferences`, `CubeSessionCodec` |
| `application` | Définit les contrats et types applicatifs utilisés par les ViewModels et l'infrastructure. | `AppResult`, `AppError`, `ClockProvider`, `LibraryRepository`, `TimerRepository`, `CloudAuthGateway`, `CloudSyncGateway`, `PdfExporter`, `CubeScanner` |
| `infrastructure` | Implémente les ports avec des technologies concrètes. Cette couche sait parler à Room, DataStore, Supabase, WorkManager, CameraX, PDF et OpenGL. | `AlgomixDatabase`, `LocalLibraryRepository`, `SupabaseAuthGateway`, `SupabaseCloudSyncGateway`, `CloudSyncWorker`, `CameraXCubeScanner`, `LocalPdfExporter`, `RubikRenderer` |
| `ui` | Contient les fragments XML, les ViewModels, les états UI et les composants visuels. | `AlgomixActivity`, `HomeFragment`, `LibraryFragment`, `TimerFragment`, `SettingsFragment`, `ScanFragment`, `RubikCubeView`, `SharedCubeViewModel`, `LibraryViewModel` |

## Flux applicatif

Le flux général est volontairement unidirectionnel côté interface: les écrans envoient des actions aux ViewModels, les ViewModels utilisent des ports, l'infrastructure exécute le travail concret, et le domaine garde les règles stables.

```text
UI Fragments (XML + ViewBinding)
    |
    v
ViewModels
    |
    v
Ports applicatifs
    |
    v
Infrastructure
    |
    v
Domain métier
    |
    v
ViewModels, puis rendu par l'UI
```

Lecture du schéma:

- `UI`: fragments XML avec ViewBinding, dialogs Material3 et `RubikCubeView` (GLSurfaceView) pour le cube OpenGL.
- `ViewModels`: état d'écran, orchestration des actions et feedback utilisateur.
- `Ports applicatifs`: interfaces stables comme `LibraryRepository`, `CloudSyncGateway` ou `CubeScanner`.
- `Infrastructure`: implémentations concrètes avec Room, DataStore, Supabase, CameraX, WorkManager, PDF et OpenGL.
- `Domain métier`: règles Rubik et modèles durables, sans dépendance technique Android.

## Dépendances

| Catégorie | Dépendance | Rôle dans Algomix |
| --- | --- | --- |
| Base Android | Android Gradle Plugin 9.0.1, compileSdk 36, minSdk 31 | Configuration du projet Android et compatibilité SDK. |
| Kotlin / Java | Kotlin, JDK 21, Gradle wrapper | Compilation, tests et build reproductible. |
| UI | Material3 (MDC Android), ViewBinding, Fragment KTX, ViewPager2, ConstraintLayout | Fragments XML, dialogs Material3, bottom navigation et composants partagés alignés sur les maquettes. |
| Architecture | Lifecycle, ViewModel, Coroutines | États UI, traitements asynchrones et lifecycle Android. |
| Persistance locale | Room, KSP, DataStore Preferences | Données durables locales, préférences et snapshot de session. |
| Cloud | Supabase Kotlin Auth, Supabase PostgREST, Ktor Android | Auth email/password, récupération et synchronisation des données cloud. |
| Sync | WorkManager | Push automatique des mutations locales via outbox. |
| Scan | CameraX | Preview caméra, analyse d'image et capture des faces. |
| Export | `PdfDocument`, FileProvider | Génération et partage local de fiches PDF. |
| Rendu | OpenGL ES | Affichage 3D interactif du cube. |
| Qualité | JUnit, Robolectric, Espresso, Detekt, Android Lint | Tests unitaires, tests Robolectric ciblés, tests instrumentés Espresso, format, analyse statique et lint. |

## Fonctionnalités principales

| Fonctionnalité | Couches concernées | Fonctionnement |
| --- | --- | --- |
| Cube 3D | `domain/cube`, `ui/components/rubik`, `infrastructure/rendering/rubik` | Le domaine calcule l'état du cube; le mapper le convertit pour le renderer OpenGL. |
| Accueil | `HomeFragment`, `RubikCubeView`, `SharedCubeViewModel`, `HomeUiState` | Regroupe les modes Libre, Play et Édition autour du même cube partagé, avec clavier par catégorie (état indépendant), lock de rotation, animation de move et reset avec confirmation. |
| Mode Libre | `MoveParser`, `MoveExecutor`, `SharedCubeViewModel` | Chaque move du clavier est parsé, appliqué au cube et ajouté à la séquence libre. |
| Mode Play | `PlaybackState`, `MoveSequence`, `SharedCubeViewModel` | Lecture pas à pas, retour arrière, reset, loop, vitesse et auto-play. |
| Mode Édition | `EditingSession`, `MoveExecutor`, `LibraryRepository` | Ajout de moves, undo, redo, suppression, vidage et sauvegarde vers la bibliothèque. |
| Bibliothèque | `LibraryFragment`, `LibraryViewModel`, `LibraryRepository`, Room | Collections, fiches, algorithmes, mélanges, tags, recherche, filtres et import de notation. |
| Export PDF | `PdfExporter`, `LocalPdfExporter`, FileProvider | Génère une fiche PDF locale et déclenche le partage Android. |
| Timer | `TimerFragment`, `TimerViewModel`, `TimerRepository` | Chrono start/pause/reprise/reset, sauvegarde des temps et historique daté. |
| Paramètres | `SettingsFragment`, `SettingsViewModel`, `SettingsRepository` | Apparence, thème cube, préférences locales, auth cloud et actions de synchronisation. |
| Scan | `ScanFragment`, `CameraXCubeScanner`, `ScanColorClassifier` | Capture six faces, classe les couleurs, permet la correction manuelle et injecte le cube validé. |
| Sync cloud | `CloudSyncGateway`, `SupabaseCloudSyncGateway`, `CloudSyncWorker` | Outbox locale, push automatique, récupération cloud, merge last-write-wins et purge remote-only. |

## Build et qualité

Prérequis:

- JDK 21 installé.
- Android Studio ou Android SDK compatible compileSdk 36.
- Variables Android habituelles configurées (`ANDROID_HOME` ou SDK détecté par Android Studio).

Commandes principales:

```powershell
.\gradlew.bat detektFormat
.\gradlew.bat detekt
.\gradlew.bat test
.\gradlew.bat lintDebug
.\gradlew.bat :app:assembleDebug
```

Si plusieurs JDK sont installés, fixer `JAVA_HOME` localement avant les commandes:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
```

Ne pas versionner de chemin local dans `gradle.properties`.

## Supabase

Supabase est utilisé comme backend cloud remplaçable. Les données de session courante restent locales.

| Données synchronisées | Données locales uniquement |
| --- | --- |
| profils utilisateur | cube courant |
| collections | undo / redo |
| fiches d'algorithmes | index de lecture |
| algorithmes | timer en cours |
| mélanges | caméra et zoom |
| tags et relations | cache de session |
| temps du timer | scan en cours |
| préférences durables | état temporaire des popups |

Mise en place d'une connexion Supabase:

1. Créer un projet sur Supabase en ligne.
2. Récupérer l'URL du projet et la clé publishable/anon.
3. Ajouter les valeurs localement dans `local.properties` ou dans l'environnement:

```properties
SUPABASE_URL=https://project-ref.supabase.co
SUPABASE_PUBLISHABLE_KEY=ey...
```

4. Exécuter la migration:

```powershell
supabase login
supabase link --project-ref <PROJECT_REF>
supabase db push --dry-run
supabase db push
```

`<PROJECT_REF>` correspond à l'identifiant public du projet Supabase, visible dans l'URL du dashboard. Il ne faut pas le remplacer par une clé API.

Pour tester les migrations sur une base Supabase locale avant le push cloud:

```powershell
supabase start
supabase db reset
```

Le fichier appliqué par `supabase db push` est dans `supabase/migrations/20260519000000_algomix_sync.sql`.

La migration crée les tables cloud, active RLS et limite l'accès par utilisateur avec `owner_id = auth.uid()`. La table `profiles` utilise `id = auth.uid()`.

La clé service-role ne doit jamais être placée dans l'application Android. Si `SUPABASE_URL` ou `SUPABASE_PUBLISHABLE_KEY` est vide, les gateways cloud restent non configurées et l'UI affiche un retour explicite.

## Moteur OpenGL

Le moteur OpenGL affiche le cube, mais ne décide pas de ses règles. Il reçoit un état déjà calculé par le domaine.

```text
Move / scan / chargement
        -> CubeState métier
        -> RubikCubeRenderStateMapper
        -> RubikCubeState rendu
        -> RubikRenderer OpenGL ES
```

| Classe | Responsabilité |
| --- | --- |
| `RubikCubeView` | Composant Android qui héberge la surface OpenGL et expose `renderCube(CubeState)`, `playMove(Move, CubeState)`, `setRotationLocked(Boolean)` et `resetRotation(Quaternion)`. |
| `RubikRenderer` | Cycle de rendu OpenGL, dessin de la scène et application du style. |
| `RubikCubeGeometry` | Construction des faces, vertex et couleurs envoyés à OpenGL. |
| `RubikCubeRenderSpec` | Paramètres visuels du cube. |
| `RubikSceneState` | État de scène utilisé par le renderer. |
| `RubikCameraState` | Rotation visuelle de la caméra autour du cube. |
| `RubikPinchZoomController` | Gestion du zoom par pincement. |
| `RubikTouchController` | Gestion des gestes de rotation. |
| `RubikCubeRenderStateMapper` | Conversion du `CubeState` métier vers le modèle de rendu. |

L'état du cube est centralisé dans `SharedCubeViewModel`. La vue OpenGL ne possède pas l'état applicatif: elle reçoit l'état à afficher. Les modes Accueil, Play, Édition, Scan et Bibliothèque travaillent donc sur le même cube.

## Tests et documentation

| Type de test | Ce qui est vérifié |
| --- | --- |
| Tests unitaires domaine | Parsing Rubik, exécution des moves, validation du cube, scramble, édition, playback et codec de session. |
| Tests repositories | CRUD Room, soft delete, tags, timer, préférences DataStore et outbox locale. |
| Tests ViewModel | États Accueil, Bibliothèque, Timer, Paramètres, feedback utilisateur et orchestration des ports. |
| Tests cloud fake | Auth, récupération, push, merge last-write-wins, purge remote-only et conservation de l'outbox en cas d'erreur. |
| Tests scan | Classification couleur, assemblage des six faces et validation du cube scanné. |
| Tests Robolectric | Style et attributs custom de `RubikCubeView`, mapping cube/theme et comportements UI sans appareil physique. |
| Tests instrumentés | Vérifications de navigation et d'état visibles via Espresso (à compléter au fil du temps). |

Un smoke test est un contrôle manuel rapide après build. Il ne remplace pas les tests automatisés. Il sert à vérifier les points qui dépendent de l'appareil, des permissions, du rendu réel ou d'un backend distant: démarrage de l'application, rendu OpenGL non noir, caméra disponible, partage PDF Android, auth Supabase et synchronisation avec un vrai projet.

Les documents versionnés sont dans `docs/`. Les sources Typst et maquettes de travail restent dans `Blueprint/`, ignoré par Git. Le smoke test manuel est décrit dans `docs/smoke-test-checklist.md`.
