# Log Perubahan Pembaruan Sistem: WowoOS Shinkai Project (POCO F5 / marble)

Target Perangkat: POCO F5 (marble)  
Versi Android: Android 17 (CP2A)  
Ukuran Paket: ~2.9 GB  
Status Kompilasi: Berhasil Disiapkan (Exit Code 0)  

Navigasi Dokumen:
- [Versi Bahasa Inggris (changelogs-en.md)](changelogs-en.md)
- [Versi Bahasa Indonesia (changelogs-id.md)](changelogs-id.md)

---

## Ringkasan Pembaruan (Versi Simpel & Ringkas)

Pembaruan ini membawa perombakan menyeluruh pada antarmuka pengguna (UI Redesign) bergaya One UI 9, penambahan fitur interaktif Dynamic Island, penyempurnaan menu Pengaturan, serta peningkatan performa dan audio:

1. Dynamic Island Interaktif (Punch-Hole POCO F5)
   - Indikator Pengisian Daya: Menampilkan animasi petir hijau dan persentase baterai selama 7.8 detik saat ponsel dicolokkan ke pengisi daya.
   - Indikator Senter: Kapsul senter ringkas minimalis dengan ikon kustom modern dan status "On". Ketuk sekali untuk langsung mematikan senter.
   - Indikator Perekam Layar (Screen Recording): Kapsul ringkas serasi dengan titik merah berdenyut dan penghitung waktu digital.
   - Dual-Island (Meta-Ball): Jika perekam layar dan senter menyala bersamaan, gelembung bundar (meta-ball) tampil di sebelah KIRI kapsul utama dengan transisi fisika fluida saat diusap (swap).
   - Kartu Kontrol Mengambang (Expanded Island): Desain lebar penuh bergaya OriginOS yang mepet ke tepi layar, dilengkapi lencana kamera rekam, tombol gelombang audio, mikrofon, pena, tombol pause putih, dan tombol stop berlingkar putih.

2. Tampilan Baru Quick Settings (One UI 9 Style)
   - Tampilan Tarik Satu Kali (QQS): Susunan rapi berisi 6 tombol lingkaran di atas, slider kecerahan tanpa flicker di tengah, dan 2 tombol pill besar (Wi-Fi dan Bluetooth) di bawah.
   - Tampilan Tarik Penuh (Expanded QS): Dilengkapi kartu kaca buram untuk tombol cepat, slider volume media terintegrasi, pemilih mode suara (Dering, Getar, Hening), serta pintasan daya dan pengaturan.
   - Mode Edit Bebas 2D: Tahan dan seret tombol ke mana saja (horizontal maupun vertikal) dengan animasi timbul dan tombol Selesai mengambang yang mudah dijangkau satu tangan.
   - Pemutar Musik Terintegrasi: Menampilkan kontrol musik langsung di panel tanpa merusak tata letak.

3. Penyempurnaan Menu Pengaturan (Settings)
   - Dashboard Halaman Utama: Kartu status cepat untuk memantau Baterai, Penyimpanan, RAM, Tampilan, dan Suara secara langsung.
   - Tombol Notifikasi Baru: Panel Aksesibilitas pada dashboard kini digantikan tombol Notifikasi untuk akses cepat. Menu Aksesibilitas tetap tersedia utuh di daftar pengaturan utama.
   - Colored Notification Icons: Opsi baru pada Pengaturan Notifikasi untuk menampilkan ikon asli aplikasi yang berwarna penuh di status bar dan panel notifikasi, bukan sekadar siluet putih.
   - Halaman Tentang Ponsel (About Phone): Banner visual Shinkai Project dengan kartu spesifikasi perangkat secara langsung (prosesor, layar, kamera) dan kredit kontributor.

4. Peningkatan Sistem & Performa
   - Dynamic RAM Expansion (zRAM): Pengaturan zRAM nyata yang terhubung langsung ke kernel Linux (Pilihan: Nonaktif, 2 GB, 4 GB, 8 GB, 16 GB), terbukti aktif di aplikasi monitor sistem.
   - Hardware Dolby Atmos Asli: Dukungan DSP perangkat keras bawaan dengan aplikasi kontrol MarbleDolby (AutoEQ dan 10-band equalizer) aktif tanpa memerlukan root atau modul KernelSU.
   - Navigasi Bersih: Opsi untuk menyembunyikan bilah navigasi bawah (navbar pill) dan menghilangkan ruang kosong di bawah keyboard.

---

## Catatan Rinci & Detail Teknis (Engineering Sections)

## Bagian I: Notula Rapat & Ringkasan Eksekutif

### 1. Masalah Awal & Kebutuhan yang Dibahas
Selama sesi pengujian dan peninjauan, beberapa ketidaksesuaian antarmuka inti dan regresi subsistem telah diidentifikasi dan diperbaiki:

1. **Ketidakkonsistenan Perilaku & Bug Visual Quick Settings (QS)**:
   - Kartu pemutar media tidak terintegrasi saat penarikan panel notifikasi sebagian.
   - Tombol pintasan panel (seperti Senter) mengalami kegagalan sinkronisasi status dua arah (berhasil menyala, namun ketukan kedua gagal mematikan).
   - Slider kecerahan masih mempertahankan tata letak bawaan Pixel, dan menggeser slider menyebabkan seluruh antarmuka panel QS menghilang sementara hingga sentuhan dilepas. Selain itu, melepas slider kecerahan menimbulkan kedipan/glitch visual sesaat akibat race condition nilai asinkron.
   - Tombol menu daya tidak berfungsi, dan mengetuk pintasan Pengaturan membuka Pengaturan tanpa menutup panel notifikasi secara otomatis.
   - Indikator header (jam, tanggal, dan persentase baterai) mengalami duplikasi tampilan di seluruh lapisan status bar.

2. **Kebutuhan Bahasa Visual & Kegunaan One UI 9**:
   - Pemisahan tegas bentuk tombol: Tombol berbentuk kapsul/pill (2x1) harus berada di luar kontainer kartu utama.
   - Tarikan tunggal Quick Quick Settings (QQS) harus menampilkan tombol pill teratas yang dikonfigurasi (2x1 berdampingan), bukan deretan tombol lingkaran kecil yang tidak serasi.
   - Transisi halus saat tarikan ganda (dari QQS ke QS penuh) tanpa meninggalkan artefak kartu kaca buram yang tertahan di atas jam header.
   - Kontainer kaca buram khusus harus memuat tombol lingkaran (1x1) yang disusun dalam kisi 4 kolom (menampung hingga 8 tombol dalam mode ringkas).
   - Garis indikator interaktif di tengah bawah kartu kisi untuk memperluas dan menampilkan baris tambahan ketika lebih dari 8 tombol ditambahkan.
   - Penyediaan slider volume media berdampingan dengan tombol mode suara lingkaran (Dering, Getar, Hening) tepat di bawah kontrol kecerahan.
   - Penerapan Mode Edit intuitif yang memungkinkan pengguna mengubah ukuran tombol antara format Pill dan Lingkaran, menyeret tombol bebas ke posisi mana pun dengan gestur tekan-lama, serta menambah atau menghapus item tanpa tombol panah yang membingungkan.

3. **Kerusakan Fitur Ekspansi RAM (zRAM)**:
   - Mengubah nilai Ekspansi RAM di pengaturan perangkat (Mati/0GB, 2GB, 8GB, 16GB) tidak berpengaruh sama sekali pada kernel Linux. zRAM kernel tetap terkunci permanen pada 4GB (terlapor 4.29 GB di DevCheck dan Tentang Ponsel), mengabaikan preferensi pengguna baik saat runtime maupun setelah boot ulang.

---

### 2. Keputusan Arsitektur yang Disepakati
1. **Tata Letak Hibrida One UI 9 & Transisi Halus**:
   - QQS tarikan tunggal menampilkan 2 tombol pill teratas (Internet dan Bluetooth secara default), serasi dengan bagian atas QS penuh.
   - Menghapus kotak latar belakang statis dari QQS agar transisi tarikan ganda berjalan mulus tanpa gangguan visual.
   - Membagi seluruh tile dinamis ke dalam beberapa zona tata letak: Zona Pill Atas, Zona Kartu Kisi Kaca Buram (tombol 1x1 dengan strip ekspansi), Zona Slider & Kontrol Audio, dan Zona Pill Bawah.
   - Menyediakan mesin Mode Edit langsung di tempat dengan lencana visual untuk beralih format ukuran dan dukungan gestur tekan-lama seret-untuk-mengatur-ulang.
   - Mengintegrasikan stream AudioManager bawaan Android langsung ke dalam pohon Jetpack Compose Quick Settings.
   - Menghilangkan tampilan jam dan tanggal duplikat di semua set constraint MotionLayout (QQS, QS, dan LargeScreen).

2. **Penjembatanan SELinux Sesuai Standar Treble & Manajemen Swap Persisten**:
   - Menjembatani properti sistem Android ke penyimpanan vendor persisten (`persist.vendor.ram_expansion`) pada tingkat root init, memastikan nilai bertahan setelah boot ulang dan langsung dapat dibaca oleh layanan vendor.
   - Memberikan izin akses SELinux yang diperlukan (`sysfs_zram`, `swap_block_device`, dan kapabilitas `sys_admin`) ke konteks eksekusi vendor.
   - Menjalankan siklus swap atomik (swapoff, reset, konfigurasi disksize, mkswap, dan swapon) untuk mencegah penolakan penulisan kernel, dengan penonaktifan total saat diatur ke 0GB (Off).

---

### 3. Hasil Implementasi
1. Panel Quick Settings kini sepenuhnya mengadopsi estetika visual One UI 9:
   - Tarikan tunggal (QQS) menampilkan:
     * Jam dan Tanggal Header dengan pintasan tindakan (Edit, Daya, Pengaturan) yang sejajar rapi dengan QS.
     * Baris 1: 1x6 tombol pintasan cepat lingkaran (format 1x1).
     * Baris 2: Slider kecerahan kapsul kustom One UI (serasi dengan slider QS, menghilangkan slider bawaan Pixel).
     * Baris 3: 2x1 tombol pill besar (Wi-Fi dan Bluetooth berdampingan).
   - Tarikan ganda berekspansi mulus ke QS penuh tanpa kartu tertinggal atau kedipan visual.
   - Slider kecerahan merespons tanpa hambatan maupun kedipan saat dilepas.
   - Mulai ulang SystemUI instan saat beralih gaya QS (Stock Pixel vs One UI) di Pengaturan.
2. Mode Edit memungkinkan pergerakan seret-dan-lepas 2D yang lancar ke mana saja (bahkan memutar melingkar dan menyeret ke tepi atas) dengan efek elevasi, tanpa bug terlepas, serta pengalihan ukuran instan antara Pill dan 1x1.
3. Fitur Ekspansi RAM (zRAM) kini menerapkan metode FurLC ZRAM:
   - Menetapkan ukuran byte presisi (`8589934592` untuk 8GB, `2147483648` untuk 2GB, `17179869184` untuk 16GB, 0 untuk Mati).
   - Prosedur `swapoff` aman dengan loop percobaan ulang 5 kali pada `/sys/block/zram0/reset` untuk meniadakan penolakan `-EBUSY` kernel.
   - Menetapkan 8 multi-streams dan algoritma kompresi `lz4`, dilanjutkan dengan `mkswap`, `swapon`, dan swappiness bernilai 100.
   - Langsung terverifikasi di `/proc/swaps`, DevCheck, dan Kernel Manager.
4. Halaman Tentang Ponsel / Versi Firmware:
   - Baris khusus untuk **Versi Shinkai** (`ro.shinkai.version`).
   - Baris khusus untuk **Versi Wowo OS** dengan nilai **Embegeh-1T**.
5. Ekspansi RAM bersifat sepenuhnya dinamis: memilih +8 GB (atau ukuran lainnya) langsung memperbarui `/sys/block/zram0/disksize` pada tingkat kernel, tercermin akurat di DevCheck dan Tentang Ponsel (misal: 8 GB + 8 GB), dan tetap bertahan setelah boot ulang.
6. Kompilasi sistem penuh selesai bersih tanpa peringatan kompilator, kesalahan sintaks, maupun pelanggaran SELinux.

---

## Bagian II: Log Perubahan Teknis & Arsitektur (Versi Pengembang)

### 1. Arsitektur SystemUI & Jetpack Compose

#### Komponen yang Terpengaruh:
- `OneUIQuickSettingsLayout.kt`
- `QSFragmentCompose.kt`
- `ShadeHeaderController.kt`

#### Detail Implementasi Teknis:
1. **Pemisahan Tile Dinamis & Persistensi Status**:
   - Integrasi `rememberPillTileSpecs()` dan `setTilePillMode()` didukung oleh `Settings.System.putString(..., "qs_oneui_pill_tiles", ...)`.
   - Aliran tile yang masuk dipartisi secara dinamis menjadi:
     * `pillTiles`: Spesifikasi tile yang ditetapkan sebagai tombol pill 2x1 (`it.spec.spec`).
     * `gridTiles`: Spesifikasi tile yang ditetapkan sebagai tombol lingkaran 1x1.
2. **Quick Quick Settings Kompak (QQS - Tarikan Tunggal)**:
   - Menggantikan kartu lingkaran 6 tile lama dengan `OneUIBigPillTile` yang menampilkan 2 tile pill teratas berdampingan.
   - Menghapus pembungkus latar belakang statis untuk mencegah pemotongan visual selama transisi ke QS yang diperluas.
