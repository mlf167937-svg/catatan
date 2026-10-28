# HansAps V1 — Android / Kotlin Compose

HansAps V1 is a native Android project based on the Gradle configuration supplied for RexAps.

- **Project:** `HansAps`
- **Namespace / application ID:** `com.hansaps`
- **UI:** Jetpack Compose + Material 3, dark navy surface and blue HansAps accent
- **Branding:** user-provided HANS logo used as launcher icon and in-app wordmark
- **Playback:** AndroidX Media3 / ExoPlayer / MediaSessionService
- **LAN sync:** host/join rooms on Wi-Fi, playback control/position sync, local-track transfer
- **Minimum Android:** Android 10 (API 29)
- **Build target:** API 35, JDK 17

## Open the project

1. Extract `HansAps-V1-Logo-Sync-Quality-Fix.zip`.
2. Open the extracted `HansAps` folder in Android Studio.
3. Install Android SDK Platform 35 and use JDK 17 if Studio prompts you.
4. Let Gradle sync, then run the `app` configuration or use `./gradlew :app:assembleDebug`.

An Android build and device test have **not** been completed in the editing environment because Gradle 8.11.1 could not be downloaded from `services.gradle.org` (DNS/network unavailable). The source was statically inspected, but this archive must still be built and tested in Android Studio, especially video muxing and local-network sync on two physical devices.

## Initial accounts

| Username | Password | Role |
|---|---|---|
| `Rex` | `RexStudio1@` | Developer |
| `Han` | `han123@@` | Owner |

There is no public registration screen. Developer can create accounts of any available role, including Developer. Owner can create other roles, but cannot assign Developer. Account checks are enforced in the UI and in `AccountStore`. Passwords are stored as PBKDF2-HMAC-SHA256 hashes with per-account salts.

**Security note:** account data and authorization are local to this installation. Treat this as a prototype, not a production authentication system. Production authorization should be enforced by a trusted backend.

## Nexray endpoints from the original HTML

The routes below were read directly from the HTML's JavaScript API constants:

- Spotify search: `https://api.nexray.eu.cc/search/spotify?q=`
- Spotify audio resolver: `https://api.nexray.eu.cc/downloader/spotify?url=`
- Lyrics: `https://api.nexray.eu.cc/search/lyrics?q=`
- YouTube search: `https://api.nexray.eu.cc/search/youtube?q=`
- YouTube media/quality resolver: `https://api.nexray.eu.cc/downloader/aio?url=`

The app uses these endpoints directly through `core/NexrayApi.kt`:

- Spotify search results are presented in the **Cari musik** screen. Play resolves the audio URL through Nexray; Download resolves audio, fetches lyrics when available, and stores metadata and thumbnail when available.
- Playlist items first match against the offline collection. If a match is not found, the app searches Nexray for the track and attempts online playback.
- YouTube results are shown in **Jelajahi**. Selecting a video reads `result.medias[]`, showing the `label`/`quality` entries for available MP4 video tracks. It does not look for a `resolution` property. The API's successful `result.error = false` value is treated as success, not as a failure.
- MP4 video-only renditions can use an accompanying M4A audio track. Offline download downloads both tracks and remuxes them into one MP4 with Android `MediaExtractor` and `MediaMuxer` (no re-encoding). Video-only renditions can also preview with the separate M4A audio through Media3's merged media source when the API provides it.
- Search/audio/lyrics results are cached with TTLs and bounded LRU maps based on the original RexMusic approach; direct media URLs have a shorter cache lifetime because they expire.

The endpoint names and response field handling follow the original HTML. Live service availability and the API's current response schema have not been independently verified in this environment; errors from the service are shown in the UI.

## Intro and banner videos

- `app/src/main/res/raw/intro.mp4` plays fullscreen **once after login succeeds**. It is not used as the login or Home banner. The video can be skipped.
- `app/src/main/res/raw/banner_vid.mp4` is the separate looping Home banner. Android resource names must be lowercase with underscores; `BannerVid.mp4` would fail resource compilation, so the file is named `banner_vid.mp4` and referenced as `R.raw.banner_vid`.

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

The playlist model is deliberately HansAps-specific: `Playlist.ini` declares named sets, `PlaylistParser` converts entries into artist/title records, playback checks the HansAps offline collection first, and only unresolved entries are matched through Nexray. Matching entries are then sent to the HansAps playback queue. This keeps the playlist source simple to edit while making playback offline-first rather than copying RexMusic's playlist storage implementation. Spotify search/download UI has no manual import controls.

## Download folder layout

The app uses Android MediaStore/scoped storage and writes inside the shared `Download/HansAps/` tree without requesting `MANAGE_EXTERNAL_STORAGE`:

```text
Download/HansAps/
├── Spotify/
│   └── Artist-Track/
│       ├── thumbnail.jpg     # if a thumbnail URL was supplied
│       ├── music.mp3
│       ├── meta.json
│       └── lyrics.json
└── Youtube/
    └── Video-Title/
        ├── thumbnail.jpg     # if a thumbnail URL was supplied
        ├── video.mp4
        └── meta.json
```

The offline collection scans items in these HansAps folders. There are no manual import buttons for songs or videos. These locations are intentionally separate from RexMusic's storage paths.

## Implemented structure

- `home/`: login, post-login intro video, Home dashboard, Developer and Owner consoles.
- `spotify/`: Spotify-style home, Hans Mix playlists, offline collection, Nexray search and music playback/download view models.
- `spotify/Engine/`: music downloader and playback queue logic, storing under `Download/HansAps/Spotify/`.
- `spotify/SpotifySyncEngine.kt` + `SpotifySyncScreen.kt`: LAN room sync on TCP ports 38741/38742. Host playback state and controls are shared; local media can be transferred within the LAN to a joining device. Both devices must be on the same reachable Wi-Fi/LAN; allow local network traffic in the device/router firewall.
- `youtube/`: Nexray search, selectable MP4 quality, separate M4A audio handling, offline collection and merged online preview. YouTube downloads remain under `Download/HansAps/Youtube/`.
- `core/`: Nexray API client, models, account store, MediaStore saving, Media3 playback/session and shared UI components.

Other original HTML features such as wallet/balance, breached counter, chat and inbox are not fully connected to a production backend in this V1. They should not be treated as production-backed features.
