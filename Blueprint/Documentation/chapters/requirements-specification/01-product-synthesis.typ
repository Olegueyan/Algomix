= Synthèse produit et processus d'usage

Algomix est une application mobile dédiée au Rubik's Cube 3x3.
Le produit couvre la manipulation du cube, la lecture et l'édition d'algorithmes, la bibliothèque d'entraînement, le timer et les paramètres applicatifs.

== Vocabulaire métier

#table(
  columns: (1.2fr, 2.8fr),
  table.header([Terme], [Définition précise]),

  [État du cube], [Configuration complète du Rubik's Cube: position et orientation de chaque pièce au moment où l'application l'affiche.],
  [Mouvement], [Action élémentaire appliquée au cube selon une notation standard (ex: U, R, F, U').],
  [Algorithme], [Suite ordonnée de mouvements conçue pour produire un résultat précis sur le cube.],
  [Mélange], [Suite de mouvements utilisée pour passer d'un cube résolu à un état de départ d'entraînement.],
  [Fiche], [Conteneur métier regroupant plusieurs algorithmes liés à un même cas, thème ou objectif d'entraînement.],
  [Collection], [Conteneur de niveau supérieur permettant d'organiser des fiches et des mélanges nommés par l'utilisateur.],
  [Tag], [Étiquette métier appliquée à une fiche ou à un mélange pour filtrer, rechercher et catégoriser les contenus.],
  [Persistance locale], [Mécanisme de sauvegarde sur l'appareil de l'état de l'application, du cube et des données métier nécessaires à la reprise.],
  [Backend cloud], [Service distant utilisé pour l'authentification, la récupération et la suppression des données synchronisées.],
)

== Lecture de l'application

#table(
  columns: (1.1fr, 2.9fr),
  table.header([Menu], [Rôle dans l'application]),

  [Accueil], [Point d'entrée principal pour visualiser le cube, exécuter des mouvements, lire une séquence et éditer un algorithme.],
  [Bibliothèque], [Espace d'organisation des collections, fiches et mélanges avec recherche, filtres et actions de gestion.],
  [Timer], [Écran de chronométrage centré sur l'enregistrement des temps et la consultation de l'historique.],
  [Paramètres], [Écran de configuration de l'apparence, du thème du cube et des interactions avec le backend cloud.],
)

== Processus produit

Le processus d'usage est continu et centré sur un état de cube partagé.

#table(
  columns: (1.2fr, 1.5fr, 1.6fr),
  table.header([Objectif], [Fonction de l'app], [Résultat observable]),

  [Entrer rapidement en pratique], [Écran Accueil avec cube central et modes], [Le cube est visible immédiatement et prêt à être manipulé],
  [Construire une séquence], [Mode Libre et mode Édition], [Les mouvements s'enchaînent et la séquence évolue en direct],
  [Exécuter une séquence chargée], [Mode Play + chargement algo/mélange], [Lecture pas à pas, lecture auto, reset du début],
  [Structurer les contenus], [Bibliothèque (collections, fiches, mélanges, tags)], [Les contenus sont classés, filtrables et réutilisables],
  [Mesurer les performances], [Timer + historique], [Chaque session est enregistrée avec date et temps],
  [Conserver le contexte], [Persistance locale + backend cloud], [Reprise de session locale et options de synchronisation],
)

== Cycle d'usage quotidien

#table(
  columns: (0.8fr, 2.7fr),
  table.header([Étape], [Description]),

  [1], [L'utilisateur ouvre l'app et retrouve son état de travail local.],
  [2], [Il choisit un mode Accueil (Visualisation, Libre, Play, Édition) selon son besoin immédiat.],
  [3], [Il peut scanner un cube (Scan N1 requis), mélanger, charger une séquence ou éditer un algorithme.],
  [4], [Il sauvegarde dans la Bibliothèque (fiche ou mélange) et applique des tags pour retrouver ses contenus.],
  [5], [Il chronomètre une session et enregistre le résultat dans l'historique.],
  [6], [Il ajuste ses paramètres (apparence, thème cube, backend cloud) selon sa configuration.],
)

== Continuité de l'état du cube

Le cœur applicatif manipule un état unique du cube.
Chaque écran lit et met à jour cette même source d'état, ce qui garantit:
- cohérence entre Visualisation, Libre, Play et Édition,
- lecture exacte du dernier mouvement exécuté,
- reprise fidèle après relance de l'application.

