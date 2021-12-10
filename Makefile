SCRIPTS_PATH := readium/navigator/src/main/assets/_scripts

help:
	@echo "Usage: make <target>\n\n\
	  install\tDownload NPM dependencies\n\
	  scripts\tBundle EPUB scripts with Webpack\n\
	  lint-scripts\tCheck quality of EPUB scripts\n\
	"

install:
	yarn --cwd "$(SCRIPTS_PATH)" install --frozen-lockfile

scripts:
	yarn --cwd "$(SCRIPTS_PATH)" run format
	yarn --cwd "$(SCRIPTS_PATH)" run bundle

lint-scripts:
	yarn --cwd "$(SCRIPTS_PATH)" run lint