3. **Hierarki Tata Letak Hibrida (Full QS - Tarikan Ganda)**:
   - **Bagian Pill Atas**: Menampung dua tombol pintasan pill pertama di luar kontainer kartu menggunakan `Row(horizontalArrangement = Arrangement.spacedBy(10.dp))`.
   - **Kartu Kisi Kaca Buram (`OneUITileGridCard`)**:
     * Menampilkan `gridTiles` dalam `GridCells.Fixed(4)` dengan latar belakang melingkar (`CircleShape`).
     * Menerapkan pemotongan status diciutkan: menampilkan maksimal 8 tile saat diciutkan (`!isExpanded && gridTiles.size > 8`).
     * Memuat strip indikator horizontal interaktif (`Box(40.dp x 4.dp)` dengan `RoundedCornerShape(2.dp)`) yang memicu `onClick = { isExpanded = !isExpanded }`, disertai dengan animasi `animateContentSize()`.
   - **Slider Audio & Kecerahan**:
     * Menyelesaikan glitch pelepasan pada `OneUIBrightnessSlider` dengan mengunci `sliderProgress` pada `onDragEnd` dan menghindari pengembalian sementara ke nilai framework asinkron sebelumnya.
     * Mengintegrasikan `OneUIVolumeSlider` yang terikat ke `AudioManager.STREAM_MUSIC`, dengan pelacakan tingkat volume dan pengalihan mode senyap.
     * Mengintegrasikan `OneUISoundModeButton` yang berganti secara berurutan: `RINGER_MODE_NORMAL` -> `RINGER_MODE_VIBRATE` -> `RINGER_MODE_SILENT`.
   - **Bagian Pill Bawah**: Menampilkan sisa tombol pill yang ditempatkan di bawah Kartu Pemutar Media.
4. **Mode Edit Seret-untuk-Mengatur-Ulang Intuitif (`OneUIEditMode`)**:
   - Menyematkan `detectDragGesturesAfterLongPress` ke item tile aktif dengan perhitungan perpindahan indeks dinamis (ambang batas `dx`, `dy`).
   - Menambahkan efek visual terangkat (`graphicsLayer { scaleX = 1.18f; scaleY = 1.18f; shadowElevation = 12f }`).
   - Membersihkan tombol panah pengatur urutan lama `<` dan `>`, menyisakan antarmuka minimal dan modern dengan lencana pengubah ukuran dan penghapusan interaktif.
5. **Penghilangan Duplikasi Header**:
   - Memperbarui `loadConstraints()` di `ShadeHeaderController.kt` untuk menerapkan `applyOneUiConstraints()` di ketiga status MotionLayout: `QQS_HEADER_CONSTRAINT`, `QS_HEADER_CONSTRAINT`, dan `LARGE_SCREEN_HEADER_CONSTRAINT`.
   - Menyembunyikan tampilan bawaan Pixel `clock` dan `date` (`GONE`, `alpha = 0f`) serta meratakan `carrier_group` ke bagian awal induk.

---

### 2. Subsistem Ekspansi RAM (Kernel, Init, SELinux, & Android Treble)

#### Komponen yang Terpengaruh:
- `ram_expansion.sh`
- `init.kernel.post_boot.sh`
- `init.marble.rc`
- `device.mk`
- `property.te`
- `property_contexts`
- `vendor_qti_init_shell.te`
- `RamExpansionSettings.kt`
- `RamExpansionPreferenceController.kt`

#### Analisis Akar Masalah:
1. **Volatilitas & Isolasi Properti**: Properti `vendor.ram_expansion` bersifat non-persisten dan terhapus saat boot ulang. Proses shell vendor diblokir oleh SELinux untuk membaca `persist.sys.*`. Skrip pasca-boot selalu kembali ke nilai default `4096MB`.
2. **Ketiadaan Kapabilitas SELinux**: `vendor_qti_init_shell` tidak memiliki izin baca/tulis `sysfs_zram` dan kapabilitas `CAP_SYS_ADMIN`, menyebabkan eksekusi `swapoff`, `reset`, dan `swapon` gagal secara diam-diam dengan galat penolakan izin.

#### Resolusi Arsitektur:
1. **Pendaftaran Properti Vendor Persisten**:
   - Mendeklarasikan `vendor_public_prop(vendor_ram_expansion_prop)` di `property.te`.
   - Memetakan `persist.vendor.ram_expansion` dan `vendor.ram_expansion` ke `vendor_ram_expansion_prop:s0` di `property_contexts`.
2. **Sinkronisasi Root Init**:
   - Menambahkan pemicu di `init.marble.rc` pada `persist.sys.ram_expansion=*`, `sys.boot_completed=1`, dan `post-fs-data` untuk menyinkronkan nilai ke `persist.vendor.ram_expansion`.
   - Mendaftarkan `/vendor/bin/ram_expansion.sh` di bawah layanan `vendor.ram_expansion` dengan `seclabel u:r:vendor_qti_init_shell:s0`.
3. **Akses Kebijakan SELinux**:
   - Memberi label `ram_expansion.sh` sebagai `vendor_qti_init_shell_exec:s0` di `file_contexts`.
   - Memberikan izin `sysfs_zram:dir`, `sysfs_zram:file`, `swap_block_device:blk_file`, dan `self:capability sys_admin` kepada `vendor_qti_init_shell`.
4. **Integrasi Aplikasi Pengaturan**:
   - Memperbarui `RamExpansionSettings.kt` dan `RamExpansionPreferenceController.kt` untuk membaca dan menyinkronkan nilai langsung dengan `persist.vendor.ram_expansion`.
5. **Rutinitas Reset Swap Atomik**:
   - Di dalam `ram_expansion.sh` dan `init.kernel.post_boot.sh`:
     * Membaca `persist.vendor.ram_expansion` dengan normalisasi unit.
     * Jika diatur ke `0` atau `off`: mengeksekusi `swapoff /dev/block/zram0` dan menulis `1` ke `/sys/block/zram0/reset`.
     * Jika diatur ke `> 0`: mengeksekusi `swapoff` -> `reset` -> menulis ukuran byte ke `/sys/block/zram0/disksize` -> `mkswap` -> `swapon /dev/block/zram0 -p 32758`.

---

### 3. Ringkasan Modifikasi Komponen

| Nama Komponen | Cakupan | Ringkasan Modifikasi |
|---|---|---|
| `OneUIQuickSettingsLayout.kt` | UI / Fitur | Tata letak QQS 2-pill, slider kecerahan tanpa flicker, mode edit seret-lepas bebas |
| `QSFragmentCompose.kt` | Arsitektur | Jembatan fragmen compose One UI dan integrasi view model |
| `ShadeHeaderController.kt` | Perbaikan Bug | Menghilangkan jam/tanggal duplikat pada constraint QQS, QS, dan LargeScreen |
| `RamExpansionSettings.kt` | Pengaturan / UI | Penulisan properti vendor persisten dan pelacakan status |
| `RamExpansionPreferenceController.kt` | Pengaturan / Inti | Pengikatan ringkasan preferensi ke properti persisten |
| `ram_expansion.sh` | Daemon / Subsistem | Skrip swapoff runtime, reset zRAM, dan alokasi disksize dinamis |
| `init.kernel.post_boot.sh` | Kernel / Boot | Konfigurasi zRAM pasca-boot memanfaatkan properti vendor persisten |
| `init.marble.rc` | Init / Pemicu | Pemicu sinkronisasi properti, pendaftaran layanan, dan pemetaan seclabel |
| `device.mk` | Aturan Build | Mendaftarkan `ram_expansion.sh` ke dalam `PRODUCT_COPY_FILES` vendor |
| `file_contexts` | Konteks SELinux | Pemetaan konteks untuk `ram_expansion.sh` |
| `property.te` | Kebijakan SELinux | Mendeklarasikan properti vendor publik `vendor_ram_expansion_prop` |
| `property_contexts` | Konteks SELinux | Pemetaan konteks untuk `persist.vendor.ram_expansion` |
| `vendor_qti_init_shell.te` | Akses Keamanan | Memberikan izin akses zram, swap block device, dan kapabilitas sys_admin |
| `device.mk` (dolby) | Device Tree / Konfigurasi | Mengaktifkan pewarisan produk `hardware/dolby/dolby.mk` |
| `BoardConfig.mk` (dolby) | VINTF / Build | Memulihkan manifes HAL Dolby & C2 serta matriks kerangka kerja |
| `vendor.dolby.hardware.dms@2.0-service` | Vendor HAL | Daemon HIDL DMS asli untuk pengelolaan efek audio Dolby |
| `MarbleDolby` | UI / Aplikasi | Aplikasi kontrol Dolby Atmos berbasis Compose dengan AutoEQ & QS tile |

---

## Bagian IV: Penyempurnaan Terkini & Penambahan Berdasarkan Masukan Pengguna

### 1. Perombakan Tata Letak QQS One UI 1-Tarikan
- **Baris 1 (Atas)**: 1x6 tombol pengaturan cepat melingkar (`OneUIQQSCircularTileItem`), terpisah dari tombol pill, lengkap dengan animasi status aktif/tidak aktif dan interaksi klik serta klik-tahan.
- **Baris 2 (Tengah)**: Slider kecerahan yang terintegrasi responsif dan dibungkus dalam `systemGestureExclusionInShade`.
- **Baris 3 (Bawah)**: 2x1 tombol pill besar (`OneUIBigPillTile`) untuk Wi-Fi dan Bluetooth.
- Integrasi pemutar media langsung di panel tarikan tunggal saat audio sedang aktif.
- Transisi scene bolak-balik yang simetris (`quickQuickSettingsToQuickSettings` dan simetris `toEditMode`) memastikan perubahan bentuk yang halus antara panel 1-tarikan dan 2-tarikan.

### 2. Pengalaman Edit Quick Settings One UI
- **Kontrol Tombol Selesai Tetap (Sticky)**: Tombol Selesai ganda—satu tombol tetap di bilah header atas (mencegah bergulir keluar layar) dan tombol kapsul Selesai mengambang yang ergonomis di tengah bawah layar untuk pengoperasian satu tangan.
- **Seret & Lepas 2D Bebas**: Menghapus ketergantungan kunci `currentIndex` dari `pointerInput` untuk memastikan pelacakan gestur berkelanjutan tanpa reset saat penataan ulang item. Menghitung delta kolom relatif 2D (sumbu X) dan delta baris (sumbu Y) secara dinamis berdasarkan batas tile yang dirender.
- **Umpan Balik Visual**: Peningkatan skala tile (1.15x), elevasi bayangan (16dp), pelapisan zIndex (100f), dan penonaktifan otomatis pengguliran halaman selama gestur seret berlangsung.
- **Lencana Tindakan**: Tindakan ketuk cepat pada lencana (`-` untuk menghapus tile aktif, `+` untuk menambahkan dari daftar yang tersedia).

### 3. Kontroler Ekspansi zRAM Sesuai Standar POSIX
- Menggantikan perintah `let` khusus bash dengan aritmatika standar POSIX `$((ramExpansion * 1024))` di `/vendor/bin/sh` (toybox ash).
- Dukungan penuh untuk mode `0` dan `off` dengan eksekusi `swapoff` yang tepat dan reset perangkat zRAM.
- Sinkronisasi properti multi-tahap di `init.marble.rc` pada pemicu `boot`, `post-fs-data`, dan perubahan properti untuk menjamin persistensi setelah boot ulang.

---

## Bagian V: Integrasi Perangkat Keras Asli Dolby Atmos & MarbleDolby

### 1. Kebutuhan & Gambaran Umum Sistem
- **Verifikasi Dukungan Perangkat Keras Dolby**: Dikonfirmasi bahwa perangkat keras POCO F5 (`marble`) (Snapdragon 7+ Gen 2 / SM7475) secara native mendukung offload perangkat keras Qualcomm Hexagon DSP untuk Pemrosesan Audio Dolby (DAP).
- **Pelepasan Ketergantungan Modul Root Pihak Ketiga**: Menggantikan ketergantungan sebelumnya pada modul eksternal KernelSU/Magisk (OnePlus 9 Pro Dolby Atmos + OPlus Core), yang memerlukan modul overlayfs invasif (`mountify`), framework non-native ColorOS (`oplus-framework-res`), dan penghentian paksa daemon runtime (`killall audioserver`/layanan kamera) pada sistem AOSP.
- **Integrasi Pohon Penuh (In-Tree)**: Mengintegrasikan seluruh subsistem perangkat keras Dolby Atmos langsung ke dalam pohon build ROM melalui `hardware/dolby` (cabang `audio-fixed` marble-indonesia).

### 2. Detail Implementasi Arsitektur & Build
1. **Integrasi Pohon Perangkat (`device/xiaomi/marble`)**:
   - `device.mk`: Mengaktifkan kembali pewarisan untuk `hardware/dolby/dolby.mk` (`$(call inherit-product, hardware/dolby/dolby.mk)`).
   - `BoardConfig.mk`: Memulihkan manifes VINTF HAL (`vendor.dolby.hardware.dms@2.0-service.xml`, `vendor.dolby.media.c2.xml`) dan matriks kompatibilitas framework (`dolby_framework_matrix.xml`).
   - `PRODUCT_SOONG_NAMESPACES`: Mendaftarkan `hardware/dolby` ke dalam namespace Soong secara bersih.
2. **Daemon HAL & Offload Perangkat Keras (`hardware/dolby`)**:
   - `vendor.dolby.hardware.dms@2.0-service`: HAL HIDL Dolby Mobile Service yang berjalan sebagai daemon vendor asli dengan `init.dolby.rc` khusus.
   - Pustaka SoundFX & DSP Offload:
     * `libhwdap.so`: Pustaka efek DAP perangkat keras terdaftar di `audio_effects.xml` dengan UUID `9d4921da-8225-4f29-aefa-39537a04bcaa`.
     * `libdlbvol.so`: Dolby Volume Listener (DVL) untuk aliran musik, dering, alarm, notifikasi, dan panggilan suara.
     * `libswspatializer.so`: Pemrosesan Audio Spasial Dolby.
     * `libswgamedap.so` & `libswvqe.so`: Dolby Game DAP dan Voice Quality Enhancement.
   - Dekoder Codec 2.0: AC4 (`libcodec2_soft_ac4dec`) dan DDP (`libcodec2_soft_ddpdec`) bersama dengan `c2.dolby.*`.
   - Lapisan kompatibilitas: `libstagefright_foundation-v33.so` disediakan melalui `hardware/lineage/compat`.
