# Log Perubahan Pembaruan Sistem: Palapa OS (Edisi Triwulan)
Target Perangkat: POCO F5 (marble) / AOSP Generic  
Versi Android: Android 17 (CP2A)  
Branch: `palapa-triwulan`  
Status Rilis: Stabil & Terverifikasi  

Navigasi Dokumen:
- [Versi Bahasa Indonesia (changelogs-id.md)](changelogs-id.md)
- [English Version (changelogs-en.md)](changelogs-en.md)

---

## Ringkasan Pembaruan (Edisi Triwulan)

Pembaruan cabang baru **`palapa-triwulan`** menghadirkan pembaruan besar antarmuka sistem (UI Redesign) yang terinspirasi oleh **Samsung One UI 9**, penyegaran total halaman Pengaturan Baterai, penyempurnaan Pusat Kontrol (Control Center), fitur interaktif Dynamic Island, serta optimasi performa kernel dan memori:

---

### 1. Redesain Total UI Baterai (Samsung One UI 9 Style)
Halaman Pengaturan Baterai (`Settings -> Battery`) telah dirombak secara menyeluruh mengadopsi bahasa desain Samsung One UI 9 murni dengan tetap mempertahankan seluruh fitur dan opsi sistem yang ada:

* **Eliminasi Total Sisa Elemen Pixel**:
  * Menghilangkan seluruh komponen header bawaan Google Pixel (`battery_header` ProgressBarPreference dan `battery_header_text` subtitle) serta duplikasi menu `battery_usage_summary`.
  * Pengaturan ditata rapi ke dalam kontainer kartu membulat (*grouped rounded cards*, corner radius 26dp) berbasis palet dinamis Material You Monet (`@color/settingslib_materialColorSurfaceContainer`).
  * Penanganan dependensi pengontrol header bawaan (`BatteryHeaderTextPreferenceController` dan `BatteryTipPreferenceController`) secara aman (*null-safe*) pada `PowerUsageSummary` serta penambahan placeholder preference tak terlihat guna menjamin fragment stabil tanpa crash NullPointerException saat inisialisasi.

* **Hero Overview Header**:
  * Angka persentase baterai berukuran besar dan tebal (`81%`) di sisi kiri.
  * Estimasi sisa waktu pemakaian real-time di sisi kanan sejajar secara visual.
  * *Pill Progress Bar* melengkung penuh (*full rounded capsule*, radius 100dp) berwarna hijau menyala khas One UI (`#2ED158`) dengan track Monet container elegan.
  * Keterangan durasi pengisian penuh atau sisa waktu hingga penuh ("... lagi sampai penuh") disertai ikon info `(i)` interaktif.

* **Kartu Penggunaan Baterai Interaktif Terpadu (Data Riil)**:
  * Berada di dalam kartu khas One UI yang adaptif terhadap tema AMOLED dan Material You Monet.
  * **Dropdown Selector Interaktif**:
    * **Mode Daily Usage**: Menampilkan grafik 7 batang vertikal berbentuk kapsul biru Monet (`@color/settingslib_materialColorPrimary`), garis batas rata-rata putus-putus (`--- Avg.`), skala sumbu Y (`300` dan `0 (%)`), serta lingkaran penanda hari aktif saat ini sesuai locale bahasa. Data batang dihitung dinamis dari penggunaan foreground harian riil.
    * **Mode Since Last Charge**: Menampilkan subteks riwayat pengisian (*"Terakhir diisi hingga 100%"*, *"Berhenti mengisi daya..."*), area chart kurva Bezier dengan gradien biru Monet transparan, badge awal `⚡100`, pin bubble penanda persentase saat ini, garis proyeksi perkiraan habis, dan legenda tingkat baterai.
  * **Ringkasan Penggunaan Aplikasi Riil (No Dummy Data)**:
    * Label subheader *"Penggunaan baterai: X%"*.
    * Menampilkan daftar aplikasi riil yang terpasang dan aktif mengonsumsi daya berdasarkan kalkulasi `BatteryStatsManager` dan `UsageStatsManager`, lengkap dengan ikon asli, nama aplikasi, persentase konsumsi riil, dan garis pemisah tipis.
    * Tombol footer *"Lihat detail"* untuk membuka riwayat penggunaan baterai lanjutan (`PowerUsageAdvanced`).

