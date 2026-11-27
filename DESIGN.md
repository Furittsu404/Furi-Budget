---
name: Furi Budget
description: A personal budget tracker whose look belongs to the user: five theme packs, sixteen accents, live backgrounds, motion as a feature, bills on a calendar.
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
    fontFamily: "Manrope, Sora, Bricolage Grotesque, Hanken Grotesk, Fredoka, sans-serif"
    fontSize: "36px"
    fontWeight: 400
    lineHeight: "44px"
  headline-figures:
    fontFamily: "Manrope, Sora, Bricolage Grotesque, Hanken Grotesk, Fredoka, sans-serif"
    fontSize: "28px"
    fontWeight: 400
    lineHeight: "36px"
    fontFeature: "tnum"
  title-large:
    fontFamily: "Manrope, Sora, Bricolage Grotesque, Hanken Grotesk, Fredoka, sans-serif"
    fontSize: "22px"
    fontWeight: 400
    lineHeight: "28px"
  title:
    fontFamily: "Manrope, Sora, Bricolage Grotesque, Hanken Grotesk, Fredoka, sans-serif"
    fontSize: "16px"
    fontWeight: 500
    lineHeight: "24px"
  body:
    fontFamily: "Manrope, Sora, Bricolage Grotesque, Hanken Grotesk, Fredoka, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
  body-small:
    fontFamily: "Manrope, Sora, Bricolage Grotesque, Hanken Grotesk, Fredoka, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: "16px"
  label:
    fontFamily: "Manrope, Sora, Bricolage Grotesque, Hanken Grotesk, Fredoka, sans-serif"
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
  bill-row-glyph:
    size: "44px"
    rounded: "{rounded.pill}"
  calendar-cell:
    height: "54px"
  calendar-status-dot:
    size: "5px"
    rounded: "{rounded.pill}"
  upcoming-status-dot:
    size: "8px"
    rounded: "{rounded.pill}"
  widget-plus:
    size: "44px"
    rounded: "{rounded.pill}"
  widget-shell:
    rounded: "24px"
    padding: "14px"
---

# Design System: Furi Budget

## Overview

**Creative North Star: "The Wardrobe"**

The look is the user's. The system is one set of roles (page, card, text, accent, income, expense, warning) that every screen reads through `Furi.colors`, plus three axes the user turns independently: pack, accent, background. Any combination stays readable because roles are derived, not picked: accents are re-tuned in OKLCH against the worst surface text can land on, and neutrals are pushed to contrast floors. The app does not have a house style with a dark/light toggle; it has five wardrobes.

Material 3 supplies structure (ColorScheme, Typography, Shapes, dialogs, sheets, text fields). Brand lives in the theme roles: `FuriTheme` maps the derived palette into a Material scheme, swaps the type family per pack, lets the user firm up see-through cards (`ThemeSpec.cardAlpha`), and scales the five Material shape steps from one per-pack corner value. Screens sit directly on the background; the card is the one container, and list rows (transactions) sit bare on the page.

Motion is a feature, not a coat. Springs differ by pack (Gumdrop bounces, Afterhours snaps), digits roll when money changes, rings and charts sweep in, water rises in goals. All of it multiplies through one user slider and drops to zero under system Remove animations.

**Key Characteristics:**
- Five packs: Tidepool (glass), Afterhours (neon outline), Ledger (paper, light only), Void (true black, dark only), Gumdrop (pastel, bouncy).
- Sixteen named accent swatches plus Dynamic (Material You), Custom and Photo-picked; default Iris.
- Six backgrounds: mesh, aurora, particles, gradient, solid, photo (blur and dim).
- Tabular figures for every money amount.
- Semantic income, expense and warning with a colorblind-safe alternate set.
- One bill status vocabulary (overdue, due soon, upcoming, paid) shared by the calendar, bill rows, the Home card, the widget and notifications.
- A Glance home-screen widget that wears the pack and accent as colour only.

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
- **Card opacity**: for see-through packs (Tidepool, Gumdrop) the user may set card alpha 0.05-1.0 (`cardAlpha`). The fill is page colour lifted 10% toward white (dark) or white (light) at that alpha, never a bright fill under light text, so contrast only improves as the card firms up. Null keeps the pack default; opaque packs ignore it.
- **Semantic**: `income-green`, `expense-rose`, `warning-amber`; colorblind set `income-blue-cb`, `expense-orange-cb`, `warning-yellow-cb`. These are seeds: each is pushed to 5.5:1 on the page colour and 3.2:1 on the worst blob surface.

