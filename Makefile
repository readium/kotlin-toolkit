SCRIPTS_PATH := readium/navigator/src/main/assets/_scripts
SCRIPTS_NAVIGATOR_WEB_PATH := readium/navigators/web/scripts

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
scripts-legacy:
	@which corepack >/dev/null 2>&1 || (echo "ERROR: corepack is required, please install it first\nhttps://pnpm.io/installation#using-corepack"; exit 1)

	cd $(SCRIPTS_PATH); \
	corepack install; \
	pnpm install --frozen-lockfile; \
	pnpm run format; \
	pnpm run lint; \
	pnpm run bundle

.PHONY: scripts
scripts-new:
	@which corepack >/dev/null 2>&1 || (echo "ERROR: corepack is required, please install it first\nhttps://pnpm.io/installation#using-corepack"; exit 1)

	cd $(SCRIPTS_NAVIGATOR_WEB_PATH); \
	corepack install; \
	pnpm install --frozen-lockfile; \
	pnpm run format; \
	pnpm run lint; \
	pnpm run bundle; \
	mv dist/* ../src/main/assets/readium/navigators/web/

.PHONY: scripts
scripts: scripts-legacy scripts-new
