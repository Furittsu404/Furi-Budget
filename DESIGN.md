---
name: Furi Budget
description: A personal budget tracker whose look belongs to the user: five theme packs, sixteen accents, live backgrounds, motion as a feature.
colors:
  accent-iris: "#8B7CFF"
  tidepool-ink: "#07121C"
  tidepool-ink-text: "#EAF2F7"
  tidepool-ink-muted: "#9FB3C2"
  tidepool-frost: "#EAF3F7"
  tidepool-frost-text: "#0E1E2A"
  tidepool-frost-muted: "#50657A"
  tidepool-teal-blob: "#0FA3A3"
  tidepool-indigo-blob: "#4B4DED"
  afterhours-black: "#05050A"
  afterhours-card: "#0E0E18"
  afterhours-edge: "#23233A"
  afterhours-text: "#F2F2FA"
  afterhours-muted: "#9A9AB8"
  ledger-sheet: "#F3F5F8"
  ledger-paper: "#FFFFFF"
  ledger-edge: "#E1E6ED"
  ledger-text: "#141922"
  ledger-muted: "#5A6575"
  void-black: "#000000"
  void-card: "#0A0A0C"
  void-edge: "#222226"
  void-text: "#F5F5F7"
  void-muted: "#8E8E98"
  gumdrop-night: "#1A1330"
  gumdrop-night-text: "#F5EEFF"
  gumdrop-night-muted: "#D2C9EA"
  gumdrop-lilac: "#F1ECFF"
  gumdrop-lilac-text: "#2A2140"
  gumdrop-lilac-muted: "#6C6385"
  gumdrop-pink-blob: "#FF7DB0"
  gumdrop-mint-blob: "#5BE0C0"
  income-green: "#22C55E"
  expense-rose: "#F43F5E"
  warning-amber: "#F59E0B"
  income-blue-cb: "#3B82F6"
  expense-orange-cb: "#F97316"
  warning-yellow-cb: "#EAB308"
typography:
  display:
    fontFamily: "Manrope, Sora, Fredoka, serif, sans-serif"
    fontSize: "36px"
    fontWeight: 400
    lineHeight: "44px"
  headline-figures:
    fontFamily: "Manrope, Sora, Fredoka, serif, sans-serif"
    fontSize: "28px"
    fontWeight: 400
    lineHeight: "36px"
    fontFeature: "tnum"
  title-large:
    fontFamily: "Manrope, Sora, Fredoka, serif, sans-serif"
    fontSize: "22px"
    fontWeight: 400
    lineHeight: "28px"
  title:
    fontFamily: "Manrope, Sora, Fredoka, serif, sans-serif"
    fontSize: "16px"
    fontWeight: 500
    lineHeight: "24px"
  body:
    fontFamily: "Manrope, Sora, Fredoka, serif, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
  body-small:
    fontFamily: "Manrope, Sora, Fredoka, serif, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: "16px"
  label:
    fontFamily: "Manrope, Sora, Fredoka, serif, sans-serif"
    fontSize: "14px"
    fontWeight: 500
    lineHeight: "20px"
rounded:
  tidepool: "28px"
  afterhours: "14px"
  ledger: "20px"
  void: "12px"
  gumdrop: "32px"
  pill: "9999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  gutter: "20px"
  xl: "24px"
components:
  card:
    backgroundColor: "{colors.tidepool-ink}"
    rounded: "{rounded.tidepool}"
  keypad-key:
    rounded: "{rounded.tidepool}"
    height: "56px"
  nav-bar:
    rounded: "{rounded.tidepool}"
    height: "68px"
    padding: "6px"
  nav-pill-selected:
    rounded: "{rounded.pill}"
  tx-row:
    height: "64px"
    padding: "8px 4px"
  tx-row-glyph:
    size: "44px"
    rounded: "{rounded.pill}"
---

# Design System: Furi Budget

## Overview

**Creative North Star: "The Wardrobe"**

The look is the user's. The system is one set of roles (page, card, text, accent, income, expense, warning) that every screen reads through `Furi.colors`, plus three axes the user turns independently: pack, accent, background. Any combination stays readable because roles are derived, not picked: accents are re-tuned in OKLCH against the worst surface text can land on, and neutrals are pushed to contrast floors. The app does not have a house style with a dark/light toggle; it has five wardrobes.

Material 3 supplies structure (ColorScheme, Typography, Shapes, dialogs, sheets, text fields). Brand lives in the theme roles: `FuriTheme` maps the derived palette into a Material scheme, swaps the type family per pack, and scales the five Material shape steps from one per-pack corner value. Screens sit directly on the background; the card is the one container, and list rows (transactions) sit bare on the page.

Motion is a feature, not a coat. Springs differ by pack (Gumdrop bounces, Afterhours snaps), digits roll when money changes, rings and charts sweep in, water rises in goals. All of it multiplies through one user slider and drops to zero under system Remove animations.

