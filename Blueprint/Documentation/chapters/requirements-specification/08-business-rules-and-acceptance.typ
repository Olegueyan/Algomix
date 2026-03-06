= Règles métier et critères d'acceptation de l'app

== Règles métier

#table(
  columns: (0.9fr, 3.1fr),
  table.header([ID], [Règle]),

  [BR-01], [Le scan n'est validé que lorsque 6 faces exploitables sont capturées.],
  [BR-02], [Le cube manipulé dans tous les modes Accueil provient de la même source d'état.],
  [BR-03], [Reset en Play/Édition repositionne la lecture au début de la séquence active.],
  [BR-04], [Une collection contient des fiches et des mélanges; elle ne porte pas de tags propres.],
  [BR-05], [Les tags sont attachés aux fiches et aux mélanges, jamais à la collection.],
  [BR-06], [Un chargement algo/mélange nécessite une sélection unique explicite.],
  [BR-07], [Un import de code invalide n'altère jamais la fiche cible.],
  [BR-08], [La suppression d'un item exige une confirmation explicite avant exécution.],
  [BR-09], [Le timer historise la date et le temps réalisé; aucune donnée de mélange n'est imposée.],
  [BR-10], [Le thème Filled reste la base de rendu du cube; les thèmes avancés sont une extension N2.],
  [BR-11], [Le backend cloud est disponible dans Paramètres N1/N2 via connexion, récupération et purge.],
  [BR-12], [Une opération cloud sans session valide est refusée avec message utilisateur.],
  [BR-13], [La persistance locale restaure l'état cube et le contexte de session au redémarrage.],
)

== Critères d'acceptation de l'app

#table(
  columns: (0.9fr, 3.1fr),
  table.header([ID], [Critère testable]),

  [AC-01], [Le flux scan complet permet de reconstruire un état cube utilisable sur l'accueil.],
  [AC-02], [Les quatre modes Accueil sont cohérents entre eux sur le même état de cube.],
  [AC-03], [Play prend en charge chargement, navigation prev/next, lecture auto, loop, vitesse et reset.],
  [AC-04], [Édition prend en charge save, undo, redo, suppress et delete all avec feedback utilisateur.],
  [AC-05], [Bibliothèque supporte création de collection, gestion des fiches/mélanges, tags, rename et delete.],
  [AC-06], [L'import de code algo ajoute un bloc valide et rejette un code invalide avec message explicite.],
  [AC-07], [Le timer enregistre un temps daté et affiche l'historique mis à jour.],
  [AC-08], [Paramètres N1/N2 permettent de configurer l'apparence, les thèmes cube et le backend cloud.],
  [AC-09], [La reprise locale après fermeture/réouverture restaure un état de session exploitable.],
  [AC-10], [Les écrans de documentation restent lisibles avec maquettes affichées deux par deux.],
)

== Scénarios de test fonctionnels

#table(
  columns: (0.9fr, 3.1fr),
  table.header([ID], [Scénario]),

  [TST-01], [Démarrer en Visualisation, appliquer un mélange, passer en Play puis Édition: le cube reste cohérent à chaque transition.],
  [TST-02], [Créer une collection, ajouter une fiche via sauvegarde édition, ajouter des tags puis filtrer la vue globale.],
  [TST-03], [Lancer timer, pause, reset, save time: la ligne date+temps est présente dans l'historique.],
  [TST-04], [Se connecter au backend cloud, lancer récupérer puis vider, vérifier le retour utilisateur à chaque étape.],
)