3. **Aplikasi Antarmuka Pengguna (`MarbleDolby`)**:
   - UI Material 3 modern yang dibangun dengan Jetpack Compose (`org.lunaris.dolby`), diinstal langsung sebagai aplikasi sistem dengan hak istimewa di `system_ext/priv-app/MarbleDolby`.
   - Fitur Utama:
     * Equalizer grafis interaktif 10-band dengan kurva respons frekuensi langsung.
     * Integrasi database AutoEQ (ratusan model headphone/earphone yang telah dikalibrasi).
     * Profil Dolby (Dinamis, Film, Musik, Kustom).
     * Kontrol Bass Boost, Dialogue Enhancer, Stereo Widening, dan Volume Leveler.
     * Layanan tile Quick Settings (`DolbyTileService`) untuk akses pengalihan instan.
     * Otomatisasi profil audio per aplikasi.

### 3. Status Verifikasi & Kompilasi
- Target `MarbleDolby`: Berhasil dikompilasi, melalui proses DEX-preopt, dan terverifikasi bersih (`out/target/product/marble/system_ext/priv-app/MarbleDolby/MarbleDolby.apk`).
- Target `vendor.dolby.hardware.dms@2.0-service`: Ditautkan, diverifikasi terhadap aturan ELF, dan diinstal bersih ke dalam `/vendor/bin/hw/`.
- Pemeriksaan build penuh berhasil dengan kode keluar 0 (`#### build completed successfully ####`).

### 4. Analisis Akar Masalah Bootloop & Solusi
- **Pemisahan Konflik Codec2 / Dolby Vision**: Menghapus layanan video/audio yang berkonflik `vendor.dolby.media.c2@1.0-service` dan `dolbycodec2` serta menonaktifkan `vendor.audio.c2.preferred=true`, mengalihkan decoding media langsung ke HAL C2 asli Qualcomm sambil mengkhususkan Dolby hanya untuk pasca-pemrosesan audio.
- **Penghapusan Batasan Sensor yang Tidak Tersedia**: Menonaktifkan `ro.audio.headtracking_enabled=true` dan mengecualikan `android.hardware.sensor.dynamic.head_tracker.xml` untuk menghindari deadlock inisialisasi AudioService pada perangkat tanpa pelacak kepala dinamis.
- **Pengalihan Pemicu Init ke post-fs-data**: Memindahkan pembuatan direktori data Dolby dari `early-boot` ke `post-fs-data` di `init.dolby.rc` untuk mendukung flashing bersih dengan `/data` yang terenkripsi/diformat.
- **Debugging ADB Persisten Sejak Awal Boot**: Mengaktifkan `persist.sys.usb.config=adb` dan `ro.adb.secure=0` di `system.prop` dan `vendor.prop` untuk memastikan enumerasi USB instan dan pelacakan logcat langsung sejak detik pertama boot.

---

## Bagian VI: Sub-halaman Khusus Pusat Kontrol & Pemilih Gaya Visual

### 1. Kebutuhan & Gambaran Umum
- **Mengganti Tombol Sakelar Baris**: Sebelumnya, peralihan antara Pusat Kontrol One UI dan Quick Settings bawaan Pixel dikelola melalui satu sakelar tunggal di Pengaturan Tampilan (`qs_oneui_style`).
- **Pengalaman Sub-halaman Khusus**: Meningkatkan pengalaman pemilihan menjadi sub-halaman khusus berjudul **Pusat kontrol** (Bahasa Indonesia) / **Control Center** (Bahasa Inggris), menampilkan pratinjau maket visual smartphone berdampingan yang terinspirasi dari tata letak modern HyperOS dan One UI.

### 2. Detail Implementasi
1. **Arsitektur Preferensi Antarmuka (`packages/apps/Settings`)**:
   - `display_settings.xml`: Mengganti `SystemSettingSwitchPreference` lama dengan `Preference` navigasi (`control_center_settings`) yang mengarah ke `com.android.settings.display.ControlCenterSettings`.
   - `ControlCenterPreferenceController.java`: Menyediakan ringkasan subtitle dinamis pada menu Pengaturan Tampilan (menampilkan `One UI` atau `Pixel (Default)` tergantung pada pilihan saat ini).
   - `SettingsGateway.java`: Mendaftarkan `ControlCenterSettings` ke dalam `ENTRY_FRAGMENTS` untuk memastikan navigasi fragmen SubSettingLauncher aman tanpa pelanggaran intent saat runtime.
2. **Pemilih Visual & Kartu Utama Interaktif**:
   - `control_center_settings.xml` & `control_center_layout.xml`: Kartu melengkung premium (radius 24dp, `@color/settingslib_materialColorSurfaceBright`) yang menampung dua maket ponsel interaktif berdampingan:
     * **Opsi 1 (One UI / Terpisah)**: Ilustrasi vektor kustom (`ic_cc_preview_oneui.xml`) menampilkan tata letak Pusat Kontrol terpisah dengan kartu konektivitas besar (Wi-Fi/Bluetooth), slider ganda kecerahan/volume, dan kisi tile kecil, berlabel "Terpisah / One UI" dengan tombol radio.
     * **Opsi 2 (Pixel / Gabungkan)**: Ilustrasi vektor kustom (`ic_cc_preview_pixel.xml`) menampilkan tata letak Quick Settings bawaan yang digabung dengan tombol pill 2x2, slider kecerahan, dan kartu notifikasi, berlabel "Gabungkan / Pixel (Default)" dengan tombol radio.
   - **Deskripsi Dinamis**: Teks deskripsi kontekstual yang langsung diperbarui saat pemilihan dilakukan:
     * *One UI*: "Tampilkan Pusat kontrol dan Pusat notifikasi secara terpisah. Gesek ke bawah dari kanan atas layar untuk membuka Pusat kontrol."
     * *Pixel*: "Tampilkan Quick Settings dan Pusat notifikasi secara digabung dalam tata letak bawaan Google Pixel."
   - **Pembaruan SystemUI yang Mulus**: Mengetuk salah satu opsi akan memperbarui `Settings.System ("qs_oneui_style")` dan secara otomatis memulai ulang SystemUI untuk transisi antarmuka seketika.
3. **Lokalisasi**:
   - Sumber daya teks lengkap ditambahkan ke `shinkai_strings.xml` (EN) dan `values-in/shinkai_strings.xml` (ID) untuk pengalaman asli bahasa Indonesia dan Inggris.

---

## Bagian VII: Desain Ulang Pemutar Musik Kompak One UI, Relokasi Nama Operator, & Penyempurnaan Kaca Buram

### 1. Kebutuhan & Gambaran Umum
- **Penyempurnaan Palet Kaca Buram**: Palet kaca buram awal pada Quick Settings One UI terlalu putih susu (opasitas putih 22%–54%), membuat latar belakang pudar dan menutupi efek blur wallpaper sistem. Disempurnakan menjadi kaca berasap translusen gelap dengan tepian berkilau.
- **Relokasi Teks Operator Seluler**: Dalam mode Pusat Kontrol One UI, nama operator sebelumnya diposisikan canggung di atas indikator baterai di dalam header status bar. Dipindahkan ke kiri atas tepat di atas jam digital di `OneUIHeaderClockRow`.
- **Kartu Pemutar Media Kompak One UI & Integrasi QQS**:
  * Menambahkan kartu pemutar media ke QQS (tarikan 1 kali) yang diposisikan tepat di bawah tile pill 2x1 (Wi-Fi & Bluetooth).
  * Mendesain ulang tata letak UMO Pixel yang besar menjadi kartu kompak gaya Samsung One UI (~130dp) yang menampilkan latar belakang cover album, baris header atas, gelombang seekbar/timeline cyan dinamis, dan kontrol pemutaran bawah.
  * Memastikan transisi animasi elemen bersama antara QQS (tarikan 1 kali) dan QS (tarikan 2 kali) untuk ekspansi panel yang lancar.
  * Mempertahankan pemutar media bawaan Google Pixel QS tanpa perubahan saat mode One UI dinonaktifkan.

### 2. Detail Implementasi

1. **Penyempurnaan Palet Blur Kaca Buram (`OneUIQuickSettingsLayout.kt`)**:
   - Menggantikan kontainer putih susu datar dengan token kaca translusen berasap pekat:
     * Mode Gelap: `cardBg = Color(0x3514171E)`, batas kaca berkilau `cardBorder = Color(0x24FFFFFF)`, `pillInactiveBg = Color(0x26FFFFFF)`.
     * Mode Terang: `cardBg = Color(0x66FFFFFF)`, `cardBorder = Color(0x24000000)`, `pillInactiveBg = Color(0x14000000)`.
   - Memperbarui latar belakang track slider di `OneUIBrightnessSlider` dan `OneUIVolumeSlider` ke `Color(0x26FFFFFF)` untuk memungkinkan efek blur wallpaper asli bersinar dengan kedalaman dan kejernihan tinggi.
   - Menyinkronkan token palet di seluruh Quick Settings, QQS, dan Mode Edit Tile (`OneUIEditMode`).

2. **Relokasi Operator di Atas Jam (`ShadeHeaderController.kt` & `OneUIQuickSettingsLayout.kt`)**:
   - `ShadeHeaderController.kt`: Di `applyOneUiConstraints` dan `updatePosition`, mengatur `carrier_group` dan `mShadeCarrierGroup` menjadi `View.GONE` dengan `alpha = 0f` saat `qs_oneui_style` aktif.
   - `OneUIQuickSettingsLayout.kt`:
     * Membuat composable `rememberCarrierName()` yang mengamati `TelephonyManager` dan `SubscriptionManager` (melalui `activeSubscriptionInfoList` dan pendengar siaran).
     * Menempatkan `carrierText` di `OneUIHeaderClockRow` dalam `Column` tepat di atas waktu jam digital 28sp dengan `12sp, FontWeight.Medium, color = textSecondary`.

3. **Kartu Pemutar Media Kompak One UI (`OneUICompactMediaPlayer`)**:
   - **Bentuk Fisik**: Tinggi kompak ~130dp dengan radius sudut melengkung 26dp (`RoundedCornerShape(26.dp)`), bingkai kaca, dan latar belakang sampul album yang dipotong dengan gradien vertikal (`Brush.verticalGradient(listOf(Color(0x9905070A), Color(0xCC05070A)))`).
   - **Baris Header Atas**:
     * Kiri: Chip perangkat "Ponsel ini" dengan kapsul kaca melengkung (`Color(0x38FFFFFF)`).
     * Tengah/Kiri: Ikon aplikasi aktif (melalui `PackageManager`) + teks berjalan satu baris/elipsis untuk `Judul Lagu • Artis`.
     * Kanan: Tombol pengalih output melingkar (26dp, `ic_speaker_on`) yang memicu `com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG`.
   - **Seekbar Gelombang (`OneUIWaveSeekbar`)**:
     * Waktu berlalu di sebelah kiri (`mm:ss`) dan durasi di sebelah kanan (`mm:ss`).
     * Kurva gelombang cairan cyan (`Color(0xFF29B6F6)`) di sepanjang trek yang telah diputar dengan puncak/lembah bezier kubik dan isian gradien di bawahnya.
     * Penunjuk scrubber pada posisi kemajuan (lingkaran luar cyan dengan titik tengah putih).
     * Jalur belum diputar dengan garis abu-abu melengkung halus (`Color(0x40FFFFFF)`).
     * Gestur seret dan ketuk interaktif untuk mencari posisi lagu melalui `activeController.transportControls.seekTo()`.
   - **Kontrol Media Bawah**:
     * Tombol acak/shuffle (`ic_oneui_media_shuffle`).
     * Tombol trek sebelumnya (`ic_media_prev`).
     * Tombol putar/jeda kaca buram melingkar besar (42dp, `ic_oneui_media_play` / `ic_oneui_media_pause`).
     * Tombol trek berikutnya (`ic_media_next`).
     * Tombol favorit / centang (`ic_oneui_media_check`).
   - **Transisi & Penempatan QQS**:
     * Pada QQS: Ditempatkan tepat di bawah tombol pill 2x1 (Wi-Fi & Bluetooth).
     * Pada QS: Ditempatkan pada Kartu 5 di bawah slider volume/mode suara.
     * Keduanya dibungkus dalam `Modifier.element(OneUIElements.MediaPlayer)` di `SceneTransitionLayout` untuk animasi ekspansi yang sangat mulus antara status tarikan 1 kali dan 2 kali.
   - **Aset Vektor Ditambahkan**:
     * `packages/SystemUI/res/drawable/ic_oneui_media_play.xml`
     * `packages/SystemUI/res/drawable/ic_oneui_media_pause.xml`
     * `packages/SystemUI/res/drawable/ic_oneui_media_shuffle.xml`
     * `packages/SystemUI/res/drawable/ic_oneui_media_check.xml`

---

## Bagian VIII: Penyempurnaan Geometri Pemutar Musik One UI & Perbaikan Pemotongan Ikon Status QS Header

### 1. Kebutuhan & Gambaran Umum
- **Informasi Lagu di Tengah**: Sebelumnya, `Judul Lagu • Artis` dan ikon aplikasi menumpuk ke kiri berdekatan dengan chip perangkat "Ponsel ini". Dipusatkan langsung di antara chip perangkat dan pengalih output audio.
- **Kontrol Trek Sebelumnya & Berikutnya Melingkar Kaca Buram**: Pada One UI, kontrol pemutaran berada di atas latar belakang lingkaran kaca buram yang serasi. Menambahkan latar belakang kaca buram melingkar ke tombol sebelumnya dan berikutnya, memperbesar diameter kontrol, dan merapatkan jarak tombol.
- **Seekbar Gelombang Cair Autentik**: Menggantikan garis sinusoidal cepat dengan kurva gelombang cair organik khas Samsung (gradien isian `Color(0xFF00C7FF)` ke `Color(0xFF008FD6)` di bawahnya), penunjuk scrubber cyan dengan titik putih di tengah, dan garis lurus untuk jalur belum diputar (`Color(0x45FFFFFF)`).
- **Pemisahan Seekbar & Penanda Waktu**: Mengekstrak penanda waktu ke baris khusus tepat di bawah seekbar gelombang untuk rentang horizontal seekbar yang maksimal dan ritme visual yang lebih bersih.
- **Sudut Lengkung Kartu**: Meningkatkan kelengkungan kartu media dari 26dp menjadi 32dp (`RoundedCornerShape(32.dp)`), secara presisi mencocokkan radius kartu Samsung One UI.
- **Perbaikan Pemotongan Ikon Status Bar di QS Penuh**: Pada QS penuh (tarikan 2 kali), ikon baterai, Wi-Fi, dan sinyal sempat tergeser dan terpotong di tepi atas layar (meskipun tampak normal di QQS). Diselesaikan dengan mengaitkan kembali constraint ke panduan status bar dan mengunci progress MotionLayout ke 0 dalam mode One UI.

