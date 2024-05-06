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
	@which corepack >/dev/null 2>&1 || (echo "ERROR: corepack is required, please install it first\nhttps://pnpm.io/installation#using-corepack"; exit 1)

	cd $(SCRIPTS_PATH); \
	corepack install; \
	pnpm install --frozen-lockfile; \
	pnpm run format; \
	pnpm run lint; \
	pnpm run bundle

# Will copy the failed Navigator UI tests snapshots from the Android test output to the assets folder.
# This is useful to update the snapshots after a test failure.
.PHONY: copy-snapshots
copy-snapshots:
	cp -R readium/navigator/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/*/snapshots/* readium/navigator/src/androidTest/assets/snapshots

