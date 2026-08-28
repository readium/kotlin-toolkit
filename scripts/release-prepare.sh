#!/usr/bin/env bash
# =============================================================================
# release-prepare.sh [--dry-run] [--skip-git-checks] VERSION
# =============================================================================
# Create the release branch, bump all version strings, close the CHANGELOG and
# Migration Guide, commit, and open a PR.
#
# VERSION - The new version to release (e.g. 3.0.1)
# --dry-run - Skip `git push` and `gh pr create`
# --skip-git-checks - Skip `develop` branch checks
# =============================================================================

set -euo pipefail

. "$(cd "$(dirname "$0")" && pwd)/release-common.sh"

parse_flags "$@"

VERSION="$(positional_args "$@")"
[[ -n "$VERSION" ]] || error "Usage: $(basename "$0") [--dry-run] [--skip-git-checks] VERSION"
check_semver "$VERSION"

# Prerequisite checks
command -v gh &>/dev/null || error "'gh' CLI not found — install from https://cli.github.com"
command -v python3 &>/dev/null || error "'python3' not found"

if [[ $SKIP_GIT_CHECKS -eq 0 ]]; then
    CURRENT_BRANCH="$(git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD)"
    [[ "$CURRENT_BRANCH" == "develop" ]] || \
        error "Must be on the 'develop' branch (currently on '$CURRENT_BRANCH')"

    git -C "$REPO_ROOT" fetch origin

    LOCAL_SHA="$(git -C "$REPO_ROOT" rev-parse HEAD)"
    REMOTE_SHA="$(git -C "$REPO_ROOT" rev-parse origin/develop)"
    [[ "$LOCAL_SHA" == "$REMOTE_SHA" ]] || \
        error "Local 'develop' is not in sync with 'origin/develop'. Pull or push first."
fi

# Old version
OLD_VERSION="$(git -C "$REPO_ROOT" describe --tags --abbrev=0)"
check_semver "$OLD_VERSION"
info "Preparing release $OLD_VERSION → $VERSION"

# Branch
BRANCH="release-$VERSION"
if git -C "$REPO_ROOT" show-ref --verify --quiet "refs/heads/$BRANCH"; then
    read -r -p "Branch '$BRANCH' already exists. Delete and recreate it? [y/N] " CONFIRM
    [[ "$CONFIRM" =~ ^[Yy]$ ]] || error "Aborted."
    git -C "$REPO_ROOT" branch -D "$BRANCH"
fi
info "Creating branch '$BRANCH'"
git -C "$REPO_ROOT" checkout -b "$BRANCH"

# gradle.properties
GRADLE_PROPS="$REPO_ROOT/gradle.properties"
info "Bumping version in gradle.properties"
sed_inplace "s/^pom\.version=.*/pom.version=$VERSION/" "$GRADLE_PROPS"

# test-app/build.gradle.kts
BUILD_GRADLE="$REPO_ROOT/test-app/build.gradle.kts"
info "Bumping version in test-app/build.gradle.kts"
sed_inplace "s/versionName = \"$OLD_VERSION\"/versionName = \"$VERSION\"/" "$BUILD_GRADLE"

# README.md
info "Bumping version in README.md"
python3 "$SCRIPT_DIR/release-md-tools.py" update-readme "$VERSION" "$OLD_VERSION" "$REPO_ROOT/README.md"

# CHANGELOG.md
info "Closing CHANGELOG.md for $VERSION"
python3 "$SCRIPT_DIR/release-md-tools.py" close-changelog "$OLD_VERSION" "$VERSION" "$REPO_ROOT/CHANGELOG.md"

# docs/migration-guide.md
MIGRATION_GUIDE="$REPO_ROOT/docs/migration-guide.md"
info "Closing Migration Guide (if needed)"
python3 "$SCRIPT_DIR/release-md-tools.py" close-migration-guide "$VERSION" "$MIGRATION_GUIDE"

# Commit
info "Staging and committing"
if [[ $DRY_RUN -eq 1 ]]; then
    dry_skip "git add *"
    dry_skip "git commit -m \"$VERSION\""
else
    git -C "$REPO_ROOT" add \
        "$GRADLE_PROPS" \
        "$BUILD_GRADLE" \
        "$REPO_ROOT/README.md" \
        "$REPO_ROOT/CHANGELOG.md" \
        "$MIGRATION_GUIDE"
    git -C "$REPO_ROOT" commit -m "$VERSION"
fi

# Push + PR
info "Pushing branch '$BRANCH'"
if [[ $DRY_RUN -eq 1 ]]; then
    dry_skip "git push -u origin $BRANCH"
    dry_skip "gh pr create --base develop --title \"$VERSION\" --body \"\""
else
    git -C "$REPO_ROOT" push -u origin "$BRANCH"
    PR_URL="$(gh pr create --base develop --title "$VERSION" --body "" | tail -1)"
    info "PR created: $PR_URL"
    open_url "$PR_URL"
fi
