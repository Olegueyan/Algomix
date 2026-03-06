#import "theme.typ": *

#let book-author = "Gaëtan Rousselin"
#let book-version = "1.0"
#let book-brand = "Algomix"
#let book-date = datetime.today().display("[day]/[month]/[year]")
#let shared-logo-path = "../../Assets/Images/algomix_logo.png"

#let setup-book(title, subtitle, document-label) = {
  set document(
    title: title,
    author: book-author,
  )

  show: book-theme

  cover(
    book-brand,
    subtitle,
    book-version,
    book-date,
    document-label: document-label,
    logo-path: shared-logo-path,
  )
}