**Key Characteristics:**
- Five packs: Tidepool (glass), Afterhours (neon outline), Ledger (paper, light only), Void (true black, dark only), Gumdrop (pastel, bouncy).
- Sixteen named accent swatches plus Dynamic (Material You), Custom and Photo-picked; default Iris.
- Six backgrounds: mesh, aurora, particles, gradient, solid, photo (blur and dim).
- Tabular figures for every money amount.
- Semantic income, expense and warning with a colorblind-safe alternate set.

## Colors

Each pack is a fixed neutral set; the accent is the only free variable. Hex values below are the pack constants; accent roles are computed.

### Primary
- **Iris** (`accent-iris`): the default accent swatch. Only a starting point: the fill actually drawn is the swatch re-lit into a lightness band (dark themes 0.62-0.86 OKLCH L, light themes 0.40-0.66) at the swatch's own chroma and hue.

### Secondary
- **Second stop**: the accent hue rotated +38 degrees at the same L and C, used when the gradient accent is on (ring sweep, accent brush). Equals the accent when off.

### Tertiary
- **Mesh blobs**: two fixed per-pack colours (Tidepool teal and indigo, Gumdrop pink and mint, Afterhours deep indigo and plum) plus a third that follows the accent at the pack's blob strength (0.10 Void to 0.55 Tidepool light).

### Neutral
- **Tidepool Ink** (`tidepool-ink`) with text `tidepool-ink-text` and muted `tidepool-ink-muted`: dark glass page. Light twin: `tidepool-frost`, `tidepool-frost-text`, `tidepool-frost-muted`.
- **Afterhours Black** (`afterhours-black`), card `afterhours-card`, edge `afterhours-edge`, text and muted as named. Dark only.
- **Ledger Sheet** (`ledger-sheet`), card `ledger-paper`, edge `ledger-edge`, text and muted as named. Light only.
- **Void Black** (`void-black`), card `void-card`, hairline `void-edge`. Dark only; mesh blobs fully transparent, default background solid.
- **Gumdrop Night** (`gumdrop-night`) and **Gumdrop Lilac** (`gumdrop-lilac`) with their text and muted values; card is white at 28% (dark) or 90% (light).
- **Semantic**: `income-green`, `expense-rose`, `warning-amber`; colorblind set `income-blue-cb`, `expense-orange-cb`, `warning-yellow-cb`. These are seeds: each is pushed to 5.5:1 on the page colour and 3.2:1 on the worst blob surface.

### Named Rules
**The Derived Accent Rule.** No screen uses a raw swatch. Accent fill, on-accent, container, on-container, text-accent and glow all come from `AccentDerivation.derive` against the worst surface; accent text holds 4.5:1, on-container 7:1.
**The Worst Surface Rule.** Contrast is measured against the page colour composited with the most hostile blob (aurora ribbons count at 60%), not the bare page. Body text holds 7:1, muted 4.6:1.
**The Photo Flip Rule.** Over a photo background, light or dark text is chosen from photo luminance after dim, not from the system theme.
**The One Edge Rule.** Pack neutrals carry their own border colour; accent edges appear only in the outline card style.

## Typography

**Display Font:** Manrope (Tidepool), Sora (Afterhours), Fredoka (Gumdrop); bundled variable fonts, weights 400-800.
**Body Font:** same family as the pack. Ledger uses the system serif and Void the system sans; these are placeholders until their intended faces are bundled.
**Label/Mono Font:** none; figures use the pack face with `tnum`.

**Character:** One family per pack applied to every Material text style, so a pack change retypes the whole app. Sizes stay on the stock Material 3 scale; personality comes from the face, not from a custom ramp.

### Hierarchy
- **Display** (400, 36/44): hero balance moments (displaySmall, rare).
- **Headline / Figures** (400, 28/36, tabular): all money amounts via `Furi.figures`; keypad digits, ring centres.
- **Title Large** (400, 22/28): screen titles.
- **Title** (500, 16/24): card headings and list titles; titleSmall size reused for row amounts.
- **Body** (400, 14/20; large 16/24): default reading text; bodySmall 12/16 for row subtitles.
- **Label** (500, 14/20; medium 12/16): nav item, chips, buttons.

### Named Rules
**The Tabular Rule.** Every money figure uses `Furi.figures` (`tnum`) so digits align and tickers do not jitter.
**The Roll Rule.** Changing money rolls digit by digit (`AnimatedAmount`); non-digits stay still; first show rolls up from zeros.

## Layout

Single-column phone layout, screens on the live background. Dominant horizontal gutter is 20dp; stacks use 8, 12 and 16dp gaps, with 4dp for tight pairs and 24dp between sections. Touch targets are 48dp or more: keypad keys 56dp (46dp when system font scale exceeds 1.15), nav bar 68dp, transaction rows at least 64dp. Navigation is a floating bar over content. The Appearance screen leads with a live preview card over the real background, then pack carousel, accent grid (4x4 plus Dynamic and Custom), mode and background chips, motion slider.

