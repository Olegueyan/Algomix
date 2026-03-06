TYPST=typst
BUILD_DIR=build
ROOT=..

REQ_SRC=books/requirements-specification/main.typ
NOT_SRC=books/rubiks-cube-notation-system/main.typ

REQ_OUT=$(BUILD_DIR)/requirements-specification.pdf
NOT_OUT=$(BUILD_DIR)/rubiks-cube-notation-system.pdf

.PHONY: books requirements notations clean

books: requirements notations

requirements:
	if not exist $(BUILD_DIR) mkdir $(BUILD_DIR)
	$(TYPST) compile $(REQ_SRC) $(REQ_OUT) --root $(ROOT)

notations:
	if not exist $(BUILD_DIR) mkdir $(BUILD_DIR)
	$(TYPST) compile $(NOT_SRC) $(NOT_OUT) --root $(ROOT)

clean:
	if exist $(BUILD_DIR) rmdir /s /q $(BUILD_DIR)
