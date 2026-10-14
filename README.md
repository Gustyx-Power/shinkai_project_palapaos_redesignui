# Shinkai Project - WowoOS Redesign UI (POCO F5 & Generic Android 17)

Repositori ini berisi kode sumber antarmuka, layout XML, aset drawable, skrip kernel, dan konfigurasi sistem untuk desain ulang antarmuka terinspirasi One UI 9 serta fitur Dynamic Island pada Shinkai Project / WowoOS Android 17 (POCO F5 / marble dan perangkat AOSP lainnya).

---

## Ikhtisar Komponen & Fitur

### 1. Dynamic Island Interaktif
- **Penyelarasan Punch-Hole POCO F5**: Disesuaikan dengan posisi kamera depan tengah perangkat POCO F5 (`device/xiaomi/marble`).
- **Fisika Pegas (Spring Overshoot)**: Animasi ekspansi horizontal dan retraksi elastis dua fase saat muncul dan menghilang.
- **Tiga Peristiwa Utama**:
  - **Pengisian Daya (Charging)**: Tampilan persentase baterai real-time dan animasi petir hijau neon (durasi 7.8 detik).
  - **Senter (Flashlight)**: Kapsul aktif selama senter menyala. Ketuk sekali untuk mematikan senter.
  - **Perekam Layar (Screen Record)**: Kapsul simetris (~200dp) dengan indikator titik merah berkedip di kiri, spacer bebas kamera punch-hole di tengah, dan digital timer berjalan di kanan.
- **Dual-Island Meta-Ball**: Saat senter dan perekam layar aktif bersamaan, perekam layar menjadi pulau utama dan senter menjadi gelembung bundar di sampingnya.
- **Gestur Usap (Swipe to Swap)**: Usap horizontal pada pulau untuk menukar prioritas antara perekam layar dan senter.
- **Kartu Mengambang (Expanded Island Card)**: Ketukan pada kapsul membuka kartu kontrol mengambang dengan tombol tindakan cepat (Stop, Pause, Matikan Senter) dan timer digital besar.

### 2. Quick Settings One UI 9 (Jetpack Compose)
- **Tampilan Tarik Satu Kali (QQS / 1-Pull)**:
  - Baris 1: 6 tombol cepat berbentuk lingkaran (1x1).
  - Baris 2: Slider kecerahan halus tanpa flicker.
  - Baris 3: 2 tombol pill besar (2x1) untuk Wi-Fi dan Bluetooth.
  - Pemutar media terintegrasi saat audio aktif.
- **Tampilan Tarik Penuh (Expanded QS / 2-Pull)**:
  - Kartu kontainer kaca buram (frosted glass) untuk tombol cepat.
  - Slider volume media terintegrasi dan tombol pemilih mode suara (Dering, Getar, Hening).
  - Pemutar musik responsif dan pintasan daya serta pengaturan.
- **Mode Edit 2D Bebas**:
  - Seret dan susun ulang posisi tombol secara bebas (horizontal maupun vertikal).
  - Tombol 'Selesai' mengambang di bagian bawah layar untuk kemudahan jangkauan satu tangan.

### 3. Desain Ulang Menu Pengaturan (Settings)
- **Dashboard Beranda Cepat (Quick Dashboard)**:
  - Kartu pemantau status cepat: Baterai, Penyimpanan, RAM, Tampilan, dan Suara.
  - Tombol Notifikasi: Panel akses cepat langsung ke pengaturan notifikasi.
  - Menu Aksesibilitas: Tetap tersedia utuh pada daftar pengaturan utama.
- **Colored Notification Icons**:
  - Opsi untuk menampilkan ikon asli aplikasi yang berwarna penuh pada status bar dan panel notifikasi.
- **Halaman Tentang Ponsel (About Phone)**:
  - Banner visual bertema Shinkai Project dengan kartu spesifikasi real-time (prosesor, arsitektur CPU, refresh rate layar, kamera) dan kredit kontributor.

### 4. Performa Sistem, Kernel & Audio
- **Dynamic RAM Expansion (zRAM)**:
  - Pengaturan ukuran swap zRAM nyata yang terhubung langsung ke kernel Linux (Pilihan: Nonaktif/0 GB, 2 GB, 4 GB, 8 GB, 16 GB).
