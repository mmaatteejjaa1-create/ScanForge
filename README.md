# 🔷 ScanForge — Android 3D Scanner

Dark tech, production-grade Android app za 3D skeniranje kamerom.

## Ekrani

| Ekran | Opis |
|-------|------|
| 🏠 Home | Nedavni modeli, statistike, izbor moda |
| 📸 Photogrammetry | CameraX snimanje 30+ kadrova + vodič |
| 🤖 AI Depth | Live MiDaS depth heatmap + instant sken |
| ⚙️ Processing | Animirani progress sa step-by-step prikazom |
| 🥽 AR Viewer | SceneView/ARCore prikaz sa transform toolima |
| ⬇ Export | 6 formata (.glb .gltf .obj .stl .fbx .ply) |

## Tech stack

```
Kotlin + Jetpack Compose
CameraX          — kamera preview + capture
ARCore + SceneView — AR rendering
TFLite (MiDaS)  — AI depth estimation
Hilt             — Dependency Injection
Room             — lokalna baza modela
Navigation       — type-safe navigacija
```

## Setup

### 1. Kloniraj i otvori u Android Studio

```bash
git clone <repo>
# Minimalni SDK: 26 (Android 8.0)
```

### 2. Dodaj MiDaS TFLite model

Preuzmi `midas_v2_1_small.tflite` sa:
https://tfhub.dev/intel/midas/v2_1_small/1

Stavi u: `app/src/main/assets/midas_v2_1_small.tflite`

### 3. ARCore

ARCore je označen kao **optional** u manifestu — app radi i bez njega.
Za potpunu AR podršku: https://developers.google.com/ar/develop/java/quickstart

### 4. Photogrammetry (napredna opcija)

Za pravu rekonstrukciju iz više kadrova, postoje 2 opcije:

**A) Cloud API** — pošalji snimke na server sa OpenMVG/Meshroom  
**B) Lokalno JNI** — kompajliraj OpenMVG kao `.so` biblioteku:
```bash
# Vidi scripts/build_openmvg_android.sh
```

U fajlu `Processors.kt`, `PhotogrammetryProcessor.reconstruct()` je stub koji čeka JNI poziv.

### 5. Build

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/
```

## Arhitektura

```
UI Layer (Compose Screens)
    ↓
ViewModel (StateFlow)
    ↓
Repository (Room + File System)
    ↓
Processors (CameraX, TFLite, JNI)
```

## Export formati

| Format | Koristi |
|--------|---------|
| `.glb`  | Web, Blender, Unity, Unreal — preporučeno |
| `.gltf` | WebGL, Three.js — sa eksternim fajlovima |
| `.obj`  | Blender, Maya, 3ds Max — universalno |
| `.stl`  | 3D print (PrusaSlicer, Cura) |
| `.fbx`  | Unity, Unreal, Maya — game engines |
| `.ply`  | Istraživanje, CloudCompare, MeshLab |

## Struktura projekta

```
app/src/main/java/com/scanforge/
├── ScanForgeApp.kt          ← Hilt DI + Application
├── MainActivity.kt          ← NavHost
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt
│   │   ├── PhotogrammetryScreen.kt
│   │   ├── AiDepthScreen.kt
│   │   ├── ProcessingScreen.kt
│   │   ├── ArViewerScreen.kt
│   │   └── ExportScreen.kt
│   ├── components/
│   │   └── Components.kt    ← GlassCard, PrimaryButton, ScanProgressRing...
│   └── theme/
│       └── Theme.kt         ← ScanColors, ScanTypography, MaterialTheme
├── viewmodel/
│   └── ViewModels.kt        ← Home/Photogrammetry/AiDepth/Processing/AR/Export
├── data/
│   └── model/
│       └── Models.kt        ← Room entities + DAO + Database
└── processing/
    └── Processors.kt        ← PhotogrammetryProcessor, DepthProcessor, ModelExporter
```
