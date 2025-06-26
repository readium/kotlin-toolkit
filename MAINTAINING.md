# Maintaining the Readium Kotlin toolkit

## Releasing a new version

You are ready to release a new version of the Kotlin toolkit? Great, follow these steps:

1. Figure out the next version using the [semantic versioning scheme](https://semver.org).
2. Test a migration from the last released version.
3. Update the [migration guide](docs/migration-guide.md) in case of breaking changes.
4. Issue the new release.
    1. Create a branch with the same name as the future tag, from `develop`.
    2. Bump the version numbers in:
        * `README`
        * `gradle.properties`
        * `test-app/build.gradle.kts`
    5. Close the version in the `CHANGELOG.md` and `docs/migration-guide.md`, [for example](https://github.com/readium/kotlin-toolkit/commit/011e0d74adc66ec2073f746d815310b838af4fbf).
    6. Create a PR to merge in `develop` and verify the CI workflows.
    7. Squash and merge the PR.
    8. Tag the new version from `develop`.
        ```shell
        git checkout develop
        git pull
        git tag -a 3.0.1 -m 3.0.1
        git push --tags
        ```
5. Create a new release on GitHub.
    * Add an APK to the release page **with LCP enabled**.
6. Publish to Maven Central.
    1. Verify that the [`Publish` workflow](https://github.com/readium/kotlin-toolkit/actions/workflows/publish.yml) successfully pushed and closed the release to Maven Central.
    2. Sign in to https://central.sonatype.com/publishing/deployments
    3. Verify the content of the components.
    4. Publish the components
7. Check that the new modules can be imported in an Android project from Maven Central.
8. Merge `develop` into `main`.

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