### 2. Detail Implementasi

1. **Perbaikan Pemotongan Status Bar Header QS SystemUI (`ShadeHeaderController.kt`)**:
   - `applyOneUiConstraints`: Menghapus pengait atas/bawah/awal/akhir yang berkonflik untuk `R.id.shade_header_system_icons` dan `R.id.privacy_container`, menyambungkannya kembali langsung ke bagian atas `PARENT_ID`, bagian bawah `R.id.qqs_header_bottom_guideline`, dan bagian akhir `R.id.end_guide`.
   - `updatePosition`: Dalam mode One UI (`isOneUi == true`), mengunci `header.progress = 0f` alih-alih menerapkan `qsExpandedFraction`. Ini mencegah transisi MotionLayout menggeser ikon sistem ke atas saat menarik turun QS penuh, memastikan baterai, Wi-Fi, dan status seluler tetap berada sempurna pada ketinggian standar status bar setiap saat.

2. **Geometri Pemutar Media Kompak One UI & Seekbar (`OneUIQuickSettingsLayout.kt`)**:
   - **Kelengkungan & Dimensi**: Memperbarui cangkang luar kartu media ke `RoundedCornerShape(32.dp)` dengan tinggi `138.dp`.
   - **Header Tengah**: Membungkus `"Ponsel ini"` di sebelah kiri, `Row` berekspansi dengan `Modifier.weight(1f)` dan `Arrangement.Center` di tengah yang memuat ikon aplikasi dan `Judul Lagu • Artis`, serta pengalih output melingkar 28dp di sebelah kanan.
   - **Bukit Gelombang Cair (`OneUIWaveSeekbar`)**:
     * Menerapkan kurva puncak dan lembah bezier kubik yang halus (`cubicTo`) yang menciptakan puncak gelombang cair organik khas di sepanjang bagian yang telah diputar.
     * Merender isian cairan gradien vertikal cerah di bawah gelombang (`Color(0xFF00C7FF)` hingga `Color(0xFF008FD6)`).
     * Merender bagian yang belum diputar sebagai garis lurus melengkung 3.5dp (`Color(0x45FFFFFF)`).
     * Merender lingkaran penunjuk cyan 7dp dengan titik tengah putih 2.8dp pada posisi kemajuan.
   - **Baris Penanda Waktu Khusus**: Menempatkan penanda waktu `01:20` dan `04:23` tepat di bawah seekbar gelombang dalam baris terpisah dengan ukuran font `11.sp` dan `Arrangement.SpaceBetween`.
   - **Trio Tombol Pemutaran**:
     * Trek sebelumnya: Kontainer kaca buram melingkar `42.dp` (latar belakang `Color(0x38FFFFFF)`, batas `1.dp` `Color(0x28FFFFFF)`).
     * Putar/Jeda: Kontainer kaca buram melingkar besar `50.dp` (latar belakang `Color(0x48FFFFFF)`, batas `1.dp` `Color(0x35FFFFFF)`).
     * Trek berikutnya: Kontainer kaca buram melingkar `42.dp` (latar belakang `Color(0x38FFFFFF)`, batas `1.dp` `Color(0x28FFFFFF)`).
     * Jarak dirapatkan ke `Arrangement.spacedBy(10.dp)`.
     * Kontrol bantu luar: Shuffle 36dp di sebelah kiri dan favorit 34dp di sebelah kanan.

---

## Bagian IX: Tindakan Dinamis "Mulai Ulang System UI" di Pengaturan Pusat Kontrol

### 1. Kebutuhan & Gambaran Umum
- **Menghilangkan Mulai Ulang Otomatis Paksa**: Sebelumnya, beralih antara gaya One UI dan Pixel QS memicu penghentian proses SystemUI secara langsung saat diklik, yang menyebabkan kedipan layar mendadak.
- **Kartu Tindakan Dinamis**: Memperkenalkan kartu tindakan Material 3 khusus (`cc_restart_card`) di bawah kartu pratinjau utama yang muncul secara dinamis setiap kali pengguna beralih antara opsi One UI dan Pixel QS.
- **Panduan Pengguna yang Jelas**: Menampilkan deskripsi eksplisit yang menyatakan bahwa memulai ulang SystemUI bersifat opsional, namun disarankan agar seluruh perubahan tata letak dan estetika diterapkan secara sempurna ("Memulai ulang System UI bersifat opsional namun disarankan agar seluruh perubahan tata letak diterapkan secara sempurna.").
- **Tombol Tindakan Material 3**: Menyediakan tombol bergaya menonjol ("Restart System UI" / "Mulai ulang System UI") dengan ikon vektor kustom (`ic_restart_systemui`) yang memungkinkan pengguna memicu mulai ulang sesuai keinginan mereka.

### 2. Detail Implementasi
1. **Tata Letak Antarmuka (`control_center_layout.xml`)**:
   - Menambahkan `cc_restart_card` (MaterialCardView melengkung 24dp, serasi dengan warna permukaan kartu utama).
   - Berisi ikon header (`@drawable/ic_restart_systemui`), judul (`control_center_restart_title`), ringkasan deskriptif (`control_center_restart_desc`), dan tombol tindakan terisi (`cc_restart_button`).
   - Visibilitas default diatur ke `android:visibility="gone"`.
2. **Pengontrol Alur Dinamis (`ControlCenterSettings.kt`)**:
   - Saat fragment resume, menginisialisasi visibilitas `cc_restart_card` menjadi `View.GONE`.
   - Saat mengetuk opsi One UI atau Pixel, menyimpan pengaturan dan langsung memunculkan `cc_restart_card` (`View.VISIBLE`).
   - Mengklik `cc_restart_button` akan memanggil `OneUIQSStylePreferenceController.restartSystemUI(ctx)`.
3. **Aset & Lokalisasi**:
   - Ikon vektor: `ic_restart_systemui.xml` (glif Material refresh/restart standar).
   - Definisi string ditambahkan dalam bahasa Inggris (`shinkai_strings.xml`) dan bahasa Indonesia (`values-in/shinkai_strings.xml`).

---

## Bagian X: Kapsul Pengisian Daya Dynamic Island untuk POCO F5 (marble) [Eksperimental]

### 1. Kebutuhan & Gambaran Umum
- **Dynamic Island Terintegrasi Punch-Hole**: Menerapkan kapsul pengisian daya Dynamic Island elegan yang disesuaikan secara geometris untuk punch-hole kamera tengah POCO F5 (`device/xiaomi/marble`, cutout: `M 0,0 H -28 V 94 H 28 V 0 H 0 Z`).
- **Animasi Dipicu Pengisian Daya**: Aktif secara eksklusif saat pengisian daya dimulai (kabel dicolokkan), berekspansi mulus dari kamera tengah menjadi kapsul status bar.
- **Desain Visual Presisi**:
  * Cangkang Kapsul: Hitam pekat (`#000000`) dengan ujung membulat (radius sudut 100dp, bentuk stadion/pill, tinggi 35dp), tepian translusen halus (`#24FFFFFF`), dan lapisan z-order tepat di atas konten status bar.
  * Informasi Kiri: Teks status terlokalisasi ("Mengisi daya" dalam ID, "Charging" dalam EN) dengan tipografi putih sans-serif yang bersih (13.5sp).
  * Informasi Kanan: Persentase baterai langsung (misalnya "29%") disertai ikon vektor petir hijau neon beranimasi (`#00E676`).
  * Ekspansi Elastis & Penarikan Otomatis: Membesar secara organik dari tengah punch-hole ke luar dengan `OvershootInterpolator(1.18f)`, bertahan selama 4.5 detik untuk menampilkan status pengisian daya dengan jelas, dan kembali menyusut ke dalam lubang kamera melalui `AccelerateInterpolator(1.4f)`.
- **Sakelar Pengaturan Pengguna**: Mengintegrasikan kartu sakelar khusus di sub-halaman Pusat Kontrol (`ControlCenterSettings`) dengan persistensi runtime instan (`dynamic_island_charging`, default diaktifkan).
- **Kontrol Interaktif & Pengujian**: Mengetuk kapsul akan langsung menutupnya. Mendukung broadcast intent `com.android.systemui.action.TEST_DYNAMIC_ISLAND` untuk pengujian otomatis maupun manual.

### 2. Detail Implementasi
1. **Hierarki Status Bar & Custom View (`SystemUI`)**:
   - `status_bar.xml`: Menempatkan `DynamicIslandChargingView` (`@+id/dynamic_island_charging`) di dalam `PhoneStatusBarView` dengan `layout_gravity="center_horizontal|center_vertical"`, sejajar langsung di atas punch-hole kamera tengah POCO F5.
   - `DynamicIslandChargingView.kt`: `FrameLayout` kustom yang mendengarkan sticky broadcast `Intent.ACTION_BATTERY_CHANGED`. Mendeteksi transisi status pengisian daya (`!wasPluggedIn && isPluggedIn`), mengukur metrik layar, dan mengelola animasi pegas `AnimatorSet` yang halus.
   - `dynamic_island_charging.xml` & `dynamic_island_pill_bg.xml`: Tata letak kapsul khusus dan drawable kapsul stadion.
   - `ic_dynamic_island_bolt.xml`: Aset vektor petir 16dp kustom berwarna hijau neon (`#00E676`).
2. **Integrasi Pengaturan (`packages/apps/Settings`)**:
   - `control_center_layout.xml`: Menambahkan `cc_dynamic_island_card` dengan `MaterialSwitch` (`@+id/cc_dynamic_island_switch`).
   - `ControlCenterSettings.kt`: Mengikat status sakelar ke `Settings.System.getInt("dynamic_island_charging", 1)`.
   - Lokalisasi string di `shinkai_strings.xml` dan `values-in/shinkai_strings.xml`.

---

## Bagian XI: Stabilitas Boot Android Setup (OOBE) & Perbaikan Keamanan Receiver

### 1. Kebutuhan & Gambaran Umum
- **Diagnosis Masalah**: Setelah instalasi bersih atau format data, SystemUI mengalami crash berulang (crash-loop) selama Android Setup Wizard (OOBE), menyebabkan layar berkedip dan memicu Android Rescue Party (ARP) untuk reboot ke recovery.
- **Akar Masalah 1**: Pendaftaran broadcast receiver di `DynamicIslandChargingView` dengan intent non-protected (`ACTION_TEST_DYNAMIC_ISLAND`) tanpa flag wajib Android 14+ `Context.RECEIVER_EXPORTED` melempar `java.lang.SecurityException` yang tidak tertangani pada thread utama saat dipasang ke window.
- **Akar Masalah 2**: Memutar animasi status bar dan menanyakan SettingsProvider pada awal boot sebelum perangkat selesai dipersiapkan (`DEVICE_PROVISIONED == 0`) selama penyiapan awal.
- **Resolusi**:
  * Menambahkan flag `Context.RECEIVER_EXPORTED` pada pendaftaran broadcast receiver.
  * Menambahkan pemeriksaan `Settings.Global.DEVICE_PROVISIONED` agar Dynamic Island tetap tidak aktif selama Setup Wizard (OOBE).
  * Mengubah pengaturan default menjadi `0` (fitur eksperimental dengan aktivasi manual melalui pengaturan Pusat Kontrol).
  * Membungkus semua callback daur hidup (`onFinishInflate`, `onAttachedToWindow`, `onDetachedFromWindow`, `triggerChargingIsland`, `dismissIsland`, `onReceive`) dan pemanggilan `Settings.System.getInt` di `ShadeHeaderController.kt` dalam blok `try-catch` komprehensif untuk memastikan SystemUI tidak pernah mengalami crash dalam kondisi apa pun.

---

## Bagian XII: Perbaikan Crash Pengaturan Pusat Kontrol (NullPointerException pada SwitchCompat.makeLayout)

### 1. Kebutuhan & Gambaran Umum
- **Diagnosis Masalah**: Membuka **Pusat kontrol** di Pengaturan menyebabkan aplikasi crash seketika:
  ```
  java.lang.NullPointerException: Attempt to invoke interface method 'int java.lang.CharSequence.length()' on a null object reference
      at android.text.StaticLayout.<init>(StaticLayout.java:654)
      at androidx.appcompat.widget.SwitchCompat.makeLayout(SwitchCompat.java:986)
      at androidx.appcompat.widget.SwitchCompat.onMeasure(SwitchCompat.java:907)
  ```
- **Akar Masalah**: Di `control_center_layout.xml`, `MaterialSwitch` merupakan turunan dari `androidx.appcompat.widget.SwitchCompat`. Selama pengukuran tata letak (`onMeasure`), ketika `mShowText` aktif atau diwariskan dari tema dan `textOn`/`textOff` bernilai null, `SwitchCompat.makeLayout` meneruskan nilai `null` ke `StaticLayout`, melempar `NullPointerException` pada `CharSequence.length()`.
- **Resolusi**:
  * Mengganti `MaterialSwitch` di `control_center_layout.xml` dengan `Switch` bawaan platform (`android.widget.Switch`) dengan atribut eksplisit `android:showText="false"`, `android:textOn=""`, dan `android:textOff=""`.
  * Memperbarui `ControlCenterSettings.kt` untuk mengikat ke `android.widget.CompoundButton`, yang kompatibel secara universal dengan semua komponen sakelar.
  * Membungkus seluruh pengikatan sakelar Dynamic Island di `ControlCenterSettings.kt` di dalam blok `try-catch` untuk keandalan maksimal.

---

## Bagian XIII: Perbaikan Definitif Crash Loop Setup Wizard SystemUI & Isolasi Receiver

