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

Forge 1.20.1 mod that adds strike and recon drones (**Shahed-136**, **Orlan-10**), the **Add-on drones** (new in 1.3.2)
- Other mods can add strike and recon drones built on the Shahed and Orlan (SBW Electronic Warfare WarZone adds
  ZALA KUB-BLA and the Bayraktar TB2). They load on the same launchers, fly from the same tablets with the same routes,
  camera, radar and crash physics, and are named by their own name in every message and screen.

**Iskander-M**
ballistic missile, an **air defense radar**, **on-board radars** for SuperbWarfare and AshVehicles aircraft
and AA vehicles, and **alert sirens**.

modId: `ata` (same as upstream) · version: `1.3.2-warzone`

## Contents

- [Why this fork](#why-this-fork)
- [WarZone versions](#warzone-versions)
- [What's different from upstream](#whats-different-from-upstream)
- [Features](#features)
- [Requirements](#requirements)
- [Build](#build)
- [How to play](#how-to-play)
- [Vehicle radar data](#vehicle-radar-data)
- [Terrain maps](#terrain-maps)
- [Config](#config)
- [Project layout](#project-layout)
- [Mod compat](#mod-compat)
- [Credits](#credits)
- [License](#license)

## Why this fork

I made this fork for my own milsim project, the **WarZone** modpack (Superb Warfare, AshVehicles, and
more). Upstream ATA works well on its own, but it needed changes to fit that pack:

- aircraft and missiles from other mods had to appear on the radar, and their weapons had to be able to
  find, lock and shoot down ATA drones;
- damage had to be balanced against SBW vehicles and players instead of one-shotting everything;
- the pack is English-first, with Ukrainian and Russian as options;
- bugs, leftovers and missing polish found while testing had to be fixed.

The mod id stays `ata`, so it replaces upstream ATA in a pack. Don't install both.

## WarZone versions

"WarZone" versions are my own **private** forks of other mods, made for the WarZone pack. They are not
published and can't be downloaded; they're mentioned here and in the changelog only because this fork is
tested against them. ATA WarZone needs none of them and works with the public versions listed in
[Supported versions](#supported-versions).

Each one keeps the original mod id, so ATA treats it like the public version it's based on:

| WarZone version | Based on | What differs |
|-----------------|----------|--------------|
| SuperbWarfare WarZone `0.8.9.2-warzone-1.0.1` | SuperbWarfare 0.8.9.2 | own balance and fixes; lists ATA in `scan_white_list` by default; sends ATA drones and missiles to players beyond entity range, so they show on the tactical map and seekers can lock them from afar |
| AshVehicles WarZone `4.6-warzone-1.0.1` | AshVehicles 4.6 | own weapon loadouts and balance, fixes |
| MCSP WarZone `1.0.10-warzone-1.0.1` | MCSP 1.0.10 | own balance, fixes |
| SuperbWarfare `0.8.7.1` and AshVehicles `4.2` patched builds | SuperbWarfare 0.8.7.1, AshVehicles 4.2 | older WarZone builds, kept working by earlier ATA WarZone versions |

When this README or the changelog describes something "in SuperbWarfare WarZone" or "in AshVehicles
WarZone", it only applies with those private builds.

## What's different from upstream

The full history is in [CHANGELOG.md](CHANGELOG.md). In short:

**Air defense radar**
- A real sweep: the line turns once every 4 seconds and a dot only moves when the line passes over it, then
  fades until the next pass.
- One set of contact types for every mod: **PLN** planes, **HEL** helicopters, **UAV** large drones (Shahed,
  Orlan, MQ-9 Reaper, X-47B), **MSL** missiles (SuperbWarfare and AshVehicles guided missiles, the Iskander).
  Small SBW drones and swarm drones are never shown. Teammates are drawn hollow.
- The map shows the real terrain (top blocks with hill shading) from a one-time world scan plus live updates,
  or a plain dark background (`radarDarkBackground`).
- Range is a server setting (default 1,500, up to 5,000 blocks), with a minimum altitude, configurable entity
  lists and an open-sky requirement (**WEAK SIGNAL** otherwise). Uses SuperbWarfare's FuMO 25 model when SBW
  is installed.
- The redstone alarm reacts to every air contact the screen shows. It ignores everything belonging to the
  placer or the placer's scoreboard team: their aircraft, helicopters, drones, missiles and shells. It stays on
  until the sky has been clear for 5 seconds, then the next contact sets it off again. **ALARM OUTPUT: ON/OFF**
  at the top of the screen's target list turns it off and on (placer, teammates and operators).

**Vehicle radars** (new in 1.3.0)
- Fighters, attack helicopters and AA vehicles from SuperbWarfare and AshVehicles get an on-board radar in a
  small black-and-green panel on the left edge of the screen, for everyone aboard.
- Round scopes for AA vehicles, cones from the nose for aircraft and helicopters.
- **MSA** (mechanical) radars sweep a beam and miss low fliers; **AESA** radars update everything in range
  several times a second. Stealth aircraft are seen from shorter range.
- The server does the scanning, so it works far past view distance; everything is data-driven and can be
  changed by datapacks.

**Drones**
- Shot-down Shaheds and Orlans fall out of the sky: an engine hit makes them glide down smoking, a wing hit
  sends them spinning (flat spin, corkscrew or steep plunge), other kills make them tumble. A falling Shahed
  explodes on impact; an Orlan crashes with a small blast.
- Every drone is a little different: random toughness, 2-5 hits before it can start falling, 3-8 more before a
  falling drone gives up. A Shahed's warhead goes off at once 15% of the time.
- SuperbWarfare anti-air shells deal 15% of a drone's health per hit instead of deleting it; direct cannon
  hits and missiles still bring it down. A missile kill makes a loud air burst.
- Routes up to 30,000 blocks (default 10,000), checked by the server; drones no longer loop back to waypoints
  they've passed. Configurable Orlan flight time (default 30 min) and tablet range to the launcher (default
  100 blocks).

**Iskander-M**
- Finds the real ground at the impact point, so it no longer bursts in mid-air over valleys, mountains or flat
  worlds.
- The loudest explosion in the pack, and a flight roar (by Kalitovskiy) audible from 1,500 blocks that ends
  with the impact.
- Can't be locked by seekers or deleted by anti-air guns and the laser tower.

**Damage**
- On the SuperbWarfare scale: the Shahed hits like SBW's Mk-82, the Iskander like a heavy bomb (1600 / 40
  blocks); with SBW both count as aerial bombs against vehicle armour. Round craters; claim and protection
  mods still apply.

**Gear names**
- The launch gear is generic, ready for future addons: Strike UAV Launcher / Tablet, Recon UAV Launcher /
  Tablet, Missile Launchpad, Missile Targeting Terminal, Missile Link Cable. Old worlds are converted.

**With SuperbWarfare installed**
- Recipes use SBW parts; explosions use SBW's sounds, mixed by distance; SBW seekers and AshVehicles missiles
  can lock Shaheds and Orlans; anti-air missiles burst next to a locked drone instead of slipping past.
- Shahed, Orlan and Iskander are added to SBW's radar scan list automatically.

**Sound, language and UI**
- Engine and countdown sounds come back when Minecraft drops them and only reach players in the same
  dimension; the siren behaves properly; each Shahed gets one of two engine sounds (second one by
  Kalitovskiy).
- English, Ukrainian and Russian, following each player's Minecraft language. No hard-coded Russian text.
- Screens scale to fit; HUD and radar overlaps fixed.

**Fixes and cleanup**
- Broken pickaxe tag fixed, a server crash with the Orlan camera near marker entities fixed, dead code,
  config options and assets removed, versions and metadata fixed.

## Features

### Strike

- **Shahed-136**: kamikaze UAV with a planned route and two attack profiles (low and high dive)
- **Iskander-M**: ballistic missile, launched from the 5x5 Missile Launchpad after a 7-second countdown
- chunks along the flight path stay loaded

### Recon

- **Orlan-10**: recon drone flown along a route, with an FPV camera and night vision
- waypoint route planning on a terrain map

### Control and detection

- **Strike UAV Launcher** and **Strike UAV Tablet**: the tablet links up to 10 launchers, plans routes and
  launches
- **Recon UAV Launcher** and **Recon UAV Tablet**: the same for the Orlan
- **Missile Launchpad** and **Missile Targeting Terminal**, linked with the **Missile Link Cable** (up to 60
  blocks): the terminal manages several launchpads, sets targets and shows countdown and status
- **Air Defense Radar**: tactical map with zoom, pan, a sweeping beam and typed contacts; redstone alarm
- **Vehicle radars**: on-board radar HUD for SBW and AshVehicles aircraft and AA vehicles
- **Alert Siren**: redstone-driven alarm

### Other

- **Paint Stand** and **Spray Can** to repaint drones
- `/ata` command for config, terrain scans and admin launches
- target whitelist and blacklist zones

## Requirements

- Minecraft 1.20.1, Forge 47.2+
- GeckoLib 4.4.9+ (works with 4.8.2)
- Optional: SuperbWarfare, AshVehicles, TacZ, Create
- To build: Java 17 and internet (Gradle downloads the dependencies)

## Build

```bat
.\gradlew.bat build
```

Output: `build\libs\air-tactical-arsenal-1.3.2-warzone.jar`. A dev client runs with `.\gradlew.bat runClient`
(also `runServer`, `runData`); `build.bat` is a small helper script.

## How to play

### Shahed-136

1. Place a Strike UAV Launcher and right-click it with a Shahed to mount the drone.
2. Right-click the launcher with the Strike UAV Tablet to link it.
3. Open the tablet, plan the route on the map and save it.
4. Stand within 100 blocks of the launcher (`tabletLauncherRange`) and press LAUNCH.

Admin shortcut: `/ata attack shahed <start> <target> low|high` (and `/ata attack iskander <start> <target>`).

### Iskander-M

1. Place the 5x5 Missile Launchpad on flat ground with open sky above.
2. Load the Iskander onto the launchpad through its loading hatch.
3. Link the launchpad to a Missile Targeting Terminal with the Missile Link Cable.
4. Open the terminal, pick the launchpad and the target, and confirm: the 7-second countdown starts.

### Orlan-10

1. Place the Recon UAV Launcher and mount an Orlan-10 on it.
2. Link it with the Recon UAV Tablet, open the tablet and plan the route.
3. Launch, then switch to the FPV view. **N** toggles night vision, **R** locks the camera on a point, **Left
   Alt** leaves the camera.

### Air Defense Radar

1. Place it with open sky above: no blocks at all over the 5x5 area around it, or it only shows
   **WEAK SIGNAL**.
2. Right-click it to open the map. Its redstone output grows as an air contact gets closer; the placer's and
   their team's own aircraft, drones and missiles don't count. Wire it to an **Alert Siren**; the
   **ALARM OUTPUT** button in the target list switches the output off and on.

### Vehicle radars

Get into a fighter, attack helicopter or AA vehicle that has a radar: the scope appears on the left edge of the
screen. **I** hides or shows it. Filled squares are contacts, hollow squares are teammates, and the short line
on each shows where it is heading. On an **MSA** radar a contact only updates when the beam passes it, and
anything lower than about 30 blocks above the ground is lost in ground clutter; an **AESA** radar has no beam
and sees low fliers.

| Vehicles | Radar | Range | Cone | Refresh |
|---|---|---|---|---|
| F-22, J-20, Su-57, YF-23 | AESA | 2000 | ±60° | 4/s |
| F-35A, F-35B | AESA | 1800 | ±60° | 4/s |
| JAS 39E Gripen | AESA | 1600 | ±90° | 4/s |
| F-2 | AESA | 1500 | ±60° | 3/s |
| Su-34 | AESA (PESA) | 1400 | ±55° | 2/s |
| B-2 | AESA | 800 | ±45° | 2/s |
| F-14 | MSA | 2000 | ±65° | 3 s sweep |
| F-15, Su-27, Su-33 | MSA | 1600 | ±60° | 2.5 s |
| Eurofighter, F/A-18 | MSA | 1500 | ±60° | 2 s |
| F-16, MiG-29 | MSA | 1300 | ±60° | 2 s |
| F-4 | MSA | 900 | ±45° | 2.5 s |
| AH-64, Ka-52, Mi-28, RAH-66 | MSA | 900 | ±90° | 2 s |
| Pantsir | MSA, round | 1500 | | 1.5 s per turn |
| Gepard | MSA, round | 1000 | | 2 s per turn |
| LAV-AD | MSA, round | 700 | | 2 s per turn |

Stealth aircraft are seen at a share of that range: F-117 and B-2 35%, F-22 and YF-23 40%, F-35 45%, J-20 and
X-47B 50%, Su-57 and RAH-66 60%; Shaheds 70%, Orlans 60%.

## Vehicle radar data

Radars are data files in `data/ata/vehicle_radars/`, so a datapack can rebalance them or give radars to other
addon vehicles:

```json
{
  "vehicles": ["ashvehicle:f-16"],
  "radar": {
    "type": "msa",
    "shape": "cone",
    "range": 1300,
    "half_angle": 60,
    "sweep_seconds": 2.0,
    "min_altitude": 30
  }
}
```

- `type`: `msa` (sweeping beam) or `aesa` (no beam)
- `shape`: `cone` (from the nose) or `round` (all around)
- `range`: blocks; `half_angle`: cone only, 5-90
- `sweep_seconds` (msa): one side-to-side pass, or one full turn for a round scope
- `refresh_per_second` (aesa): updates per second
- `min_altitude`: contacts lower than this above the ground are hidden (default 30 for msa, 0 for aesa)

`"radar": null` removes a radar. To change a built-in radar, put a file with the same name in a datapack
(`data/ata/vehicle_radars/light_fighters.json` etc.). A file with `"signatures": {"ashvehicle:f-22": 0.4}`
sets how far away a vehicle can be seen, as a share of the radar's range. Vehicle ids are their entity ids,
for example `ashvehicle:f-22` or `superbwarfare:lav_ad`.

## Terrain maps

Run once on the server (op level 2), ideally after pregenerating the world (for example with Chunky):

```
/ata terrain scan                      # 10000x10000 square around 0, 0
/ata terrain scan <size>               # size x size around 0, 0
/ata terrain scan <size> <x> <z>       # size x size around x, z
/ata terrain rescan [...]              # same, but rescans existing tiles
/ata terrain status | cancel
```

The result is stored in `world/ata_map/`. All radar and route maps inside that area are built from it, and
chunks loaded around players are painted in every `mapRefreshMinutes`.

## Config

- `/ata config` for live tweaks
- `config/ata-common.toml` (server): explosions and craters, drone health and shootdown behaviour, Iskander
  flight, radar range and altitude, `vehicleRadars`, route lengths, `tabletLauncherRange`, Orlan flight time,
  whitelist/blacklist zones
- `config/ata-client.toml` (each player): vehicle radar panel shown or hidden, position (left centre by
  default), size (`vehicleRadarPanelSize`, default 73) and heading-up for round scopes

## Project layout

```
src/main/java/com/synarsis/airtacticalarsenal
├── ShahedMod.java              entry point, @Mod("ata")
├── block/                      missile terminal, missile launchpad, radar, siren (+ block entities)
├── entity/                     Shahed, Iskander, Orlan, launchers, paint stand
│   └── crash/                  shot-down flight and durability
├── item/                       drones, missiles, tablets, cable
├── radar/                      terrain scanning, shared contact rules
│   └── vehicle/                vehicle radar profiles and server scanning
├── client/
│   ├── gui/                    terminal, tablet, radar and route screens
│   ├── radar/                  terrain maps, vehicle radar HUD
│   ├── shader/                 night vision
│   └── model/ renderer/ sound/ rendering and audio
├── command/                    /ata command
├── compat/                     Create / TacZ / SuperbWarfare / AshVehicles
├── mixins/compat/              optional AshVehicles hooks (loaded only if installed)
├── config/                     ata-common.toml and ata-client.toml specs
├── chunk/                      forced chunk loading for long flights
└── network/                    client-server packets
src/main/resources
├── assets/ata/                 textures, lang, models, shaders, sounds
├── data/ata/vehicle_radars/    vehicle radar profiles and stealth signatures
└── META-INF/mods.toml          mod metadata
```

## Mod compat

- **Create**: ATA blocks are protected from Create contraption movement.
- **TacZ**: TacZ bullets can shoot Shaheds down (detected at runtime).
- **SuperbWarfare**: its aircraft, missiles and those of its addons show on both radars, its weapons can shoot
  drones down, and the radar uses SBW's FuMO 25 model. Shahed and Iskander count as aerial bombs against
  vehicle armour, recipes use SBW parts, and explosions play SBW's sounds. ATA adds its drones and missile to
  SBW's `scan_white_list` itself. SBW files are loaded from the SBW jar and are not included in this mod.
- **AshVehicles**: its aircraft and missiles show on the radars, its missiles can lock Shaheds and Orlans
  (never the Iskander), and its aircraft and AA vehicles get vehicle radars.

All compat is optional; the mod runs fine on its own.

### Supported versions

The compat code detects what is installed at runtime, so one jar covers all of these:

| Mod | Public versions | WarZone versions (private) |
|-----|-----------------|----------------------------|
| SuperbWarfare | 0.8.8 (incl. hotfix / final), 0.8.9 (snapshot, release, final), 0.8.9.1, 0.8.9.2 | 0.8.7.1 patched build, 0.8.9.2-warzone-1.0.1 |
| AshVehicles | 4.3, 4.5, 4.6 | 4.2 patched build, 4.6-warzone-1.0.1 |
| MCSP | no MCSP-specific code: its vehicles and missiles are handled as SuperbWarfare ones | 1.0.10-warzone-1.0.1 |

- Radar model: uses the FuMO 25 model from wherever the installed SBW keeps it; if a future SBW version
  removes or changes it, the radar falls back to the original ATA block instead of crashing.
- Detection: aircraft are recognised through SBW's vehicle types (or its aircraft interfaces on older SBW);
  missiles through SBW's missile base class, or by class name on older builds. Ground vehicles, boats and
  small drones are never shown.
- Locking: SBW seekers (and AshVehicles missiles, which use them) lock Shahed and Orlan through the
  `superbwarfare:destroyable_projectile` tag; the Iskander is never lockable.

## Credits

- **Synarsis**: author of Air Tactical Arsenal, the original mod, code, models, textures and sounds.
- **lilDorito**: WarZone fork, the changes listed in [CHANGELOG.md](CHANGELOG.md).
- **Kalitovskiy**: the second Shahed engine sound (`shahed_engine_2.ogg`) and the Iskander flight sound
  (`iskander_flight.ogg`).
- **SuperbWarfare**: FuMO 25 radar model and explosion sounds (used from the SBW jar when installed).
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
