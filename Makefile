SCRIPTS_PATH := readium/navigator/src/main/assets/_scripts

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
	yarn --cwd "$(SCRIPTS_PATH)" install --frozen-lockfile
	yarn --cwd "$(SCRIPTS_PATH)" run format
	yarn --cwd "$(SCRIPTS_PATH)" run lint
	yarn --cwd "$(SCRIPTS_PATH)" run bundle