### 1. Kebutuhan & Gambaran Umum
- **Diagnosis Masalah**: Log crash DropBox (`system_app_crash`) mengungkapkan bahwa SystemUI mengalami crash berulang dengan galat:
  ```
  java.lang.SecurityException: com.android.systemui: One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified when a receiver isn't being registered exclusively for system broadcasts
      at android.app.ContextWrapper.registerReceiver(ContextWrapper.java:793)
      at com.android.systemui.statusbar.phone.DynamicIslandChargingView.onAttachedToWindow
  ```
- **Akar Masalah**:
  * Di `DynamicIslandChargingView.kt`, receiver didaftarkan di dalam `onAttachedToWindow()` tanpa memeriksa apakah perangkat telah dipersiapkan (`DEVICE_PROVISIONED == 1`).
  * `IntentFilter` sebelumnya didaftarkan untuk mendengarkan `Intent.ACTION_BATTERY_CHANGED` (siaran sistem terlindungi) sekaligus `ACTION_TEST_DYNAMIC_ISLAND` (siaran kustom tidak terlindungi). Pada Android 14+, mencampur aksi terlindungi dan tidak terlindungi memaksa `BroadcastController` memberlakukan flag export eksplisit dan melempar `SecurityException`.
  * Di `ShadeHeaderController.kt`, `updateCarrierGroupPadding()` memanggil `Settings.System.getInt("qs_oneui_style")` tanpa perlindungan `try-catch`.
  * Di `OneUIQuickSettingsLayout.kt`, `rememberIsOneUiStyle()` membaca `Settings.System.getInt` langsung selama inisialisasi Compose tanpa penanganan toleransi kegagalan boot awal.
- **Resolusi**:
  * Di `DynamicIslandChargingView.kt`, menambahkan penjaga langsung di awal `onAttachedToWindow()`: jika `DEVICE_PROVISIONED != 1`, atur `visibility = View.GONE` dan segera kembali tanpa mendaftarkan receiver apa pun atau menyentuh window.
  * Menghapus `ACTION_TEST_DYNAMIC_ISLAND` dari `batteryReceiver` sehingga receiver hanya mendengarkan `Intent.ACTION_BATTERY_CHANGED` menggunakan `Context.RECEIVER_NOT_EXPORTED`.
  * Membungkus `Settings.System.getInt` di `ShadeHeaderController.kt:updateCarrierGroupPadding()` dan `OneUIQuickSettingsLayout.kt` di dalam blok `try-catch`.

---

## Bagian XIV: Implementasi Andal "Mulai Ulang System UI" dari Pengaturan

### 1. Kebutuhan & Gambaran Umum
- **Diagnosis Masalah**: Tombol "Mulai ulang System UI" di sub-halaman Pusat Kontrol (`ControlCenterSettings`) tidak berhasil memulai ulang SystemUI saat diklik, padahal tindakan "Mulai ulang System UI" di Menu Daya (Advanced Reboot) berfungsi dengan baik.
- **Akar Masalah**:
  * Menu Daya dieksekusi di dalam `com.android.systemui` dan dapat langsung memanggil `Process.killProcess(Process.myPid())`.
  * `com.android.settings` berjalan sebagai proses eksternal dan tidak memiliki kapabilitas POSIX `CAP_KILL` untuk mengirim `SIGKILL` arbitrer ke proses lain secara langsung melalui `Process.killProcess(proc.pid)`.
  * Pemanggilan `ActivityManager.getService().killApplicationProcess("com.android.systemui", -1)` gagal karena `-1` tidak cocok dengan UID paket sebenarnya milik SystemUI di `ProcessList`.
  * Di SystemUI, `SysuiRestartReceiver` memiliki `android:exported="false"` dan mengalami crash jika `intent.getData()` bernilai null.
- **Resolusi**:
  * Di `SysuiRestartReceiver.java`, membuat `onReceive()` aman terhadap nilai null untuk `intent.getData()` sehingga dapat dipanggil dengan broadcast intent standar, memicu `Process.killProcess(Process.myPid())` secara bersih dari dalam SystemUI.
  * Di `AndroidManifest.xml` milik SystemUI, mengekspor `SysuiRestartReceiver` yang dilindungi oleh izin `android.permission.STATUS_BAR_SERVICE`.
  * Di `AndroidManifest.xml` milik Settings, menambahkan izin `STATUS_BAR_SERVICE` dan `KILL_UID`.
  * Di `OneUIQSStylePreferenceController.java`, menerapkan pipa mulai ulang dengan tiga tingkat redundansi:
    1. Mengirim siaran `com.android.systemui.action.RESTART` ke `com.android.systemui` sebagai `UserHandle.ALL`.
    2. Cadangan: Memanggil `ActivityManager.getService().killApplicationProcess` menggunakan UID paket yang diperoleh dari `PackageManager`.
    3. Cadangan: Memanggil `ActivityManager.getService().killUid` dengan `evenPersistent = true`.
  * Di `ControlCenterSettings.kt`, secara otomatis menampilkan kartu "Mulai ulang System UI" ketika sakelar Dynamic Island diubah.

---

## Bagian XV: Penyesuaian Durasi Tampilan Kapsul Dynamic Island

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**: Kapsul pengisian daya Dynamic Island sebelumnya menarik diri terlalu cepat (sebelumnya 4.5 detik).
- **Penyesuaian**: Memperpanjang waktu tampilan dan penahanan menjadi **7.8 detik** (`7800L`) di `DynamicIslandChargingView.kt` sehingga status pengisian daya, persentase baterai langsung, dan animasi petir hijau neon tetap terlihat jelas sebelum secara perlahan menarik diri kembali ke dalam lubang kamera punch-hole.

---

## Bagian XVI: Evolusi Penuh Dynamic Island – Fisika Pegas, Dual Island Meta-Ball, dan Kartu Kontrol Mengambang

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**:
  1. Mengubah Dynamic Island menjadi sistem Dynamic Island yang komprehensif dan nyata, melampaui sekadar animasi pengisian daya baterai.
  2. Menerapkan fisika **Pegas & Overshoot (Spring Physics)** ala iOS yang sangat halus:
     - Pembukaan: Ekspansi horizontal ke kiri dan ke kanan dari lubang kamera tengah POCO F5 dengan pantulan pegas yang elastis.
     - Penutupan: Rekoil elastis dua tahap (sedikit peregangan karet horizontal sebelum menyusut cepat kembali ke dalam lubang kamera).
  3. Mendukung tiga peristiwa inti sistem:
     - **Pengisian Daya (Charging)**: Tampilan efemeral selama 7.8 detik dengan persentase baterai langsung dan animasi petir hijau neon berdenyut.
     - **Senter (Flashlight)**: Aktivitas langsung persisten selama lampu senter menyala (ON).
     - **Perekaman Layar (Screen Recording)**: Aktivitas langsung persisten selama perekaman aktif, dilengkapi dengan penghitung waktu digital berjalan langsung (`00:00`, `02:31`).
  4. **Arsitektur Dual-Island Meta-Ball**:
     - Ketika Senter dan Perekaman Layar berjalan bersamaan, Perekaman Layar mengambil prioritas di **Kapsul Utama** (menampilkan penghitung waktu) dan Senter menjadi **Meta-Ball** (gelembung bundar terpisah di sisi kanan).
     - **Gestur Usap Horizontal**: Mengusap ke kiri atau ke kanan secara mulus menukar posisi dan prioritas antara kapsul utama dan meta-ball dengan animasi pegas.
  5. **Kartu Kontrol Mengambang Interaktif (Ketuk untuk Memperluas)**:
     - Mengetuk kapsul pulau yang ringkas akan membuka kartu mengambang yang diperluas di bawah lubang kamera (`WindowManager.LayoutParams.TYPE_STATUS_BAR_SUB_PANEL`).
     - Tampilan Diperluas Perekaman Layar: Tombol Stop melingkar dengan aksen merah, tombol Jeda, dan font jam digital besar untuk waktu yang telah berjalan di bawah label huruf kapital emas `WAKTU BERJALAN`.
     - Tampilan Diperluas Senter: Tombol bulat kuning amber yang menyala dengan ikon daya, status `MENYALA`, dan tombol ketuk sekali untuk memadamkan lampu senter.
     - Deteksi sentuhan luar (`FLAG_WATCH_OUTSIDE_TOUCH`) menutup kartu kembali ke dalam kapsul kompak di lubang kamera secara bersih.

### 2. Berkas yang Dimodifikasi & Ditambahkan
- `frameworks/base/packages/SystemUI/src/com/android/systemui/Dependency.java`: Menambahkan `@Inject Lazy<FlashlightController>` dan `@Inject Lazy<ScreenRecordUxController>` ke dalam `mProviders` untuk penemuan kontroler yang aman.
- `frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/DynamicIslandChargingView.kt`: Merancang ulang mesin status, animasi pegas overshoot, runnable ticker timer langsung, detektor gestur usap dual-island, dan siklus hidup kartu mengambang window manager.
- `frameworks/base/packages/SystemUI/res/layout/dynamic_island_charging.xml`: Membangun ulang kontainer horizontal induk, `island_capsule` (dengan tampilan charging, torch, dan record), serta gelembung melingkar `island_meta_ball`.
- `frameworks/base/packages/SystemUI/res/layout/dynamic_island_expanded_card.xml`: Membuat tata letak kartu mengambang yang diperluas dengan tombol aksi melingkar dan pembacaan digital besar.
- `frameworks/base/packages/SystemUI/res/drawable/dynamic_island_circle_bg.xml`: Latar belakang hitam murni oval dengan batas putih halus untuk meta-ball.
- `frameworks/base/packages/SystemUI/res/drawable/dynamic_island_expanded_bg.xml`: Latar belakang hitam murni dengan radius 28dp untuk kartu yang diperluas.
- `frameworks/base/packages/SystemUI/res/drawable/dynamic_island_btn_bg.xml`: Latar belakang tombol efek ripple melingkar untuk kontrol stop dan aksi.
- `frameworks/base/packages/SystemUI/res/drawable/dynamic_island_btn_amber_bg.xml`: Latar belakang tombol kuning amber bercahaya untuk kontrol senter dan jeda.
- `frameworks/base/packages/SystemUI/res/drawable/ic_dynamic_island_record_dot.xml`: Titik merah berdenyut untuk indikator perekaman layar.
- `frameworks/base/packages/SystemUI/res/drawable/ic_dynamic_island_stop.xml`: Aset vektor kotak stop untuk penghentian rekaman.
- `frameworks/base/packages/SystemUI/res/values/shinkai_strings.xml` & `res/values-in/shinkai_strings.xml`: Sumber daya string bahasa Inggris dan Indonesia untuk label senter dan perekaman layar.

---

## Bagian XVII: Kartu Dashboard Notifikasi Pengaturan Utama & Colored Notification Icons

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**:
  1. Pada Dashboard Cepat Halaman Utama Pengaturan (`homepage_quick_dashboard.xml`), ganti kartu **Aksesibilitas** dengan kartu panel khusus **Notifikasi**.
  2. Di dalam Pengaturan Notifikasi (`ConfigureNotificationSettings`), tambahkan fitur baru: **Colored Notification Icons** (`notification_colored_icons`).
  3. "Colored Notification Icons": menampilkan logo asli aplikasi yang berwarna penuh (`pm.getApplicationIcon(packageName)`) untuk notifikasi di bilah status dan kartu panel notifikasi, bukan sekadar siluet monokrom yang diwarnai (seperti fitur populer di Evolution X, crDroid, dan Iconify).

### 2. Detail Implementasi
1. **Penggantian Panel Dashboard Halaman Utama Pengaturan (`packages/apps/Settings`)**:
   - `homepage_quick_dashboard.xml`: Mengganti `home_card_accessibility` dengan `home_card_notification` yang menampilkan kontainer ikon oranye hangat (`#FB923C`), `@drawable/ic_notifications_filled`, judul `@string/configure_notification_settings`, dan subjudul `@string/home_notification_sub`.
   - `HomepageQuickDashboardPreferenceController.kt`: Mengikat pendengar klik `home_card_notification` untuk meluncurkan `Settings.ACTION_NOTIFICATION_SETTINGS` / `ConfigureNotificationSettings`.
2. **Pengontrol Preferensi Pengaturan & Integrasi XML**:
   - `ColoredNotificationIconsPreferenceController.java`: Membuat `TogglePreferenceController` yang mengontrol `Settings.System.notification_colored_icons`.
   - `configure_notification_settings.xml`: Menambahkan `<SwitchPreferenceCompat>` dengan kunci `notification_colored_icons` pada urutan 0 di bawah `general_notifications_category`.
   - `res/values/shinkai_strings.xml` & `res/values-in/shinkai_strings.xml`: Menambahkan string lokalisasi untuk bahasa Inggris dan Indonesia.
3. **Penyajian Ikon Notifikasi Berwarna Asli di SystemUI (`frameworks/base`)**:
   - `StatusBarIconView.java`:
     * Di `loadDrawable`: Jika `isNotification()` dan `notification_colored_icons == 1`, memuat dan mengembalikan ikon aplikasi peluncur asli melalui `pm.getApplicationIcon(packageName)`.
     * Di `updateIconColor`: Melewati pewarnaan matriks (tint) dan efek monokrom saat layar aktif, memastikan warna merek asli aplikasi tetap terjaga utuh.
     * Di `setNotification`: Langsung memperbarui status drawable dan warna saat notifikasi terpasang.
   - `NotificationHeaderViewWrapper.java`:
     * Di `onContentUpdated`: Mengganti ikon kecil header monokrom dengan logo aplikasi berwarna penuh (`mIcon.setImageDrawable(appIcon)`), menghapus filter warna, mereset latar belakang, dan menormalkan padding untuk tampilan bersih di kartu panel notifikasi dan layar kunci.
   - `IconManager.kt`:
     * Di `attach()`: Mendaftarkan `ContentObserver` pada `Settings.System.getUriFor("notification_colored_icons")` yang secara dinamis merender ulang semua ikon notifikasi yang ada melalui `updateIconsSafe(entry)` secara seketika saat sakelar diubah, tanpa memerlukan mulai ulang SystemUI.

---