* **Halaman Informasi Baterai Khusus (Battery Information)**:
  * Sub-halaman terintegrasi yang dapat diakses langsung dari tombol info `(i)` header atau menu pengaturan baterai, dengan judul action bar yang tepat (`< Informasi baterai`).
  * Menampilkan kartu terpadu lengkap dengan metrik hardware riil:
    1. **Status baterai**: Status pengisian daya (*Mengisi daya*, *Tidak mengisi daya*, *Penuh*).
    2. **Tingkat baterai**: Persentase daya saat ini.
    3. **Kesehatan baterai**: Persentase kesehatan riil baterai (*Health percentage*, misal `100% (Baik)`) yang dihitung dari rasio kapasitas maksimum terhadap desain serta sysfs SOH.
    4. **Siklus pengisian**: Jumlah siklus pengisian baterai (*Cycle count*, misal `142 kali`) dari intent hardware atau sysfs kernel.
    5. **Kapasitas baterai**: Kapasitas tipikal baterai (`5000 mAh (tipikal)`) yang dibaca secara dinamis dari hardware dan PowerProfile perangkat.
    6. **Suhu baterai**: Suhu baterai real-time dalam format Celcius (`31.5 °C`).
    7. **Voltase baterai**: Tegangan baterai real-time dalam format Volt (`4.12 V`).
  * Paragraf disclaimer resmi standar pengujian laboratorium IEC 61960 di bawah kartu.

* **Pelestarian 100% Fitur Daya**:
  * Seluruh opsi bawaan tetap dipertahankan dan dikelompokkan ke dalam kategori kartu One UI yang rapi:
    * **Manajemen Daya**: *Penghemat Baterai (Battery Saver)* dan *Baterai Adaptif (Smart Battery)*.
    * **Pengaturan Pengisian Daya**: *Kontrol Pengisian Daya Lineage Health* dan *Kecepatan Pengisian Daya (Charging Speed)*.
    * **Info & Tampilan Baterai**: *Persentase Baterai Bilah Status*, *Info Pengisian di Layar Kunci*, dan pintasan *Informasi Baterai*.

---

### 2. Pusat Kontrol & Quick Settings (One UI Control Center Style)
* **Single-Pull Quick Settings (QQS)**:
  * Tata letak terpisah yang bersih: 6 ikon toggle bulat di baris atas, slider kecerahan tanpa flicker di bagian tengah, dan 2 tombol kapsul besar (Wi-Fi & Bluetooth) di bagian bawah.
* **Expanded Control Center**:
  * Efek frosted glass card, slider volume media terintegrasi, pemilih mode suara (Dering, Getar, Hening), serta pintasan menu daya dan pengaturan.
* **Mode Edit Ubin Bebas (Free 2D Drag Edit)**:
  * Pengguna dapat menyeret ubin pengaturan cepat ke segala arah (horizontal dan vertikal) dengan animasi elevasi halus dan tombol "Selesai" ergonomis yang mudah dijangkau satu tangan.

---

### 3. Dynamic Island Interaktif (Punch-Hole Camera)
* **Indikator Pengisian Daya**: Menampilkan animasi petir hijau dan persentase baterai real-time saat ponsel dicolokkan ke charger.
* **Indikator Senter**: Kapsul minimalis dengan ikon senter modern dan status "On". Ketuk sekali untuk langsung mematikan senter.
* **Indikator Perekaman Layar**: Kapsul ringkas dengan titik merah berdenyut (*pulsing dot*) dan penghitung waktu digital.
* **Dual-Island (Meta-Ball Fluid Physics)**: Saat perekaman layar dan senter aktif bersamaan, gelembung sekunder merapat di sisi kiri kapsul utama dengan animasi cairan yang mulus.

