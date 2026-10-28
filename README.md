# HansAps V1 — Android / Kotlin Compose

HansAps V1 is a native Android project based on the Gradle configuration supplied for RexAps, renamed to:

- **Project:** `HansAps`
- **Namespace / application ID:** `com.hansaps`
- **UI:** Jetpack Compose + Material 3
- **Playback:** AndroidX Media3 / ExoPlayer / MediaSessionService
- **Minimum Android:** Android 10 (API 29)
- **Build target:** API 35, JDK 17

## Open the project

1. Extract `HansAps-V1-Source.zip`.
2. Open the extracted `HansAps` folder in Android Studio.
3. Install Android SDK Platform 35 and use JDK 17 if Studio prompts you.
4. Let Gradle sync, then run the `app` configuration or use `./gradlew :app:assembleDebug`.

The environment used to assemble this source archive did not have an Android SDK installed, so an APK has not been built or device-tested here.

## Initial accounts

| Username | Password | Role |
|---|---|---|
| `Rex` | `RexStudio1@` | Developer |
| `Han` | `han123@@` | Owner |

There is no public registration screen. The Developer and Owner consoles can create additional local accounts and set their role. Passwords for stored accounts are PBKDF2-HMAC-SHA256 hashes with per-account salts, not plain text, but this is still a **local prototype**: users with access to the device/app data may tamper with local state. For production, move authentication and role authorization to a trusted server and use a password KDF such as Argon2id or scrypt.

## Included V1 features

- Premium-simple dark UI with HansAps lime accent and the warm red/orange music player card inspired by the supplied screenshot.
- Intro video on the login screen and looping banner video on Home.
- Login with the two initial accounts, role labels, logout, Developer console and Owner console.
- Account creation from both consoles.
- Spotify-style Home, `Playlist.ini` parser, local collection, queue model, import audio, direct authorized-audio downloader, metadata JSON, lyrics JSON and optional thumbnail.
- YouTube-style provider response parser, selectable quality options, download of a direct authorized media URL, local video import, offline video collection and in-app playback.
- Files written under the public Downloads tree using scoped storage/MediaStore, without requesting `MANAGE_EXTERNAL_STORAGE`.
- Media3 session for Android playback notification/lock-screen controls, and in-app player controls resembling the supplied reference.

## Media resources

`app/src/main/res/raw/intro.mp4` was extracted from the HTML's Base64 video source.

The banner video is stored as `app/src/main/res/raw/banner_vid.mp4`. Android resource filenames must use lowercase letters, numbers and underscores; `BannerVid.mp4` would fail resource compilation, so the source name is normalized to `banner_vid.mp4` and is referenced as `R.raw.banner_vid`.

## Playlist configuration

Edit `app/src/main/assets/Playlist.ini`. Use one section per playlist; each song is `Artist - Title`:

```ini
[Playlist Populer]
Raim Laode - Dunia Yang Nanti
Tulus - Jatuh Suka
Sal Priadi - Id like to watching you sleep

[Playlist Santai]
Nadifh Basalamah - Bergema Sampai Selamanya
```

When a playlist item is played, the app looks for a matching track in its offline collection first. If the song is not local, V1 explains that no online music provider is configured. It does not scrape Spotify or bypass DRM. `DownloaderMusic` accepts a direct HTTPS URL from a provider authorized to distribute that audio.

## Download folder layout

The app writes to the Android shared `Download/HansAps/` tree via MediaStore:

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

On Android 10+, the app uses scoped storage via MediaStore instead of direct filesystem writes. Files remain visible under the public Downloads location. V1 only scans content stored under these HansAps folders into its offline collections.

## YouTube provider response format

The YouTube screen currently accepts a JSON response from a trusted provider. The exact external API endpoint was not included in the request, so no specific third-party API has been hard-coded. The response should be shaped like this:

```json
{
  "title": "My own video",
  "videoId": "my-video-id",
  "thumbnailUrl": "https://cdn.example.com/thumb.jpg",
  "formats": [
    {
      "label": "720p MP4",
      "quality": "720p",
      "mimeType": "video/mp4",
      "url": "https://cdn.example.com/my-video.mp4",
      "fileSizeBytes": 24000000,
      "downloadable": true,
      "license": "authorized"
    }
  ]
}
```

The parser only accepts an HTTPS **direct media URL** when the provider explicitly marks the format `downloadable: true` and `license` as `authorized` or `owned`. This is an adapter contract, not a built-in YouTube stream extractor. Use a provider that is allowed to distribute the requested media. Official YouTube Data API responses provide catalog metadata, not a general video-file download URL.

## Notes / not yet connected

- No external music or video provider credentials/API URL were supplied. Therefore live catalog search and provider-side resolution are not falsely represented as working. The downloader engine and response contract are included; connect an authorized provider adapter when its API details are available.
- Balance/credit/breached cards remain zero-value dashboard placeholders because the HTML's account/wallet/backend semantics are not provided as a native API contract.
- The source HTML includes additional wallet, store, status/chat/inbox, and admin workflows. This V1 prioritizes the requested native media app and role-based account UI; those web-only business flows have not been migrated into production-backed Kotlin features.