## Bagian XVIII: Pengembalian Aksesibilitas ke Daftar Pengaturan & Perbaikan Dual-Slot / Kartu Mengambang Dynamic Island

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**:
  1. Kembalikan menu Aksesibilitas (`top_level_accessibility`) ke dalam daftar utama Pengaturan (karena panel dashboard cepatnya telah digantikan oleh Notifikasi).
  2. Perbaiki bug Dynamic Island pada Foto 1: Mengetuk island (senter atau perekam layar) menyebabkan kartu membulat hitam yang diperluas muncul dalam keadaan kosong tanpa tombol atau indikator apa pun.
  3. Perbaiki bug Dynamic Island pada Foto 2: Ketika senter dan perekaman layar aktif secara bersamaan, gelembung bundar meta-ball kedua gagal muncul di samping kapsul utama.
  4. Perbaiki ukuran kapsul perekam layar: Panjang kapsul perekam layar sebelumnya terasa kurang seimbang, sehingga perlu didesain ulang agar simetris ala iOS (~200dp).

### 2. Detail Implementasi
1. **Pemulihan Aksesibilitas di Pengaturan Utama (`packages/apps/Settings`)**:
   - `top_level_settings.xml`: Menambahkan kembali `<com.android.settings.widget.HomepagePreference>` dengan kunci `top_level_accessibility`, ikon `@drawable/ic_settings_accessibility_filled`, dan controller `TopLevelAccessibilityPreferenceController` di dalam `top_level_support_category` pada urutan `-20`.
   - `top_level_settings_expressive.xml`: Menambahkan kembali `top_level_accessibility` dengan ikon `@drawable/ic_homepage_accessibility` di dalam `top_level_support_category`.
2. **Perbaikan Kartu Diperluas Dynamic Island (`frameworks/base/packages/SystemUI`)**:
   - `DynamicIslandChargingView.kt`:
     * Memperbaiki urutan inisialisasi kritis di `showExpandedCard()`: Mengekstrak `populateExpandedCard(slot)`, dan menjamin `isExpandedCardShowing = true` sebelum menampilkan tata letak turunan. Sebelumnya, `updateExpandedCardIfShowing()` langsung berhenti lebih awal melalui `if (!isExpandedCardShowing) return`, meninggalkan ketiga sub-tampilan (`expandedRecordContent`, `expandedTorchContent`, `expandedChargingContent`) dalam status `View.GONE` sehingga menghasilkan kotak hitam kosong.
     * Menyembunyikan kapsul ringkas (`capsuleView` dan `metaBallView`) saat kartu mengambang ditampilkan, dan memulihkannya kembali secara mulus saat ditutup.
     * Menyempurnakan `stopScreenRecording()` menggunakan `PendingIntent.getService(...)` dengan flag cadangan `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`.
   - `dynamic_island_expanded_card.xml`:
     * Mengubah `expanded_card_container` dari `LinearLayout` menjadi `FrameLayout` dengan `android:layout_gravity="center_vertical"` pada semua tata letak turunan untuk menghilangkan masalah pengukuran lebar horizontal dan memastikan tampilan penuh yang rapi.
3. **Perbaikan Dual-Slot Meta-Ball & Deteksi Senter Dynamic Island**:
   - `DynamicIslandChargingView.kt`:
     * Memperbaiki bug visibilitas meta-ball: Animasi penutupan sebelumnya melewatkan reset `alpha = 1.0f` di `withEndAction`, meninggalkan meta-ball transparan secara permanen pada `alpha = 0f` setelah ditutup. Kini secara eksplisit menjamin `alpha = 1.0f`, `scaleX = 1.0f`, dan `scaleY = 1.0f` setiap kali `metaSlot != IslandSlot.NONE`.
     * Menambahkan pelacakan status senter dengan redundansi tiga tingkat:
       1. `FlashlightController.addCallback(flashlightListener)`
       2. Panggilan langsung `CameraManager.registerTorchCallback(cameraTorchCallback, mainHandler)` untuk semua ID kamera
       3. `ContentObserver` pada `Settings.Secure.getUriFor(Settings.Secure.FLASHLIGHT_ENABLED)`
       4. Evaluasi sinkron langsung di `refreshIslandState()`: `isTorchOn || controllerTorch || secureTorch`.
     * Memperbaiki penanganan sentuhan `GestureDetector` dengan meng-override `onDown(e: MotionEvent): Boolean = true` dan `onSingleTapUp(e: MotionEvent): Boolean` untuk menjamin responsivitas instan tanpa penundaan ketukan.
   - `status_bar.xml`:
     * Menambahkan `android:clipChildren="false"` dan `android:clipToPadding="false"` ke `PhoneStatusBarView` untuk memastikan tampilan pulau multi-gelembung tidak pernah terpotong pada batas status bar.
4. **Panjang Kapsul & Simetri Perekam Layar Dynamic Island (Gaya iOS 200dp)**:
   - `dynamic_island_charging.xml`:
     * Mendesain ulang `island_record_content` dengan `minWidth="200dp"`, menghilangkan tampilan yang terlalu pendek/sempit dan menyelaraskannya dengan lebar seimbang kapsul pengisian daya.
     * Arsitektur sayap ganda yang simetris: Sayap kiri memuat titik indikator merah berdenyut (`gravity="start"`), bagian tengah menyediakan ruang kosong 36dp tepat di posisi punch-hole kamera, dan sayap kanan menampung penghitung waktu digital merah tebal (`00:00`) serta ikon rekam (`gravity="end"`). Desain ini memastikan lubang kamera tidak pernah menutupi konten dan menjaga simetri Dynamic Island ala iOS.

---

## Bagian XIX: Penyempurnaan Teks Dynamic Island di Pengaturan & Perbaikan Penghentian Perekaman Layar

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**:
  1. Teks Dynamic Island pada Pengaturan masih bertuliskan "Dynamic Island charging", padahal fiturnya telah diperluas mencakup senter dan perekam layar.
  2. Lokalisasi teks masih belang-belang antara bahasa Indonesia dan Inggris (misalnya teks waktu perekaman yang sebelumnya bertuliskan "Sisa Waktu").
  3. Bug pada Dynamic Island saat perekaman layar: menekan tombol kotak stop hanya menghilangkan pulau/island sementara proses perekaman di latar belakang tetap berjalan. Tombol pause juga tidak berfungsi menjeda rekaman.

### 2. Detail Implementasi
1. **Pembaruan String & Judul Pengaturan (`packages/apps/Settings`)**:
   - `shinkai_strings.xml` (EN): Memperbarui `dynamic_island_title` menjadi "Dynamic Island" dan `dynamic_island_summary` menjadi "Interactive punch-hole capsule for charging, flashlight, and screen recording (Experimental)".
   - `values-in/shinkai_strings.xml` (ID): Memperbarui `dynamic_island_title` menjadi "Dynamic Island" dan `dynamic_island_summary` menjadi "Kapsul interaktif notch kamera untuk pengisian daya, senter, dan perekam layar (Eksperimental)".
2. **Penyelarasan Lokalisasi SystemUI (`frameworks/base/packages/SystemUI`)**:
   - `values-in/shinkai_strings.xml`: Memperbaiki label `dynamic_island_record_elapsed` menjadi "Waktu Berjalan" (menggantikan "Sisa Waktu" yang keliru), dan menyelaraskan `dynamic_island_record_label` menjadi "Perekam Layar".
   - `values/shinkai_strings.xml`: Memperbarui `dynamic_island_record_label` menjadi "Screen Recording".
3. **Perbaikan Penghentian Perekaman Layar Berlapis (Multi-Layer Stop Dispatch)**:
   - `DynamicIslandChargingView.kt`: Mengatasi akar masalah di mana pemanggilan tunggal `stopRecording()` pada `RecordingController` mengembalikan nilai tanpa aksi jika `mStop` bernilai null sambil tergesa-gesa mengubah status `isRecording` menjadi false. Menggantikannya dengan pipa penghentian redundan 5 lapis:
     * Lapisan 1: Memicu langsung `actionIntent` (PendingIntent berhenti) dari notifikasi perekam layar aktif (`channelId == "screen_record"`).
     * Lapisan 2: Mengirim intent `ScreenRecordingService.ACTION_STOP` dengan `EXTRA_STOP_REASON = STOP_HOST_APP` ke `ScreenRecordingService` (layanan coroutine/flow Android 15/17).
     * Lapisan 3: Mengirim intent `STOP_FROM_NOTIF` dengan `currentUserId` ke layanan `RecordingService` warisan (legacy).
     * Lapisan 4: Mengirim PendingIntent interaktif dengan `BroadcastOptions` aktif untuk melewati batasan eksekusi latar belakang.
     * Lapisan 5: Menjalankan `screenRecordController?.stopRecording(...)` sebagai cadangan framework.
4. **Penyederhanaan Kartu Diperluas Rekam Layar (Gaya Asli iOS)**:
   - `dynamic_island_expanded_card.xml`: Menghapus tombol jeda (`pause`) yang tidak didukung secara native oleh arsitektur AOSP MediaProjection, dan membesarkan tombol Stop lingkaran merah (44dp dengan ikon kotak 18dp) sehingga fokus kontrol menjadi jelas, responsif, dan sesuai dengan Dynamic Island asli iOS.

---

## Bagian XX: Penyempurnaan Form Factor Dynamic Island (Kapsul Rekam Kompak, Senter Minimalis, dan Meta-Ball Sisi Kiri)

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**:
  1. Ukuran kapsul perekam layar (Screen Record) dibuat ringkas seperti senter agar tidak terlalu panjang.
  2. Teks label senter ("Senter" / "Flashlight") dihapus pada kapsul compact Dynamic Island, sehingga hanya menampilkan logo senter dan teks status "On" saja.
  3. Posisi gelembung sekunder meta-ball dipindahkan ke sebelah KIRI kapsul utama (bukan di sebelah kanan).

### 2. Detail Implementasi
1. **Relokasi Meta-Ball ke Sisi Kiri (`dynamic_island_charging.xml`)**:
   - Memindahkan kontainer FrameLayout `island_meta_ball` ke urutan pertama di dalam `island_root_container`, mendahului `island_capsule`.
   - Mengubah margin pemisah dari `android:layout_marginStart="8dp"` menjadi `android:layout_marginEnd="8dp"`, sehingga saat mode dual-aktivitas aktif (misalnya senter dan rekam layar berjalan bersamaan), gelembung bundar meta-ball otomatis berlabuh di sebelah kiri kapsul utama.
2. **Penyederhanaan Kapsul Senter Menjadi Minimalis**:
   - `dynamic_island_charging.xml`: Menghapus teks judul senter (`island_torch_text`) dan spacer pemisah tengah 24dp. Menyisakan ikon vektor senter (`16dp`, warna amber `#FFD60A`) dan teks status "On" (`island_torch_action_text`, tebal, 13sp) dengan margin awal 8dp serta padding samping 12dp/14dp.
   - `values-in/shinkai_strings.xml`: Memperbarui nilai string `dynamic_island_torch_on` dari "Menyala" menjadi "On" untuk menghasilkan tampilan antarmuka yang seragam dan bersih.
3. **Kapsul Perekam Layar Kompak (Compact Sizing)**:
   - `dynamic_island_charging.xml`: Menghilangkan atribut lebar paksa `minWidth="200dp"`, spacer tengah lubang kamera 36dp, dan struktur sayap ganda. Menggantikannya dengan tata letak horizontal ringkas sejajar yang memuat titik rekam merah berdenyut (`11dp`) dan penghitung waktu digital tebal (`00:00`) dengan padding simetris 12dp/14dp. Hal ini menghasilkan kapsul perekam yang ringkas dan seimbang secara visual dengan kapsul senter.
4. **Pembersihan Kode Framework (`DynamicIslandChargingView.kt`)**:
   - Menghapus referensi deklarasi dan inisialisasi `torchTextView` (`R.id.island_torch_text`) yang sudah tidak lagi digunakan pada tata letak kapsul ringkas.

---

## Bagian XXI: Desain Kartu Dynamic Island OriginOS (Lebar Penuh Layar, Ikon Vektor Kustom, dan Transisi Fisika Mulus)

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**:
  1. Desain kartu mengambang (expanded) Dynamic Island saat perekaman layar dirombak menyerupai gaya antarmuka OriginOS (sesuai foto referensi pengguna).
  2. Kartu diperlebar agar mepet dengan sisi kanan dan kiri layar (menghilangkan lebar kaku 326dp dan menggantinya dengan lebar adaptif penuh layar bersisi margin 10dp).
  3. Mengganti semua ikon bawaan Material dengan ikon vektor buatan sendiri yang lebih modern dan orisinal.
  4. Menghaluskan animasi transisi saat menutup kartu diperluas dan saat melakukan penukaran prioritas kapsul (swap).

### 2. Detail Implementasi
1. **Tata Letak Kartu Lebar Penuh Bergaya OriginOS (`dynamic_island_expanded_card.xml`)**:
   - Mengubah kontainer `expanded_island_root` dan `expanded_card_container` menjadi `match_parent` dengan margin sisi 10dp, menempel presisi di dekat tepi layar ponsel.
   - Mengatur ulang tata letak rekam layar menjadi 2 baris bergaya OriginOS:
     * Baris atas: Lencana merah transparan di kiri berisi ikon kamera perekam kustom (`16dp`, `#FF3B30`) dan penghitung waktu digital (`00:00`), serta judul "Merekam layar" di sisi kanan.
     * Baris bawah: Kelompok kiri berisi 3 tombol melingkar kaca buram (Gelombang Suara Sistem, Mikrofon, dan Pena Anotasi) berukuran 38dp; kelompok kanan berisi tombol Putar/Jeda melingkar putih solid (`42dp`, bilah vertikal hitam ganda) dan tombol Berhenti berlingkar tepi putih dengan kotak merah di tengahnya (`42dp`).
   - Kartu Senter dan Pengisian Daya juga diselaraskan menjadi lebar penuh yang proporsional.
