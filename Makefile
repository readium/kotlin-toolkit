SCRIPTS_PATH := readium/navigator/src/main/assets/_scripts
YARN := yarn --cwd $(SCRIPTS_PATH) services/client/

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
	$(YARN) install --frozen-lockfile
	$(YARN) run format
	$(YARN) run lint
	$(YARN) run bundle
