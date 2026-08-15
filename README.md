# Presensi Pagi Android APK

Wrapper Android untuk aplikasi **Presensi Pagi SMP Islam Cipaku**.

## URL aplikasi
https://hayatinajma5-boop.github.io/absensi-kegiatan-pagi/

## Fitur wrapper
- Tampilan full-screen seperti aplikasi Android.
- Kamera WebView untuk QR scanner.
- Permission kamera native.
- JavaScript, localStorage, cookie, dan module script aktif.
- Navigasi index / rekap / admin tetap di dalam aplikasi.
- Export file Blob (Excel/PDF) diarahkan ke folder `Download/Presensi Pagi`.
- `window.print()` diarahkan ke Android Print Manager.
- Pop-up admin/QR print didukung.
- Tombol Back Android mengikuti riwayat halaman web.
- Screen tetap menyala saat aplikasi digunakan.

## Build lewat GitHub Actions
1. Buat repository baru, misalnya `presensi-pagi-apk`.
2. Upload seluruh isi folder project ini, termasuk folder `.github`.
3. Commit ke branch `main`.
4. Buka tab **Actions** dan tunggu workflow `Build APK Presensi Pagi` selesai.
5. Buka run yang hijau -> **Summary** -> **Artifacts** -> `Presensi-Pagi-APK`.
6. Extract ZIP artifact, lalu install `app-debug.apk` di Android.

## Catatan penting
APK memuat website GitHub Pages secara langsung. Jadi perubahan tampilan/fungsi di repo web utama akan otomatis ikut terlihat di APK tanpa rebuild, selama URL GitHub Pages tetap sama.

## Branding SMPI Cipaku
Versi ini memakai logo SMP Islam Cipaku sebagai launcher icon dan splash screen.
Nama aplikasi Android: **Presensi Pagi**
Package ID: `id.smpislamcipaku.presensipagi`
Web utama: `https://hayatinajma5-boop.github.io/absensi-kegiatan-pagi/`

Catatan update:
- Perubahan fitur/tampilan pada web GitHub Pages tidak memerlukan build APK ulang selama URL web tetap sama.
- Perubahan icon, nama aplikasi Android, splash screen, permission, atau kode native memerlukan build APK baru.
- Untuk distribusi final dan update APK tanpa uninstall, gunakan signing key yang sama pada setiap versi.
