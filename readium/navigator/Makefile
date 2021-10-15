SCRIPTS_PATH := src/main/assets/_scripts

help:
	@echo "Usage: make <target>\n\n\
	  install\tDownload NPM dependencies\n\
	  scripts\tBundle EPUB scripts with Webpack\n\
	  lint-scripts\tCheck quality of EPUB scripts\n\
	"

install:
	npm install --prefix "$(SCRIPTS_PATH)"

scripts:
	yarn --cwd "$(SCRIPTS_PATH)" run format
	yarn --cwd "$(SCRIPTS_PATH)" run bundle

lint-scripts:
	yarn --cwd "$(SCRIPTS_PATH)" run lint
