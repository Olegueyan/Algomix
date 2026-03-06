#import "../../helpers/helpers.typ": *

= Outils et architecture du projet

== Outils de travail

#table(
  columns: (1.1fr, 2.9fr),
  table.header([Outil], [Usage dans le projet]),

  [Android Studio], [Développement, exécution et débogage de l'application Android],
  [GitHub], [Versioning, revues de code, suivi des évolutions],
  [Trello], [Pilotage des tâches, priorisation et avancement],
  [Postman], [Tests des endpoints backend cloud],
  [Java / Kotlin], [Langages cibles pour l'implémentation Android],
  [Cloud (à définir)], [Hébergement des services de synchronisation et des données utilisateur],
)

== Architecture cible basée sur DDD

Le projet suit une approche *Domain-Driven Design* afin d'isoler les règles métier et de réduire le couplage entre UI, logique applicative et infrastructure.

#table(
  columns: (1fr, 3fr),
  table.header([Couche], [Responsabilité]),

  [Domain], [Modèles métier (cube, algorithmes, bibliothèque, timer), règles métier, services de domaine],
  [Application], [Cas d'usage, orchestration des actions utilisateur, gestion des transactions fonctionnelles],
  [Infrastructure], [Persistance locale, client backend cloud, implémentations techniques],
  [UI], [Écrans Android, états visuels, interactions utilisateur],
)

=== Arborescence cible (indicative)

```text
app/
  src/main/java/com/algomix/
    domain/
      cube/
      library/
      timer/
      settings/
    application/
      usecases/
      dto/
    infrastructure/
      local/
      cloud/
      repository/
    ui/
      home/
      library/
      timer/
      settings/
      components/
```

Cette structure vise à maintenir la lisibilité du code et la capacité d'évolution des fonctionnalités sans refonte globale.
