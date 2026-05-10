#!/usr/bin/env bash
# =============================================================================
# release-tag.sh [--dry-run]
# =============================================================================
# Tag the new version from `develop` and push the tag. The version is extracted
# automatically from the last commit message (`VERSION (#N)` or `VERSION`).
#
# --dry-run - Skip the actual creation of the tag
# =============================================================================

set -euo pipefail

. "$(cd "$(dirname "$0")" && pwd)/release-common.sh"

parse_flags "$@"

# Branch check
CURRENT_BRANCH="$(git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD)"
[[ "$CURRENT_BRANCH" == "develop" ]] || \
    error "Must be on the 'develop' branch (currently on '$CURRENT_BRANCH')"

# Fast-forward to latest origin/develop
info "Fetching and fast-forwarding develop"
git -C "$REPO_ROOT" fetch origin
git -C "$REPO_ROOT" merge --ff-only origin/develop

# Extract version from last commit message (e.g. "3.0.1 (#42)" or "3.0.1")
LAST_MSG="$(git -C "$REPO_ROOT" log -1 --format="%s")"
if [[ "$LAST_MSG" =~ ^([0-9]+\.[0-9]+(\.[0-9]+)?)( \(#[0-9]+\))?$ ]]; then
    VERSION="${BASH_REMATCH[1]}"
else
    error "Last commit on develop does not look like a release commit.
  Expected subject: \"x.y.z (#N)\" or \"x.y.z\"
  Got:              \"$LAST_MSG\""
fi
check_semver "$VERSION"
info "Tagging $VERSION"

# Tag and push
if [[ $DRY_RUN -eq 1 ]]; then
    dry_skip "git tag -a \"$VERSION\" -m \"$VERSION\""
    dry_skip "git push --tags"
else
    git -C "$REPO_ROOT" tag -a "$VERSION" -m "$VERSION"
    git -C "$REPO_ROOT" push --tags
fi

info "Tagged and pushed $VERSION."