2. **Aset Ikon Vektor Kustom Mandiri (`res/drawable`)**:
   - `ic_origin_rec_cam.xml`: Ikon kamera video modern dengan sudut bodi membulat dan lensa trapesium.
   - `ic_origin_sound_wave.xml`: Ikon gelombang spektrum suara 5-batang vertikal dengan ujung melengkung.
   - `ic_origin_mic.xml`: Ikon kapsul mikrofon elegan dengan penyangga melingkar.
   - `ic_origin_pencil.xml`: Ikon stylus pena gambar modern miring 45 derajat.
   - `ic_origin_pause.xml`: Ikon dua bilah jeda hitam solid.
   - `ic_origin_play.xml`: Ikon segitiga putar hitam solid untuk melanjutkan perekaman yang dijeda.
   - `ic_origin_stop.xml`: Ikon kotak merah membulat (`#FF3B30`).
   - `ic_origin_torch.xml`: Ikon senter modern amber kustom dengan sakelar tengah.
   - `dynamic_island_origin_stop_bg.xml`: Latar tombol stop lingkaran gelap dengan garis batas cincin putih solid 2.2dp.
   - `dynamic_island_origin_btn_circle.xml`: Latar lingkaran kaca buram semi-transparan untuk kontrol pendukung.
   - `dynamic_island_origin_record_badge.xml`: Lencana kapsul merah semi-transparan berpenanda waktu.
3. **Peningkatan Kelancaran Transisi (`DynamicIslandChargingView.kt`)**:
   - **Transisi Menutup (Dismiss)**: Menetapkan titik tumpu animasi (`pivotX = displayWidth / 2f`, `pivotY = 0f`) tepat di tengah kamera punch-hole. Kartu menyusut secara mulus ke atas dengan kurva `PathInterpolator(0.4f, 0f, 0.2f, 1f)`. Pada saat bersamaan, kapsul ringkas dan meta-ball dipulihkan dengan animasi muncul lembut berskala (`scaleX 0.75f -> 1.0f`) dan interpolator elastis (`OvershootInterpolator`), menghilangkan sentakan/kedipan mendadak secara permanen.
   - **Transisi Tukar Posisi (Swap)**: Mengimplementasikan simulasi fisika fluida metaball 2-fase. Saat digeser, kapsul dan meta-ball meluncur saling mendekati secara horizontal (`translationX`), mengecil dan melembutkan transparansi secara simultan selama 130ms, lalu bertukar status dan meluncur kembali ke posisi netral dengan pantulan elastis (`OvershootInterpolator`), menghasilkan gerakan organik seperti cairan yang menyatu dan memisah.
   - **Interaktivitas Perekaman**: Menghubungkan tombol jeda dengan status pembekuan waktu langsung serta respon getar haptic pada semua tombol kontrol.



---

## Bagian XXII: Optimalisasi Geometri Kapsul Punch-Hole, Supresi Chip Rekam Bawaan Android, dan Integrasi Kontrol Pengaturan Perekaman Layar pada Expanded Island

### 1. Kebutuhan & Gambaran Umum
- **Latar Belakang & Masalah**:
  1. **Geometri Kapsul Punch-Hole Tertutup Kamera**: Pada perangkat berlubang kamera tengah (seperti POCO F5 / `marble`), kapsul compact senter dan perekam layar terlalu pendek sehingga logo senter, teks status "On", titik merah perekam, dan waktu durasi tertutup oleh punch-hole fisik kamera.
  2. **Duplikasi Indikator Status Bar (Chip Perekam Android)**: Saat Dynamic Island aktif merekam, chip/pill merah perekaman bawaan Android (Ongoing Activity Chip) di sebelah kanan jam status bar tetap muncul bersamaan, menyebabkan redundansi visual yang mengganggu. Chip bawaan seharusnya disupresi saat Dynamic Island aktif dan hanya muncul kembali jika fitur Dynamic Island dinonaktifkan oleh pengguna.
  3. **Penyesuaian Kontrol Expanded Island**: Fitur jeda (`pause`) tidak memiliki dukungan native pada arsitektur pipeline MediaProjection AOSP, dan tombol ornamen pendukung lainnya kurang fungsional. Seluruh kontrol selain tombol Stop diubah menjadi 6 opsi pengaturan perekaman layar bawaan Android yang interaktif: Rekam audio perangkat, Rekam mikrofon, Tampilkan sentuhan, Kualitas lebih rendah (SD), Batas ukuran file lebih besar, dan Pengodean HEVC (yang secara default aktif otomatis dan berfungsi). Tombol Stop tetap dipertahankan dan berfungsi penuh.

### 2. Detail Implementasi
1. **Pelebaran Kapsul & Pembersihan Ruang Punch-Hole (`dynamic_island_charging.xml`)**:
   - Memperbesar batas minimum lebar kapsul (`minWidth="142dp"`) dengan bantalan padding awal dan akhir (`paddingStart="14dp"`, `paddingEnd="14dp"`).
   - Menerapkan arsitektur simetris sayap ganda (`minWidth="38dp"` per sayap) dengan spacer penyeimbang kamera tengah (`island_torch_spacer` dan `island_record_spacer` masing-masing selebar `38dp`) yang membentang tepat di balik area cutout punch-hole fisik.
   - Logo senter dan titik rekam berlabuh rapi di sayap kiri, sedangkan teks "On" dan waktu durasi (`00:00`) berlabuh di sayap kanan, sehingga konten informasi tidak lagi tertutup atau terpotong oleh kamera depan.
   - Spacer pengisian daya (`island_spacer`) juga diperlebar menjadi `36dp` untuk menjaga konsistensi clearance di seluruh status island.
2. **Supresi Chip Perekam Bawaan Android Berbasis Status Dynamic Island (`ScreenRecordChipViewModel.kt` & `ScreenRecordChipViewModelKosmos.kt`)**:
   - Menyuntikkan `SystemSettingsRepository` ke dalam `ScreenRecordChipViewModel` untuk memantau nilai pengaturan `Settings.System.DYNAMIC_ISLAND_CHARGING` secara reaktif melalui Kotlin StateFlow.
   - Memadukan kondisi chip: ketika Dynamic Island aktif (`dynamic_island_charging == 1`), `ScreenRecordChipViewModel` memancarkan `OngoingActivityChipModel.Inactive()`, sehingga chip merah bawaan di sebelah jam status bar disembunyikan sepenuhnya.
   - Jika pengguna mematikan Dynamic Island di Pengaturan (`dynamic_island_charging == 0`), chip perekaman bawaan Android otomatis aktif dan muncul kembali secara normal.
   - Memperbarui fixture pengujian Kosmos (`ScreenRecordChipViewModelKosmos.kt`) untuk menyertakan ketergantungan `systemSettingsRepository`.
3. **Kartu Kontrol Expanded Rekam Layar dengan 6 Opsi Pengaturan Cepat (`dynamic_island_expanded_card.xml`)**:
   - Menghapus tombol jeda dan tombol ornamen gelombang suara/pena yang tidak fungsional.
   - Memindahkan tombol Stop merah solid OriginOS (`expanded_record_stop_btn`, 38dp) ke bilah header kanan atas kartu, memudahkan penghentian rekaman dengan satu ketukan.
   - Menambahkan 6 baris sakelar pengaturan perekaman layar bawaan bergaya `@style/ScreenRecord.Switch`:
     * Sakelar Audio Perangkat (`expanded_record_switch_device_audio`)
     * Sakelar Mikrofon (`expanded_record_switch_mic`)
     * Sakelar Tampilkan Sentuhan (`expanded_record_switch_touches`)
     * Sakelar Kualitas Lebih Rendah / SD (`expanded_record_switch_low_quality`)
     * Sakelar Batas Ukuran File Lebih Besar (`expanded_record_switch_longer_duration`)
     * Sakelar Pengodean HEVC (`expanded_record_switch_hevc`, default ON)
4. **Logika Reaktif & Persistensi Pengaturan di Framework (`DynamicIslandChargingView.kt`)**:
   - Menghubungkan seluruh sakelar secara langsung ke penyimpanan SharedPreferences bawaan SystemUI (`Prefs`) dan `Settings.System.SHOW_TOUCHES`.
   - Menginisialisasi sakelar HEVC otomatis aktif (`Prefs.getInt(context, "screenrecord_use_hevc", 1) == 1`) dan memperbarui preferensi secara persisten setiap kali sakelar diubah.
   - Menghubungkan tombol Stop dengan intent penghentian rekaman bawaan (`RecordingService.REQUEST_STOP`) dan memberikan respon haptic pada setiap interaksi.
5. **Lokalisasi Multibahasa (`shinkai_strings.xml` & `values-in/shinkai_strings.xml`)**:
   - Menambahkan string lokalisasi lengkap Bahasa Indonesia dan Bahasa Inggris untuk seluruh label opsi perekaman layar (`screen_record_show_touches_title`, `screen_record_low_quality_title`, `screen_record_longer_duration_title`, `screen_record_hevc_title`).

---

## Bagian XXIII: Integrasi Kartu Pratinjau Interaktif Kecepatan Refresh (Refresh Rate Animated Preview Card) pada Pengaturan Layar

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**:
  - Menambahkan kartu pratinjau animasi kecepatan refresh layar pada menu Setelan > Layar > Kecepatan Refresh (`Settings > Display > Refresh Rate`), menampilkan visualisasi pembanding fluiditas gerakan antara 60 Hz, 90 Hz, dan 120 Hz sesuai foto referensi pengguna.
  - Kartu pratinjau ditempatkan di bagian paling atas halaman di atas opsi radio button, menampilkan puck/lingkaran geser bergerak untuk tiap tingkatan refresh rate, dengan baris aktif yang terpilih disorot dengan kontainer pill hijau gelap, teks hijau mint cerah, dan puck hijau mint.

### 2. Detail Implementasi
1. **Komponen Visual Pratinjau Interaktif (`RefreshRatePreviewView.java`)**:
   - Merancang custom view turunan `LinearLayout` yang secara dinamis membangun baris pratinjau sesuai refresh rate yang didukung perangkat (`60 Hz`, `90 Hz`, `120 Hz`).
   - Setiap baris memiliki label frekuensi tebal di sisi kiri dan lintasan geser horizontal (`trackLayout`) dengan puck bundar (`20dp`) di sisi kanan.
   - Mengimplementasikan `ValueAnimator` independen untuk tiap baris dengan interpolator `AccelerateDecelerateInterpolator` dan mode `REVERSE`, menggerakkan puck bolak-balik secara kontinu dengan durasi proporsional terhadap refresh rate (2200ms untuk 60Hz, 1500ms untuk 90Hz, 1100ms untuk 120Hz) guna mendemonstrasikan kelancaran dan kecepatan frame pacing secara nyata.
   - Baris yang sedang aktif/dipilih otomatis dibungkus dengan latar belakang pill membulat (`14dp`, `#0D2E1C`), warna teks hijau mint (`#6BD598`), serta puck hijau mint bercahaya, sedangkan baris yang tidak dipilih menggunakan gaya minimalis dengan puck abu-abu netral (`#787E7A`).
   - Garis pemisah horizontal antar baris disembunyikan secara cerdas pada baris yang berbatasan dengan pill aktif agar tampilan tetap bersih dan elegan.
   - Menambahkan event listener sentuhan pada kartu: mengetuk baris mana saja di dalam kartu pratinjau akan langsung mengubah kecepatan refresh sistem, memperbarui radio button di bawahnya, dan memberikan umpan balik getar haptic.
   - Mengelola siklus hidup animasi secara efisien (`onAttachedToWindow`, `onDetachedFromWindow`, `onVisibilityChanged`, `onResume`, `onPause`) agar animasi berhenti saat aplikasi diminimalkan atau layar mati, sehingga menghemat konsumsi daya baterai.
2. **Aset Drawable & Tata Letak Antarmuka (`res/drawable` & `res/layout`)**:
   - `refresh_rate_preview_card_bg.xml`: Latar kartu membulat 20dp gelap pekat (`#131614`) dengan garis tepi halus (`#1E2420`).
   - `refresh_rate_preview_selected_pill_bg.xml`: Bentuk pill hijau gelap (`#0D2E1C`) bersudut membulat 14dp untuk menandai baris yang aktif.
   - `refresh_rate_preview_puck_grey.xml`: Lingkaran puck abu-abu solid untuk tingkat refresh rate non-aktif.
   - `refresh_rate_preview_puck_green.xml`: Lingkaran puck hijau mint solid (`#6BD598`) untuk tingkat refresh rate aktif.
   - `refresh_rate_preview_preference.xml`: Tata letak kontainer preferensi dengan padding simetris 16dp.
3. **Integrasi ke Fragment Pengaturan (`RefreshRateSettings.java`)**:
   - Mengaitkan `LayoutPreference` dengan urutan teratas (`setOrder(-100)`) dan menyetel `setOrderingAsAdded(false)`, memastikan kartu pratinjau selalu berada di posisi paling atas di atas daftar pilihan radio button.
   - Menghubungkan perubahan nilai refresh rate di radio button (`setDefaultKey`) dengan pembaruan visual real-time pada `RefreshRatePreviewView`.
   - Mengintegrasikan callback pemilihan langsung dari kartu pratinjau ke alur `onRadioButtonConfirmed(key)` bawaan `RadioButtonPickerFragment`.

---

## Bagian XXIV: Rollback Desain Dynamic Island Rekam Layar ke Gaya Asli OriginOS

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**:
  - Membatalkan (rollback) integrasi 6 sakelar pengaturan perekaman bawaan AOSP pada kartu expanded Dynamic Island karena membuat desain melenceng dari konsep estetika aslinya.
  - Mengembalikan seluruh tata letak Dynamic Island perekaman layar ke gaya murni OriginOS yang presisi, minimalis, dan elegan.
  - Memastikan kapsul ringkas (pill) tetap kompak dan kartu expanded menampilkan tata letak kamera badge, penghitung durasi rekam, tombol jeda/lanjutkan, tombol berhenti cincin putih beraksen merah, serta 3 tombol alat bantu bundar di baris bawah.

