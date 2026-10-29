# HansAps — YouTube UI Revision

## Fokus revisi
- Halaman jelajah/pencarian dan halaman pemutar online sekarang merupakan dua route UI terpisah. Saat video dipilih, UI pencarian, hasil, dan tab bawah disembunyikan. Tombol kembali dan tombol Back Android kembali ke halaman jelajah.
- Halaman pemutar online memiliki player yang responsif, kontrol fullscreen, kontrol orientasi, pemilihan resolusi, download, metadata video, state loading, dan aksi coba lagi. Pergantian resolusi mempertahankan instance ExoPlayer dan berupaya melanjutkan dari posisi pemutaran saat ini.
- Video offline pada Koleksi juga dibuka sebagai halaman pemutar terpisah, bukan inline di daftar koleksi.
- Daftar video memakai grid adaptif pada layar lebar dan daftar vertikal pada layar sempit. Halaman player memakai layout satu kolom di ponsel dan dua kolom di layar lebar. Semua ukuran UI berbasis dp/sp dan area aman sistem.
- State video aktif disimpan melalui Saver Compose; state login tidak diubah dalam revisi ini.

## Validasi
- XML `AndroidManifest.xml` dapat diparse.
- Pemindaian parser Kotlin tidak menemukan error sintaks (pengecekan dengan `kotlinc` tanpa Android/Compose classpath).
- Arsip hasil harus lolos `unzip -t`.
- Build Gradle penuh belum bisa dijalankan di lingkungan ini karena wrapper memerlukan unduhan Gradle 8.11.1 dari `services.gradle.org`, dan DNS/jaringan host tidak tersedia. Karena itu keberhasilan `assembleDebug` belum dapat dikonfirmasi di sini.

Build di Android Studio atau lingkungan yang punya koneksi Gradle dengan:

```bash
./gradlew :app:assembleDebug
```