---

### 4. Peningkatan Dashboard Menu Pengaturan
* **Homepage Quick Dashboard**: Kartu pemantau real-time untuk Baterai, Penyimpanan, RAM, Tampilan, dan Suara di halaman utama Settings.
* **Ikon Notifikasi Berwarna**: Opsi baru di Pengaturan Notifikasi untuk menampilkan logo aplikasi berwarna asli pada bilah status dan kartu notifikasi.
* **Halaman About Phone Palapa OS**: Banner visual Shinkai Project dengan meteran spesifikasi hardware (SoC, layar, kamera) dan kredit kontributor.

---

### 5. Optimasi Performa & Kernel
* **Ekspansi RAM Dinamis (zRAM)**: Konfigurasi zRAM terintegrasi langsung dengan kernel Linux (pilihan: Mati, +2 GB, +4 GB, +8 GB, +16 GB).
* **Tweaks Navigasi Gestur**: Tombol toggle untuk menyembunyikan bilah navigasi gestur di bagian bawah serta menghilangkan ruang kosong di bawah keyboard IME.
* **Enhanced Power Menu**: Menu daya bergaya Material 3 Expressive dengan kontrol mulai ulang lanjutan (*Advanced Reboot* ke Recovery/Bootloader).

---

### 6. Proteksi Keamanan, Key Attestation & Status Bootloader Terkunci
* **Hardware-Backed Key Attestation Spoofing**:
  * Integrasi penuh sertifikat Keybox resmi Google Root CA (`Key Attestation CA1`) langsung ke dalam framework sistem (`custom_config.xml` & `KeyProviderManager`).
  * Dukungan penuh keybox ECDSA-only RKP modern dengan pelestarian urutan rantai sertifikat numerik (`CERT_1 .. CERT_4`).
  * Aplikasi Key Attestation (Nikolay Elenkov / Key Attestation Demo) mendeteksi perangkat dengan status **Device locked: true** dan **Verified Boot State: Verified (Green)**.
* **Integrasi Play Integrity (GMS DroidGuard Fallback) & Sertifikasi Play Store**:
  * Menanamkan fingerprint tersertifikasi resmi dari Google Pixel Fold (Android 17, `felix_beta`) ke dalam konfigurasi ROM (`config_verifiedDeviceSpecs`) lengkap dengan `TYPE:user`, `TAGS:release-keys`, dan `INCREMENTAL`.
  * Memperbaiki `PropImitationHooks` untuk menyamarkan seluruh proses `com.google.android.gms` (termasuk CheckinService), sehingga status di Google Play Store dinyatakan resmi sebagai **"Perangkat telah tersertifikasi"**.
  * Mengutamakan pemblokiran key attestation untuk pemanggil DroidGuard (`isCallerPlayIntegrity()`) sebelum pengecekan Keybox, memastikan Play Integrity API otomatis melakukan *fallback* ke evaluasi software dan berhasil meraih **`MEETS_DEVICE_INTEGRITY`**.
