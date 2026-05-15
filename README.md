# F1 TV Patcher

Android TV helper app that checks the latest patched F1 TV release from
[`Alexvbp/f1tv-4k-patch`](https://github.com/Alexvbp/f1tv-4k-patch), compares it
with the installed `com.formulaone.production` package, and guides the user
through Android's required uninstall/install confirmations.

## Build

This project is a standard Kotlin Android app. Open it in Android Studio or run:

```sh
./gradlew test assembleDebug
```

The Gradle wrapper JAR is not committed by this scaffolding environment. If it is
missing, open the project in Android Studio or run `gradle wrapper` from a
machine with Gradle installed.
