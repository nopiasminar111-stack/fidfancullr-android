# FidFanCullr — Android

An Android redesign of the original Windows FidFanCullr photo-culling app,
built with Kotlin + Jetpack Compose and Material 3 Expressive styling.

## What's included

- **Onboarding** — pick a photo inbox folder (Storage Access Framework), choose theme/accent/language
- **Culling workspace** — large preview, pinch-to-zoom & pan, swipe-to-sort gestures, tap buttons as an alternative to swiping
- **Grouping** — files sharing a filename stem (`DSC_1042.JPG` + `DSC_1042.NEF`) are treated and moved as one group
- **EXIF panel** — camera, lens, ISO, shutter speed, aperture, focal length, date/time
- **RAW support** — reads EXIF metadata and the embedded preview/thumbnail from common RAW formats (NEF, CR2, CR3, ARW, DNG, RAF, ORF, RW2, PEF, SRW, and more)
- **Settings** — rename destination folders (KEEP/SELECT/REJECT or your own), reassign sorting keys, switch theme/accent (Violet Pixel, Ocean Blue, Mint Green, Coral Peach), switch language (English, Indonesian, Japanese, Spanish, Chinese)
- **Grouped move** — sorting a group moves every file in it together into a subfolder of your inbox, using `DocumentsContract.moveDocument` (fast, no re-encoding)

## Important limitation: RAW preview, not RAW decode

Full RAW pixel decoding (turning sensor data into a demosaiced image) needs a
native library such as **LibRaw** compiled via the NDK — that's a much bigger
undertaking than a UI rewrite. Instead, this app reads the **embedded JPEG
preview** that virtually every RAW file already contains (via
`androidx.exifinterface`), which is what most mobile culling apps do in
practice. Metadata (EXIF) is read directly and is fully accurate; the *image*
you see for a RAW-only file (no paired JPEG) is the camera's own embedded
preview, not a full-resolution RAW render.

If you later want true RAW decoding, the natural next step is bundling LibRaw
via the NDK, or shipping only the JPEG half of each pair for review while
keeping the RAW purely as the "keeper" file that gets moved alongside it.

## Opening the project

1. Install **Android Studio** (Koala or newer recommended).
2. `File → Open`, select the `FidFanCullrAndroid` folder.
3. Let Gradle sync (first sync downloads dependencies — needs internet).
4. Run on a device or emulator running **Android 8.0 (API 26)** or newer.

## Project structure

```
app/src/main/java/com/fidfanstudios/fidfancullr/
  data/          Models, PhotoGroup, AppSettings, DataStore-backed SettingsRepository
  util/          PhotoGrouper (SAF scan + grouping), ExifReader, FileMover
  viewmodel/     CullingViewModel, SettingsViewModel
  ui/theme/      Material 3 color schemes for the 4 accent palettes, light/dark
  ui/screens/    OnboardingScreen, CullingScreen, SettingsScreen
  MainActivity.kt  Navigation between the three screens
```

## Known gaps to build on

- Swipe gesture currently maps left/right to the first/last configured
  destination; a 3+ destination setup may want a direction picker UI.
- No hardware-keyboard key binding wiring yet (the `gestureKey` field exists
  in settings but isn't hooked to `onKeyEvent` — natural next addition for
  tablets with a keyboard).
- No batch operations or advanced metadata filtering (also on the original
  app's own roadmap).
- App icon uses the system default; add a real launcher icon via
  Android Studio's Image Asset tool.
