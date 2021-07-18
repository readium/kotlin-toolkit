SCRIPTS_PATH := r2-navigator/src/main/assets/_scripts

help:
	@echo "Usage: make <target>\n\n\
	  scripts\tBundle EPUB scripts with Webpack\n\
	  lint-scripts\tCheck quality of EPUB scripts\n\
	"

scripts:
	yarn --cwd "$(SCRIPTS_PATH)" run format
	yarn --cwd "$(SCRIPTS_PATH)" run bundle


lint-scripts:
	yarn --cwd "$(SCRIPTS_PATH)" run lint
