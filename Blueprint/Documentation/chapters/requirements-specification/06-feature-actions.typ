= Détail des actions utilisateur

Le tableau ci-dessous formalise le comportement attendu pour chaque action clé.

#table(
  columns: (0.7fr, 1fr, 1.1fr, 1.7fr, 1.4fr, 1.4fr, 1.4fr),
  table.header([ID], [Contexte], [Action], [Effet UI immédiat], [Effet métier], [Données impactées], [Retour utilisateur / messages]),

  [ACT-01], [Accueil], [Scanner un cube], [Ouverture écran scan], [Initialise le cycle de capture], [Session scan], [Indication de la face attendue],
  [ACT-02], [Scan], [Capturer], [Face validée visuellement], [Stocke la capture courante], [Faces capturées], [Message succès ou face à reprendre],
  [ACT-03], [Scan], [Valider les 6 faces], [Retour à l'accueil], [Reconstruit CubeState], [CubeState], [Erreur explicite si reconstruction impossible],
  [ACT-04], [Accueil], [Mélanger], [Cube modifié], [Génère une séquence de mélange], [CubeState, scramble courant], [Confirmation non bloquante],
  [ACT-05], [Play], [Charger un algo], [Popup load ouverte], [Prépare la lecture], [Algo actif, index lecture], [Refus si aucune sélection],
  [ACT-06], [Play], [Charger un mélange], [Popup load ouverte], [Prépare lecture mélange], [Mélange actif, index lecture], [Refus si aucune sélection],
  [ACT-07], [Play/Édition], [Prev/Next], [Position de lecture bouge], [Recalcule l'état au move ciblé], [CubeState, move index], [Position affichée mise à jour],
  [ACT-08], [Play/Édition], [Auto/Loop/Speed], [Contrôles visuellement actifs], [Ajuste la stratégie de lecture], [Paramètres de lecture], [État des toggles visible],
  [ACT-09], [Play/Édition], [Reset], [Retour au début], [Replace la séquence à l'étape 0], [Move index], [Étiquette de progression réinitialisée],
  [ACT-10], [Édition], [Save], [Popup sauvegarde], [Prépare insertion vers fiche/mélange], [Séquence temporaire], [Message de confirmation à la validation],
  [ACT-11], [Édition], [Undo / Redo], [Séquence ajustée], [Annule/rétablit le dernier changement], [Historique édition], [Action impossible signalée si pile vide],
  [ACT-12], [Édition], [Suppress], [Dernier move retiré], [Suppression unitaire], [Séquence édition], [Indication de suppression],
  [ACT-13], [Édition], [Delete all], [Séquence vide], [Réinitialise l'édition], [Séquence édition], [Demande de confirmation si nécessaire],
  [ACT-14], [Bibliothèque], [Créer une collection], [Popup création], [Ajoute collection], [Collection], [Erreur si nom vide ou doublon],
  [ACT-15], [Bibliothèque], [Rename], [Nom changé], [Met à jour l'item cible], [Collection/Fiche/Mélange], [Message de succès],
  [ACT-16], [Bibliothèque], [Supprimer], [Item retiré], [Suppression définitive], [Collection/Fiche/Mélange], [Popup de confirmation],
  [ACT-17], [Bibliothèque], [Tags], [Pastilles mises à jour], [Attache/détache tags], [Tags liés], [Retour de validation],
  [ACT-18], [Fiche], [Importer un code d'algo], [Nouveau bloc visible], [Parse et insertion algo], [Algorithmes fiche], [Erreur syntaxe en cas de code invalide],
  [ACT-19], [Fiche], [Export PDF], [Lancement export], [Génère document de fiche], [Fiche + algorithmes], [Message d'export réussi/échoué],
  [ACT-20], [Création mélange], [Charger un code], [Zone code remplie], [Prépare la séquence], [Mélange en édition], [Erreur de format],
  [ACT-21], [Création mélange], [Générer], [Nouveau code affiché], [Création d'une séquence], [Mélange en édition], [Feedback de génération],
  [ACT-22], [Création mélange], [Sauvegarder ce mélange], [Retour bibliothèque], [Persist le mélange], [Mélange], [Confirmation de sauvegarde],
  [ACT-23], [Timer], [Pause], [Chrono figé], [Stoppe l'incrémentation], [État timer], [État pause visible],
  [ACT-24], [Timer], [Reset], [Chrono remis à zéro], [Réinitialise la session], [État timer], [Confirmation locale],
  [ACT-25], [Timer], [Save time], [Nouvelle ligne historique], [Insère un temps daté], [Historique timer], [Refus si temps invalide],
  [ACT-26], [Paramètres N1/N2], [Changer apparence], [Thème app actif], [Met à jour préférence UI], [Préférences], [Statut actif visible],
  [ACT-27], [Paramètres N1/N2], [Changer thème cube], [Theme cube actif], [Met à jour rendu cube], [Préférences cube], [Badge actif mis à jour],
  [ACT-28], [Backend cloud], [Se connecter], [Popup auth], [Ouvre une session cloud], [Session cloud], [Erreur credentials/réseau],
  [ACT-29], [Backend cloud], [Récupérer], [Indicateur d'opération], [Récupère données cloud], [Jeu de données local], [Résultat synchronisation],
  [ACT-30], [Backend cloud], [Vider le cloud], [Action confirmée], [Purge données distantes], [Cloud dataset], [Confirmation explicite obligatoire],
)

