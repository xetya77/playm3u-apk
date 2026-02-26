# Play M3U — Android TV APK

Aplikasi IPTV player berbasis WebView untuk Android TV, Android smartphone, dan tablet.

## ✅ Keunggulan vs versi web (GitHub Pages)

- **CORS policy tidak berlaku** — file HTML dimuat dari assets lokal
- **HTTP streams bisa diputar** — Mixed Content tidak diblokir
- **Semua remote TV support** — D-Pad, Channel Up/Down, angka, Back, Info
- **Autoplay tanpa interaksi** — media langsung diputar saat buka app
- **Fullscreen permanen** — tidak ada address bar atau system UI
- **Screen on** — layar tidak mati saat menonton

---

## 🔧 Cara Build APK (tanpa Android Studio)

### Metode: GitHub Actions (build di cloud, gratis)

**Langkah 1 — Fork atau buat repo baru**
- Buka github.com → New repository
- Nama repo: `playm3u-apk` (atau apa saja)
- Visibility: Public atau Private (keduanya bisa)
- Klik **Create repository**

**Langkah 2 — Upload semua file**

Cara termudah: klik **Upload files** di GitHub, lalu drag-and-drop seluruh isi folder ini.

Atau via Git di terminal:
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/USERNAME/playm3u-apk.git
git push -u origin main
```

**Langkah 3 — Tunggu build selesai**
- Setelah push, buka tab **Actions** di repo GitHub kamu
- Akan ada workflow **Build APK** yang berjalan otomatis
- Tunggu sekitar **5–10 menit** (proses build di server GitHub)
- Kalau ada tanda ✅ hijau → build berhasil

**Langkah 4 — Download APK**
- Klik workflow yang sudah selesai
- Scroll ke bawah ke bagian **Artifacts**
- Download **PlayM3U-debug** → file .zip berisi APK
- Extract zip → dapat file `app-debug.apk`

**Langkah 5 — Install ke Android TV**

*Via USB (ADB):*
```bash
# Aktifkan Developer Mode di Android TV
# Settings → About → klik Build Number 7x
# Settings → Developer Options → ADB Debugging ON

adb connect IP_ADDRESS_TV:5555
adb install app-debug.apk
```

*Via sideload (file manager):*
- Copy APK ke USB flashdisk
- Colok ke Android TV
- Buka File Manager di TV
- Install APK (pastikan "Unknown Sources" diaktifkan)

*Via app sideload (cara paling mudah):*
- Install **Downloader** di Android TV (dari Play Store)
- Di laptop, upload APK ke Google Drive atau transfer.sh
- Di Downloader masukkan link download APK

---

## 📁 Struktur File

```
playm3u-apk/
├── app/src/main/
│   ├── assets/
│   │   └── index.html          ← File HTML aplikasi utama
│   ├── java/com/playm3u/app/
│   │   └── MainActivity.java   ← WebView wrapper
│   ├── res/...                 ← Icon dan resources
│   └── AndroidManifest.xml
├── .github/workflows/
│   └── build.yml               ← GitHub Actions (build otomatis)
└── ...
```

---

## 🔄 Update Aplikasi

Kalau ingin update file HTML (misalnya ada fitur baru):
1. Replace file `app/src/main/assets/index.html`
2. Push ke GitHub
3. GitHub Actions otomatis build APK baru
4. Download dan install ulang

---

## 🎮 Kontrol Remote TV

| Tombol | Fungsi |
|--------|--------|
| ↑ / Channel+ | Channel berikutnya |
| ↓ / Channel- | Channel sebelumnya |
| ← | Buka daftar saluran |
| → | Channel berikutnya |
| OK / Enter | Pause / Play |
| BACK | Kembali ke menu |
| INFO | Tampilkan info channel |
| 0–9 | Input nomor channel |

---

## ⚙️ Persyaratan

- Android 5.0 (Lollipop) ke atas
- Android TV, Fire TV, atau HP/Tablet Android
- Koneksi internet untuk streaming

---

## 🐛 Troubleshooting

**APK tidak bisa diinstall:** Aktifkan "Install from Unknown Sources" di Settings → Security

**Video tidak muncul:** Pastikan URL stream aktif. Beberapa stream mungkin geo-blocked.

**Layar hitam:** Coba ganti channel, mungkin stream tersebut sedang down.
