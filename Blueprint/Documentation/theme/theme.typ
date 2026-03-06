#let brand = rgb("#0f3b57")
#let accent = rgb("#f97316")
#let ink = rgb("#102a43")
#let muted = rgb("#627d98")
#let surface = rgb("#f4f7fb")
#let line = rgb("#d9e2ec")

#let cover(
  title,
  subtitle,
  version,
  document-label,
  date,
  logo-path: "../../Assets/Images/algomix_logo.png",
) = [
  #set page(numbering: none)
  #align(center)[
    #v(2.8cm)
    #image(logo-path, width: 145pt)
    #v(0.7cm)
    #text(size: 36pt, weight: "bold", fill: brand)[#title]
    #v(0.25cm)
    #text(size: 15pt, fill: muted)[#subtitle]
    #v(1.4cm)
    #box(
      inset: (x: 16pt, y: 12pt),
      radius: 9pt,
      fill: surface,
      stroke: (paint: line, thickness: 0.7pt),
    )[
      #set text(size: 10.5pt, fill: ink)
      #table(
        columns: (auto, auto),
        align: (left, right),
        stroke: none,
        column-gutter: 2.2cm,
        row-gutter: 7pt,
        [Document], [#document-label],
        [Version], [#version],
        [Date], [#date],
      )
    ]
  ]
]

#let panel(title, body) = box(
  inset: (x: 12pt, y: 10pt),
  radius: 8pt,
  fill: surface,
  stroke: (paint: line, thickness: 0.7pt),
)[
  #set text(weight: "bold", fill: brand)
  #title
  #v(6pt)
  #set text(weight: "regular", fill: ink)
  #body
]

#let book-theme(doc) = {
  set page(
    paper: "a4",
    margin: (x: 2.2cm, y: 2cm),
    numbering: "1",
    number-align: bottom + right,
  )

  set text(
    font: "Verdana",
    size: 10.5pt,
    fill: ink,
    lang: "fr",
  )

  set par(
    justify: true,
    leading: 0.62em,
  )

  set heading(numbering: "1.")

  show heading.where(level: 1): set block(above: 1.6em, below: 0.9em)
  show heading.where(level: 2): set block(above: 1.2em, below: 0.7em)
  show heading.where(level: 3): set block(above: 0.9em, below: 0.5em)

  show heading.where(level: 1): set text(size: 22pt, weight: "bold", fill: brand)
  show heading.where(level: 2): set text(size: 15pt, weight: "bold", fill: brand)
  show heading.where(level: 3): set text(size: 12pt, weight: "semibold", fill: ink)

  set table(
    stroke: 0.6pt + line,
    inset: 6pt,
  )

  show table.cell: set text(size: 9.3pt)
  show table.header: set text(size: 10pt, weight: "bold", fill: brand)
  show figure.caption: set text(size: 9pt, fill: muted)
  show raw: set text(size: 9pt, fill: muted)

  doc
}
