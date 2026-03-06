#let mockup-pair(left, right, caption) = figure(
  grid(
    columns: 2,
    gutter: 10pt,
    [#image(left, width: 100%)],
    [#image(right, width: 100%)],
  ),
  caption: [#caption],
)

#let mockup-single(path, caption) = figure(
  image(path, width: 76%),
  caption: [#caption],
)

#let wide-diagram(path, caption, width: 92%) = figure(
  align(center)[
    #image(path, width: width)
  ],
  caption: [#caption],
)

#let compact-table(body) = context {
  set text(size: 9.4pt)
  body
}
