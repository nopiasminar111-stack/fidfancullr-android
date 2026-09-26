# FidFanCullr — Android

## Update 2: real font + settings bug fix + completion flow

- **Google Sans Flex is now really bundled** (`res/font/google_sans_flex.ttf`,
  ~1.1 MB). It's a genuine variable font (axis `wght` 100–900), so
  Light/Regular/Medium/SemiBold/Bold/Black are all real instances of that
  one file via `FontVariation.weight(...)` — no faux-bold, no duplicate font
  files. It's the app-wide typeface now (`FidFanTypography` in `Type.kt`).
- **Fixed the Settings "items disappearing" issue.** The actual cause: the
  Accent Palette section was conditionally *hidden* whenever dynamic color
  was on (or on Android <12), which is exactly the kind of "Settings
  structure shrinks" bug described. It's now always visible — just
  disabled/greyed out when not applicable — so the number of visible
  Settings items never changes based on other settings or OS version.
- **Completion screen redesigned**: when there's nothing left to sort (or
  the folder had nothing supported to begin with), you get a clear "All
  done!" state with a primary **Choose another folder** button that opens
  the folder picker directly and starts a new session — no dead end.

## Update: M3 Expressive redesign + performance pass

- **Look & feel**: bigger rounded corners (M3 Expressive shapes), pill-shaped
  sort buttons, Pixel-style dynamic color on Android 12+ (falls back to the
  4 accent palettes on older versions or if turned off in Settings), proper
  edge-to-edge with system bar insets handled everywhere.
- **Settings screen redesign**: now grouped into clearly separated cards
  (Inbox, Theme, Language, Sorting) instead of one long list of chips.
- **RAW preview, bigger**: instead of only the small EXIF thumbnail, the app
  now scans each RAW file for the largest embedded JPEG preview (most RAW
  formats store more than one — a tiny EXIF thumbnail *and* a much larger
  "camera LCD" preview) and shows that. See the "RAW preview" section below
  for what this can and can't do.
- **Async decode + caching + preload**: decoding happens off the main thread,
  results are cached in memory (see Memory management below), and the
  next/previous image's preview is preloaded in the background so paging
  through photos feels instant after the first view.
- **Loading / error states**: each image now shows a spinner while decoding
  and a retry button if decoding fails, instead of silently showing nothing.
- **Smoother gestures**: swipe-to-sort and pinch-zoom/pan now use
  `Animatable` with spring physics (snaps back smoothly if a swipe doesn't
  clear the threshold), and swipe-to-sort is disabled while zoomed in so the
  two gestures never fight each other.
- **Fewer recompositions**: the image/gesture area is now its own composable
  so fast-changing drag/zoom state doesn't recompose the top bar, EXIF panel,
  or sort buttons on every frame.

### Google Sans Flex — not bundled yet

I could not safely bundle real Google Sans Flex in this pass: doing it right
needs either the actual `.ttf` files (no network access here to fetch them)
or Google Play Services' Downloadable Fonts API, which requires a provider
certificate hash that must exactly match Google's — a hand-typed guess at
that hash fails silently rather than loudly, which is worse than not trying.
Typography currently uses the platform's sans-serif with M3 Expressive's
bolder weights/sizes. To add the real font later:

1. Download the 4 weights you want from
   [Google Fonts – Google Sans Flex](https://fonts.google.com/specimen/Google+Sans+Flex)
2. Add them under `app/src/main/res/font/` (e.g. `google_sans_flex_regular.ttf`)
3. In `ui/theme/Type.kt`, replace `FontFamily.SansSerif` with a `FontFamily(Font(R.font.google_sans_flex_regular), ...)`

### RAW preview: what "largest embedded preview" means

Camera RAW files (NEF, CR2/CR3, ARW, DNG, RAF, ORF, RW2, PEF, SRW) typically
embed more than one JPEG inside the container: a tiny thumbnail (~160×120,
for file browsers) and often a much larger preview (sometimes near full
resolution) used for the camera's own LCD/EVF display. `EmbeddedJpegScanner`
scans the raw bytes for every JPEG segment and keeps the largest one, which
is what gets shown. This is **not** a full sensor-data RAW decode (that
needs a native decoder like LibRaw via the NDK) — a small number of RAW
files only carry the tiny thumbnail and nothing bigger, in which case the
app falls back to that.

### Memory management

- Every decode goes through `PreviewLoader`, which first reads the target
  image's real dimensions, then decodes at an `inSampleSize` that fits the
  device's screen resolution — a 45 MP RAW preview never gets fully decoded
  into memory just to be shown on a 1080p screen.
- Decoded bitmaps are kept in `BitmapMemoryCache`, an `LruCache` sized to
  1/8th of the app's available heap (the standard Android-recommended
  sizing), evicting the oldest entries automatically under memory pressure.
- Preloading the next/previous image reuses this same cache and the same
  downsampling, so it doesn't add uncapped memory growth.


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
