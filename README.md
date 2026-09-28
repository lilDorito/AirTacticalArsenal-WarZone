# Air Tactical Arsenal WarZone

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.1-3C8527?style=for-the-badge" alt="Minecraft 1.20.1" />
  <img src="https://img.shields.io/badge/Forge-47.2%2B-F58220?style=for-the-badge" alt="Forge 47.2+" />
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/GeckoLib-4.4.9%2B-2D7FF9?style=for-the-badge" alt="GeckoLib 4.4.9+" />
</p>

**Air Tactical Arsenal by Synarsis** - unofficial fork by **lilDorito**.

> **Non-commercial use only.** This fork is under the original Air Tactical Arsenal License
> (see [LICENSE](LICENSE)): personal play and non-commercial servers only, no paid ranks, paid access,
> ads or other monetization. Read the license before downloading or hosting.
>
> This is not an official release. It is not affiliated with or endorsed by Synarsis.

Forge 1.20.1 mod that adds tactical drones, ballistic missiles, an air-defense
radar and alert sirens to Minecraft.

modId: `ata` (same as upstream) · version: `1.1.0-warzone`

## Contents

- [Why this fork](#why-this-fork)
- [What changed](#what-changed)
- [Features](#features)
- [Requirements](#requirements)
- [Build](#build)
- [How to play](#how-to-play)
- [Project layout](#project-layout)
- [Mod compat](#mod-compat)
- [Credits](#credits)
- [License](#license)

## Why this fork

I made this fork for my own milsim project, the **WarZone** modpack (Superb Warfare, AshVehicles, and
more). Upstream ATA works well on its own, but it needed fixes and changes to fit that pack:

- aircraft from other mods had to appear on the radar, and AshVehicles jets had to be able to lock and shoot
  down ATA drones;
- damage had to be balanced against SBW vehicles and players instead of one-shotting everything;
- the pack is English-first, with Russian and Ukrainian as an option;
- bugs, leftovers and missing polish found while testing had to be fixed.

The mod id stays `ata`, so it replaces upstream ATA in a pack. Don't install both.

## What changed

Full list in [CHANGELOG.md](CHANGELOG.md). Main points:

- **Radar**: detects all SBW and addon aircraft (including AshVehicles), with new UAV / SHD / BAL / ORL tags
  and legend colours; uses SBW's FuMO 25 model when SBW is installed; needs open sky (**WEAK SIGNAL**);
  tracks the Iskander for its whole flight; configurable horizon and entity lists; drops itself when mined.
- **Air-to-air**: AshVehicles jets see and lock Shahed / Orlan with AAM-4, never the Iskander.
- **Damage**: no more ×3.5 vehicle multiplier, lower and configurable explosion damage, round craters for
  Iskander and Shahed.
- **UAVs**: configurable max route length (default 10,000 blocks), checked by the server; configurable
  Orlan flight time (default 30 min).
- **Sound**: engine and countdown sounds come back when Minecraft drops them, only reach players in the
  same dimension, and the siren behaves properly.
- **Language**: English by default, Russian and Ukrainian per player, can be forced to English per server;
  no hard-coded Russian text left.
- **UI**: screens scale to fit, HUD and radar overlaps fixed.
- **Cleanup**: broken pickaxe tag fixed, dead code, config options and assets removed, versions and
  metadata fixed.

## Features

### Strike

- **Shahed-136** — kamikaze UAV, two attack profiles (low dive and high dive)
- **Iskander-M** — ballistic missile, needs a 5x5 launch pad, 7-second
  countdown
- chunk-aware flight: chunks along the flight path stay loaded

### Recon

- **Orlan-10** — recon drone with launch pad, control tablet and FPV camera
- night vision shader for the FPV view
- waypoint route planning with terrain preview

### Control and detection

- **Shahed Control Tablet** — links up to several Shahed launch platforms,
  plans routes on a terrain map and launches
- **Iskander Control Terminal** — unified launcher control, can manage
  several pads, shows ETA and status
- **Air Defense Radar** — tactical map with zoom, pan and typed target markers (SHD / BAL / ORL / PLN / HEL / UAV);
  needs open sky above it
- **Launch Interface Cable** — links launcher to terminal, up to 60 blocks
- **Alert Siren** — alarm block with sound

### Other

- **Paint Stand** and **Spray Can** — repaint your gear
- `/ata` command for config and admin launches
- target whitelist and blacklist zones
- GeckoLib animations and custom sounds
- English by default; Russian and Ukrainian selectable per player
  (`config/ata-client.toml` or `/atalang`)

## Requirements

- Minecraft 1.20.1, Forge 47.2+
- GeckoLib 4.4.9+ (works with 4.8.2)
- Optional: Superb Warfare, AshVehicle, TacZ, Create
- To build: Java 17 and internet (Gradle downloads the dependencies)

## Build

Build the jar:

```bat
.\gradlew.bat build
```

Output: `build\libs\air-tactical-arsenal-1.1.0-warzone.jar`.

Run a dev client:

```bat
.\gradlew.bat runClient
```

There is also a small helper script: `build.bat`.

Other tasks:

```bat
.\gradlew.bat runServer
.\gradlew.bat runData
```

## How to play

### Shahed-136

1. Place a Shahed launch platform and right-click it with a Shahed to mount the drone.
2. Right-click the platform with the Shahed Control Tablet to link it.
3. Right-click with the tablet to open it, plan the route on the map, save it.
4. Stand within 30 blocks of the platform and hit LAUNCH.

Admin shortcut: `/ata attack shahed <start> <target> low|high`
(and `/ata attack iskander <start> <target>`).

### Iskander-M

1. Build a 5x5 launch pad on flat ground with open sky above.
2. Install the Iskander rocket on the pad.
3. Open the Iskander Control Terminal, pick the pad and the target.
4. Confirm - 7 second countdown starts.

### Orlan-10

1. Place the Orlan launch pad and spawn the drone.
2. Open the Orlan Tablet, plan the route.
3. Switch to FPV view. Toggle NVG if you need it.

### Air Defense Radar

1. Place it with open sky above: no blocks at all over the 5x5 area around it,
   or it only shows **WEAK SIGNAL**.
2. Right-click it to open the map. Its redstone output grows as a Shahed or an
   Iskander gets closer.

### Config

- `/ata config` for live tweaks
- `config/ata-common.toml` for server settings (explosion damage, craters,
  radar, UAV route length, Orlan flight time, whitelist/blacklist zones,
  language policy)
- `config/ata-client.toml` for per-player settings (language)

## Project layout

```
src/main/java/com/synarsis/airtacticalarsenal
├── ShahedMod.java              entry point, @Mod("ata")
├── block/                      blocks: terminal, launcher, radar, siren
│   └── entity/                 block entities
├── entity/                     Shahed, Iskander, Orlan, paint stand
├── item/                       rockets, drones, tablets, cable
├── client/
│   ├── gui/                    terminal, tablet, radar, route screens
│   ├── lang/                   per-player language selection
│   ├── radar/                  terrain map and route preview
│   ├── shader/                 night vision shader
│   ├── model/ renderer/ sound/ rendering and audio
├── command/                    /ata command
├── compat/                     Create / TacZ / Superb Warfare / AshVehicle
├── mixins/compat/              optional AshVehicle hooks (loaded only if installed)
├── config/                     ata-common.toml spec
├── chunk/                      forced chunk loading for long flights
└── network/                    client-server packets
src/main/resources
├── assets/ata/                 textures, lang, models, shaders, sounds
└── META-INF/mods.toml          mod metadata
```

## Mod compat

- **Create** — ATA blocks are protected from Create contraption movement
- **TacZ** — optional, TacZ bullets can shoot Shaheds down (detected at runtime)
- **Superb Warfare** — optional. Its aircraft and those of every addon show on the radar, SBW bullets can
  shoot Shaheds down, and the radar uses SBW's FuMO 25 model. SBW files are loaded from the SBW jar and
  are not included in this mod.
- **AshVehicles** — optional. Its aircraft can see and lock Shahed / Orlan with AAM-4 (never the Iskander).
  These hooks load only when AshVehicles is installed.

All compat is optional. The mod runs fine on its own.

## Credits

- **Synarsis** — author of Air Tactical Arsenal: the original mod, code, models, textures and sounds.
- **lilDorito** — WarZone fork: the changes listed in [CHANGELOG.md](CHANGELOG.md).
- GeckoLib, Forge and other third-party material stay under their own licenses.

## License

This fork inherits the original **Air Tactical Arsenal License** unchanged - see [LICENSE](LICENSE).
Attribution: **"Air Tactical Arsenal by Synarsis"**.

Short version (the LICENSE file is what counts):

- personal use: allowed
- non-commercial servers: allowed (income may only cover hosting costs)
- redistribution and forks: must keep the same license unchanged, keep the attribution, charge nothing,
  and tell people about the non-commercial limit before they download
- commercial use: needs written permission from Synarsis
- the names "Air Tactical Arsenal" and "ATA" belong to their owners; this fork does not claim endorsement
- third-party libraries, fonts, sounds and textures stay under their upstream licenses
