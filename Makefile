SCRIPTS_PATH := readium/navigator/src/main/assets/_scripts
SCRIPTS_NAVIGATOR_WEB_PATH := readium/navigators/web/internals/scripts

help:
	@echo "Usage: make <target>\n\n\
	  lint\t\t\tLint the Kotlin sources with ktlint\n\
	  format\t\tFormat the Kotlin sources with ktlint\n\
	  scripts\t\tBundle the Navigator EPUB scripts\n\
	  update-a11y-l10n\tUpdate the Accessibility Metadata Display Guide localization files\n\
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
	rm -r ../src/main/assets/readium/navigator/web/internals/generated/*; \
	mv dist/* ../src/main/assets/readium/navigator/web/internals/generated/

.PHONY: scripts
scripts: scripts-legacy scripts-new

.PHONY: update-a11y-l10n
update-a11y-l10n:
	@which node >/dev/null 2>&1 || (echo "ERROR: node is required, please install it first"; exit 1)
	rm -rf publ-a11y-display-guide-localizations
	git clone https://github.com/w3c/publ-a11y-display-guide-localizations.git
	node scripts/convert-a11y-display-guide-localizations.js publ-a11y-display-guide-localizations android readium/shared/src/main readium_a11y_
	rm -rf publ-a11y-display-guide-localizations