* **Ketahanan Dinamis Anti-Revoked & Integrasi UI Spoof Option**:
  * **Pembaruan Otomatis PIF Langsung dari Pengaturan (`Auto PIF Generator`)**: Menambahkan opsi pembaruan profil PIF otomatis langsung dari menu *Spoof Option* Pengaturan. Pengguna dapat mengunduh fingerprint Pixel Canary/Beta resmi yang aktif dari Google Flash Station atau Developer Builds tanpa akses root atau terminal.
  * **Pembersih Data & Cache Play Store/GMS Sekali Sentuh**: Menambahkan opsi untuk membersihkan data dan cache Google Play Store serta Google Play Services langsung dari UI Pengaturan menggunakan `clearApplicationUserData` berbasis izin sistem, sehingga status sertifikasi perangkat langsung terdaftar ulang seketika.
  * Mendukung pembacaan konfigurasi PIF berlapis: `Settings.Secure.PIF_DATA`, file lokal `/data/system/pif.json` atau `/data/adb/pif.json`, OTA `FETCHED_PIF`, dan ROM baked-in.
  * Mendukung pembacaan Keybox XML berlapis: `Settings.Secure.KEYBOX_DATA`, file lokal `/data/system/keybox.xml` atau `/data/adb/tricky_store/keybox.xml`, dan overlay ROM.
  * Menyertakan utilitas CLI `/system/bin/autopif` lengkap dengan aksi `fetch`, `clear`, `reset`, dan `status`.
* **Perbaikan Alur Login Akun Google & Pembersihan GSF**:
  * Mengisolasi `sIsGms` hanya untuk proses background DroidGuard (`com.google.android.gms.unstable`), mencegah proses antarmuka login akun (`com.google.android.gms.ui` / `MinuteMaidActivity`) membunuh dirinya sendiri saat alur login akun berlangsung.
  * Menghilangkan pesan kesalahan *"Perangkat perlu direset ke setelan pabrik"* dengan menyertakan paket *Google Services Framework* (`com.google.android.gsf`) ke dalam rutinitas pembersihan data Play Store & GMS, sehingga GSF ID lama yang berstatus uncertified langsung diperbarui tanpa reset perangkat.
* **Kernel Command Line Filter (`/proc/cmdline`)**:
  * Filter cerdas pada kernel (`cmdline.c`) yang secara otomatis mengganti flag `androidboot.verifiedbootstate=orange` menjadi `green`, `androidboot.flash.locked=0` menjadi `1`, dan `androidboot.vbmeta.device_state=unlocked` menjadi `locked` untuk pembacaan langsung di level sistem operasi.

* **Penyempurnaan Key Attestation Hardware & Perbaikan Mode "Attest Key"**:
  * Menghapus pembatasan `if (attestationKey == null)` pada `KeyStoreSecurityLevel.java`, sehingga saat pengujian "Attest key" dijalankan pada aplikasi Key Attestation, permintaan tetap ditangani oleh rantai `KeyboxImitationHooks`. Hal ini memperbaiki bug di mana status bootloader sebelumnya terbaca *Bootloader is Unlocked* pada mode tersebut.
* **Jaminan Play Store Tersertifikasi & Framework Signature Spoofing (DEVICE_INTEGRITY)**:
  * Mengisolasi penggunaan Keybox publik agar tidak pernah diberikan kepada proses background DroidGuard (`com.google.android.gms.unstable`). Hal ini mencegah Google mendeteksi serial keybox yang telah di-revoke dan memaksa DroidGuard melakukan *fallback* mulus ke evaluasi software.
  * Mengintegrasikan penyamaran signature paket kerangka kerja (`"android"`) khusus pada proses `com.google.android.gms.unstable` dengan sertifikat rilis resmi Google Pixel (`GOOGLE_RELEASE_SIGNATURE`), menjamin Google Play Integrity meloloskan **`MEETS_DEVICE_INTEGRITY`** dan status Google Play Store dinyatakan resmi sebagai **"Perangkat telah tersertifikasi"**.
* **Penetapan Paten Release-Keys pada Seluruh Partisi Build**:
  * Menetapkan `BUILD_KEYS := release-keys` dan `BUILD_VERSION_TAGS := release-keys` secara menyeluruh pada `vendor/custom/config/common.mk`, `shinkai_marble.mk`, dan `build/make/core/config.mk`.
  * Memastikan seluruh file `build.prop` pada partisi `system`, `vendor`, `product`, `system_ext`, dan `odm` secara native meng-generate `ro.*.build.tags=release-keys` dan `BuildTag=release-keys` saat proses build berlangsung, tanpa bergantung hanya pada runtime spoofing.

