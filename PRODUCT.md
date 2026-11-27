# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack
Kotlin + Jetpack Compose, native (user decision 2026-11-26).

## Users
A single person tracking their own money on their phone. Quick check-ins (log an expense in seconds, glance at what is left this month) plus occasional deeper review of where money goes. Inferred: the primary user is the app's author; no other audience confirmed.

## Product Purpose
Personal budget tracker: income and expenses, categories, per-category monthly budgets, accounts, savings goals, recurring transactions and reminders. The logic is deliberately simple; the product bets on a front end people enjoy opening.

## Positioning
A budget app whose look is the user's own: theme packs, accent colors, photo and animated backgrounds, and animation as a feature, not a coat of paint. Offline-first, no account, local data. The one optional network use is fetching exchange rates, off until the user switches it on.

## Operating Context
Used one-handed on a phone, often in short moments (after a purchase, at a till). Dark and light both matter; the user changes appearance often. Money is stored as whole minor units.

## Capabilities and Constraints
- Core: accounts, categories, transactions (income, expense, transfer), monthly/weekly budgets with rollover, charts (donut, bars, trend).
- Extras: recurring transactions, savings goals, reminders, backup/export, app lock.
- Works fully offline, no accounts. The only network use is the optional exchange-rate update (Settings, Exchange rates): one request carrying just the main currency code, cached so everything still works offline, off by default. No bank linking, ever. Currency selectable (37 supported).
- minSdk 26. Shader and blur effects need API 33/31 and must have fallbacks.
- Final app name undecided (working name Furi Budget, part of the user's Furi app family).

## Brand Commitments
Part of the user's "Furi" app family. Visual direction delegated to the designer by the user; the user explicitly asked to go all out on presentation, theming and animation.

## Evidence on Hand
No existing assets, logos or user data. App icon is a placeholder. No fabricated metrics; demo data must be labelled synthetic.

## Product Principles
- Presentation is the product: every screen is a chance for craft, but logging a transaction stays fast.
- The look belongs to the user: theme, accent, background and motion are all theirs to change, and any combination stays legible.
- Motion explains change (money moving, limits approaching) and is skippable via system Remove animations and an in-app intensity setting.
- Numbers are exact and calm: tabular figures, no floating-point money.

## Accessibility & Inclusion
Contrast floors enforced for every accent (4.5:1 text), 48dp touch targets, honor system font scale and Remove animations, colorblind-safe semantic palette option.

## Assumptions (inferred, not interviewed)
The impeccable init interview was substituted by the answers given during planning (stack, scope, theming, direction delegated). Users, operating context and accessibility lines above are inferred from the brief.