### Bill status vocabulary
One mapping, `statusColor` (ui/bills/BillCommon.kt), reads through `Furi.colors`:
- **Overdue**: expense colour.
- **Due today / due soon**: warning colour (same hue for both).
- **Upcoming**: `accentText`.
- **Paid**: income colour.
- **Skipped**: `onSurfaceVariant` (muted).
The colour tints the due label and a status dot; the due label text always says the state ("Overdue since yesterday", "Due in 3 days", "Paid on Nov 3"), so colour is never the only carrier on rows. Incoming bills of kind income say "received", not "paid".

### Named Rules
**The Status Colour Rule.** Bill state is coloured only through `statusColor`; a new surface never picks its own hue for overdue, soon, upcoming or paid.
**The Derived Accent Rule.** No screen uses a raw swatch. Accent fill, on-accent, container, on-container, text-accent and glow all come from `AccentDerivation.derive` against the worst surface; accent text holds 4.5:1, on-container 7:1.
**The Worst Surface Rule.** Contrast is measured against the page colour composited with the most hostile blob (aurora ribbons count at 60%), not the bare page. Body text holds 7:1, muted 4.6:1.
**The Photo Flip Rule.** Over a photo background, light or dark text is chosen from photo luminance after dim, not from the system theme.
**The One Edge Rule.** Pack neutrals carry their own border colour; accent edges appear only in the outline card style.
**The Widget Colour Rule.** The widget calls `resolveColors` with the saved pack and accent and the system night flag, and takes colour only: page colour at 94% alpha, `onSurface`, `onSurfaceVariant`, `accent`/`onAccent`, and the status tints. It never takes the animated background, glass fills, photo or pack springs, and it never hard-codes a hex.

## Typography

**Display Font:** Manrope (Tidepool), Sora (Afterhours), Bricolage Grotesque (Ledger), Hanken Grotesk (Void), Fredoka (Gumdrop); five bundled variable fonts, weights 400-800 (`FuriFonts`).
**Body Font:** same family as the pack.
**Label/Mono Font:** none; figures use the pack face with `tnum`.

**Character:** One family per pack applied to every Material text style, so a pack change retypes the whole app. Sizes stay on the stock Material 3 scale; personality comes from the face, not from a custom ramp. The widget is the exception: Glance uses the system face (bold 24sp balance, 11-13sp rows).

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

Single-column phone layout, screens on the live background. Dominant horizontal gutter is 20dp; stacks use 8, 12 and 16dp gaps, with 4dp for tight pairs and 24dp between sections. Touch targets are 48dp or more: keypad keys 56dp (46dp when system font scale exceeds 1.15), nav bar 68dp, transaction rows at least 64dp. Navigation is a floating bar over content; scrolling screens (Home, Bills) leave 168dp at the bottom to clear it. Home stacks with 24dp between sections. The Bills tab is a column at the same 20dp gutter and 16dp gap: view chips (Calendar, All), overdue banner card, filter chips, month summary card, month pager, 7-column calendar, selected-day rows, then a "next 30 days" list. The Appearance screen leads with a live preview card over the real background, then pack carousel, accent grid (4x4 plus Dynamic and Custom), mode and background chips, motion slider.

## Elevation & Depth

Depth is tonal and optical, not shadowed. Glass cards use a vertical-fade fill (full card alpha to 60%) and a 1dp lit border fading to 25% at the bottom. Outline and flat cards are opaque fills with a 1dp edge. The only drop shadow is on flat cards in light themes (Ledger, light Gumdrop): 8dp, ambient `#14000000`, spot `#26000000`. Accent glow is a colour role, used sparingly on primary or focused elements.

### Named Rules
**The Pack Owns Depth Rule.** Depth treatment is chosen by the pack's card style (GLASS, OUTLINE, FLAT); screens never add their own shadows or borders.

## Shapes

One corner value per pack scales five Material steps: extraSmall = c/3, small = c/2, medium = 0.7c, large = c, extraLarge = c+8dp. Cards use large. Keypad keys use medium. The selected nav item and glyph discs are full pills or circles. Corner values: Tidepool 28dp, Afterhours 14dp, Ledger 20dp, Void 12dp, Gumdrop 32dp. Ring and donut strokes use round caps; donut slices are separated by a 2.5 degree gap. Calendar cells use the small step (c/2); status dots and bill glyph discs are circles. The widget shell is a fixed 24dp corner and its plus button a 44dp circle.

