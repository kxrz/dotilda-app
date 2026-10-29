# Dotilda

*nulla dies sine linea* — not a day without a line.

A gentle daily journaling app for e-ink Android devices, first of all the Mudita Kompakt.
One question a day, a short answer, and a galaxy of dots that fills up, one dot per day.
Free, offline, private: no account, no network, everything stays on the device.

Built with Kotlin, Jetpack Compose and [Mudita Mindful Design](https://github.com/mudita/MMD).

## Build

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Every push also builds it on GitHub Actions
(artifact `dotilda-debug-apk`). Install it on the Kompakt with Mudita Center.

## Docs

- Product spec (French): [docs/spec.md](docs/spec.md)

## License

Apache 2.0, see [LICENSE](LICENSE).