---

### 5. Integrasi Terpadu Play Integrity, TrickyStore & TEESimulator di Menu Pengaturan
Mentransformasikan menu **Pengaturan -> Sistem -> Spoof Option** menjadi pusat kendali attestation dan Play Integrity terlengkap setara TrickyStore WebUI dan TEESimulator WebUI langsung di dalam ROM tanpa memerlukan modul root Magisk atau KernelSU:

* **Header Card Material Modern dengan Diagnostik Real-Time**:
  * Kartu hero elegan dengan ikon perisai keamanan, status badge, ringkasan fingerprint aktif, dan 3 chip status langsung:
    * **Play Integrity**: `MEETS_DEVICE` (Hijau)
    * **Status Keybox**: `ACTIVE` (Sian)
    * **Status Bootloader**: `LOCKED` (Hijau)
* **Katalog & Pemilih Profil PIF Tersertifikasi (Suite PIF)**:
  * Pilihan profil bawaan perangkat resmi siap pakai dengan 1 ketukan:
    * Google Pixel 8 Pro (`husky`), Pixel 7 Pro (`cheetah`), Pixel 6a (`bluejay`)
    * Google Pixel 5 (`redfin`) dengan Initial API 30 (melewati kewajiban RKP Google)
    * POCO F5 5G (`marble` HyperOS Global Stock)
    * Xiaomi 13 Pro (`nuwa` Global Stock)
    * Nothing Phone (2) (`Pong`)
    * ASUS ROG Phone 6 (`AI2201` Initial API 31)
  * Penerapan langsung ke `Settings.Secure.PIF_DATA`, `/data/system/pif.json`, dan auto-restart layanan Google.
  * Tetap mempertahankan fitur pembaruan online live build Google Flash Station dan impor manual `pif.json`.
* **Suite TrickyStore (Keybox & Root of Trust)**:
  * Inspeksi hardware keybox aktif (kunci EC / RSA dan status sertifikat).
  * **Custom Verified Boot Hash (`verified_boot_hash`)**: Pengguna dapat memasukkan 64 karakter SHA-256 boot hash (TrickyStore `boot_hash`) yang langsung disuntikkan ke ekstensi ASN.1 `KeyDescription` root of trust.
  * **Filter Target Aplikasi**: Mode target fleksibel (Direkomendasikan vs Semua Aplikasi) serta dukungan file `/data/system/target.txt`.
* **Suite TEESimulator (Perilaku Attestation & Patch Level)**:
  * **Toggle Paksa RKP Attestation (`tee_rkp_only`)**: Mengaktifkan atau menonaktifkan kewajiban RKP sesuai rekomendasi komunitas untuk meloloskan Strong integrity pada profil Pixel 6-9 di bootloader terbuka.
  * **Sinkronisasi Otomatis Security Patch Level (`tee_auto_spl`)**: Otomatis menyelaraskan level patch boot dan vendor simulasi TEE dengan tanggal profil sertifikasi PIF aktif.
  * **Tingkat Keamanan Simulasi**: Pilihan antara TEE Keymaster dan StrongBox Keymaster.
* **Pemeliharaan & Pengujian Langsung**:
  * Tombol 1 ketukan pembersih data & cache Google Play Store, GMS, dan GSF dengan auto-restart.
  * Peluncur langsung (*intent launcher*) ke aplikasi Key Attestation.

