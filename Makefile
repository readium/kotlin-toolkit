SCRIPTS_PATH := readium/navigator/src/main/assets/_scripts
PNPM := pnpm --dir $(SCRIPTS_PATH)

help:
	@echo "Usage: make <target>\n\n\
	  lint\t\tLint the Kotlin sources with ktlint\n\
	  format\tFormat the Kotlin sources with ktlint\n\
	  scripts\tBundle the Navigator EPUB scripts\n\
	"

.PHONY: lint
lint:
	./gradlew ktlintCheck

.PHONY: format
format:
	./gradlew ktlintFormat

.PHONY: scripts
scripts:
	$(PNPM) install --frozen-lockfile
	$(PNPM) run format
	$(PNPM) run lint
	$(PNPM) run bundle
