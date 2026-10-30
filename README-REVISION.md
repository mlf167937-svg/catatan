# HansAps — YouTube UI, Playback & Notifications Revision

## Upgrade UI/UX
- Home sekarang menggunakan dashboard responsif dengan greeting/avatar, hero banner dan CTA, metric cards, serta feature cards adaptif: kolom untuk ponsel dan tiga kolom untuk layar lebar.
- UI YouTube mempertahankan tampilan putih/merah yang terinspirasi YouTube, thumbnail video, grid adaptif, halaman pemutar terpisah, kontrol kualitas, fullscreen, dan orientasi.
- Koleksi video offline tetap memakai halaman pemutar terpisah.

## Notification playback
- Channel `Nonton YT` menampilkan judul video, kualitas, status play/pause, dan thumbnail besar bila thumbnail HTTPS berhasil diambil.
- Aksi notifikasi Putar/Jeda mengontrol ExoPlayer aktif; aksi Mulai Ulang mengembalikan playhead ke awal.
- Foreground service bertipe `mediaPlayback` menjaga notifikasi/player tetap hidup ketika aplikasi dipindah ke background. Saat halaman pemutar ditutup, notifikasi dan service dibersihkan.
- Playback video offline juga memakai notifikasi `Nonton YT` (tanpa artwork remote karena model video lokal tidak menyimpan URL thumbnail).

## Download notifications
- Channel `Download Video YT` menampilkan status dan progress unduh yang diperbarui selama stream diunduh, menyimpan video/metadata, dan saat proses selesai atau gagal.
- Progress unduhan didorong oleh byte aktual ketika server memberikan panjang konten; jika ukuran tidak tersedia, indikator berubah menjadi indeterminate.

## Validasi
- `AndroidManifest.xml` berhasil diparse; deklarasi foreground service, receiver, dan izin `FOREGROUND_SERVICE_MEDIA_PLAYBACK` terdeteksi.
- Pemeriksaan parser Kotlin (`kotlinc`) tidak menemukan diagnostic sintaks. Pemanggilan compiler tanpa Android/Compose/Media3 dependencies menghasilkan diagnostic unresolved-reference, jadi pemeriksaan ini bukan pengganti build Android.
- Build Gradle penuh belum dapat dijalankan di lingkungan ini karena Gradle Wrapper perlu mengunduh Gradle 8.11.1 dari `services.gradle.org`, tetapi resolusi host/jaringan gagal (`UnknownHostException`). Jangan menganggap `assembleDebug` sudah terverifikasi sampai dijalankan di Android Studio atau mesin dengan distribusi Gradle tersedia.

## Build check di lingkungan Android
```bash
./gradlew :app:assembleDebug
```
