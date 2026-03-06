= Use cases détaillés

== Catalogue complet

#table(
  columns: (0.9fr, 1.1fr, 1.2fr, 1.8fr),
  table.header([ID], [Domaine], [Déclencheur], [Résultat attendu]),

  [UC-01], [Scan], [Ouvrir le scan], [L'interface de capture d'une face est prête],
  [UC-02], [Scan], [Capturer une face], [La face est enregistrée],
  [UC-03], [Scan], [Valider les 6 faces], [L'état cube est reconstruit],
  [UC-04], [Accueil], [Changer de mode], [Le mode ciblé est actif],
  [UC-05], [Accueil], [Appliquer un move en Libre], [Le cube et la séquence sont mis à jour],
  [UC-06], [Accueil], [Charger un algorithme en Play], [La lecture est initialisée],
  [UC-07], [Accueil], [Charger un mélange en Play], [La lecture du mélange est initialisée],
  [UC-08], [Accueil], [Utiliser prev/next/reset], [La position de lecture est ajustée],
  [UC-09], [Édition], [Ajouter des moves], [La séquence en cours grandit],
  [UC-10], [Édition], [Undo/redo/suppress/delete all], [La séquence est corrigée],
  [UC-11], [Édition], [Sauvegarder depuis Édition], [La séquence est stockée],
  [UC-12], [Bibliothèque], [Créer une collection], [Nouvelle collection disponible],
  [UC-13], [Bibliothèque], [Renommer un item], [Nom mis à jour],
  [UC-14], [Bibliothèque], [Supprimer un item], [Élément supprimé],
  [UC-15], [Bibliothèque], [Gérer tags], [Tags mis à jour],
  [UC-16], [Bibliothèque], [Importer un code algo], [Algorithme ajouté à la fiche],
  [UC-17], [Bibliothèque], [Exporter une fiche], [PDF généré],
  [UC-18], [Timer], [Sauvegarder un temps], [Nouvelle ligne d'historique],
  [UC-19], [Paramètres], [Modifier apparence/thème], [Préférences enregistrées],
  [UC-20], [Cloud], [Connexion + récupération + purge], [Session cloud et données synchronisées],
)

== Scénarios nominaux et exceptions

=== UC-03 - Reconstruire l'état cube après scan

#table(
  columns: (1fr, 3fr),
  table.header([Élément], [Description]),
  [Préconditions], [Le scan est ouvert et les 6 faces peuvent être capturées.],
  [Déclencheur], [Validation de la sixième face.],
  [Scénario nominal], [1) Capturer chaque face dans la grille. 2) Contrôler la lisibilité. 3) Valider l'ensemble. 4) Revenir à l'accueil avec cube reconstruit.],
  [Exceptions], [Face manquante, image floue, couleur non détectable: la validation est bloquée et la face à reprendre est indiquée.],
  [Postconditions], [CubeState mis à jour et immédiatement exploitable dans tous les modes.],
)

=== UC-06 - Charger un algorithme en Play

#table(
  columns: (1fr, 3fr),
  table.header([Élément], [Description]),
  [Préconditions], [Un algo est présent dans la bibliothèque.],
  [Déclencheur], [Action *Charger un algo* puis sélection dans la popup.],
  [Scénario nominal], [1) Ouvrir la popup. 2) Filtrer/rechercher. 3) Sélectionner un algo. 4) Charger. 5) Afficher progression et dernier move.],
  [Exceptions], [Aucune sélection: chargement refusé et message explicite.],
  [Postconditions], [Séquence active en Play, positionnée au début.],
)

=== UC-11 - Sauvegarder une séquence depuis Édition

#table(
  columns: (1fr, 3fr),
  table.header([Élément], [Description]),
  [Préconditions], [Une séquence non vide existe dans l'éditeur.],
  [Déclencheur], [Action *Save* dans la barre d'édition.],
  [Scénario nominal], [1) Ouvrir popup de sauvegarde. 2) Choisir *Ajouter à une fiche* ou *Créer un mélange*. 3) Sélectionner une collection. 4) Valider.],
  [Exceptions], [Collection inexistante: création à la volée dans la popup.],
  [Postconditions], [Séquence persistée dans la destination choisie.],
)

=== UC-14 - Supprimer un élément de bibliothèque

#table(
  columns: (1fr, 3fr),
  table.header([Élément], [Description]),
  [Préconditions], [L'élément cible existe.],
  [Déclencheur], [Menu trois points puis *Supprimer*.],
  [Scénario nominal], [1) Ouvrir menu. 2) Cliquer supprimer. 3) Confirmer. 4) Rafraîchir la liste sans l'élément.],
  [Exceptions], [Annulation en popup: aucun changement de données.],
  [Postconditions], [Suppression définitive de l'item.],
)

=== UC-16 - Importer un code d'algorithme

#table(
  columns: (1fr, 3fr),
  table.header([Élément], [Description]),
  [Préconditions], [La fiche cible est ouverte.],
  [Déclencheur], [Action *Importer un code d'algo*.],
  [Scénario nominal], [1) Coller le code. 2) Valider le format. 3) Ajouter un bloc d'algorithme. 4) Sauvegarder la fiche.],
  [Exceptions], [Code invalide: message d'erreur et aucun ajout.],
  [Postconditions], [Nouvel algorithme visible dans la grille de mouvements.],
)

=== UC-18 - Sauvegarder un temps

#table(
  columns: (1fr, 3fr),
  table.header([Élément], [Description]),
  [Préconditions], [Un chronométrage est en cours ou terminé.],
  [Déclencheur], [Action *Save time*.],
  [Scénario nominal], [1) Stopper/pause si nécessaire. 2) Cliquer *Save time*. 3) Ajouter la ligne date+temps à l'historique.],
  [Exceptions], [Temps nul ou invalide: message de refus, aucune insertion.],
  [Postconditions], [Historique enrichi.],
)

=== UC-20 - Synchroniser avec le backend cloud

#table(
  columns: (1fr, 3fr),
  table.header([Élément], [Description]),
  [Préconditions], [Fonctions cloud activées dans Paramètres N1/N2.],
  [Déclencheur], [Se connecter puis lancer récupérer ou vider.],
  [Scénario nominal], [1) Authentifier l'utilisateur. 2) Ouvrir session cloud. 3) Lancer récupération ou purge. 4) Afficher le résultat.],
  [Exceptions], [Échec auth, réseau indisponible, conflit distant: action stoppée avec message explicite.],
  [Postconditions], [État local/cloud aligné selon l'opération validée.],
)