### 5. Jaminan Sertifikasi Play Protect & Paritas Suite Attestation (TrickyStore & TEESimulator)
* **Jaminan Sertifikasi Play Protect Bawaan ROM**:
  * Menambahkan pemalsuan properti untuk `com.google.android.gsf` (Google Services Framework) pada `PropImitationHooks.java`, memastikan checkin perangkat ke server Google mendaftarkan profil build tersertifikasi asli, bukan nilai mentah `userdebug` ROM.
  * Mengonfigurasi spesifikasi perangkat terverifikasi default ke **Google Pixel 5 (redfin)** dengan `DEVICE_INITIAL_SDK_INT: 30` (standar emas Pre-RKP). Karena Pixel 5 diproduksi sebelum adanya Remote Key Provisioning (RKP), server Google secara teknis tidak dapat mewajibkan hardware attestation, menjamin tercapainya `MEETS_DEVICE_INTEGRITY` dan Perangkat Tersertifikasi Play Protect pada software attestation murni.
* **Redesain Kartu Header - Penghapusan Chip Metrik yang Menyesatkan**:
  * Menghapus baris chip metrik 3 kolom (*"INTEGRITY STRONG | HARDWARE TEE | BOOTLOADER LOCKED"*) dari `spoof_option_header_card.xml` untuk mencegah ilusi status yang salah saat keybox publik dicabut (revoked) oleh Google.
  * Digantikan dengan kontainer status yang transparan dan elegan menampilkan **Mode Mesin Attestation** aktif, **Profil Perangkat Tersertifikasi**, dan string fingerprint monospace, disertai badge dinamis.
* **Paritas Lengkap WebUI TrickyStore & Pengelola Aplikasi Target Per-App**:
  * **100% Implementasi Murni Framework-Base (FWB)**: Bekerja murni di dalam `frameworks/base` dan setelan sistem Android tanpa memerlukan akses root, modul Magisk/KernelSU, maupun injeksi Zygisk, sehingga kebal dari pemindaian modul DroidGuard.
  * **Pengelola Aplikasi Target Per-App (Paritas `target.txt`)**: Memperkenalkan dialog pemilih aplikasi Material interaktif di Pengaturan (`Kelola Aplikasi Target (Per-App)`). Pengguna dapat memilih secara spesifik aplikasi terpasang (seperti aplikasi bank atau pemeriksa integritas) yang menerima attestation Keybox dan pemalsuan properti bersertifikasi, sementara aplikasi lain (kamera, game, medsos) tetap berjalan alami dengan identitas asli POCO F5.
  * **Mode Target Aplikasi**: Beralih mudah antara *Rekomendasi (GMS, Play Store & Key Attestation)*, *Aplikasi Terpilih (Per-App List)*, dan *Semua Aplikasi (Global)*.
  * **Pemilih Mode Mesin Attestation**: Beralih mudah antara *Play Integrity Fix (Mode Software - Dijamin Bersertifikasi)*, *TrickyStore (Hardware TEE Keybox)*, dan *Mode Otomatis*.
  * **Hapus Keybox Aktif**: Aksi satu ketukan untuk membersihkan kredensial keybox kedaluwarsa/tercabut dari pengaturan sistem dan direktori data, seketika memulihkan software attestation bersih tanpa repot edit file manual.
  * **Generator Verified Boot Hash Acak**: Seketika membuat dan menerapkan boot hash 32-byte (64 karakter heksadesimal) kriptografis acak untuk mensimulasikan status bootloader terverifikasi.
* **Paritas Lengkap WebUI TEESimulator**:
  * **Paksa Attestation RKP (`tee_rkp_only`)**: Mengaktifkan atau menonaktifkan aturan RKP untuk menghindari pemblokiran attestation perangkat keras Google DroidGuard.
  * **Sinkronisasi Otomatis Security Patch Level (`tee_auto_spl`)**: Otomatis menyelaraskan level patch boot dan vendor simulasi TEE dengan tanggal profil sertifikasi PIF aktif.
  * **Tingkat Keamanan Simulasi**: Beralih antara TEE (Trusted Execution Environment) dan StrongBox Keymaster.
