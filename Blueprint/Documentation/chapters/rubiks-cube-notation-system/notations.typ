#let move-block(name, image-path, caption) = [
  #block(breakable: false, width: 100%)[
    === Mouvement #name
    #align(center)[
      #figure(
        image(image-path, width: 40%),
        caption: [#caption]
      )
    ]
  ]
  #v(10pt)
]

#block(breakable: false, inset: (x: 5pt), below: 28pt)[
  #set par(justify: true)
  #text(size: 28pt, weight: "bold")[Système de notations Rubik's Cube 3x3]
  #v(16pt)
  #set par(justify: false)
  #set text(size: 12pt)
  Ce chapitre explique les notations standards du Rubik's Cube 3x3.
  L'objectif est de savoir exactement quelle couche tourner, dans quel sens, et comment lire un algorithme.
]

#v(8pt)

#block(breakable: false)[
= Règles générales
]

- Les algorithmes se lisent de gauche à droite.
- Une lettre seule (`R`, `U`, `F`, etc.) signifie une rotation de 90 degrés.
- Le sens est toujours vu depuis la face nommée.
- Le suffixe prime (`'`) signifie le sens inverse.
- Le suffixe `2` signifie 180 degrés (demi-tour).
- `R2` est identique à `R2'` : sur 180 degrés, le sens n'a pas d'effet.

#v(12pt)

#block(breakable: false)[
= Position de référence
]

Pour toute la suite, on part de la position classique :
- `U` = Up (haut)
- `D` = Down (bas)
- `F` = Front (avant)
- `B` = Back (arrière)
- `R` = Right (droite)
- `L` = Left (gauche)

#align(center)[
  #figure(
    image("../../../Assets/Images/Notations/Cube_Rotations/no_rotation.png", width: 40%),
    caption: [Orientation de référence du cube]
  )
]

#v(12pt)

#block(breakable: false)[
= Répertoire des notations

#table(
  columns: (1.1fr, 3.2fr),
  table.header([Notation], [Description]),
  [`U D R L F B`], [Mouvements de face (quart de tour)],
  [`U' D' R' L' F' B'`], [Mouvements de face inverses],
  [`U2 D2 R2 L2 F2 B2`], [Mouvements de face en demi-tour],
  [`Rw Uw Fw Lw Dw Bw`], [Mouvements larges (2 couches)],
  [`r u f l d b`], [Forme courte des mouvements larges],
  [`M E S`], [Mouvements de tranches],
  [`M' E' S'`], [Mouvements de tranches inverses],
  [`M2 E2 S2`], [Mouvements de tranches en demi-tour],
  [`x y z`], [Rotations globales du cube],
  [`x' y' z'`], [Rotations globales inverses],
  [`x2 y2 z2`], [Rotations globales en demi-tour],
)
]

#v(12pt)

#block(breakable: false)[
= Mouvements de face (Face Turns)

Ce sont les mouvements de base. Une seule face est tournée.
]

#v(6pt)

#block(breakable: false)[
== Faces principales
]

#move-block("U", "../../../Assets/Images/Notations/Face_Turns/U.png", [`U` : tourne la face du haut])
#move-block("D", "../../../Assets/Images/Notations/Face_Turns/D.png", [`D` : tourne la face du bas])
#move-block("R", "../../../Assets/Images/Notations/Face_Turns/R.png", [`R` : tourne la face de droite])
#move-block("L", "../../../Assets/Images/Notations/Face_Turns/L.png", [`L` : tourne la face de gauche])
#move-block("F", "../../../Assets/Images/Notations/Face_Turns/F.png", [`F` : tourne la face avant])
#move-block("B", "../../../Assets/Images/Notations/Face_Turns/B.png", [`B` : tourne la face arrière])

#v(8pt)

#block(breakable: false)[
== Primes et doubles
]

- Exemple prime : `R'` = même face que `R`, mais en sens inverse.
- Exemple double : `U2` = deux quarts de tour sur la face `U`.
- Pour une même lettre, mouvement puis inverse s'annulent :
  `R R'` = aucun effet, `F F'` = aucun effet.

