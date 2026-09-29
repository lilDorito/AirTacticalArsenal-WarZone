# Changelog — Air Tactical Arsenal WarZone

**Air Tactical Arsenal by Synarsis** - unofficial fork by **lilDorito**.
Based on Air Tactical Arsenal 1.1.0 (Forge 1.20.1). The original [LICENSE](LICENSE) is kept unchanged and
applies to this fork too.

## 1.1.2-warzone

### Radar and route maps: real terrain
- The radar map and the Shahed/Orlan route maps now show the **real terrain** instead of biome colours:
  the top block of every spot, like a vanilla map (grass, forests, sand, stone, snow, roads and buildings),
  with hill shading. Water gets darker the deeper it is.

### Whole-map terrain scan (new command)
- `/ata terrain scan [size] [centerX centerZ]` scans the whole map once and stores it in the world folder
  (`world/ata_map/`, tiles of 512×512 blocks). Default: a 10000×10000 square centred on 0, 0.
  - Reads the chunks saved on disk (e.g. a world pregenerated with Chunky mod) in the background, without
    stalling the server. Progress is shown in chat every 10% and in the server log.
  - Tiles that already exist are skipped, so an interrupted scan continues where it stopped.
    `/ata terrain rescan [size] [centerX centerZ]` scans everything again.
  - `/ata terrain status` shows progress and how many tiles are stored; `/ata terrain cancel` stops it
    (tiles done so far are kept).
  - When the scan finishes, all radar and route maps are rebuilt from the tiles.
- Every radar and route map inside the scanned area is then assembled from the tiles almost instantly,
  including radars placed later. The tiles travel with the world folder.
- While a terrain scan runs, radar and route maps wait for it (they show as queued).

### Where map data comes from
1. chunks the server has loaded right now,
2. the terrain tiles from `/ata terrain scan`,
3. with `mapReadSavedChunks = true` (default), other chunks saved on disk (read in the background),
4. biome colours for everything else, so maps have no holes.

### Keeping maps up to date
- Every `mapRefreshMinutes` (default 10, 0 = never) the chunks loaded around players are painted into the
  terrain tiles and into the radar and route maps, so they show new buildings and craters. This runs in
  the background.
- Players get the updated map when they next open a radar or route screen (clients keep a downloaded map
  for up to 5 minutes).
- `mapReadSavedChunks` and `mapRefreshMinutes` are in the `[Radar]` section of `ata-common.toml`.
- Existing radar and route maps are rebuilt once after the update (old map files are deleted).
- `radarDarkBackground = true` still disables the radar map and its scanning completely.

### Upgrade notes
- The network protocol is unchanged (still 12), so 1.1.1 clients can join a 1.1.2 server, but only 1.1.2
  clients pick up map updates without reconnecting. Updating both is recommended.

## 1.1.1-warzone

### Radar
- New server option `radarDarkBackground` (`[Radar]` section of `ata-common.toml`, default `false`).
  Set it to `true` and the radar screen shows a plain dark background instead of the biome/terrain map;
  targets, range rings, sweep and everything else work as before.
- With it on, the terrain around radars is never scanned, which also saves server work.
- Players see the change the next time they open a radar.

### Upgrade notes
- Network protocol 11 -> 12: server and all clients must update to 1.1.1-warzone together.

### Fixes
- Fixed a server crash ("Markers should never be sent") while a player watched through an Orlan camera near
  a `minecraft:marker` entity (used by datapacks, maps and command setups). The camera now skips entities
  that are never sent to clients.

## 1.1.0-warzone

First release of the fork. Everything below is relative to upstream Air Tactical Arsenal 1.1.0.

### Upgrade notes
- **Server and clients must run the same build**: the network protocol changed (6 -> 11), so upstream
  clients cannot join a WarZone server and the other way round.
- `ata-common.toml`: unused keys are dropped by Forge automatically. Existing configs keep their old values,
  so check `iskanderCraterRadius` (new default 11) and the new damage values below.
