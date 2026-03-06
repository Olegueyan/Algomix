= Schémas techniques et maquettes d'appui

Les éléments visuels de référence du projet sont regroupés ici: schémas de flux et exports de maquettes.

== Navigation de l'application

#block(breakable: false)[
  #figure(
    grid(
      columns: 1,
      gutter: 10pt,
      [#image("../../../Assets/Diagrams/generated/flow-navigation-home.svg", width: 100%)],
      [#image("../../../Assets/Diagrams/generated/flow-navigation-library.svg", width: 100%)],
      [#image("../../../Assets/Diagrams/generated/flow-navigation-settings.svg", width: 100%)],
    ),
    caption: [Schémas de navigation des menus Accueil, Bibliothèque et Paramètres],
  )
]

#pagebreak()

#let centered-figure(titre, path, cap) = {
  block(breakable: false, width: 100%)[
    == #titre
    
    #figure(
      box(
        width: 100%,
        align(center + horizon)[
          #image(
            path,
            width: 70%,
            height: auto,
            fit: "contain"
          )
        ]
      ),
      caption: cap,
    )
  ]
}

// Utilisation
#centered-figure(
  "Persistance locale",
  "../../../Assets/Diagrams/generated/flow-persistence-local.svg",
  [Cycle de sauvegarde et de reprise de l'état local]
)

#centered-figure(
  "Backend cloud",
  "../../../Assets/Diagrams/generated/flow-connection-cloud.svg",
  [Flux de connexion, récupération et purge côté backend cloud]
)

#pagebreak()

== Maquettes d'appui

#figure(
  grid(
    columns: 2,
    gutter: 10pt,
    [#image("../../../Assets/Mockups/Exports/home-visualization.png", width: 100%)],
    [#image("../../../Assets/Mockups/Exports/home-free.png", width: 100%)],
  ),
  caption: [Accueil: modes Visualisation et Libre],
)

#pagebreak()

#figure(
  grid(
    columns: 2,
    gutter: 10pt,
    [#image("../../../Assets/Mockups/Exports/home-play.png", width: 100%)],
    [#image("../../../Assets/Mockups/Exports/home-edit.png", width: 100%)],
  ),
  caption: [Accueil: modes Play et Édition],
)

#pagebreak()

#figure(
  grid(
    columns: 2,
    gutter: 10pt,
    [#image("../../../Assets/Mockups/Exports/library-overview.png", width: 100%)],
    [#image("../../../Assets/Mockups/Exports/library-sheet-preview.png", width: 100%)],
  ),
  caption: [Bibliothèque: vue globale et consultation d'une fiche],
)

#pagebreak()

#figure(
  grid(
    columns: 2,
    gutter: 10pt,
    [#image("../../../Assets/Mockups/Exports/library-scramble-create.png", width: 100%)],
    [#image("../../../Assets/Mockups/Exports/timer-overview.png", width: 100%)],
  ),
  caption: [Création de mélange et écran Timer],
)

#pagebreak()

#figure(
  grid(
    columns: 2,
    gutter: 10pt,
    [#image("../../../Assets/Mockups/Exports/settings-level-1.png", width: 100%)],
    [#image("../../../Assets/Mockups/Exports/settings-level-2.png", width: 100%)],
  ),
  caption: [Paramètres: configuration de base et enrichissement des thèmes du cube],
)

#pagebreak()

#figure(
  grid(
    columns: 2,
    gutter: 10pt,
    [#image("../../../Assets/Mockups/Exports/popup-scan-preview.png", width: 100%)],
    [#image("../../../Assets/Mockups/Exports/popup-load-item.png", width: 100%)],
  ),
  caption: [Popups de scan et de chargement d'un élément],
)

#pagebreak()

#figure(
  grid(
    columns: 2,
    gutter: 10pt,
    [#image("../../../Assets/Mockups/Exports/popup-save-edit.png", width: 100%)],
    [#image("../../../Assets/Mockups/Exports/popup-delete-confirm.png", width: 100%)],
  ),
  caption: [Popups de sauvegarde depuis l'édition et de confirmation de suppression],
)

#pagebreak()

#figure(
  grid(
    columns: 2,
    gutter: 10pt,
    [#image("../../../Assets/Mockups/Exports/popup-tags-manage.png", width: 100%)],
    [#image("../../../Assets/Mockups/Exports/popup-rename.png", width: 100%)],
  ),
  caption: [Popups de gestion des tags et de renommage],
)

#pagebreak()

#figure(
  grid(
    columns: 2,
    gutter: 10pt,
    [#image("../../../Assets/Mockups/Exports/popup-auth-login.png", width: 100%)],
    [#image("../../../Assets/Mockups/Exports/popup-create-account.png", width: 100%)],
  ),
  caption: [Popups d'authentification et de création de compte],
)
