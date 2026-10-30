# HansAps — Android 10 Storage & Local Account Compatibility Fix

## Root cause addressed

The project targets Android 15 (API 35), while the failing custom ROM is Android 10 (API 29). Android 10 applies scoped storage by default to apps targeting API 29 or higher. Direct `File` writes into public `Download/HansAps/...` folders are therefore not made reliable merely by granting the legacy `WRITE_EXTERNAL_STORAGE` permission. Android's own compatibility guidance also says scoped storage is enforced for target-29+ apps and direct filesystem access outside app-specific directories is restricted.

## Local Account
- Profile photos, profile/banner videos, stories, and profile JSON now live under the app's private `filesDir/HansAps/.config` directory.
- Local Account no longer gates the page on broad storage access.
- The profile JSON is saved with `AtomicFile`, reducing the chance of corruption if the app/ROM stops a write mid-save.
- Image/document picker read access is persisted when the provider supports persistable URI grants; unsupported provider grants are handled safely.
- The former `Download/HansAps/.config` directory is imported best-effort without deleting the source files. On Android 10 scoped storage may prevent access to legacy non-media files; if that happens, select the profile image again from the picker.
- Save errors now show the actual failure detail rather than the misleading generic “cek izin” message.

## Music offline downloads
- RexMusic offline tracks and playlist JSON are stored in `filesDir/HansAps/Spotify`, which does not require public-storage permissions on Android 10 or custom ROMs.
- The offline storage readiness check performs a real one-byte write test instead of trusting `File.canWrite()` or runtime permission flags.
- Legacy `Download/HansAps/Spotify` content is copied best-effort when readable. Original files are not deleted.
- The obsolete `WRITE_EXTERNAL_STORAGE` and `MANAGE_EXTERNAL_STORAGE` declarations were removed from the manifest for this flow. File import continues to use Android's system document picker.
- The app UI labels the library as app-private storage. These downloads are managed inside HansAps and are not directly browsable as public `Download/` files. App-private files are removed when the app is uninstalled.

## Download notifications
- Notification channel creation and `notify()` are wrapped as best-effort operations. A ROM-specific notification error must not abort an otherwise valid download.
- Notification taps open HansAps. Progress, completion, cancellation, and failure status remain supported.
- Android 13+ still requires notification permission for visible notifications. Android 10 does not use the `POST_NOTIFICATIONS` runtime permission; the user can still disable notifications per app/channel in system settings.

## Validation performed
- `AndroidManifest.xml` and the notification vector drawables parse as valid XML.
- Structural delimiter checks passed for all changed Kotlin files.
- A full Android compile was not completed: Gradle Wrapper needs Gradle 8.11.1 from `services.gradle.org`, but the environment cannot resolve that host (`UnknownHostException`). No claim is made that `assembleDebug` has passed.

## Build on a machine with Gradle distribution/network access
```bash
./gradlew :app:assembleDebug
```