- Radars that were already placed switch to the new model on their own.
- The default blacklist zones (coordinates of the original author's event server) were removed.

---

### Radar

**Detection**
- SuperbWarfare aircraft are detected through SBW's own aircraft/helicopter interfaces, which covers every
  SBW addon, including all AshVehicles planes and helicopters.
- New contact type **UAV**. MQ-9 Reaper and X-47B are shown as UAVs.
- ATA contacts have type tags like the others: **SHD** (Shahed), **BAL** (Iskander, ballistic),
  **ORL** (Orlan-10), next to **PLN**, **HEL** and **UAV**.
- Colours match the legend: Shahed bright red, Iskander bright yellow, Orlan bright cyan. They apply to map
  markers, trails and the target list.
- The Iskander no longer disappears a moment after launch. The radar used to search only up to its own
  Y + 350, while the Iskander climbs to `iskanderMaxFlightHeight` (Y=600 by default). It now searches the
  whole world height, up to the Iskander's maximum height. The redstone output uses the same range.

**Open sky requirement**
- The radar needs open sky. Any block above its level in the 5×5 area around it (glass, leaves and water
  included) blocks it. A blocked radar shows an empty map with a blinking red **WEAK SIGNAL** and a hint,
  and its redstone output is 0.
- Can be turned off with `radarRequiresOpenSky`.

**Looks** (when SuperbWarfare is installed)
- The radar uses the model, texture, item icon and hitbox of SuperbWarfare's **FuMO 25** radar. The dish
  spins all the time, one turn every 10 s, like a charged FuMO 25.
- SBW's assets are loaded from the SBW jar while the game runs. Nothing from SBW is included in this mod.
- Without SuperbWarfare the radar keeps the original ATA block.

**Screen**
- Fixed panning: the range circle no longer slides across the terrain map while dragging.
- The status bar no longer overlaps when there are 10 or more targets.
- The radar drops itself when mined with a pickaxe in survival (not when blown up).

**Config** (`[Radar]` section)
- `radarMinAltitude` (default 90): the radar horizon, which used to be hard-coded. The legend shows the
  server's value.
- `radarPlaneIds`, `radarHelicopterIds`, `radarDroneIds`, `radarExcludedIds`: force a contact type for any
  entity id, or hide it.
- `radarRequiresOpenSky` (default true).

### Air-to-air: AshVehicles vs ATA drones
- AshVehicles aircraft can **see** the Shahed-136 and Orlan-10 on their HUD and **lock** them with AAM-4
  missiles, with single and multi lock.
- The **Iskander-M is never** shown or lockable. SuperbWarfare's own drone stays unlockable, as before.
- Side effect: SuperbWarfare's own seekers (for example the Igla MANPADS) can lock ATA drones too.
- These hooks load only when AshVehicles is installed. If a future AshVehicles version changes the hooked
  code, the feature turns itself off instead of crashing the game.
- A Shahed on a paint stand, or with shoot-down disabled, cannot be locked.

### Damage and destruction
- Removed the extra damage multiplier against vehicles (it was ×3.5). Vehicles take normal damage, and
  their own SuperbWarfare armour still applies.
- Lower explosion damage. It is full inside a core radius, then falls off to 0:
  - Shahed: 60 damage, 3-block core, 0 at 14 blocks (was 800 to players / 1100 to entities, 25 blocks).
  - Iskander: 160 damage, 5-block core, 0 at 26 blocks (was 1000 / 1500, 36–40 blocks).
  - All of it can be changed in the config.
- The block-breaking blast no longer damages entities a second time. `vanillaExplosionHurtsEntities`
  brings that back.
- **Round craters**: the Iskander used to dig a square crater, and the Shahed used to dig one about as deep
  as the Iskander's. Both now dig a round bowl with a ragged rim:
  - `iskanderCraterRadius` default 11 (~20 wide, ~7–8 deep); `shahedCraterRadius` default 5.
  - An air burst (over trees or tall grass) digs the crater into the ground below it (up to 16 blocks down).
  - Drops, claim and protection mods, and TNT still work as with a normal explosion. Blocks with blast
    resistance above 100 (obsidian and similar), unbreakable blocks and liquids survive.
- Shooting Shaheds down works with SuperbWarfare guns as well as TacZ. No integration was needed: SBW
  bullets already count as normal hits (hit zones, minimum-damage filter, multiplier, smoke, critical
  detonation).

### UAVs
- The maximum route length can be set in the config: `shahedMaxRouteDistance` and `orlanMaxRouteDistance`,
  default 10,000 blocks (range 1,000–20,000). The server checks it when routes are saved, when drones launch
  and when an Orlan's route changes in flight. Route screens show the server's value.
- Orlan flight time can be set with `orlanFlightTimeMinutes`, default 30 minutes (it was a fixed 20).

### Sound
- Engine sounds of the Shahed and Orlan, and the Iskander countdown, come back by themselves if Minecraft
  drops them. Minecraft does this on dimension change, respawn, F3+T, or when too many sounds play at once.
- Players who arrive during an Iskander countdown now hear it. It used to start only on the countdown's
  first tick.
- The Shahed engine sound no longer stops after 5 minutes, which silenced long flights.
- Flight, explosion and countdown sounds only reach players in the same dimension. Players in the Nether no
  longer hear Overworld launches at the same coordinates.
- Siren: stops as soon as its redstone signal turns off, is heard only in its own dimension, and loops
  cleanly.

### Language
- ATA is in **English by default** for every player, whatever their Minecraft language.
- Russian and Ukrainian (new) can be chosen per player in `config/ata-client.toml` or with
  `/atalang <en_us|ru_ru|uk_ua>`.
- Servers can force English for everyone: `allowClientLanguage = false`.
- Removed all hard-coded Russian text (about 330 strings in 37 files). Server messages are translated for
  each player. Server logs are in English.
- Fixed keys that existed only in Russian, so English players no longer see raw keys. Fixed the
  "Select all / Deselect all" tablet button showing a raw key after a click.
- The mod name "Air Tactical Arsenal WarZone" is never translated. This includes the creative tab and the
  item tooltips in creative search.

### Interface
- Tablets, route maps, the Iskander terminal and the target settings screen scale down to fit instead of
  being cut off at large GUI scales.
- Orlan camera HUD: the bottom line (distance, SPD, controls) no longer overlaps.

### Fixes and cleanup
- Fixed a pickaxe mining tag that listed a block which doesn't exist. In 1.20.1 this can break pickaxe
  mining for every block in the pack.
- Removed sound events whose files never existed, which caused missing-sound log spam.
- Removed parts that never worked: the unregistered Shahed Control Terminal and its packets, the unused
  coin launch cost, config options that did nothing, an empty mixin, dead code, unused translation keys and
  unused assets.
- Versions have a single source (`gradle.properties`). Metadata was fixed (license, attribution, Forge and
  GeckoLib ranges, a logo that didn't exist). Checked against GeckoLib 4.8.2.
- Project renamed. `build.bat` works from any folder.
- The jar includes the license file.
