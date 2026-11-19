# Alur kerja tim PaduKampus

Setiap file memiliki satu pemilik utama. Ini mengurangi kemungkinan konflik; Git tidak dapat menjamin tidak ada konflik. Nama anggota mengikuti `KONTEKS_KELOMPOK.md`. Jangan menambahkan GitHub handle atau CODEOWNERS sampai username asli anggota diketahui.

## Batas kepemilikan

| Pemilik | Direktori atau file | Pekerjaan |
|---|---|---|
| Alfarisy — Frontend | `ui/`, `res/layout/`, `res/menu/`, `res/navigation/`, `res/values/strings_*.xml`, theme | Fragment, ViewModel, UiState, adapter, navigasi dan XML |
| Rantizi — Backend | `data/`, `core/`, `di/`, Gradle, manifest, `PaduKampusApp.kt`, `app/schemas/` | Kontrak repository, Room, API, network, DI, integrasi dan konfigurasi |
| Fadhli — Dokumentasi dan QA | `worker/`, `app/src/test/`, `app/src/androidTest/`, `docs/`, `scripts/`, `.github/`, laporan | Pengingat, pengujian, bukti, CI dan dokumentasi |

File bersama tetap mempunyai satu pemilik: `AppContainer.kt`, `AppConstants.kt`, Gradle dan manifest dikoordinasikan Rantizi; `nav_graph.xml` dan `MainActivity.kt` dikoordinasikan Alfarisy. Fadhli mengusulkan perubahan konstanta/channel/permission melalui PR, bukan mengedit file bersama bersamaan dengan backend. Dokumen konteks kelompok diperbarui Fadhli setelah kesepakatan anggota.

String dipisah per fitur (`strings_home.xml`, `strings_schedule.xml`, `strings_tasks.xml`, `strings_map.xml`, `strings_profile.xml`) agar semua UI tidak menumpuk di `strings.xml`. Satu entitas per file, DAO per domain, dan adapter terpisah menghindari file gabungan yang harus disentuh semua anggota.

## Urutan integrasi

1. **PR baseline**: sepakati interface repository, model UiState/relasi, namespace, dan graph sebelum pekerjaan fitur bercabang. Integrator adalah Rantizi.
2. **Frontend** mengembangkan fitur melalui interface repository. Untuk eksperimen UI gunakan fake di test/debug source set, bukan data kampus contoh yang ikut masuk build utama.
3. **Backend** melengkapi implementasi di balik interface yang disepakati. Perubahan signature membutuhkan persetujuan frontend dan pembaruan test pada PR yang sama.
4. **QA** memakai kontrak scheduler dan repository yang sama. Test fixture berada di source set test; pengujian tidak menulis data contoh ke database produksi.
5. Merge satu PR kecil setelah build, unit test, lint dan architecture guardrail lulus. Ambil perubahan main kembali sebelum memulai pekerjaan berikutnya.

## Branch dan sinkronisasi

Contoh nama branch: `frontend/task-form`, `backend/schedule-repository`, `qa/reminder-tests`. Sesuaikan nama remote/base branch bila repository tim memakai nama lain.

```bash
git switch main
git pull --ff-only
git switch -c frontend/task-form
# Kerjakan hanya file dalam cakupan; stage file yang disengaja.
git add app/src/main/java/com/unpad/padukampus/ui/tasks app/src/main/res/layout/fragment_task_form.xml
git commit -m "feat: improve task form"
git fetch origin
git rebase origin/main
```

Rebase hanya branch fitur milik sendiri yang belum menjadi dasar pekerjaan anggota lain. Jika branch dipakai bersama, koordinasikan merge dan hindari force push. Saat konflik terjadi, pemilik fitur dan pemilik file menyelesaikan perilakunya bersama, lalu menjalankan ulang pemeriksaan yang relevan. Jangan menyelesaikan konflik dengan memilih semua sisi ours/theirs tanpa membaca perubahan.

## Aturan database, dependency dan bukti

- Frontend tidak mengubah Entity/DAO untuk memperbaiki tampilan; minta penambahan kontrak yang diperlukan kepada backend.
- Ubah database dalam PR tersendiri: versi, Migration, schema JSON hasil KSP, serta test migrasi. Jangan edit JSON skema manual.
- Tambahan dependency atau perubahan Gradle dikerjakan integrator; baseline versi tidak dinaikkan sepihak.
- Jangan commit `.gradle/`, `build/`, `local.properties`, SDK/JDK, `.idea/`, atau kredensial. Jangan commit `google-services.json` sebelum kebijakan konfigurasi Firebase disepakati.
- Status pengujian harus menyebut pemeriksaan yang benar-benar dijalankan. APK tes yang berhasil dikompilasi belum berarti pengujian emulator/perangkat lulus.

## Pemeriksaan sebelum PR

```powershell
python scripts/verify_architecture.py
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Dengan perangkat/emulator yang tersambung, QA juga menjalankan `:app:connectedDebugAndroidTest` dan test case manual. CI yang disertakan menjalankan build/unit test/lint/kompilasi test APK, bukan pengujian perangkat.
