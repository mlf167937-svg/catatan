# HansAps V1 — Spotify rebuilt around the supplied RexMusic source

**Application ID:** `com.hansaps`  
**Version:** `1.0` (`versionCode = 1`)  
**Minimum Android:** Android 10 / API 29  
**Compile target:** API 35 · JDK 17 · Jetpack Compose

## Spotify V1

The Spotify entry screen now launches the full RexMusic-based experience rather than the earlier HansAps local-player home card. The imported music UI/controller has been placed under `com.hansaps.spotify.rexmusic`; its API adapter calls HansAps' existing Nexray client. The visual palette inherits HansAps dark theme, with HansAps naming and storage paths.

Included flows:

- Home with quick tiles, **Mix Pintar**, an **Autoplay Pintar / Smart Next Play** toggle, popular artists, last-played history, recommendations, and a visible **30 Lagu Pilihan** shelf.
- Search, playlists, library, local downloads, user queue, playback history, repeat, synchronized/plain lyrics, and a dedicated Now Playing experience.
- Audio resolution through Nexray, a foreground media notification, offline playback/download management, and LAN listen-together sync. Music downloads use a `dataSync` foreground service with grouped, cancelable progress notifications; the music and video download notification channels are compatible with Android 10 and newer.
- Audio/lyrics caches and learned artist preferences used by Smart Next Play. When enabled, the controller consumes the user queue first, then selects a next song using recent-play avoidance and listening taste/popular songs.
- Music downloads and playlists use `Download/HansAps/Spotify/`; sync cache uses `Download/HansAps/.cache/spotifySync/`.

The older Spotify UI files remain in the source tree for compatibility but are no longer the Spotify entry point. `SpotifyScreen.kt` delegates to the integrated RexMusic-based screen. The old home card labeled `LOCAL PLAYER` is not used by this entry point.

## YouTube and other HansAps features

The existing HansAps YouTube screen, local collection, login/roles, intro and banner resources, and account-management screens remain in the project. YouTube's default 360p/autoplay behavior is unchanged from the previous V1 source.

## Build and verification

1. Extract the archive and open the `HansAps` folder in Android Studio.
2. Use JDK 17 and install Android SDK Platform 35.
3. Sync Gradle, then build with `./gradlew :app:assembleDebug`.

**Build status: not verified in this environment.** The Gradle wrapper attempted to download Gradle 8.11.1 from `services.gradle.org`, but DNS/network access failed before compilation could begin. The archive is source code, not a built APK. After Gradle sync, test playback, notification permission, all-files access for offline storage, lyrics, and LAN sync on-device.

## Nexray routes

- Spotify search: `https://api.nexray.eu.cc/search/spotify?q=`
- Spotify audio: `https://api.nexray.eu.cc/downloader/spotify?url=`
- Lyrics: `https://api.nexray.eu.cc/search/lyrics?q=`
- YouTube search: `https://api.nexray.eu.cc/search/youtube?q=`
- YouTube media: `https://api.nexray.eu.cc/downloader/aio?url=`

Live endpoint availability is not guaranteed; network/API failures are surfaced by the app.

## Local accounts

| Username | Password | Role |
|---|---|---|
| `Rex` | `RexStudio1@` | Developer |
| `Han` | `han123@@` | Owner |

Account data is local to the installation and should be treated as prototype authentication, not production authorization.
