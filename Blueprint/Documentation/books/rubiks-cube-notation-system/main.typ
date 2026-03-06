#import "../../theme/shared.typ": *

#set document(
  title: "Algomix - Système de notations",
  author: "Gaëtan Rousselin",
)

#show: book-theme

#cover(
  "Algomix",
  "Système de notations Rubik's Cube 3x3",
  "1.0",
  "Support",
  datetime.today().display("[day]/[month]/[year]"),
)

#pagebreak()

#outline(title: "Sommaire")

#pagebreak()

#include "../../chapters/rubiks-cube-notation-system/notations.typ"