### 2. Detail Implementasi
1. **Pemulihan Tata Letak Expanded Card OriginOS (`dynamic_island_expanded_card.xml`)**:
   - Menghapus 6 baris sakelar konfigurasi perekaman bawaan AOSP yang memadati kartu expanded.
   - Mengembalikan tata letak 2 baris murni OriginOS:
     * Baris Atas: Badge kamera OriginOS dengan titik merah berdenyut halus dan teks penghitung waktu durasi berjalan di sisi kiri; tombol Jeda/Lanjutkan (lingkaran putih solid dengan ikon bar hitam) dan tombol Berhenti (bingkai cincin putih membulat dengan kotak merah di tengah) di sisi kanan.
     * Baris Bawah: Tiga tombol alat bantu bundar berdiameter 36dp berlatar belakang gelap transparan (`#1AFFFFFF`) dengan ikon kustom modern (mikrofon, indikator sentuhan/pena, dan gelombang suara audio).
   - Menjaga tinggi kartu expanded tetap proporsional (92dp) dengan sudut melengkung 28dp (`dynamic_island_expanded_bg.xml`) dan margin simetris yang mepet rapi ke tepi kiri-kanan layar.
2. **Pemulihan Kapsul Ringkas (`dynamic_island_charging.xml`)**:
   - Mengembalikan ukuran kapsul utama yang ringkas dan pas (`paddingStart="12dp"`, `paddingEnd="14dp"`), sejajar dengan tampilan senter tanpa pelebaran berlebih.
   - Menjaga posisi metaball cairan organik tetap di sebelah kiri sesuai preferensi pengguna sebelumnya.
3. **Pembersihan Logika & Handler Pengendali (`DynamicIslandChargingView.kt`)**:
   - Menghapus listener sakelar pengaturan AOSP dan mengembalikan logika status kontrol OriginOS.
   - Memulihkan manajemen status jeda (`isRecordPaused`) dengan penghitungan akumulasi jeda (`pausedAccumulatedMs`) agar waktu yang ditampilkan tetap sinkron dan akurat saat rekaman dilanjutkan.
   - Mempertahankan sistem multi-layer dispatch (5 layer) untuk penghentian perekaman layar yang andal dan tuntas.
   - Mempertahankan supresi otomatis chip status bar bawaan AOSP agar tidak terjadi duplikasi indikator visual saat Dynamic Island aktif.
4. **Pembersihan Aset & String Sumber Daya**:
   - Menghapus aset ikon sakelar yang tidak lagi digunakan (`ic_hevc.xml`, `ic_sr_quality.xml`, `ic_storage.xml`, `ic_touch.xml`).
   - Membersihkan entri string sakelar di `shinkai_strings.xml` dan `values-in/shinkai_strings.xml` sehingga sumber daya tetap ramping dan teratur.
---

## Bagian XXV: Proporsionalitas Ukuran Kapsul dan Penyederhanaan Kartu Rekam Layar OriginOS

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**:
  - Ukuran kapsul ringkas (pill) sebelumnya terasa terlalu kecil dan terhimpit. Pengguna menginginkan ukuran kapsul diperbesar menjadi proporsional (~120dp) dengan ruang penyeimbang kamera tengah agar seimbang dan tidak terpotong.
  - Fitur jeda (pause) serta tombol alat bantu lainnya tidak didukung pada alur perekaman MediaRecorder AOSP sehingga hanya bersifat dummy dan membingungkan pengguna. Pengguna memilih untuk menyederhanakan kartu expanded OriginOS agar hanya menampilkan elemen yang benar-benar berfungsi nyata (tombol Stop, live timer, dan status rekaman), tanpa tombol palsu.

### 2. Detail Implementasi
1. **Penyesuaian Geometri Kapsul Proporsional (~120dp) (`dynamic_island_charging.xml`)**:
   - Menetapkan batas lebar minimum `minWidth="120dp"` pada kontainer pill senter (`island_torch_content`) dan rekam layar (`island_record_content`).
   - Menerapkan arsitektur simetris sayap kiri (34dp) untuk ikon/titik rekam, spacer tengah (32dp) untuk clearance lubang kamera punch-hole fisik, dan sayap kanan (34dp) untuk teks status/timer berjalan dengan padding simetris 12dp.
   - Menyelaraskan spacer pengisian daya (`island_spacer`) menjadi 32dp untuk memastikan konsistensi visual di seluruh mode Dynamic Island.
2. **Penyederhanaan Kartu Expanded Rekam Layar OriginOS (`dynamic_island_expanded_card.xml`)**:
   - Menghapus baris tombol alat bantu dan kontrol jeda yang tidak berfungsi di AOSP (gelombang suara, mikrofon, pena, dan tombol jeda).
   - Menata ulang kartu menjadi tata letak satu baris terfokus yang sangat elegan dan konsisten dengan kartu senter dan pengisian daya:
     * Kiri: Badge kamera OriginOS sirkular 48dp (`dynamic_island_origin_record_badge.xml`) dengan ikon kamera merah (`ic_origin_rec_cam`) dan titik indikator rekam berdenyut (`ic_dynamic_island_record_dot`).
     * Tengah: Teks judul "Merekam Layar" / "Screen Recording" dan subteks durasi berjalan dengan live timer presisi (`expanded_record_timer`).
     * Kanan: Tombol Berhenti (Stop) bundar 48dp khas OriginOS (`dynamic_island_origin_stop_bg`) dengan cincin putih elegan dan kotak merah (`ic_origin_stop`) yang responsif terhadap ketukan.
3. **Penyederhanaan Logika Pengendali Framework (`DynamicIslandChargingView.kt`)**:
   - Menghapus seluruh listener dan variabel status jeda (`isRecordPaused`, `pausedAccumulatedMs`, `pauseStartTimestamp`) serta tombol dummy auxiliary.
   - Mengaitkan tombol Stop secara eksklusif ke fungsi `stopScreenRecording()` dengan respon getar haptic dan animasi dismiss mulus.
   - Menyederhanakan kalkulasi timer rekam layar langsung dari `recordingStartTimestamp` sistem waktu nyata (`SystemClock.elapsedRealtime()`).
4. **Pembersihan Aset Grafis Tidak Terpakai**:
   - Menghapus 7 berkas drawable yang sebelumnya digunakan untuk tombol dummy: `dynamic_island_origin_btn_circle.xml`, `dynamic_island_origin_pause_bg.xml`, `ic_origin_pause.xml`, `ic_origin_play.xml`, `ic_origin_pencil.xml`, `ic_origin_mic.xml`, dan `ic_origin_sound_wave.xml`.

---

## Bagian XXVI: Fisika Animasi Evasif Status Bar dan Transisi Fluida Asli OriginOS

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**:
  - Animasi ekspansi (expand) dan penutupan (dismiss) kartu Dynamic Island sebelumnya terasa terlalu statis, cepat, dan kaku (hanya menimpa status bar secara langsung).
  - Pada perangkat asli OriginOS, status bar memiliki perilaku animasi fisik yang sangat lembut dan organik:
    * Saat island mengembang (expand), konten status bar di sisi kiri (jam dan ikon notifikasi) meluncur halus keluar ke arah kiri (`translationX < 0` dan fade out), sedangkan konten status bar di sisi kanan (baterai, sinyal seluler, dan Wi-Fi) meluncur halus keluar ke arah kanan (`translationX > 0` dan fade out).
    * Saat island ditutup kembali (dismiss), kedua sisi status bar meluncur kembali ke posisi semula secara anggun dan alami.
  - Memperhalus kurva fisika dan memperpanjang durasi animasi ekspansi/penutupan kartu agar terasa lembut, elastis, dan mengalir seperti cairan (liquid physics) khas OriginOS.

### 2. Detail Implementasi
1. **Penemuan & Pengikatan Sisi Status Bar (`DynamicIslandChargingView.kt`)**:
   - Menambahkan pengikatan dinamis ke kontainer status bar bawaan AOSP melalui hierarki `PhoneStatusBarView`:
     * Sisi Kiri / Start: `status_bar_start_side_content` (membungkus jam dan area ikon notifikasi).
     * Sisi Kanan / End: `status_bar_end_side_content` (membungkus indikator baterai, sinyal jaringan, dan Wi-Fi).
   - Menyediakan metode `ensureStatusBarSideViews()` yang aman dan reaktif untuk menjamin referensi elemen status bar selalu siap kapan pun island berinteraksi.
2. **Animasi Evasif Status Bar Saat Expand (`animateStatusBarSidesOnExpand`)**:
   - Menggeser kontainer sisi kiri sejauh 56dp ke arah kiri (`translationX = -56dp`) disertai fade out halus (`alpha = 0f`) selama 420ms dengan kurva percepatan fluida `PathInterpolator(0.32f, 0.0f, 0.12f, 1.0f)`.
   - Menggeser kontainer sisi kanan sejauh 56dp ke arah kanan (`translationX = +56dp`) disertai fade out halus (`alpha = 0f`) selama 420ms dengan kurva yang sama.
   - Efek ini memberikan ilusi optik seolah-olah pulau Dynamic Island yang membesar secara fisik mendorong dan menyisihkan konten status bar ke tepian layar.
3. **Animasi Pemulihan Status Bar Saat Dismiss (`animateStatusBarSidesOnCollapse`)**:
   - Mengembalikan kontainer sisi kiri dari -56dp kembali ke posisi normal (`translationX = 0f`) dengan fade in (`alpha = 1f`) selama 440ms menggunakan kurva redaman pegas lembut `PathInterpolator(0.18f, 1.0f, 0.35f, 1.0f)`.
   - Mengembalikan kontainer sisi kanan dari +56dp kembali ke posisi normal (`translationX = 0f`) dengan fade in (`alpha = 1f`) secara serempak.
   - Menambahkan metode pengaman `resetStatusBarSides()` pada siklus hidup `onDetachedFromWindow()` dan skenario pembatalan darurat agar status bar sistem tidak pernah tertinggal dalam kondisi tersembunyi.
4. **Penyempurnaan Fisika Fluida Kartu Expanded & Kapsul**:
   - Memperhalus animasi pembukaan kartu (`expandedCardContainer`) dengan durasi 440ms (sebelumnya 320ms) dan interpolator kurva cairan elastis `PathInterpolator(0.22f, 1.0f, 0.36f, 1.0f)` berpangkal dari cutout punch-hole kamera (`pivotY = 0f`, `scale` 0.32x ke 1.0x).
   - Memperhalus animasi penutupan kartu dengan durasi 380ms (sebelumnya 240ms) dan kurva hisap `PathInterpolator(0.38f, 0.0f, 0.2f, 1.0f)`.
   - Memperhalus animasi pemulihan kapsul ringkas (`capsuleView`) dan metaball cairan (`metaBallView`) dari 260ms menjadi 400ms dengan interpolator redaman lembut.
---

## Bagian XXVII: Peningkatan Fisika Fluida Squash & Stretch dan Deteksi Gestur Geser pada Meta-Ball

### 1. Kebutuhan & Gambaran Umum
- **Permintaan Pengguna**:
  - Animasi perpindahan (swap/geser) antara kapsul utama dan meta-ball sebelumnya masih terlalu singkat (130ms) dengan jarak perpindahan yang minim (12dp), sehingga terkesan melompat kaku tanpa sensasi cairan.
  - Pengguna menginginkan peningkatan (enhancement) animasi geser meta-ball agar terasa seperti cairan hidup (liquid physics) khas OriginOS.
  - Mengintegrasikan efek renggang-pipih cairan (squash & stretch), jarak luncur penuh ke dalam kapsul (~38dp), umpan balik getar haptic yang renyah saat melebur, serta deteksi geser santai (drag/scroll) yang lebih peka.

### 2. Detail Implementasi
1. **Deteksi Gestur Geser Responsif (`onScroll` & `onFling`)**:
   - Memperluas detektor gestur pada kapsul utama (`gestureDetector`) dan meta-ball (`metaGestureDetector`) dengan penanganan `onScroll`.
   - Mengaktifkan pertukaran prioritas saat terdeteksi geseran horizontal santai melebihi ambang batas 16dp-18dp tanpa harus melakukan kibasan cepat (`fling`), sehingga pengguna dapat menggeser gelembung dengan santai dan presisi.
   - Mengunci status `isSwappingDualIsland` selama animasi berlangsung untuk mencegah pemicuan ganda yang tidak diinginkan.
2. **Fase 1: Tarikan Magnetik, Renggang-Pipih Cairan (Squash & Stretch), dan Peleburan**:
   - Menggerakkan gelembung meta-ball sejauh 38dp langsung melebur ke dalam kapsul utama selama 190ms dengan kurva percepatan fluida `PathInterpolator(0.35f, 0.0f, 0.15f, 1.0f)`.
   - Menerapkan deformasi cairan realistis: meta-ball merenggang secara horizontal (`scaleX = 1.36x`) dan memipih secara vertikal (`scaleY = 0.70x`) di sepanjang sumbu gerak, mensimulasikan tetesan cairan (droplet) yang ditarik gaya gravitasi/magnetik kapsul.
   - Kapsul utama bereaksi timbal balik dengan bergerak ke arah meta-ball (-14dp) dan memampat (`scaleX = 0.92x`, `scaleY = 1.06x`) saat menyerap tetesan cairan.
3. **Umpan Balik Taktil Haptic di Titik Fusi**:
   - Memicu getaran haptic yang renyah (`HapticFeedbackConstants.CONTEXT_CLICK`) tepat pada milidetik ketika meta-ball menyatu sepenuhnya ke dalam kapsul, memberikan umpan balik fisik yang memuaskan di tangan.
4. **Fase 2: Pelepasan Tetesan Baru & Pemulihan Elastis Pegas**:
   - Tetesan meta-ball sekunder yang baru memancar keluar dari kapsul ke arah kiri dengan lonjakan vertikal elastis (`scaleX = 0.72x`, `scaleY = 1.28x`) dari posisi -22dp.
   - Menstabilkan tetesan ke bentuk bulat sempurna (1.0x) selama 400ms menggunakan kurva redaman pegas `OvershootInterpolator(1.35f)`.
   - Mengembalikan kapsul utama ke posisi netral dan skala normal selama 380ms dengan kurva fluida `PathInterpolator(0.18f, 1.0f, 0.35f, 1.0f)`.
5. **Animasi Ketukan Taktil pada Meta-Ball (`onMetaBallClicked`)**:
   - Menambahkan efek denyut kenyal saat meta-ball diketuk (memampat ke 0.85x selama 100ms dan memantul kembali ke 1.0x dengan `OvershootInterpolator(1.4f)` selama 220ms) disertai respon getar haptic instan.
