# RPN Calculator

An RPN (Reverse Polish Notation) scientific calculator for Android, forked from
[FossifyOrg/Calculator](https://github.com/FossifyOrg/Calculator) and rebuilt around a stack-based
entry model instead of infix expressions.

<p float="left">
  <img src="screenshots/calculator.png" width="30%" alt="Main calculator screen" />
  <img src="screenshots/tools.png" width="30%" alt="Unit conversions and calculators" />
</p>

## Features

- **RPN entry** with a visible stack, undo, last-X recall, and register storage (STO/RCL).
- **Scientific functions**: trig, logs, powers, roots, factorial, combinations/permutations,
  constants, DEG/RAD/GRAD and engineering-notation display modes.
- **Unit conversions** across length, area, volume, mass, temperature, time, speed, pressure,
  energy, power, fuel, data size, density, flow rate, cooking, torque, and lighting.
- **Currency conversion** across 25 major currencies, with a manual "Refresh rates" button
  (never fetches automatically) and rates cached for offline use.
- **Formula calculators**: Reynolds number, Prandtl number, hydraulic diameter, compound
  interest, and a per-country progressive income tax / take-home pay calculator (UK, US,
  Poland, Hungary, Germany, France).
- **History** of past calculations, and persistent per-converter unit choices.

## Installing

Releases are published as signed APKs on the [Releases page](../../releases), tracked via
[Obtainium](https://github.com/ImranR98/Obtainium):

1. In Obtainium, tap **Add App**.
2. Paste this repo's URL: `https://github.com/kvvba/android-rpn`.
3. Obtainium will pick up new versions automatically whenever a release is published here.

## Building

```
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## License

GPLv3 — see [LICENSE](LICENSE). Based on [FossifyOrg/Calculator](https://github.com/FossifyOrg/Calculator).
