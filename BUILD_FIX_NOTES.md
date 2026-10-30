HansAps v1.2 — build repair notes

Changes applied:
- Made RexDownloadNotification.CHANNEL_ID public so foreground download services can access it.
- Added RexDownloadNotification.GROUP_KEY and grouped per-track notifications under the shared key.
- Added/registered RexMusicDownloadService as a dataSync foreground service.
- Start the music foreground service when a music download is enqueued.
- Added a v2 music notification channel to avoid inheriting old channel importance settings.
- Added Kotlin daemon JVM arguments disabling Parallel GC auto-selection as suggested by the submitted build log.
- Bumped versionCode/versionName to 3 / 1.2.

YouTube download progress UI and the YouTube dataSync service from the previous project are retained.

Build from the project root:
  ./gradlew clean :app:assembleDebug

The submitted log references RexDownloadForegroundService.kt, which is not present in the delivered source snapshot. The CHANNEL_ID visibility fix is included for callers using that shared helper; the delivered project uses RexMusicDownloadService as its registered Spotify download foreground service.
