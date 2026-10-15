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
Halaman Pengaturan Baterai (`Settings -> Battery`) telah dirombak secara menyeluruh mengadopsi bahasa desain Samsung One UI 9 dengan tetap mempertahankan seluruh fitur dan opsi sistem yang ada:

* **Hero Overview Header**:
  * Angka persentase baterai berukuran besar dan tebal (`81%`) di sisi kiri.
  * Estimasi sisa waktu pemakaian real-time (`8 h 35 m left`) di sisi kanan sejajar secara visual.
  * *Pill Progress Bar* melengkung penuh (*full rounded capsule*, radius 100dp) berwarna hijau menyala khas One UI (`#2ED158`) dengan track abu-abu gelap elegan.
  * Keterangan durasi pengisian penuh (*"A full charge will last about..."*) disertai ikon info `(i)` interaktif.

* **Kartu Penggunaan Baterai Interaktif Terpadu**:
  * Berada di dalam kartu gelap khas One UI (corner radius 26dp) yang adaptif terhadap tema AMOLED dan Material You Monet.
  * **Dropdown Selector Interaktif**:
    * **Mode Daily Usage**: Menampilkan grafik 7 batang vertikal berbentuk kapsul biru One UI (`#2979FF`), garis batas rata-rata putus-putus (`--- 195 Avg.`), skala sumbu Y (`300` dan `0 (%)`), serta lingkaran penanda hari aktif saat ini (`M`).
    * **Mode Since Last Charge**: Menampilkan subteks riwayat pengisian (*"Last charged to 100%"*, *"Stopped charging..."*), area chart kurva landai dengan gradien biru transparan, badge awal `⚡100`, pin bubble penanda persentase saat ini `81%`, garis proyeksi perkiraan habis dengan mode hemat daya, dan legenda tingkat baterai.
  * **Ringkasan Penggunaan Aplikasi**:
    * Label subheader *"Battery usage: X%"*.
    * Daftar mini 3 aplikasi teratas pengguna baterai terbanyak lengkap dengan ikon squircle aplikasi, nama aplikasi, persentase penggunaan riil, dan garis pemisah tipis.
    * Tombol footer *"View details"* untuk membuka riwayat penggunaan baterai lanjutan (`PowerUsageAdvanced`).

* **Halaman Informasi Baterai Khusus (Battery Information)**:
  * Sub-halaman baru yang dapat diakses langsung dari tombol info `(i)` header atau menu pengaturan baterai.
  * Menampilkan kartu terpadu dengan 3 baris data:
    1. **Battery status**: Status pengisian daya (*Not charging*, *Charging*, *Full*).
    2. **Battery level**: Persentase daya saat ini.
    3. **Battery capacity**: Kapasitas tipikal baterai (`4500 mAh (typical)`) yang dibaca secara dinamis dari hardware dan PowerProfile perangkat.
  * Paragraf disclaimer resmi standar pengujian laboratorium IEC 61960 di bawah kartu.

* **Pelestarian 100% Fitur Daya**:
  * Seluruh opsi bawaan tetap dipertahankan dan ditata dalam kartu rapi: *Penghemat Baterai (Battery Saver)*, *Baterai Adaptif (Smart Battery)*, *Kontrol Pengisian Daya Lineage Health*, *Kecepatan Pengisian Daya (Charging Speed)*, *Persentase Bilah Status*, dan *Info Pengisian di Layar Kunci*.

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

## Detail Teknis & Perubahan Kode

| Komponen | File Terkait | Deskripsi Perubahan |
|---|---|---|
| **Header UI Baterai** | `OneUIBatteryHeaderPreference.kt`<br>`oneui_battery_header_card.xml` | Menampilkan persentase besar, estimasi waktu tersisa, dan kapsul progress bar One UI |
| **Grafik Interaktif** | `OneUIBatteryChartView.kt` | Custom canvas view untuk grafik batang 7-hari dan kurva area gradien |
| **Kartu Penggunaan** | `OneUIBatteryUsageCardPreference.kt`<br>`OneUIBatteryUsageCardController.kt`<br>`oneui_battery_usage_card.xml` | Kartu utama penggabung dropdown filter, grafik, dan daftar 3 aplikasi teratas |
| **Info Baterai** | `OneUIBatteryInfoFragment.kt`<br>`oneui_battery_info_fragment.xml`<br>`SettingsGateway.java` | Sub-halaman info kapasitas baterai IEC 61960 dan status pengisian |
| **Pengaturan Terpadu** | `power_usage_summary.xml` | Pengelompokan seluruh preferensi daya lama ke dalam tata letak kartu terstruktur |
| **Tema & Bahasa** | `colors.xml`, `values-night/colors.xml`<br>`shinkai_strings.xml`, `values-in/` | Palet warna tema gelap AMOLED / Monet dan lokalisasi lengkap ID/EN |

---
*Palapa OS - Edisi Triwulan | Shinkai Project (2026)*
