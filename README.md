# Dotilda

A gentle daily journaling app for e-ink Android devices, first of all the Mudita Kompakt.
One question a day, a short answer, and a galaxy of dots that fills up, one dot per day.
Free, offline, private: no account, everything stays on the device. The only network access
is the optional update check against this repository's releases.

Built with Kotlin, Jetpack Compose and [Mudita Mindful Design](https://github.com/mudita/MMD).

## Build

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Every push also builds it on GitHub Actions
(artifact `dotilda-debug-apk`). Install it on the Kompakt with Mudita Center.

## Release

1. Bump `appVersion` in `app/build.gradle.kts` and add its section to `CHANGELOG.md`.
2. Push the tag `v<version>` (or run the Build workflow by hand on `main` with the version).
3. The workflow publishes `dotilda.apk` and `manifest.json` on GitHub Releases; installed apps
   find the update in Settings.

The release key is read from the repository secrets `RELEASE_KEYSTORE_BASE64`,
`RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`.

## Docs

- Product spec (French): [docs/spec.md](docs/spec.md)
- Changes: [CHANGELOG.md](CHANGELOG.md)

## License

Apache 2.0, see [LICENSE](LICENSE).
