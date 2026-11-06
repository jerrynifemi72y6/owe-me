# Owe Me

A small-debts ledger for the "I paid, you owe half" moments. Android, offline, no
account, no permissions. Built as a showcase for hand-drawn liquid glass: every
panel is a custom view with a drifting specular highlight, sitting over slow-moving
colour blobs, and all motion runs on damped-spring physics.

![no dependencies](https://img.shields.io/badge/dependencies-none-brightgreen)

## What it does

- Log an entry: who, how much, and which way the money points.
- Home shows the two numbers that matter: total owed to you, and total you owe.
- Tap a person for their history and a running balance.
- Settle up clears them out, with undo. Long-press any entry to delete it, also undoable.
- Everything lives in one JSON blob in SharedPreferences. Nothing leaves the phone.

## The glass

- `LiquidBackgroundView` - four colour blobs drifting on a Choreographer loop, given a
  real GPU blur on Android 12+ (`RenderEffect`) and left as soft gradients below that.
- `GlassPanel` - translucent fill, top sheen, hairline border, and a specular streak
  that slowly sweeps across (set `living = true`). It is drawn with a clip path, so
  children stay inside the rounded glass.
- `Motion` - `SpringInterpolator` turns any animation into a damped spring. Cards press
  in and spring back, screens rise in, numbers count up, the sheet overshoots softly.
- The hero panel tilts a few degrees with the accelerometer, so highlights shift as you
  move the phone.

## Build

```
gradle assembleRelease
```

CI builds and publishes a signed APK on every push to `main`. The signing key is
`keystore/oweme.p12`, committed on purpose so updates install over the old app
instead of demanding an uninstall first.

## Edit the data model

`Entry` is (person, amountMinor, direction, note, timestamp). Amounts are kobo, so
there is no floating point in the ledger. `Store` does the grouping and balancing.