## Elevation & Depth

Depth is tonal and optical, not shadowed. Glass cards use a vertical-fade fill (full card alpha to 60%) and a 1dp lit border fading to 25% at the bottom. Outline and flat cards are opaque fills with a 1dp edge. The only drop shadow is on flat cards in light themes (Ledger, light Gumdrop): 8dp, ambient `#14000000`, spot `#26000000`. Accent glow is a colour role, used sparingly on primary or focused elements.

### Named Rules
**The Pack Owns Depth Rule.** Depth treatment is chosen by the pack's card style (GLASS, OUTLINE, FLAT); screens never add their own shadows or borders.

## Shapes

One corner value per pack scales five Material steps: extraSmall = c/3, small = c/2, medium = 0.7c, large = c, extraLarge = c+8dp. Cards use large. Keypad keys use medium. The selected nav item and glyph discs are full pills or circles. Corner values: Tidepool 28dp, Afterhours 14dp, Ledger 20dp, Void 12dp, Gumdrop 32dp. Ring and donut strokes use round caps; donut slices are separated by a 2.5 degree gap.

## Components

### Card (FuriCard)
The app's only container. Style by pack: Tidepool glass, Afterhours accent outline (accent at 45%), others flat with the pack edge. Caller supplies inner padding. Optional click.

### Keypad
3x4 grid, 8dp gaps. Keys are card-filled with a 1dp edge at 60% strength, dip to 0.93 scale on press with a spring, fill with accent container while pressed, and fire a haptic tick. Long-press backspace clears.

### Navigation (FuriNavBar)
A floating FuriCard, 68dp tall, 6dp inner padding. The selected item widens (weight 2.6 vs 1, spring) into an accent-container pill with icon and label; others show icon only, tinted on-surface-variant.

### Transaction row (TxRow)
No card. 44dp circular glyph on 20% category tint, title (bodyLarge), subtitle (bodySmall, muted), right-aligned tabular amount in income, expense or muted (transfer) colour. Expense uses a true minus sign.

### Budget ring (BudgetRing)
14dp stroke ring on a 12% on-surface track. OK sweeps an accent-to-accent2 gradient from 12 o'clock; CLOSE switches to warning; OVER fills to 360 in expense with a pulsing halo (alpha 0.25-0.7, 900ms). Sweeps with the pack spring.

### Liquid fill (LiquidFill)
Goal progress as rising water: two sine waves, 3200ms drift, wave height 3.5% of the box, flat when motion is off.

### Charts
Donut (ring stroke 17% of size, tap pops a slice, others dim to 45%), paired income/expense bars (150dp tall), trend line (170dp tall, 35% to transparent area fill). Entrance sweep 900ms. Series colours come from `chartSeries`: evenly spaced hues around the accent, L 0.74 dark or 0.55 light, 3:1 floor.

### Backgrounds
Mesh and aurora run an AGSL shader on API 33+ and fall back to drifting radial blobs; particles twinkle; photo takes blur up to 24dp (API 31+) and a 0.3-0.9 dim scrim; optional tilt parallax.

### Motion
Per-pack spring (damping, stiffness): Tidepool 0.8/220, Afterhours 0.9/500, Ledger 1.0/420, Void 0.95/450, Gumdrop 0.45/260. Stiffness scales by 0.6 + 0.4 x intensity; tweens default 320ms (colour changes 360ms) scaled by 1.2 - 0.2 x intensity. Intensity 0 or system Remove animations makes every spec instant.

## Do's and Don'ts

### Do:
- **Do** read colour from `Furi.colors` and shape from `MaterialTheme.shapes`; never hard-code a hex in a screen.
- **Do** wrap grouped content in `FuriCard` and leave list rows bare on the page.
- **Do** run every animation through `Furi.motion` (`spring`, `tween`) so the slider and Remove animations apply.
- **Do** show money with `Furi.figures` and `AnimatedAmount`.
- **Do** keep 48dp minimum touch targets and a 20dp screen gutter.
- **Do** derive any new accent-based colour with `AccentDerivation` against the worst surface.

### Don't:
- **Don't** ship a single house style with a dark/light toggle; a new surface must look right in all five packs.
- **Don't** offer Ledger in dark or Afterhours and Void in light; those packs are locked by palette.
- **Don't** use raw swatch colours for text or thin strokes.
- **Don't** put shadows, borders or fills on a screen that the pack's card style should supply.
- **Don't** animate outside `Furi.motion` or ignore the intensity setting.

## Not canonized (build defects or placeholders)
- Ledger (system serif) and Void (system sans) use platform faces as placeholders; the type table records them as fallbacks, not as a decision.
- Transaction rows fall back to hard-coded `#94A3B8` for a missing category colour; not a system token.
- Custom screens carry some one-off dp values (14, 168dp) outside the spacing scale; the scale above lists only the reused steps.
