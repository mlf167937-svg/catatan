# HansAps — YouTube watch-page and rotation revision

## Perubahan

- Menambahkan pemulihan sesi lokal di `AccountStore`: hanya nama pengguna sesi yang disimpan; password plaintext tidak disimpan. Logout menghapus sesi lokal.
- Menangani perubahan konfigurasi orientasi pada `MainActivity` agar pergantian portrait/landscape tidak menghancurkan composition dan menampilkan ulang layar login.
- Mengembalikan orientasi ke `unspecified` saat keluar dari halaman YouTube.
- Mengubah alur pilih video menjadi watch page: pemutar tampil lebih dahulu, kemudian judul, channel/metadata, opsi kualitas, tombol unduh, dan daftar video lain.
- Menambahkan menu resolusi dari ikon pengaturan di pemutar inline maupun fullscreen.
- Mengganti sumber video pada instance ExoPlayer yang sama ketika resolusi berubah, lalu melanjutkan dari posisi pemutaran sebelumnya jika memungkinkan.
- Menyediakan tampilan fullscreen dan tombol orientasi pada overlay fullscreen.

## Validasi

- Arsip input dan output diperiksa untuk integritas ZIP.
- Berkas Kotlin yang diedit diperiksa secara statis; tidak ditemukan diagnostic parser/sintaks oleh compiler Kotlin standalone.
- `:app:assembleDebug` belum dapat diselesaikan di lingkungan kerja ini karena Gradle Wrapper harus mengunduh Gradle 8.11.1 dari `services.gradle.org`, tetapi koneksi DNS/jaringan tidak tersedia. Kotlin standalone juga tidak memiliki Android SDK dan classpath Jetpack Compose/Media3, sehingga pemeriksaan itu bukan pengganti build Android penuh.

Untuk validasi final pada komputer yang memiliki Android SDK dan koneksi dependensi, jalankan:

```bash
./gradlew :app:assembleDebug
```
