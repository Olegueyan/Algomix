= Référence écrans de l'application

Ce tableau décrit les écrans et leur rôle opérationnel, sans hiérarchie de priorité.

#table(
  columns: (1.4fr, 1.6fr, 1.6fr, 1.6fr, 1.1fr),
  table.header([Écran], [Actions principales], [Objectif utilisateur], [Sortie attendue], [Maquette]),

  [Accueil - Visualisation], [Tourner le cube, scanner, mélanger], [Observer et poser un état de départ], [État cube visible et manipulable], [`home-visualization`],
  [Accueil - Libre], [Clavier de mouvements, scanner, mélanger], [Exécuter des mouvements manuels], [Séquence appliquée au cube], [`home-free`],
  [Accueil - Play], [Charger algo/mélange, prev/next, auto, loop, reset], [Lire une séquence de manière contrôlée], [Progression et dernier move affichés], [`home-play`],
  [Accueil - Édition], [Composer, corriger, sauvegarder, charger], [Créer ou modifier une séquence], [Séquence prête à être stockée], [`home-edit`],
  [Bibliothèque - vue globale], [Recherche, filtres, menu item, création], [Retrouver et organiser les contenus], [Collections et contenus filtrés], [`library-overview`],
  [Bibliothèque - fiche], [Raccourcis Play/Édition, import code, export PDF], [Consulter et exploiter une fiche], [Algorithmes accessibles et exportables], [`library-sheet-preview`],
  [Bibliothèque - création mélange], [Charger code, générer, sauvegarder], [Créer un mélange rejouable], [Mélange stocké dans la bibliothèque], [`library-scramble-create`],
  [Timer], [Pause, reset, save time], [Mesurer un solve], [Nouvelle entrée dans l'historique], [`timer-overview`],
  [Paramètres N1], [Apparence, Filled, backend cloud], [Configurer l'app et le cloud], [Préférences et session cloud mises à jour], [`settings-level-1`],
  [Paramètres N2], [N1 + thèmes Sticker sur noir/Carbone], [Étendre le rendu cube], [Theme cube sélectionnable], [`settings-level-2`],
  [Popup Scan], [Capture d'une face], [Acquérir une face du cube], [Face validée], [`popup-scan-preview`],
  [Popup Chargement], [Recherche et sélection], [Charger algo ou mélange], [Élément injecté dans écran source], [`popup-load-item`],
)