#move-block("U'", "../../../Assets/Images/Notations/Face_Turns/U_prime.png", [`U'` : quart de tour inverse])
#move-block("U2", "../../../Assets/Images/Notations/Face_Turns/U2.png", [`U2` : demi-tour])

#v(12pt)

#block(breakable: false)[
= Mouvements larges (Wide Moves)

Un wide move tourne 2 couches en même temps : la face externe + la couche du milieu adjacente.

- Notation longue : `Rw`, `Uw`, `Fw`, `Lw`, `Dw`, `Bw`
- Notation courte équivalente : `r`, `u`, `f`, `l`, `d`, `b`
]

#v(6pt)

#block(breakable: false)[
== Série des wide moves
]

#move-block("Rw / r", "../../../Assets/Images/Notations/Wide_Moves/Rw_r.png", [`Rw` / `r` : deux couches côté droit])
#move-block("Uw / u", "../../../Assets/Images/Notations/Wide_Moves/Uw_u.png", [`Uw` / `u` : deux couches du haut])
#move-block("Fw / f", "../../../Assets/Images/Notations/Wide_Moves/Fw_f.png", [`Fw` / `f` : deux couches de devant])
#move-block("Lw / l", "../../../Assets/Images/Notations/Wide_Moves/Lw_l.png", [`Lw` / `l` : deux couches côté gauche])
#move-block("Dw / d", "../../../Assets/Images/Notations/Wide_Moves/Dw_d.png", [`Dw` / `d` : deux couches du bas])
#move-block("Bw / b", "../../../Assets/Images/Notations/Wide_Moves/Bw_b.png", [`Bw` / `b` : deux couches arrière])

Comme les face turns, les wide moves acceptent `'` et `2` :
`Rw'`, `Rw2`, `u'`, `f2`, etc.

#v(12pt)

#block(breakable: false)[
= Mouvements de tranches (Slice Moves)

Ce sont les couches internes uniquement (sans tourner les faces externes).

- `M` : tranche entre `L` et `R`, dans le sens de `L`
- `E` : tranche entre `U` et `D`, dans le sens de `D`
- `S` : tranche entre `F` et `B`, dans le sens de `F`
]

#v(6pt)

#block(breakable: false)[
== Série des slice moves
]

#move-block("M", "../../../Assets/Images/Notations/Slice_Moves/M.png", [`M` : tranche verticale centrale])
#move-block("E", "../../../Assets/Images/Notations/Slice_Moves/E.png", [`E` : tranche horizontale centrale])
#move-block("S", "../../../Assets/Images/Notations/Slice_Moves/S.png", [`S` : tranche centrale avant/arrière])

Les suffixes `'` et `2` s'appliquent aussi : `M'`, `E2`, `S'`, etc.

#v(12pt)

#block(breakable: false)[
= Rotations globales du cube (Cube Rotations)

Ici, on ne tourne pas une couche : on tourne tout le cube.

- `x` : rotation comme un `R` global
- `y` : rotation comme un `U` global
- `z` : rotation comme un `F` global
]

#v(6pt)

#block(breakable: false)[
== Série des rotations
]

#move-block("x", "../../../Assets/Images/Notations/Cube_Rotations/x.png", [`x` : rotation complète sur axe gauche-droite])
#move-block("y", "../../../Assets/Images/Notations/Cube_Rotations/y.png", [`y` : rotation complète sur axe vertical])
#move-block("z", "../../../Assets/Images/Notations/Cube_Rotations/z.png", [`z` : rotation complète sur axe avant-arrière])

Les rotations globales prennent aussi `'` et `2` : `x'`, `y2`, `z'`.

#v(12pt)

#block(breakable: false)[
= Particularités utiles
]

- `R R` = `R2`
- `R R R` = `R'`
- `R R R R` = aucun effet
- `R U R' U'` est une séquence classique qui conserve une partie de la structure du cube
- Les rotations (`x y z`) changent le référentiel de lecture des coups suivants

En pratique, pour bien tourner le cube :
- identifier la lettre (quelle couche)
- vérifier le suffixe (`'` ou `2`)
- exécuter dans l'ordre exact de l'algorithme
