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

modId: `ata` (same as upstream) · version: `1.2.0-warzone`

## Contents

- [Why this fork](#why-this-fork)
- [WarZone versions](#warzone-versions)
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

## WarZone versions

"WarZone" versions are my own **private** forks of other mods, made for the WarZone pack. They are not
published and can't be downloaded; they're mentioned in this README and the changelog only because this
fork is tested against them. ATA WarZone needs none of them and works with the public versions listed in
[Supported versions](#supported-versions).

Each one keeps the original mod id, so ATA treats it like the public version it's based on:

| WarZone version | Based on | What differs |
|-----------------|----------|--------------|
| SuperbWarfare WarZone `0.8.9.2-warzone-1.0.1` | SuperbWarfare 0.8.9.2 | own vehicle and damage balance (for example lower aerial-bomb armour multipliers), Ukrainian translation, bug fixes; lists ATA in `scan_white_list` by default |
| AshVehicles WarZone `4.6-warzone-1.0.1` | AshVehicles 4.6 | own weapon loadouts and balance (for example the FAB-500 at 1600 / 40 blocks), Ukrainian translation, bug fixes |
| MCSP WarZone `1.0.10-warzone-1.0.1` | MCSP 1.0.10 | own balance, Ukrainian translation, bug fixes |
| SuperbWarfare `0.8.7.1` and AshVehicles `4.2` patched builds | SuperbWarfare 0.8.7.1, AshVehicles 4.2 | older WarZone builds, kept working by earlier ATA WarZone versions |

When this README or the changelog gives a number or behaviour "in SuperbWarfare WarZone" or "in AshVehicles
WarZone", it only applies with those private builds; with the public mods the public values apply.

## What changed

Full list in [CHANGELOG.md](CHANGELOG.md). Main points:

- **Radar**: radar and route maps show the real terrain (top blocks with hill shading), from a one-time whole-map scan (`/ata terrain scan`) plus live updates; one set of contact types for every mod: **PLN** planes, **HEL** helicopters, **UAV** large drones (Shahed, Orlan, MQ-9, X-47B), **MSL** missiles (guided missiles of SBW and AshVehicles and the Iskander); small SBW drones and swarm drones are never shown; configurable range (default 1,500, up to 5,000 blocks); uses SBW's FuMO 25 model when SBW is installed; needs open sky (**WEAK SIGNAL**); tracks the Iskander for its whole flight; configurable horizon and entity lists; drops itself when mined; servers can swap the biome map for a plain dark background (`radarDarkBackground`).
- **Shootdowns**: shot-down Shaheds and Orlans fall instead of exploding in the air: an engine hit makes them glide down smoking, a wing hit sends them into a spin, other kills make them tumble; a falling Shahed still explodes on impact.
- **Air defense**: SBW and AshVehicles seekers lock Shahed and Orlan, never the Iskander; anti-air guns that
  fire SBW shells and SBW's laser tower can't delete the Iskander, and an Orlan they hit is shot down properly.
- **Damage**: the Shahed hits like SBW's Mk-82 and the Iskander like a heavy bomb (1600 / 40 blocks); with
  SBW both count as aerial bombs against vehicle armour; round craters for both.
- **UAVs**: configurable max route length (default 10,000 blocks, up to 30,000), checked by the server;
  configurable Orlan flight time (default 30 min).
- **With SuperbWarfare**: recipes use SBW parts, explosions play SBW's explosion sounds mixed by distance,
  and Shahed, Orlan and Iskander show on SBW radars and the tactical map (SBW 0.8.9.1+).
- **Sound**: the Iskander roars in flight (sound by Kalitovskiy) and can be heard from 1,500 blocks away; engine and countdown sounds come back when Minecraft drops them, only reach players in the
  same dimension, and the siren behaves properly. Each Shahed randomly gets one of two engine sounds
  (second one by Kalitovskiy).
- **Language**: English, Ukrainian and Russian, following each player's Minecraft language like SuperbWarfare
  WarZone; no hard-coded Russian text left.
- **UI**: screens scale to fit, HUD and radar overlaps fixed.
- **Cleanup**: broken pickaxe tag fixed, dead code, config options and assets removed, versions and
  metadata fixed.

## Features

### Strike

- **Shahed-136** — kamikaze UAV, two attack profiles (low dive and high dive)
- **Iskander-M** — ballistic missile, launched from the 5x5 Missile Launchpad, 7-second
  countdown
- chunk-aware flight: chunks along the flight path stay loaded

### Recon

- **Orlan-10** — recon drone, launched from the Recon UAV Launcher and flown from the Recon UAV Tablet with an
  FPV camera
- night vision shader for the FPV view
- waypoint route planning with terrain preview

### Control and detection

- **Strike UAV Launcher** and **Strike UAV Tablet** — the tablet links several launchers, plans routes on a
  terrain map and launches (Shahed-136 today)
- **Missile Launchpad** and **Missile Targeting Terminal** — the terminal manages several launchpads, sets
  targets and shows countdown and status (Iskander-M today)
- **Air Defense Radar** — tactical map with zoom, pan and typed target markers (PLN / HEL / UAV / MSL);
  needs open sky above it
- **Missile Link Cable** — links a launchpad to the terminal, up to 60 blocks
- **Alert Siren** — alarm block with sound

### Other

- **Paint Stand** and **Spray Can** — repaint your gear
- `/ata` command for config and admin launches
- target whitelist and blacklist zones
- GeckoLib animations and custom sounds
- English, Ukrainian and Russian, picked from each player's Minecraft language

## Requirements

- Minecraft 1.20.1, Forge 47.2+
- GeckoLib 4.4.9+ (works with 4.8.2)
- Optional: SuperbWarfare, AshVehicles, TacZ, Create
- To build: Java 17 and internet (Gradle downloads the dependencies)

## Build

Build the jar:

```bat
.\gradlew.bat build
```

Output: `build\libs\air-tactical-arsenal-1.2.0-warzone.jar`.

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

1. Place a Strike UAV Launcher and right-click it with a Shahed to mount the drone.
2. Right-click the launcher with the Strike UAV Tablet to link it.
3. Right-click with the tablet to open it, plan the route on the map, save it.
4. Stand within 30 blocks of the launcher and hit LAUNCH.

Admin shortcut: `/ata attack shahed <start> <target> low|high`
(and `/ata attack iskander <start> <target>`).

### Iskander-M

1. Place the 5x5 Missile Launchpad on flat ground with open sky above.
2. Load the Iskander onto the launchpad through its loading hatch.
3. Link the launchpad to a Missile Targeting Terminal with the Missile Link Cable.
4. Open the terminal, pick the launchpad and the target.
5. Confirm - 7 second countdown starts.

### Orlan-10

1. Place the Recon UAV Launcher and mount an Orlan-10 on it.
2. Link it with the Recon UAV Tablet, open the tablet and plan the route.
3. Switch to FPV view. Toggle NVG if you need it.

### Air Defense Radar

1. Place it with open sky above: no blocks at all over the 5x5 area around it,
   or it only shows **WEAK SIGNAL**.
2. Right-click it to open the map. Its redstone output grows as a Shahed or an
   Iskander gets closer.

### Terrain maps

Run once on the server (op level 2), ideally after pregenerating the world:

```
/ata terrain scan                      # 10000x10000 square around 0, 0
/ata terrain scan <size>               # size x size around 0, 0
/ata terrain scan <size> <x> <z>       # size x size around x, z
/ata terrain rescan [...]              # same, but rescans existing tiles
/ata terrain status | cancel
```

The result is stored in `world/ata_map/`. All radar and Shahed/Orlan route maps inside that area are built
from it, and chunks loaded around players are painted in every `mapRefreshMinutes`.

### Config

- `/ata config` for live tweaks
- `config/ata-common.toml` for server settings (explosion damage, craters,
  radar, UAV route length, Orlan flight time, whitelist/blacklist zones)

## Project layout

```
src/main/java/com/synarsis/airtacticalarsenal
├── ShahedMod.java              entry point, @Mod("ata")
├── block/                      blocks: missile terminal, missile launchpad, radar, siren
│   └── entity/                 block entities
├── entity/                     Shahed, Iskander, Orlan, paint stand
├── item/                       rockets, drones, tablets, cable
├── client/
│   ├── gui/                    terminal, tablet, radar, route screens
│   ├── radar/                  terrain map and route preview
│   ├── shader/                 night vision shader
│   ├── model/ renderer/ sound/ rendering and audio
├── command/                    /ata command
├── compat/                     Create / TacZ / SuperbWarfare / AshVehicles
├── mixins/compat/              optional AshVehicles hooks (loaded only if installed)
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
- **SuperbWarfare** — optional. Its aircraft, missiles and those of every addon show on the radar, SBW weapons
  can shoot Shaheds and Orlans down, and the radar uses SBW's FuMO 25 model. With SBW installed:
  - Shahed and Iskander count as aerial bombs against vehicle armour (`superbwarfare:aerial_bomb` tag);
  - Shahed, Iskander and Orlan recipes use SBW parts (vanilla recipes without SBW; the Orlan has no recipe
    without SBW, as before);
  - explosions play SBW's huge-explosion sounds, mixed by ATA by distance (old ATA sounds if they're missing);
  - Shahed, Orlan and Iskander show on SBW radars and the tactical map on SBW versions that have long-range
    entity tracking (0.8.9.1+). ATA adds them to SBW's `scan_white_list` itself, nothing to set up.

  SBW files are loaded from the SBW jar and are not included in this mod.
- **AshVehicles** — optional. Its aircraft can see and lock Shahed / Orlan (never the Iskander).
  These hooks load only when AshVehicles is installed.

All compat is optional. The mod runs fine on its own.

### Supported versions

The compat code detects what is installed at runtime, so one jar covers all of these:

| Mod | Public versions | WarZone versions (private, see [WarZone versions](#warzone-versions)) |
|-----|-----------------|------------------|
| SuperbWarfare | 0.8.8 (incl. hotfix / final), 0.8.9 (snapshot, release, final), 0.8.9.1, 0.8.9.2 | 0.8.7.1 patched build, 0.8.9.2-warzone-1.0.1 |
| AshVehicles | 4.3, 4.5, 4.6 | 4.2 patched build, 4.6-warzone-1.0.1 |
| MCSP | no MCSP-specific code: its vehicles and missiles are handled as SuperbWarfare ones | 1.0.10-warzone-1.0.1 |

- Radar model: uses the FuMO 25 model from whichever place the installed SBW keeps it (SBW moved it in
  0.8.9). If a future SBW version removes or changes it, the radar switches to the original ATA block
  instead of crashing.
- Radar detection: older SBW builds are read through SBW's aircraft interfaces, 0.8.8+ through each vehicle's
  type (airplane, airship, helicopter). Ground vehicles, boats and small drones are never shown. Missiles are
  found through SBW's missile base class where it exists, and by class name (AIM, AGM, AAM, R-60/77, Kh,
  JASSM, Tomahawk, "missile") on older SBW and on AshVehicles 4.2.
- Locking: SBW's seekers (and AshVehicles 4.3+ missiles, which use them) can lock Shahed and Orlan through
  the `superbwarfare:destroyable_projectile` tag; the Iskander is never lockable. On AshVehicles 4.2 the extra
  AshVehicles hooks are used as before; on 4.3+ the HUD highlight hook still applies.

## Credits

- **Synarsis** — author of Air Tactical Arsenal: the original mod, code, models, textures and sounds.
- **lilDorito** — WarZone fork: the changes listed in [CHANGELOG.md](CHANGELOG.md).
- GeckoLib, Forge and other third-party material stay under their own licenses.
- **Kalitovskiy** — author of the second Shahed engine sound (`shahed_engine_2.ogg`) and the Iskander flight
  sound (`iskander_flight.ogg`).

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
