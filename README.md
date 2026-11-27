# Re:XInternalSD

Modul Xposed untuk mengalihkan path penyimpanan internal ke **SD card eksternal**, diperbarui agar berjalan di **Android 11 sampai Android 16**.

> Fork dari [pyler/XInternalSD](https://github.com/pyler/XInternalSD). Kredit penuh untuk pembuat aslinya, **pyler**.

## Fitur

- Mengalihkan path yang dikembalikan ke aplikasi (`getExternalFilesDir`, `getObbDir`, `getExternalStoragePublicDirectory`, dan sejenisnya) ke SD card.
- Path SD card bisa diisi manual atau terdeteksi otomatis.
- Bisa diaktifkan untuk semua aplikasi atau hanya aplikasi tertentu, dan bisa mengecualikan aplikasi tertentu.
- Opsi menampilkan atau menyembunyikan ikon aplikasi.

## Perubahan dari versi asli

- Dibangun ulang dengan Gradle modern (AGP 8.4, compileSdk/targetSdk 34, minSdk 24).
- Bisa diinstall di Android 14+ (targetSdk sebelumnya 23 ditolak sistem).
- Hook ke system_server tidak lagi menggagalkan hook lainnya. Setiap hook dipasang terpisah dengan penanganan error.
- Tidak lagi memakai `MODE_WORLD_READABLE`. Preferensi dibaca lewat `xposedsharedprefs` (LSPosed/Vector).
- Ditambah izin `QUERY_ALL_PACKAGES` agar daftar aplikasi muncul di Android 11+.
- Perbaikan beberapa bug kecil (pengecekan null, penggantian path yang lebih aman, key preferensi yang tidak cocok).

## Kebutuhan

- Android 7.0 (API 24) atau lebih baru. Diuji di **Android 16 (custom ROM)**.
- Root dengan **Magisk**.
- **Vector / LSPosed** (dengan Zygisk aktif).

## Instalasi

1. Unduh APK dari halaman [Releases](../../releases).
2. Install APK di HP.
3. Buka **Vector**, masuk ke tab **Modules**, lalu aktifkan **Re:XInternalSD**.
4. Centang **aplikasi target** pada daftar scope. Tidak perlu mencentang System Framework.
5. Buka aplikasi **Re:XInternalSD**, lalu atur path SD card dan pilih aplikasi yang ingin dialihkan.
6. **Force stop** aplikasi target, lalu buka kembali.

Jika ada masalah, buka Vector, tab **Logs**, dan cari baris berawalan `XInternalSD:`.

## Batasan di Android 11+

Karena **scoped storage**, hasilnya bisa berbeda-beda tergantung aplikasi:

- Fitur **External SD card full access** (memberi grup `media_rw` lewat PackageManager) **tidak didukung** di Android 11+ dan otomatis dilewati.
- Aplikasi biasanya tetap bisa menulis di folder miliknya sendiri, yaitu `Android/data/<nama paket>` di SD card.
- Folder publik seperti `DCIM` atau `Download` di SD card bisa ditolak, kecuali aplikasi punya izin **All files access**.
- Modul ini hanya mengubah path yang dikembalikan ke aplikasi. Aplikasi yang memakai path tetap (hardcoded) atau Storage Access Framework tidak terpengaruh.

## Build dari source

1. Install **Android Studio** (diuji dengan Jellyfish 2023.3.1) dan Android SDK Platform 34.
2. Buka folder proyek, tunggu Gradle sync selesai.
3. Pilih **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

Dependency `de.robv.android.xposed:api:82` diambil dari repositori `https://api.xposed.info/`, jadi pastikan koneksi ke sana tidak diblokir.

## Kontribusi

Issue dan pull request dipersilakan. Saat melaporkan masalah, sertakan versi Android atau ROM, versi Vector/LSPosed, nama aplikasi target, dan log dari Vector.

## Lisensi

Proyek ini memakai lisensi BSD yang sama dengan proyek aslinya. Lihat berkas [LICENSE](LICENSE).

Copyright (c) 2014, pyler
Modifikasi (c) 2026, virga
