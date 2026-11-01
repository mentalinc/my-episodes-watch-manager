# Building and signing releases

Signing material must never be committed to this repository. The `keystore/` directory is
ignored by git, and `app/build.gradle` only configures a release signing config when the
`KEYSTORE_FILE` environment variable points at a keystore.

## Local release build

```bash
export KEYSTORE_FILE=/absolute/path/to/release.keystore
export storepwd='<keystore password>'
export keyalias='<key alias>'
export keypwd='<key password>'
./gradlew assembleRelease
```

Debug builds need no configuration: the Android Gradle Plugin generates and uses the standard
debug keystore in `~/.android/debug.keystore`.

## CI

The workflow reads these repository secrets:

| Secret | Purpose |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 of the release keystore file |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |

Create the base64 value with:

```bash
base64 -w 0 release.keystore
```

When `ANDROID_KEYSTORE_BASE64` is absent the workflow still builds a debug APK, so forks and
pull requests keep working.

## Rotating the key

The previous `keystore/dvr-android.keystore` was committed to git and must be treated as
compromised:

1. Generate a new keystore and update the four CI secrets above.
2. Remove the old files from history. This rewrites history, so coordinate with everyone who
   has a clone:

   ```bash
   git filter-repo --path keystore/ --invert-paths
   git push --force --all
   git push --force --tags
   ```

3. Anyone who installed a build signed with the old key cannot upgrade in place: Android
   rejects an APK signed with a different key. They must uninstall the old app first (which
   also removes its data), or the new key must be enrolled through Play App Signing key
   rotation if the app is distributed there.
