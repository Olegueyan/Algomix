#import "../../theme/shared.typ": *

#set document(
  title: "Algomix - Cahier des charges",
  author: "Gaëtan Rousselin",
)

#show: book-theme

#cover(
  "Algomix",
  "Cahier des charges fonctionnel et technique",
  "1.0",
  "Cahier des charges",
  datetime.today().display("[day]/[month]/[year]"),
)

#pagebreak()

#outline(title: "Sommaire")

#pagebreak()

#show heading.where(level: 1): set block(above: 2.0em, below: 1.0em)
#show heading.where(level: 2): set block(above: 1.4em, below: 0.8em)
#show heading.where(level: 3): set block(above: 1.0em, below: 0.6em)
#show figure: set block(above: 1.0em, below: 1.0em)
#show table: set block(above: 0.8em, below: 0.9em)

#include "../../chapters/requirements-specification/01-product-synthesis.typ"

#pagebreak()

#include "../../chapters/requirements-specification/02-scope-delivery.typ"

#pagebreak()

#include "../../chapters/requirements-specification/03-screen-reference.typ"

#pagebreak()

#include "../../chapters/requirements-specification/04-use-cases.typ"

#pagebreak()

#include "../../chapters/requirements-specification/05-feature-catalog.typ"

#pagebreak()

#include "../../chapters/requirements-specification/06-feature-actions.typ"

#pagebreak()

#include "../../chapters/requirements-specification/07-technical-schemas.typ"

#pagebreak()

#include "../../chapters/requirements-specification/08-business-rules-and-acceptance.typ"

#pagebreak()

#include "../../chapters/requirements-specification/09-tools-and-architecture.typ"
