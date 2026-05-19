# Checklist Smoke Test Algomix

Cette checklist complète les tests automatisés du batch 12. Elle couvre les points qui nécessitent un appareil, une caméra ou un projet Supabase réel.

## Rendu Rubik

- Lancer l'application sur un téléphone ou un émulateur.
- Vérifier que l'onglet Accueil affiche le cube sans écran noir.
- Faire tourner le cube au doigt.
- Tester le zoom pincé et le double tap de réinitialisation.
- Changer de mode Accueil puis revenir en Visualisation: le cube courant doit être conservé.

## Scan Caméra

- Ouvrir `Scanner un cube`.
- Accepter la permission caméra.
- Vérifier l'affichage de la preview CameraX, de la grille 3x3 et de la progression des 6 faces.
- Capturer six faces, corriger au moins un sticker manuellement, puis valider.
- Vérifier qu'un scan invalide affiche une erreur et ne remplace pas le cube.

## Bibliothèque Et PDF

- Créer une collection.
- Créer une fiche, importer un algorithme valide, puis vérifier son affichage.
- Tester un import invalide: aucune écriture ne doit être faite.
- Ajouter un tag, filtrer par tag, puis retirer le filtre.
- Exporter une fiche en PDF et vérifier l'ouverture du partage Android.

## Timer

- Démarrer le chrono, le mettre en pause, reprendre, puis reset.
- Enregistrer un temps strictement positif.
- Vérifier l'apparition du temps dans l'historique avec une date lisible.
- Tester l'enregistrement à zéro: l'application doit afficher un refus clair.

## Paramètres

- Modifier le thème d'application.
- Modifier le thème du cube, revenir à l'Accueil et vérifier le rendu.
- Tester `Récupérer`, `Vider le cloud` et `Changer le mot de passe` sans session: chaque action doit être refusée proprement.
- Vérifier que `Vider le cloud` demande une confirmation avant toute action.

## Supabase

- Renseigner `SUPABASE_URL` et `SUPABASE_PUBLISHABLE_KEY` en local.
- Exécuter la migration `supabase/migrations/20260519000000_algomix_sync.sql` sur le projet Supabase.
- Créer un compte email/password.
- Créer une donnée locale synchronisable, attendre ou déclencher la sync, puis vérifier la ligne côté Supabase.
- Tester `Récupérer`: les données distantes doivent revenir localement.
- Tester `Vider le cloud`: les données locales restent présentes et le remote est purgé.
