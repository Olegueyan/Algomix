= Catalogue des fonctionnalités

== Fonctionnalités par domaine

#table(
  columns: (0.8fr, 1.1fr, 1.6fr, 1.7fr, 1.2fr),
  table.header([ID], [Domaine], [Fonctionnalité], [Valeur utilisateur], [Maquettes]),

  [FT-01], [Accueil], [Cube 3D central], [Visualiser immédiatement l'état courant], [`home-visualization`],
  [FT-02], [Accueil], [Mode Visualisation], [Observer et contrôler la position du cube], [`home-visualization`],
  [FT-03], [Accueil], [Mode Libre + clavier], [Appliquer rapidement des mouvements], [`home-free`],
  [FT-04], [Accueil], [Mode Play], [Lire une séquence pas à pas ou automatiquement], [`home-play`],
  [FT-05], [Accueil], [Mode Édition], [Composer et corriger une séquence], [`home-edit`],
  [FT-06], [Accueil], [Scan N1], [Reconstruire un cube réel dans l'app], [`popup-scan-preview`],
  [FT-07], [Accueil], [Action Mélanger], [Obtenir un état de travail instantané], [`home-visualization`, `home-free`],
  [FT-08], [Bibliothèque], [Collections], [Structurer les contenus d'entraînement], [`library-overview`],
  [FT-09], [Bibliothèque], [Fiches algorithmes], [Regrouper plusieurs blocs d'algorithmes], [`library-sheet-preview`],
  [FT-10], [Bibliothèque], [Mélanges sauvegardés], [Rejouer des contextes connus], [`library-scramble-create`],
  [FT-11], [Bibliothèque], [Recherche, filtres, tags], [Retrouver rapidement le bon contenu], [`library-overview`],
  [FT-12], [Bibliothèque], [Import code algo], [Ajouter du contenu sans ressaisie manuelle], [`library-sheet-preview`],
  [FT-13], [Bibliothèque], [Export PDF], [Partager et archiver des fiches], [`library-sheet-preview`],
  [FT-14], [Timer], [Chronométrage + historique], [Mesurer la progression], [`timer-overview`],
  [FT-15], [Paramètres N1], [Apparence + Filled + backend cloud], [Configurer visuel et connectivité], [`settings-level-1`],
  [FT-16], [Paramètres N2], [Thèmes Sticker sur noir/Carbone], [Étendre le rendu du cube], [`settings-level-2`],
)

== Fonctionnalités non fonctionnelles (critères mesurables)

#table(
  columns: (0.9fr, 1.1fr, 2.5fr),
  table.header([ID], [Catégorie], [Exigence mesurable]),

  [NF-01], [Performance UI], [Sur un appareil de référence, l'interaction cube ne doit pas présenter de blocage perceptible pendant les manipulations courantes.],
  [NF-02], [Temps de reprise], [Après relance de l'application, la restauration de la session locale doit afficher un état exploitable sans action manuelle utilisateur.],
  [NF-03], [Fiabilité données], [Une action validée (save, rename, delete, tag) doit produire un état cohérent en bibliothèque sans désynchronisation visuelle.],
  [NF-04], [Ergonomie], [Toutes les actions critiques doivent avoir un retour explicite (succès, refus, erreur de validation ou erreur réseau).],
  [NF-05], [Sécurité], [Les opérations cloud exigent une session authentifiée valide; en cas d'échec, aucune opération destructive ne doit être appliquée.],
  [NF-06], [Maintenabilité], [La documentation doit rester découpée par sections métier stables et chaque schéma doit avoir une source versionnée.],
)

