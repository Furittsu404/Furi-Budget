---
version: 1
slug: "app-src-main-kotlin-app-furibudget-ui-theme"
primary_target: "app/src/main/kotlin/app/furibudget/ui/theme"
related_targets: []
---

# Theme system and Appearance (M1)

Mode: Operate (budget tool), with permission to push expression in motion and theming because the brief pins it.

## Direction contract
THESIS: The look is the user's. One accent axis plus a pack axis plus a background axis, all live-previewed, all legible. Refuses: a single house style with a dark/light toggle.
OWN-WORLD: Glass pack "Tidepool": ink-teal base #07121C, slow gradient-mesh blobs (teal, indigo, magenta at low alpha), frosted cards (white 8% fill, 1dp 16% white border, soft offset shadow), Manrope numerals tabular. Neon pack "Afterhours": near-black #05050A, flat #0E0E18 cards with an accent outline, Sora, accent glow only on the focused/primary element. Accent derived in OKLCH so any pick keeps 4.5:1 text.
STORY: The user opens Appearance, taps a pack or accent swatch, sees the whole app (preview card and live background) retint in 300 ms, and trusts every combination is readable.
FIRST VIEWPORT: Appearance screen. Top: live preview card (balance, budget ring placeholder) over the real background. Below: pack carousel, accent swatch grid (4x4 plus Dynamic, Custom), mode chips, background chips, motion slider. Back arrow top-left; no FAB.
FORM: Material 3 settings screen with a preview header; pinned by "customizable themes and background, eye candy animations, go all out"; seed: none (pinned).
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
