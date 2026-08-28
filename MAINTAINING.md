# Maintaining the Readium Kotlin toolkit

## Releasing a new version

You are ready to release a new version of the Kotlin toolkit? Great, follow these steps:

1. Figure out the next version using the [semantic versioning scheme](https://semver.org).
2. Test a migration from the last released version.
3. Update the [migration guide](docs/migration-guide.md) in case of breaking changes.
4. Prepare the release.
    ```shell
    scripts/release-prepare.sh VERSION
    ```
    This script does the following:

    1. Creates a `release-VERSION` branch from `develop`.
    2. Bumps the version in `gradle.properties` and `test-app/build.gradle.kts`.
    3. Bumps the version in `README.md` and checks the "Minimum Requirements" section.
    4. Closes the version in `CHANGELOG.md`, [for example](https://github.com/readium/kotlin-toolkit/commit/011e0d74adc66ec2073f746d815310b838af4fbf).
    5. Closes the `## Unreleased` section in `docs/migration-guide.md` (if present).
    6. Creates a PR to merge into `develop`.
5. Verify the CI checks pass for the PR, then squash and merge it.
6. Tag the new version from `develop`.
    ```shell
    scripts/release-tag.sh
    ```
    This script extracts the version from the last commit message, creates an annotated tag, and pushes it.
7. Create a new release on GitHub.
    ```shell
    scripts/release-github.sh
    ```
    The script reads the version from the tag pointing at HEAD, then creates a pre-filled draft release.
    1. Add an APK to the release page **with LCP enabled**.
    2. Review and publish the draft on GitHub.
8. Publish to Maven Central.
    1. Verify that the [`Publish` workflow](https://github.com/readium/kotlin-toolkit/actions/workflows/publish.yml) successfully pushed and closed the release to Maven Central.
    2. Sign in to https://central.sonatype.com/publishing/deployments
    3. Verify the content of the components.
    4. Publish the components
9. Check that the new modules can be imported in an Android project from Maven Central.
10. Merge `develop` into `main`.

### Publishing to Maven Central manually

If the `Publish` workflow fails, you may need to publish to Maven Central manually.

#### With the new vanniktech's Maven publish plugin

1. Make sure you have the secrets in `.envrc` and [direnv](https://direnv.net) installed.
2. Run:
    ```
    ./gradlew publishToMavenCentral --no-configuration-cache
    ```
3. Sign in to https://central.sonatype.com/publishing/deployments
4. Publish manually the components

## Troubleshooting

### GitHub CI workflow is stuck

If a CI workflow is stuck with this message:

```
Requested labels: ubuntu-18.04
Job defined at: readium/kotlin-toolkit/.github/workflows/docs.yml@refs/heads/main
Waiting for a runner to pick up this job...
```

Try to update the version of the OS image in the workflow.
