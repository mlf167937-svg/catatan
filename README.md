# HansAps V1.2 — Android / Kotlin Compose

HansAps is a native Android project based on the Gradle configuration supplied for RexAps, with music/video flows adapted from the provided RexMusic source.

- **Project:** `HansAps`
- **Namespace / application ID:** `com.hansaps`
- **UI:** Jetpack Compose + Material 3, dark UI with HansAps blue accent and a red Now Playing surface
- **Branding:** supplied HANS logo used as launcher icon and in-app wordmark
- **Playback:** AndroidX Media3 / ExoPlayer / MediaSessionService
- **Minimum Android:** Android 10 (API 29)
- **Build target:** API 35, JDK 17

## V1.2 UI changes

- Spotify's primary tabs are in a **bottom navigation bar**: Home, Playlist, Koleksi, Cari, and Lirik.
- The compact player above the navigation bar opens a dedicated **Now Playing** screen with previous/play/pause/next, seek bar and elapsed/duration labels.
- The **Lirik** tab loads lyrics for the active track from Nexray. LRC timestamps are parsed and the active line is highlighted and followed as playback progresses. Plain-text lyrics are used when synced lyrics are unavailable.
- Sync remains available from the music header.
- YouTube has a bottom navigation bar for Jelajahi and Koleksi. Tapping a search result starts resolving the video and then automatically plays the **360p** rendition where available; if 360p is unavailable it chooses the closest reported resolution.
- The YouTube player has an explicit full-screen button and an exit-full-screen control. Other quality options can still be chosen from the video panel.

## Open the project

1. Extract the ZIP archive.
2. Open the extracted `HansAps` folder in Android Studio.
3. Install Android SDK Platform 35 and use JDK 17 if Studio prompts you.
4. Let Gradle sync, then run the `app` configuration or use `./gradlew :app:assembleDebug`.

An Android build and device test have **not** been completed in the editing environment because Gradle 8.11.1 could not be downloaded from `services.gradle.org` (DNS/network unavailable). The changed UI was statically inspected, but the archive still needs a full build and playback test in Android Studio. In particular, validate Nexray playback formats, full-screen behavior on a physical device, and LAN sync between two devices.

## Initial accounts

| Username | Password | Role |
|---|---|---|
| `Rex` | `RexStudio1@` | Developer |
| `Han` | `han123@@` | Owner |

There is no public registration screen. Developer can create accounts of any available role, including Developer. Owner can create other roles, but cannot assign Developer. Account checks are enforced in the UI and in `AccountStore`. Passwords are stored as PBKDF2-HMAC-SHA256 hashes with per-account salts.

**Security note:** account data and authorization are local to this installation. Treat this as a prototype, not a production authentication system. Production authorization should be enforced by a trusted backend.

## Nexray endpoints from the original HTML

The routes below were read from the HTML's JavaScript API constants:

- Spotify search: `https://api.nexray.eu.cc/search/spotify?q=`
- Spotify audio resolver: `https://api.nexray.eu.cc/downloader/spotify?url=`
- Lyrics: `https://api.nexray.eu.cc/search/lyrics?q=`
- YouTube search: `https://api.nexray.eu.cc/search/youtube?q=`
- YouTube media/quality resolver: `https://api.nexray.eu.cc/downloader/aio?url=`

The app uses these endpoints through `core/NexrayApi.kt`. YouTube reads the `result.medias[]` response and supports video-only MP4 plus a companion M4A audio stream when supplied. The API's successful `result.error = false` value is treated as success, not as a failure. Live service availability and its current response schema have not been independently verified; failures are shown in the UI.

## Intro and banner videos

- `app/src/main/res/raw/intro.mp4` plays fullscreen once after login succeeds and can be skipped.
- `app/src/main/res/raw/banner_vid.mp4` is the separate looping Home banner. Android resource names are lowercase with underscores.

## Spotify playlists

Edit `app/src/main/assets/Playlist.ini`. Each section defines a playlist, and each line uses `Artist - Title`:

```ini
[Playlist Populer]
Raim Laode - Dunia Yang Nanti
Tulus - Jatuh Suka
Sal Priadi - Id like to watching you sleep

[Playlist Santai]
Nadifh Basalamah - Bergema Sampai Selamanya
```

The playlist model is HansAps-specific: `Playlist.ini` declares named sets, `PlaylistParser` converts entries into artist/title records, playback checks the HansAps offline collection first, and only unresolved entries are matched through Nexray.

## Download folder layout

The app uses Android MediaStore/scoped storage and writes inside the shared `Download/HansAps/` tree without requesting `MANAGE_EXTERNAL_STORAGE`:

```text
Download/HansAps/
├── Spotify/
│   └── Artist-Track/
│       ├── thumbnail.jpg
│       ├── music.mp3
│       ├── meta.json
│       └── lyrics.json
└── Youtube/
    └── Video-Title/
        ├── thumbnail.jpg
        ├── video.mp4
        └── meta.json
```

Thumbnail files are written only when a thumbnail URL is supplied. The offline collection scans the HansAps-specific folders.

## Main structure

- `home/`: login, post-login intro, Home dashboard and Developer/Owner consoles.
- `spotify/`: Home, playlists, offline collection, Nexray search/download, playback/lyrics UI and LAN sync.
- `youtube/`: Nexray search, default 360p autoplay, manual quality selection, fullscreen playback and offline collection.
- `core/`: Nexray API, models, account store, MediaStore saving, Media3 playback/session and shared UI components.

Wallet/balance, breached counter, chat and inbox are not connected to a production backend in this V1. Treat these as UI placeholders, not production-backed features.