* **1-Ketukan Perbaikan Sertifikasi Play Store & Diagnostik**:
  * **Perbaiki Sertifikasi Play Store (Direkomendasikan)**: Alur kerja pemeliharaan otomatis yang mengonfigurasi profil Pixel 5 Pre-RKP, mengunci mode ke Software PIF, membersihkan data GSF/GMS/Play Store, dan memicu checkin pendaftaran perangkat baru.
  * **Periksa Sertifikasi Play Protect**: Peluncur langsung menuju Pengaturan Play Store -> Tentang untuk memeriksa status sertifikasi perangkat secara riil.
  * **Uji Key Attestation**: Pintasan langsung untuk membuka aplikasi Key Attestation guna memeriksa rantai sertifikat kriptografi.

---

## Detail Teknis & Perubahan Kode

| Komponen | File Terkait | Deskripsi Perubahan |
|---|---|---|
| **Header UI Baterai** | `OneUIBatteryHeaderPreference.kt`<br>`oneui_battery_header_card.xml` | Menampilkan persentase besar, estimasi waktu tersisa, dan kapsul progress bar One UI |
| **Grafik Interaktif** | `OneUIBatteryChartView.kt` | Custom canvas view untuk grafik batang 7-hari dan kurva area gradien dengan adaptasi warna Monet |
| **Kartu Penggunaan** | `OneUIBatteryUsageCardPreference.kt`<br>`OneUIBatteryUsageCardController.kt`<br>`oneui_battery_usage_card.xml` | Kartu utama penggabung dropdown filter, grafik, dan daftar aplikasi riil dari `BatteryStatsManager` |
| **Info Baterai** | `OneUIBatteryInfoFragment.kt`<br>`oneui_battery_info_fragment.xml`<br>`SettingsGateway.java` | Sub-halaman info kesehatan baterai (%), siklus, suhu, voltase, dan kapasitas IEC 61960 |
| **Pengaturan Terpadu** | `power_usage_summary.xml`<br>`PowerUsageSummary.java`<br>`PowerUsageSummaryScreen.kt` | Pengelompokan seluruh preferensi daya ke dalam kartu One UI dan pembersihan total header Pixel |
| **Tema & Bahasa** | `colors.xml`, `values-night/colors.xml`<br>`shinkai_strings.xml`, `values-in/` | Palet warna dinamis Material You Monet (`surfaceContainer`, `primary`) dan lokalisasi lengkap ID/EN |
| **Key Attestation & Bootloader Lock** | `custom_config.xml`<br>`KeyProviderManager.java`<br>`KeyboxDataPreference.java`<br>`KeyStoreSecurityLevel.java`<br>`cmdline.c` | Penanaman rantai Google Root Keybox ECDSA, dukungan RKP, integrasi PIF Pixel Fold, pencegahan bypass "Attest key", dan filter `/proc/cmdline` |
| **Play Integrity & Play Store Certification** | `PropImitationHooks.java`<br>`KeyboxImitationHooks.java`<br>`KeyProviderManager.java`<br>`SystemProperties.java`<br>`custom_config.xml`<br>`common.mk`, `autopif` | Pembukaan blokir Keybox hardware untuk GMS, virtualisasi properti di `SystemProperties`, bypass blokir DroidGuard dengan Keybox, penyamaran signature Pixel release, dan CLI Auto-PIF |
| **Pusat Kendali Spoof & Suite** | `AutoPifHelper.java`<br>`SpoofOptionFragment.java`<br>`SpoofOptionController.java`<br>`spoof_option_fragment.xml`<br>`spoof_option_header_card.xml`<br>`Settings.java`<br>`KeyboxChainGenerator.java`<br>`SecureSettingsValidators.java` | Dashboard terpadu Play Integrity, TrickyStore & TEESimulator dengan kartu header Material yang disempurnakan, katalog preset PIF, indikator chip dinamis, dan boot hash spoofing |

---
*Palapa OS - Edisi Triwulan | Shinkai Project (2026)*