## Components

### Card (FuriCard)
The app's only container. Style by pack: Tidepool glass, Afterhours accent outline (accent at 45%), others flat with the pack edge. Caller supplies inner padding. Optional click.

### Keypad
3x4 grid, 8dp gaps. Keys are card-filled with a 1dp edge at 60% strength, dip to 0.93 scale on press with a spring, fill with accent container while pressed, and fire a haptic tick. Long-press backspace clears.

### Navigation (FuriNavBar)
A floating FuriCard, 68dp tall, 6dp inner padding. The selected item widens (weight 2.6 vs 1, spring) into an accent-container pill with icon and label; others show icon only, tinted on-surface-variant.

### Transaction row (TxRow)
No card. 44dp circular glyph on 20% category tint, title (bodyLarge), subtitle (bodySmall, muted), right-aligned tabular amount in income, expense or muted (transfer) colour. Expense uses a true minus sign.

### Bills calendar (BillsScreen)
Month grid of 6 weeks x 7 days from the locale's first day of week, 4dp gaps, weekday heads in labelSmall muted. Month change slides and fades through `Furi.motion`. Each cell is 54dp tall, small-step corners, day number in tabular figures, and up to three 5dp status dots (3dp apart) under it, coloured by `statusColor`; more than three bills show only the first three.
- **Default**: transparent, number in `onSurface`.
- **Outside month**: number in `onSurfaceVariant` at 60%.
- **Today**: 1.5dp `accentText` outline on the cell.
- **Selected**: `accentContainer` fill, number in `onAccentContainer`, spring scale to 1.06.
- **Semantics**: each cell announces its date and bill count; the dots add no extra semantics.
Below the grid sit the selected day's rows, then "next 30 days".

### Bill row (BillRow)
A FuriCard row: 44dp glyph disc on 22% of the bill's own colour, name (bodyLarge), due label (labelMedium in `statusColor`), tabular amount (bodySmall size; income colour for income bills, muted otherwise), then optional Auto badge (`accentText`) and "n of N" progress (muted). Trailing action: FilledTonalButton "Paid" (or "Received") when open; a Check plus Undo text button when paid; Undo when skipped. An overflow menu on open rows holds Skip and Edit. Paying opens PaySheet, a Material modal bottom sheet on `surfaceContainerHigh` with 20dp gutter, 14dp gaps, account chips and a date field.

### Month summary and overdue banner
Month summary: one FuriCard, three columns (due, paid in income colour, left), label in labelMedium muted over a tabular titleSmall-size figure. Overdue banner: a tappable FuriCard whose single titleSmall line is in the expense colour; it jumps the calendar to the first overdue day.

### Upcoming bills card and quick-add chips (Home)
Upcoming card: FuriCard, 16dp padding, 8dp gaps, title in titleSmall with the total at right; up to three rows, each an 8dp status dot, name, and due label in `statusColor`; "and n more" in labelMedium muted; an empty line when nothing is due in 7 days. Quick-add: a titleSmall heading over a horizontally scrolling row of Material AssistChips (8dp gaps) reading "Name . amount"; they are the only chips that act rather than filter.

### Widget (FuriWidget)
Glance, exact size, 24dp corners, 14dp padding, on page colour at 94%. Top row: "Total balance" (12sp muted) over the balance (24sp bold, `onSurface`) and a 44dp accent circle with an `onAccent` plus that opens Add expense. Below: up to four bill rows, each an 8dp status dot, name (13sp, one line), due text (11sp in status tint), amount (12sp). Tapping the body opens the app; tapping the list opens Bills. Status tints: overdue expense, due today and soon warning, paid income, everything else `accentText`. Empty state is one muted 12sp line.

### Bill notification
System-styled notification with one "Paid" action that records the expense at the bill's usual account and amount (PayActionReceiver); label is the same string as the in-app Paid button. No custom colour.

### Budget ring (BudgetRing)
14dp stroke ring on a 12% on-surface track. OK sweeps an accent-to-accent2 gradient from 12 o'clock; CLOSE switches to warning; OVER fills to 360 in expense with a pulsing halo (alpha 0.25-0.7, 900ms). Sweeps with the pack spring.