- **Hardware Dolby Atmos Asli**:
  - Integrasi DSP perangkat keras bawaan dengan aplikasi kontrol MarbleDolby (AutoEQ dan 10-band equalizer) tanpa memerlukan root atau modul KernelSU.
- **Kustomisasi Navigasi**:
  - Opsi untuk menyembunyikan pill bilah navigasi bawah dan menghilangkan jarak kosong di bawah keyboard.

---

## Struktur Repositori

```
.
|-- README.md
|-- changelogs.md
|-- apply.sh
|-- temp.sh
|-- build/
|   `-- blueprint/bootstrap/
|-- device/
|   `-- xiaomi/
|       `-- marble/
|           |-- rootdir/bin/ram_expansion.sh
|           |-- rootdir/bin/init.kernel.post_boot.sh
|           |-- rootdir/etc/init.marble.rc
|           `-- sepolicy/vendor/
|-- frameworks/
|   `-- base/
|       `-- packages/
|           |-- SystemUI/
|           |   |-- compose/features/src/com/android/systemui/qs/ui/composable/
|           |   `-- src/com/android/systemui/
|           `-- overlays/
`-- packages/
    `-- apps/
        `-- Settings/
            |-- res/
            `-- src/
```

---

## Panduan Pemasangan

### Metode 1: Skrip Otomatis (apply.sh)

Gunakan skrip `apply.sh` untuk menerapkan perubahan secara otomatis:

1. **Masuk ke direktori repositori**:
   ```bash
   cd shinkai_project_wowoos_redesignui
   chmod +x apply.sh
   ```

2. **Jalankan sesuai target yang diinginkan**:

   - **A. Untuk Perangkat Lain (Hanya UI / Tanpa Device Tree)**:  
     Gunakan mode ini jika Anda mengompilasi untuk perangkat selain POCO F5 agar device tree bawaan perangkat Anda tidak tertimpa:
     ```bash
     ./apply.sh /path/to/rom --ui-only
     ```

   - **B. Untuk POCO F5 (marble) Setup Lengkap**:  
     Menerapkan UI Redesign, Device Tree Marble, perbaikan Kernel zRAM, dan SELinux policy:
     ```bash
     ./apply.sh /path/to/rom --all
     ```

   - **C. Mode Interaktif**:  
     Jalankan tanpa argumen untuk memilih opsi melalui dialog interaktif:
     ```bash
     ./apply.sh
     ```

---

### Metode 2: Pemasangan Manual

Jika ingin menyalin berkas secara mandiri:

#### A. Hanya Desain Antarmuka (Kompatibel untuk Semua Perangkat):
```bash
cp -rf packages/apps/Settings/* /path/to/rom/packages/apps/Settings/
cp -rf frameworks/base/packages/* /path/to/rom/frameworks/base/packages/
```

#### B. Setup Lengkap POCO F5 (marble):
```bash
# 1. Desain Antarmuka
cp -rf packages/apps/Settings/* /path/to/rom/packages/apps/Settings/
cp -rf frameworks/base/packages/* /path/to/rom/frameworks/base/packages/

# 2. Device Tree Marble (RAM Expansion & SELinux)
cp -rf device/xiaomi/marble/* /path/to/rom/device/xiaomi/marble/

# 3. Skrip Build
cp -f temp.sh /path/to/rom/
```

---

## Panduan Kompilasi ROM

Setelah berkas disinkronkan ke direktori source ROM Anda:

```bash
cd /path/to/rom

# 1. Setup environment build Android
source build/envsetup.sh

# 2. Tentukan target perangkat
breakfast marble # atau lunch target perangkat Anda

# 3. Jalankan build ROM
./temp.sh # atau m shinkai -j$(nproc)

# Alternatif: Kompilasi SystemUI saja untuk pengetesan cepat
m SystemUI -j$(nproc)
```

---

## Catatan Perubahan (Changelog)

Detail teknis lengkap per sesi pengembangan dapat dilihat pada berkas [changelogs.md](changelogs.md).