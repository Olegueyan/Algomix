#import "../../helpers/helpers.typ": *

= Portée de réalisation

Le projet couvre l'ensemble des fonctionnalités nécessaires à la livraison d'une application complète de pratique Rubik's Cube.

== Périmètre fonctionnel à implémenter

#compact-table[#table(
  columns: (1.15fr, 2.55fr),
  table.header([Bloc], [Contenu à réaliser]),

  [Accueil], [4 modes (Visualisation, Libre, Play, Édition), cube 3D central, chargement d'algorithmes et de mélanges],
  [Scan], [Scan N1 requis avec grille 3x3 et capture des 6 faces],
  [Bibliothèque], [Collections nommables, fiches, mélanges, tags, recherche, filtres et menus d'actions],
  [Édition et sauvegarde], [Sauvegarde d'une séquence vers fiche ou mélange, avec création de collection à la volée],
  [Timer], [Démarrage, pause, reset, sauvegarde du temps et historique],
  [Paramètres], [Apparence de l'app, thème cube Filled, backend cloud (connexion, récupération, purge) et extension des thèmes avancés],
  [Persistance], [Cache local de l'état du cube et de la session active],
  [Export], [Export PDF des fiches],
)]

== Éléments optionnels explicitement différés

#compact-table[#table(
  columns: (1.05fr, 2.65fr),
  table.header([Élément], [Statut]),

  [Scan N2], [Optionnel: détection sans grille sur captures fixes],
  [Scan N3], [Optionnel: scan continu guidé],
  [Thèmes avancés], [Optionnel tant que Filled reste la base de rendu active],
)]