### Add / Edit transaction
Amount hero on top, then a row of 76dp category tiles (52dp tinted disc, 2dp ring in the category colour when chosen, 1.06x spring), then one horizontally scrolling chip row for account (dropdown), date, foreign currency and More, then the note. Everything else (split, tags, pay from fund, repeat) lives in the More sheet so the keypad never crowds the form. A foreign-currency entry shows the typed symbol in the hero and a quiet "About P x at 1 USD = P y" line beneath it.

### Icon picker (IconPickerSheet)
Full-height bottom sheet: live preview bubble in the item's colour, Icons/Emoji tabs, search, group chips, 56dp tiles with a 1.1x spring and a 2dp ring when chosen. 210 Material Rounded icons in 12 groups, or any emoji (stored as `e:<emoji>`). `FuriIcon` draws either.

### People (PeopleScreen, PersonScreen)
Hero card with owed-to-you (income colour, left) and you-owe (expense colour, right) over a two-sided `BalanceBar` that springs to its share. Person cards carry a 48dp avatar disc, name, one status line and a coloured amount. A loan card is a `BudgetRing` of repayment progress with a check when settled; settling plays confetti. Loan transactions read as "Lent to Ana" in lists and never count as spending.

### Funds (FundsScreen) and Free to spend
A fund is a 64x84 jar: `LiquidFill` clipped to a rounded border in the fund colour, icon on top in `onSurface`, a coin drops in when money is added. "Free to spend" (balance minus reserved minus bills still due this month) sits as a hero on Funds and as a compact bar on Home; the bar splits into free (accent), bills due (warning) and reserved (income), and turns solid expense red when negative.

### Sound and haptics (SoundSettingsScreen)
One 84dp style card per sound; waveform bars jump when played, the chosen card sits on `accentContainer` with a 2dp accent ring. Sounds default off, haptics on; every event goes through `Feedback`, never through `LocalHapticFeedback` directly.

### Empty states (EmptyState)
Soft accent disc with the screen's icon and three small dots orbiting it (still when motion is off), a titleMedium line and a muted hint. Used on Activity, Bills, Goals, Insights. People and Funds draw their own Canvas art in the same spirit.

### Confetti (ConfettiBurst)
One-shot burst of 48 14x8 rectangles from 40% screen height, 1600ms linear, fading as they fall, in `accent`, `accent2`, income, warning and expense. Used for goal completion; skipped entirely when motion is off.

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
- **Do** colour bill state through `statusColor` and always pair the colour with the due label text.
- **Do** build widget colours from `resolveColors` (pack, accent, night) and nothing else.
- **Do** show up to three status dots per calendar cell, 5dp, in `statusColor`.
- **Do** route every tap of haptics and every sound through `LocalFeedback`.
- **Do** put secondary fields behind the More sheet rather than adding rows to the Edit screen.

### Don't:
- **Don't** ship a single house style with a dark/light toggle; a new surface must look right in all five packs.
- **Don't** offer Ledger in dark or Afterhours and Void in light; those packs are locked by palette.
- **Don't** use raw swatch colours for text or thin strokes.
- **Don't** put shadows, borders or fills on a screen that the pack's card style should supply.
- **Don't** animate outside `Furi.motion` or ignore the intensity setting.
- **Don't** invent a second status palette (for example a purple "upcoming" or a blue "paid") on a new screen, widget or notification.
- **Don't** put the live background, glass fills or photo into the widget; colour only.
- **Don't** rely on a status dot alone to say a bill is late or paid.

## Not canonized (build defects or placeholders)
- Widget corner is a fixed 24dp and its type is the system face, so it ignores the pack's corner and font; recorded as a gap, not a rule.
- Widget status tint sends Skipped to `accentText`, while the app's `statusColor` sends it to muted; the app mapping is the rule.
- Widget plus button draws a "+" text glyph instead of an icon.
- Calendar dots carry colour only, and the cell's spoken description gives a count but no status; a possible accessibility gap.
- Bill glyph discs use the user's per-bill colour (`colorArgb`) with no contrast derivation.
- Transaction rows fall back to hard-coded `#94A3B8` for a missing category colour; not a system token.
- Custom screens carry some one-off dp values (14, 168dp) outside the spacing scale; the scale above lists only the reused steps.
