# Changelog — Air Tactical Arsenal WarZone

**Air Tactical Arsenal by Synarsis** - unofficial fork by **lilDorito**.
Based on Air Tactical Arsenal 1.1.0 (Forge 1.20.1). The original [LICENSE](LICENSE) is kept unchanged and
applies to this fork too.

"WarZone" versions of other mods (SuperbWarfare WarZone, AshVehicles WarZone, MCSP WarZone, and the older
patched SBW 0.8.7.1 / AshVehicles 4.2 builds) are the author's private forks. They are not published; they're
named here only because this fork is tested against them, and anything said about them applies only with
those builds. See the README's "WarZone versions" section.

## 1.3.3-warzone

### Strike drone cameras
- The camera of a strike drone works like a seeker. The mouse turns the camera as usual; there is no crosshair,
  only a small dot in the middle. Click to dive on what is under the dot, or hold a mouse button and move the
  mouse to draw a box from the middle (the view holds still while you draw). A box around one person, mob or
  vehicle follows that target as it moves, leading it; anything else aims at the ground under the middle of the
  box. The left button dives, the right button circles over the point or follows the target. Draw a new box at
  any time and the drone goes there instead; the dive starts at once, even while the drone is still climbing.
- R calls off the dive or the circling and sends the drone back along its route. Space frees the camera from the
  target, the wheel zooms, Left Alt leaves.
- The chosen box stays on the target with its distance (red for a dive, yellow for circling).
- Squares and boxes are placed with the exact view the frame was drawn with, so they sit on the targets on every
  drone, fast ones included.
- Drone cameras now see much further: the chunks and the people, mobs and vehicles around the drone are sent out
  to at least 16 chunks (256 blocks), or to your render distance if it is higher, up to 32. The minimum is
  `droneCameraChunkRadius` in `ata-common.toml` (16-32). New chunks are streamed nearest first: the server asks
  for them in the background with its own chunk tickets (never waiting on a load) and sends 8 per tick, and your
  game applies them a few per frame within a small time budget, so flying into new ground no longer makes the
  picture hitch.
- The drone camera moves smoothly every frame: the server sends the drone's position every tick stamped with
  the server tick, and the camera plays it back on a steady clock 100 ms behind, so late or bunched-up updates
  and short server hitches no longer make it speed up, jerk or snap back. Recon drones get the same.
- People, mobs and vehicles seen through a drone camera are updated every tick when they move (before: every
  3 ticks), so moving targets and the camera following them no longer judder.
- The ground seen through a drone camera is kept apart from the ground around you. Your own area stays loaded
  the whole time, so leaving the camera no longer reloads it and the world around you is there at once.
- A box drawn where there is no loaded ground (the sky, or terrain too far away) now sends the drone that way:
  the left button flies it there, the right button flies it there and then circles. The camera shows
  "FLYING TO SELECTED DIRECTION" or "LOITERING · flying there" instead of "ATTACKING".
- Drones, vehicles and mobs seen through a drone camera now show their current state: before, their synced data
  (a drone's heading and dive angle, flight phase, damage, a vehicle's state) stayed as it was when they first
  came into view, so other drones never pitched down in a dive and flew sideways to where they faced. The
  camera drone's own state is kept in sync the same way, so the camera's status line no longer stays on
  "ATTACKING" or "LOITERING" after you give the drone another order.
- A box drawn where your camera shows no ground always sends the drone that way, even if there is loaded ground
  out of your sight; it also frees the camera from the previous target.
- Circling a new point (right button) restarts the loiter clock.
- Banking a steered drone (A/D) rolls and turns the camera smoothly every frame instead of in 20 steps a second.
- The camera no longer jumps ahead for a moment when the drone you fly passes the edge of the area around you
  (the game drops and re-adds the drone there); this was most visible on fast steered drones. R no longer
  locks the camera onto the ground while you steer a drone by hand.
- When the drone you watch is destroyed, crashes or hits its target, the camera no longer drops you out at once:
  the picture inside the camera frame turns into black, flickering VHS noise with a red **NO SIGNAL**, without the drone's HUD.
  Left Alt leaves it, as before. Recon drones that land or are picked up still just close the camera.
- Air bursts (interceptor and missile hits on drones) are much quieter up close and fade out sooner.
- Add-on drones that blow up their own way (the Alexa Spatium's hit, for one) drop the camera before their bang
  plays, so you hear it from where you stand instead of at full volume from inside the exploding drone.
- The strike-dive key bind has a short name ("Strike drone: dive"); the long one pushed every name in the
  Controls screen off its left edge.
- Strike drones with a camera can lock a ground target with X: people, mobs and vehicles on the ground (never
  anything in the air). The target nearest the middle of the view, within 45°, is framed in yellow; X locks it and
  the drone goes for it, following it as it moves. Box-select drones dive on it as with a drawn box; drones you
  steer by hand (FP-1) fly themselves into it while you keep the camera free. X again or R drops the lock.
- Dives aimed from the camera turn tighter and slow down when needed, so close targets are hit instead of
  circled.
- Strike drones that loiter only start the loiter clock once they reach the circle over their last point;
  before that the camera shows it as on its route.
- Mouse clicks in any drone camera no longer use the item in your hand (the UAV Tablet opened again).
- The camera legend is shorter.
- The old controls (hold R to lock, press X to dive) are still there: set `strikeAim = "KEY"` in
  `ata-client.toml`.
- Drone cameras mark the people, mobs and vehicles they can see with white squares (green for you and your team).
  `targetSquares = false` turns the squares off.
- The camera no longer snaps your aim back to the middle every second or so.

### Steered drones
- Steered drones (FP-1, Alexa Spatium) start on autopilot: they fly their route until you press Space to take
  over, and Space hands them back.
- Add-on drones can be flown by hand (`isSteerable`): in their camera the drone turns toward where you look,
  and A/D bank it left and right (the camera rolls with it and the drone turns into the bank).
  Space hands it to the autopilot, which flies it on to the last point of its route (Space again takes it back).
  Leave the camera and it does the same and dives there.

### Strategic Map
- The UAV Tablet has a **Strategic Map** button: a terrain map of every drone you launched, wherever it is, not
  only those near you. Each drone shows its item icon and a dashed line to its target; drones close together
  share one icon with a count. Click an icon for the list of drones under it, then a drone to open its camera, or
  the large ↩ to call a recon drone back. Drag to pan, scroll to zoom, **Center on me** to come back. The hint
  line only lists the → route button and the map controls.
- Recon drones flying home point at their launcher. Landed recon drones are drawn without a line and stay on the
  map and in the tablet even after their chunk unloads (until the server restarts).
- The tablet's IN FLIGHT list also shows strike drones that are far away now.
- The drone list that opens on the map is as wide as its longest line instead of a fixed width.

### New route in flight
- Strike drones with a camera have a datalink: the tablet's IN FLIGHT list and the Strategic Map have a → button
  for them (and for recon drones) that opens the route map from the drone's position. The new route may be as
  long as the drone has range left, and replaces the old one, calling off a dive or a loiter.

### Salvos
- A tablet still links up to 10 launchers of both kinds, but a salvo is one kind only: checking (Shift+click) a
  launcher of the other kind starts a new selection, Select all picks the kind of the selected launcher, and the
  other kind is dimmed in the list.
- The **Strike salvo / Recon salvo** button launches the checked ready launchers (or all ready ones of that kind),
  up to 10. Under it: **Gap** between launches (0-10 s), **Spread** of the final targets (0-50 blocks, fanned out
  around the target so the drones don't all hit one spot) and **ToT** (time on target: launches are staggered so
  all drones arrive together, by route length and each drone's speed). The schedule runs on the server; you need
  to stay within tablet range of each launcher until it fires.
- Interceptor drones (registered with `DroneVariants.registerInterceptor`) get a purple dot in the tablet's
  launcher list and are never part of a salvo.
- The route screen of an interceptor is titled "INTERCEPTOR ROUTE".
- Interceptors also read "interceptor" in purple on the selected launcher's card, and their frame and target line
  on the Strategic Map are purple. Add-ons can change what a strike drone does at the end of its route:
  `loiterTicksMax()`, `attacksAtEnd()`, `onLoiterTimeout()`, with `flyHome(pos)` and `recoverAsItem(pos)`.
- Launchers without a route of their own join the salvo with the main launcher's route (the selected one, or the
  first checked launcher that has a route); their first points are skipped if they lie behind that launcher.
- Launchers far from you (outside the simulation distance) used to spin up and then stay on the rails, because
  their countdown only ran while their chunk was active. A launch now keeps its launcher's chunk active until the
  drone is away, for salvos and single launches. Each launcher spins up and plays its engine only when its own
  turn in the salvo comes.
- Add-on drones on launchers no longer all spin their propellers when the first launcher of a salvo spins up:
  `DroneVariantsClient.launcherDummy(type, launcher)` gives every launcher its own display model.

### Missile Targeting Map
- `iskanderMaxRange` in `ata-common.toml` can now be set up to 20000 blocks (was 10000); the default stays 5000.
- The Iskander target screen has a **TARGETING MAP** button: a terrain map around the launchpad with the maximum
  range as a red circle and the minimum range as a dashed ring (both from the server config). Click to set the
  target, **USE TARGET** fills in the coordinates.

### Alert Siren
- The siren sounds its alarm once (about 30 seconds) each time it is switched on, then stays quiet while the
  signal stays on. When the signal ends, the alarm stops and the siren plays the all-clear signal twice instead.
  With a radar in automatic mode that is once per raid, the all-clear when the sky is clear again; in manual mode
  **DECLARE ALL CLEAR** (the old cancel button) ends the alert and gives the all-clear. Plain redstone works the
  same way.

### Maps and the world border
- Route maps are sized by the range of the drones on the chosen launchers, so a drone with a longer route limit
  gets a bigger map. The Strategic Map covers the longest range of any drone.
- The world border is a hard limit for every map: route maps, the Strategic Map and the Air Defense Radar map
  never reach past it, and in a small world they get small (and sharp) too. Outside the border the map is shaded
  and a red line marks it; you can't pan past it, and a route point clicked beyond it lands on it.

### Launches
- A launcher that is destroyed or picked up is taken off every UAV Tablet linked to it right away (tablets
  carried by players online); an open tablet or Strategic Map updates at once.
- The Orlan's propeller spins up on the Light UAV Launcher before launch, like the Shahed's.
- Recon drones wait 5 seconds on the launcher before they go, like strike drones. Jet flames start small and grow
  while the drone waits on the launcher. The engine sound on the launcher fades out over two seconds after
  takeoff instead of cutting off.

### Drone sounds
- Drone engines get a Doppler shift (higher while coming at you, lower after passing) and reach you with the
  delay of sound travelling through air: a far drone is heard where it was a moment ago, and its engine stops
  when the sound of its explosion arrives, not when the explosion is drawn. `dopplerStrength` (0 turns it off) and
  `soundTravelDelay` in `ata-client.toml`. The engine sounds themselves are unchanged.
- The Shahed and the Orlan have a muffled far layer, so they stay audible as a faint drone out to 900 and 520
  blocks.
- Add-on drones can register their engine sounds by class (light, heavy, jet) and measured loudness, so drones of
  one class sound equally loud and carry equally far.
- Jet drones that are shot down scream: while they fall or spin the turbojet stays loud and its pitch wavers and
  climbs; with the engine knocked out it spools down instead. A drone shot down far away keeps sending its fall
  to every player, so its far engine sound changes too, and its explosion is heard from afar.
- Jet drones blast fire out of the nozzle while they wait on the launcher: anyone standing right behind gets set
  on fire and hurt, more as the engine spools up. The jet flame and trail are hidden for the player watching
  through that drone's own camera.
- `DroneVariants.registerJetBlast` marks a drone as a jet for that launch blast.

### For add-on drones
- `DroneVariants.registerRouteScale`: a drone can have its own route limit as a multiple of the Shahed's or
  Orlan's. The server sends the values to players (network protocol 28), and the route screens check the limit
  of the drones on the chosen launchers.
- `DroneVariants.registerSkins`: a drone picks one of several textures at random each time it is put on a
  launcher, and keeps it in flight.
- `DroneVariantsClient.registerJetExhaust`: a flame from a jet's nozzle while its engine runs. With AshVehicles
  installed it uses AshVehicles' afterburner texture, otherwise vanilla flames.
- `DroneVariantsClient.registerCameraOverlay`: an extra layer in a drone's camera view, and `hideCrosshair` for
  drones that draw their own sight.
- `DroneItemRenderer`: holds an add-on drone as its 3D model in hand, on the ground and in item frames, sized
  like the Shahed; the inventory keeps the flat icon (with a `forge:separate_transforms` item model).

## 1.3.2-warzone

### Item style and vehicle radar traits
- Tooltips use one layout: a short description, and controls while holding Shift (launchers, tablet, cable,
  terminal, radar, paint stand, spray can, Shahed, Orlan, Iskander). The long hint lines are gone. With Superb
  Warfare WarZone the names are also coloured by type (drones gold, launch and control gear dark aqua, the Iskander
  red); without it ATA shows the same tooltips itself.
- Vehicle containers and drone items show their radar traits: the on-board radar (type, range, cone, refresh or
  sweep) and the stealth signature, for example "Radar: AESA, 2000 m, ±60°, 4/s" and "Stealth: seen at 40%
  range". The server sends the vehicle radar data to players when they join and on `/reload`, so datapack
  changes show too (network protocol 20).
- Fixed a strike drone's model staying in the air near the crater after it hit: the client now removes the drone
  as soon as the server does.

### Launch pose and tablet icons
- A drone now leaves the catapult from exactly where it sat on it, at the rail's own angle (13° on the Heavy UAV
  Launcher, 9.75° on the Light one), so there is no jump at launch. Add-on drones tell ATA where their model sits
  on each launcher (`DroneVariants.registerRailMount`); without that they keep the old offset.
- UAV Tablet: the launcher checkboxes and the route line use the plain ✔ tick, and the return button's arrow is
  drawn twice as large.

### Recipes
- The UAV launchers, the UAV Tablet, the Air Defense Radar and the Alert Siren can be crafted (they were
  creative-only):
  - Light UAV Launcher: a rail of 2 steel ingots, a piston and 2 iron ingots for legs;
  - Heavy UAV Launcher: a rail of 2 steel ingots, a sticky piston, 2 iron blocks and a motor;
  - UAV Tablet: 6 iron ingots, a glass pane, a compass and redstone;
  - Air Defense Radar: 3 copper ingots, 4 steel ingots, an observer and a redstone block;
  - Alert Siren: 5 iron ingots, a note block and redstone.
  Without Superb Warfare, iron ingots replace the steel and a redstone block the motor.
- A mined Alert Siren drops itself (it dropped nothing).

### Thermal camera and crash sound
- With Superb Warfare installed (WarZone or upstream, any version that has thermal imaging), N in every drone
  camera switches Superb Warfare's thermal view instead of ATA's night vision: white-hot picture, warm bodies
  and vehicles stand out, sky and fog go dark. The HUD shows THERMAL (ТЕПЛОВІЗОР / ТЕПЛОВИЗОР) instead of NVG,
  and the night-time darkening and daytime NVG wash are off while it's on. Without Superb Warfare, or with a
  version that has no thermal imaging, N stays night vision. Your own thermal goggles keep working as before.
- A drone falling after it was hit no longer keeps its steady far-off flight sound: the distant layer now fades,
  drops in pitch and wobbles together with the close engine sound, as the Shahed's always did.

### Strike camera controls, explosion sound, translations
- New strike camera controls: hold R to lock a target, then press X to dive on it. The lock is a single
  sequence: the bar fills once and locks whatever you are aiming at when it fills, so moving the drone or the
  view no longer restarts it. The camera then stays on the locked point. Pressing X with nothing locked tells
  you to lock first. Press R again to drop the lock. X is Superb Warfare's lock key, or ATA's own key without it.
- A strike drone hands your view back before it explodes, so you hear the blast from where you stand, not from
  inside the drone.
- ATA's HUD texts (camera controls, phases, route screens) showed in English whatever the game language was.
  They now follow the selected language again; the Ukrainian and Russian camera hints are fully translated.

### Camera exit and lock-on
- Leaving a drone camera no longer drops the mobs and items around you into the void: each entity is sent back
  together with the chunk it stands in, as the chunks come back.
- Fixed being left frozen in an empty world after the camera closed. An explosion near the drone that didn't
  destroy it (a near miss, flak, another drone) closed the feed on your screen only, so the server kept you in the
  camera and never sent your chunks back. Closing the feed now always tells the server, and the server always
  brings you back, even if the drone is already gone.
- Drone types added by other mods through the variant API are put on Superb Warfare's radar scan list, the same
  way the Shahed, Orlan and Iskander already are.

### Route map and number formatting
- Numbers in ATA's screens and HUDs show as written in the language files again (whole metres, seconds, one
  decimal where meant). Minecraft rewrites `%.0f` and `%d` in language files to plain `%s`, so distances and times
  showed every decimal, like "3177.396938265414 bl.".
- Strike route map: distance and flight time share one line, and the waypoint list shows only as many of the last
  points as fit, so the Max line no longer runs into "Saved routes". The map scale moved into the map's
  bottom-left corner, out of the way of the TERRAIN PROFILE title (both route maps).

### Launch, dive and camera polish
- Catapult launch: drones now leave from where they sit on the rail instead of popping up half a block, and are
  thrown forward at catapult speed (18 m/s) at the rail's angle, then raise the nose steadily into the climb.
- Strike lock (hold X) no longer keeps restarting while the drone moves: once you aim at a spot, the lock stays on
  that spot as long as your view stays within 4° of it. R still only tracks a point with the camera; X locks the
  dive. R and X work together: track a spot with R, then hold X.
- Dive guidance rewritten: the drone turns toward the target in 3D at up to 7° per tick, slows to cruise speed
  while it's turning hard, then accelerates down a straight line onto the target. No more spiralling around it. A
  locked attack goes for the exact block you locked (not the top of the column above it) and dives as soon as it
  is within 350 blocks, from any heading. Shaheds without a camera dive the same way onto their target.
- Leaving a drone camera no longer stalls the game: the chunks around you come back nearest first, the closest
  25 at once and the rest over half a second, instead of all at once.
- When the drone you're watching explodes or crashes, the feed cuts at that moment and you hear the blast from
  where you actually stand, not at full volume from inside the drone.

### Strike cameras: loiter and lock
- Strike drones with a camera no longer dive on their own: after the route they circle over its last point (64
  blocks out) and wait. The camera shows LOITERING with the time left; after 15 minutes the drone dives on that
  point by itself.
- To attack, hold Superb Warfare's lock key (X by default; ATA's own "Lock strike dive target" key when Superb
  Warfare isn't installed) on a spot for about a second, as when locking with an interceptor: a ticking sound and a
  red bar fill up, a chime confirms. The drone leaves its route or loiter, turns and dives on that spot.
- The lock works on anything loaded: what you see through the camera (the terrain sent to you now reaches 160
  blocks around the drone), and past that along the line of sight on the server's loaded chunks, up to 1300 blocks
  from the drone.
- Works from the moment the drone climbs off the launcher: a lock during the climb levels it off and sends it
  straight to the target. While diving it can still be re-aimed up to 60° off its course.
- Left click no longer re-aims (it was too easy to trigger), and the drone now flies smoothly to the new target
  instead of veering about: on your screen it follows the server's position every tick instead of guessing its
  own path, which also cures the jitter when its route had turns.

### Smoother climb out
- Drones leave the launcher at the rail's angle (11°) and raise the nose gradually, at 6° a second, to the 20°
  launch angle and then the 25° climb, instead of jumping to it. Same for the Orlan.
- The Shahed's propeller on the heavy launcher spins up over the 4 seconds before launch.

### Add-on engine sounds carry properly
- A drone with its own engine sound is now heard over a set range with a smooth fade (`registerEngineSound` with a
  range), crossfading into its far-field layer. Before, its loudness also shrank its hearing range, so it went
  silent a few dozen blocks after launch until the far layer took over.

### Launch and camera fixes
- The launch flick is really gone now: a drone's first frame was drawn turning from due south and level, because
  the spawn packet set its angles without the previous-frame angles, and until its synced angles arrived the
  client pointed it south. Both are fixed for every drone built on the Shahed and the Orlan.
- The launcher spin-up uses the loaded drone's own engine sound and loudness (and an optional start one-shot,
  `DroneVariantsClient.registerStartSound`) instead of always the Shahed's.
- Drone cameras no longer freeze when the drone flies past your render distance. The game stopped sending you the
  drone (and the drones around it) once it left your own tracking range, so the view stuck until you reopened it
  from the tablet; now the camera drone and everything within 160 blocks of it are sent straight back.

### Strike drone cameras
- Strike drones can now carry a camera (`hasCamera()` on the Shahed class; the Shahed itself has none). The UAV
  Tablet opens it from the IN FLIGHT list like the Orlan's: same view, zoom, night vision, **R** lock and **Left
  Alt** to leave, plus an orange strike HUD with the target, distance to it, phase, health, speed and the aim
  point. The camera can also look 30° above the horizon to watch the route ahead.
- **Left click re-aims the dive** at the aimed block (or the locked point): the drone drops the rest of its route
  and heads for the new target, also mid-dive. Up to 1000 blocks from the drone.
- While watching, the terrain and drones around it are streamed to you as with the Orlan; your body stays where
  you left it and falls if it should.

### Engine sounds per drone
- Add-ons can give their drones their own engine loop, volume and pitch (`DroneVariantsClient.registerEngineSound`).
  Variants without one keep the Shahed or Orlan sound.
- Add-ons can also add a far-field loop (`registerFarSound`): a second, distant layer that fades in past a set
  distance and carries to a set range, follows the drone and fades out with it.

### Hit zones for add-on drones
- The Orlan's hit zone (engine, wings, nose, body) now comes from `crashZoneAt(hitPos)`, which drones built on it
  can override; the Shahed already had `determineHitZone`. Add-ons can use both to read zones from their own
  part hitboxes instead of the Shahed-sized guesses.

### Smoother launch
- Shaheds, Orlans and every drone built on them no longer flick nose-down for a moment when they leave the
  launcher: they spawned pitched 20° down and only turned to the 20° climb on the next server update.

### One UAV Tablet
- The Light and Heavy UAV Tablets are now one **UAV Tablet** (`ata:uav_tablet`). It links up to 10 launchers of
  either weight. Old Light, Heavy, Strike, Recon, Orlan and control tablets in inventories turn into it; their
  links are kept.
- One screen instead of the STRIKE / RECON pages. The launcher list shows every linked launcher: what it carries
  (red dot strike, blue dot recon, empty circle nothing), whether it has a route, and H or L for its weight. The
  selected launcher shows its drone by name. ROUTE opens the Shahed or the Orlan route map for that drone, LAUNCH
  and LAUNCH ALL start whatever is loaded. Routes for strike and recon launchers are set separately; with a mixed
  selection, ROUTE takes the ones of the selected launcher's type. An empty launcher counts as strike if it's
  heavy, recon if it's light.
- IN FLIGHT lists every drone you own that is still flying, strike and recon, from any launcher, including ones
  you have since unlinked. Drones with a camera open it on click; recon drones also have route change and return.
  Strike drones without a camera just show their position and phase.
- The screen refreshes itself every 2 seconds, so new launches, loaded launchers and landed drones show up
  without reopening it.

### Light and Heavy UAV launchers
- The Recon UAV Launcher is now the **Light UAV Launcher** and the Strike UAV Launcher the **Heavy UAV
  Launcher**, item and entity IDs included. Old worlds are converted: placed launchers,
  loaded drones and tablets in inventories keep working.
- A launcher takes every drone of its weight, strike or recon. The Shahed is heavy, the Orlan light; add-ons set
  the weight of their own drones (EW WarZone: ZALA KUB-BLA light, Bayraktar TB2 heavy).
- Putting a drone on the wrong launcher says which one it needs.
- Saved strike coordinates are kept per launcher instead of per list position, so loading a drone elsewhere no
  longer shifts them.
- Shift + right click with an empty hand takes the drone off a heavy launcher in one click, as on the light one.
  Before, the first click on a Shahed without a target silently aimed it 500 blocks ahead.

### Interceptor hits
- API for add-on interceptor drones: `interceptorHit(healthFraction, attacker, hitPos)` on the Shahed and Orlan (and
  every drone built on them) deals a share of the drone's health as a heavy hit that can bring it down at once.

### Manual radar alerts
- **ALARM OUTPUT: ON/OFF** is replaced by **ALERTS: AUTO / MANUAL**. Auto works as before. Manual never starts an
  alert on its own; a radar that had its output switched off becomes manual.
- In manual mode **DECLARE ALERT** gives full redstone output until **CANCEL ALERT**, and every radar joined to it
  by redstone dust, repeaters or comparators (up to 4,096 blocks of wiring) goes on alert too and drives its own
  sirens. A linked radar shows **ALERT FROM A LINKED RADAR** and stops about 2 seconds after the alert is
  cancelled. Wiring loops can't keep an alert going.
- The mode and a declared alert are saved with the radar.

### Drone variants for add-ons
- New API (`api/DroneVariants`, `api/DroneVariantsClient`) lets another mod add its own strike and recon drones
  built on the Shahed and Orlan. SBW Electronic Warfare WarZone 1.2.0 uses it for KUB-BLA and Bayraktar TB2.
- The strike and recon launchers remember which drone item was loaded, launch the matching drone, and give the
  same item back when it's taken off, dropped or the launcher is broken. The recon launcher keeps the item's
  data (for example missiles loaded on a TB2).
- An add-on can draw its own drone on a loaded launcher.
- Shahed and Orlan speeds, turn rate, health, blast size and the item returned on pickup can be changed by
  subclasses. The Shahed and Orlan themselves are unchanged.
- Every message, tablet line, camera header and route label names the drone that is actually flying: "Bayraktar
  TB2 launched", "ZALA KUB-BLA mounted on launcher", "BAYRAKTAR TB2 // RECON" in the camera. Shahed and Orlan read as
  before.
- Generic wording where no drone is named: route screens read STRIKE UAV ROUTE / RECON UAV ROUTE, the launcher
  tooltips no longer list one model, "No drone on the launcher!".
- API additions for add-ons: a drone name per variant, a hook when a loaded launcher is right clicked (the TB2 uses
  it for its missile loadout), and a panel slot in the recon camera overlay under the flight timer, drawn as part of
  ATA's own overlay so nothing covers it.

## 1.3.1-warzone

### Air Defense Radar alarm
- The alarm ignores everything belonging to the radar's placer or the placer's scoreboard team: their planes,
  helicopters and vehicles (anyone aboard), drones, missiles, rockets and shells, also when the shooter is in a
  vehicle, logged out or in another dimension. Before, a placer without a team counted nothing as friendly, so
  every friendly missile and plane set the siren off.
- The alarm stays on until the sky has been clear for 5 seconds. A contact that blinks out for a moment (crossing
  the minimum altitude, or several contacts coming and going) no longer restarts the siren; once the sky has
  been clear for 5 seconds, the next contact sets it off again.
- New **ALARM OUTPUT: ON/OFF** button at the top of the radar's target list turns the redstone output off and on.
  The placer, their teammates and operators can use it; it is saved with the radar.
- The siren starts at once when powered again shortly after it stopped. Before, it could stay silent for up to
  28 seconds, until its previous loop would have ended.

### Radar contacts
- The radar screen, its target count and the alarm see missiles, drones and aircraft over chunks that aren't
  loaded (for example a long-range AGM still on its way). Before, only contacts over loaded chunks showed, so two
  missiles fired a second apart could show as one, or not at all until they were close. Friendly marks on the
  screen use the same rules as the alarm, for the player looking at it.

## 1.3.0-warzone

### Vehicle radars
- Fighters, attack helicopters and AA vehicles from SuperbWarfare and AshVehicles get an on-board radar, shown in
  a small black-and-green panel on the left edge of the screen to everyone aboard. Everything is in ATA; the
  vehicle mods are unchanged, and it works with the public builds too.
- Two shapes: AA vehicles get a **round** scope around the vehicle (north up; heading up in the client config),
  aircraft and helicopters a **cone** spreading out from the nose.
- Two radar types:
  - **MSA** (mechanical): a beam sweeps the scope (it turns round on AA vehicles, swings side to side like a
    windscreen wiper on aircraft). A contact is only updated when the beam passes over it, then fades until the
    next pass. Ground clutter hides anything flying lower than 30 blocks above the ground (10 for helicopter
    radars, 20 for AA radars).
  - **AESA**: no beam; every contact in range updates several times a second, low fliers included.
- Contacts use the ground radar's classes and colours (PLN, HEL, UAV, MSL), with a short line showing where each
  is heading. Teammates (same scoreboard team) are hollow squares, others filled squares of the same size. A legend next to the scope shows the radar
  type and range. Aircraft sitting on the ground aren't shown.
- Stealth aircraft are seen from shorter range: F-117 and B-2 at 35%, F-22 and YF-23 at 40%, F-35 at 45%, J-20
  and X-47B at 50%, Su-57 and RAH-66 at 60%. Shaheds are seen at 70% and Orlans at 60%.
- The server does the scanning, so the radar sees well past view distance, and a modified client can't see more
  than its radar shows.
- Starting balance (range in blocks, half-width of the cone, refresh):

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

  No radar: Su-25, A-10, MiG-15, F-117, B-52, C-130, AC-130H/U, V-22, UH-60, MH-60M, AH-6, Reaper, X-47B, Ju-87
  and the small SBW aircraft.
- All of it is data in `data/ata/vehicle_radars/`, so a datapack can rebalance it or give radars to other addon
  vehicles (see the README). `vehicleRadars = false` in `ata-common.toml` turns vehicle radars off.
- Client settings in `ata-client.toml`: show or hide (also the **I** key, rebindable), position (left centre by
  default), size (`vehicleRadarPanelSize`, default 73), and heading-up for round scopes.

### Ground radar sweep
- The Air Defense Radar's sweep line is real now: it turns clockwise once every 4 seconds, and a contact's dot
  only moves when the line passes over it, then fades until the next pass. A contact that has left is dropped
  when the line next passes where it was. The target list shows the same, last-swept positions.
- Teammates of the player looking at the radar (same scoreboard team) show as hollow circles.

### Radar alarm (redstone)
- The redstone signal now reacts to everything the radar screen shows: SuperbWarfare and AshVehicles aircraft,
  helicopters and guided missiles (AGMs included) as well as Shaheds, Orlans and the Iskander. It used to react
  only to Shaheds and the Iskander.
- It follows the radar's own rules: contacts below `radarMinAltitude` don't count, and the distance is measured
  across the ground like on the screen.
- It ignores the team of whoever placed the radar: their teammates' aircraft and the missiles and drones they
  launched don't set it off. A radar placed before this update (or by a player without a team) still reacts to
  everything.

### Tablet range
- How close you must be to a Strike or Recon UAV launcher to save a route or launch is now one setting,
  `tabletLauncherRange` (default 100 blocks, 30-250), sent to players when they join. The tablet screens, route
  saving and launching used different, hard-coded limits (30 in some places, 100 in others), so a launch could be
  refused while the tablet said you were in range, and the other way round. Messages show the real limit.

### Long-range locking
- ATA kept registering Shaheds, Orlans and Iskanders with SuperbWarfare's long-range tracking only until the
  first error; one failed call switched it off for the rest of the session, so with SuperbWarfare WarZone the
  drones could drop off the tactical map and out of long-range locks. A single failed call is now skipped; it
  only switches off after 50 failures in a row.

### Upgrade notes
- Network protocol 15 -> 16: server and all clients must update to 1.3.0-warzone together.

## 1.2.0-warzone

### Shot-down drones fall out of the sky
- Shaheds and Orlans no longer just explode in the air when shot down. Where you hit them decides how they go
  down:
  - **engine** (the tail): the engine dies and the drone glides down in a widening turn, trailing smoke;
  - **wing**: it goes down spinning and burning, in one of three ways picked at random: a flat spin, a
    corkscrew dive rolling around its flight path, or a steep nose-down plunge rolling around its own axis;
  - **body or warhead**: it tumbles end over end.
- Engine and wings have their own share of the drone's health (40% and 30% each), so enough hits on one of them
  bring it down before the drone's total health runs out.
- A falling Shahed's warhead explodes when it hits the ground (`shahedCrashDetonation`, default on); a falling
  Orlan crashes with a small blast, fire, smoke and debris, seen up to 512 blocks and heard up to about 450
  (SuperbWarfare's explosion sounds when installed, ATA's otherwise).
- The engine sound follows what happens: it winds down after an engine hit, keeps its normal sound but fades
  and rises slightly in pitch in a spin, and cuts out in a tumble.
- A single hit of at least 3 times the drone's max health (a missile or a direct flak burst) still blows it
  apart in the air (`airburstDamageMultiplier`). Warhead hits at low health can still set the warhead off.
- Every drone is a little different: each one gets its own random toughness (incoming damage is divided by 1 to
  2.5) and slightly different engine and wing strength, and it takes 2 to 5 hits before it can start falling at
  all. Direct hits from SuperbWarfare cannon shells and missile bursts still bring it down at once.
- A Shahed's warhead sometimes goes off the moment it is shot down: 15% of the time it explodes in the air instead
  of falling (`shahedShotDetonationChance`).
- A falling drone takes another 3 to 8 hits (random for each drone) before it gives up: then a Shahed's warhead
  explodes and an Orlan breaks apart in the air with the same small blast, fire and debris as its ground crash.
  The Orlan has no warhead, so it never blows up the moment it is shot down.
- Turn it all off with `droneCrashOnShootdown = false` to get the old mid-air explosion.

### Drones stop circling waypoints
- A Shahed or Orlan no longer flies a full circle back to a waypoint it has already flown past. After launch it
  climbs straight for 300 blocks, so a first waypoint closer than that, or two waypoints close together with a
  sharp turn, used to make it loop back around. It now counts the waypoint as reached and flies on to the next one.
  A waypoint that really lies behind the launcher is still flown to.
- A Shahed whose target lies inside its turning circle starts its dive instead of circling the target forever.

### Iskander hits the ground
- The Iskander could explode in mid-air: it looked for the ground only from 50 blocks above to 100 blocks below the
  target height setting (`targetYLevel`, default 64), so on superflat worlds, deep valleys and high mountains it
  found nothing and burst at that height. It now loads the target chunk at launch and takes the real top block at
  the impact point, after the random miss distance is applied, so it also no longer bursts above a hillside next
  to the target.
- The Shahed finds the ground the same way, which is also much cheaper than scanning every block of the column.

### Fair anti-air damage
- SuperbWarfare anti-air shells and the laser tower delete any projectile they touch, so a single AA shell used to
  blow a Shahed apart and an Orlan out of the sky. Each touch now deals 15% of the drone's max health
  (`flakHitDamagePercent`), to a random part of it, so a burst of several hits brings it down, usually falling.
- Direct hits from other SuperbWarfare cannon shells (AP, HE and the rest) always bring a drone down. Their blast
  still damages a drone it bursts next to.

### Louder Iskander impact, audible missile kills
- The Iskander's impact is the loudest explosion in the pack. It stacks SuperbWarfare's epic explosion layers
  (out to 2,048 blocks; the huge ones on SBW builds without them), SBW's huge explosion close up, and ATA's own
  blast twice, out to 1,600 and 1,000 blocks with a slow fade. Without SuperbWarfare the old ATA sound plays as
  before.
- A missile that downs a Shahed or Orlan (Igla, Stinger, other anti-air missiles) now makes a full-volume air
  burst heard up to 1,200 blocks, louder than an anti-air shell burst. Before, only the missile's own small
  explosion was heard, so it was quieter than the AA guns.

### Quieter Iskander
- The flight roar now ends together with the explosion it leads to, and a late position update can no longer
  start it again after the impact.
- The Iskander's flight roar is softer and starts much closer: faintly at 1,500 blocks (was 3,000), growing
  gradually as it approaches, and peaking at about 60% of the old volume.

### Lockable from far away, sounds that don't drop out
- With SuperbWarfare WarZone, Shaheds, Orlans and Iskanders are sent to players beyond normal entity range, so
  radar-guided missiles (AIM-120 and the like) can lock a drone out to the weapon's own seek range instead of only
  inside the server's view distance. They also show on the SBW tactical map once a radar sees them.
- The Orlan engine and the Iskander countdown and flight sounds are no longer streamed, so they keep playing when
  many other long sounds are running.

### Launchers, tablets and terminal for every drone and missile
- The launch gear is no longer named after one drone or missile, so later addons can use it for theirs:

  | Old name | New name | New id |
  |----------|----------|--------|
  | Shahed Launch Platform | Strike UAV Launcher | `ata:strike_uav_launcher` |
  | Shahed Control Tablet | Strike UAV Tablet | `ata:strike_uav_tablet` |
  | Orlan-10 Launch Platform | Recon UAV Launcher | `ata:recon_uav_launcher` |
  | Orlan-10 Control Tablet | Recon UAV Tablet | `ata:recon_uav_tablet` |
  | Missile Launcher (Iskander pad) | Missile Launchpad | `ata:missile_launchpad` |
  | Iskander Control Terminal | Missile Targeting Terminal | `ata:missile_terminal` |
  | Launch Interface Cable | Missile Link Cable | `ata:missile_link_cable` |

- Existing worlds keep everything: placed blocks, launchers, items in inventories and chests are moved to the new
  ids when the world loads. Commands, datapacks and recipes from other mods that name the old ids need the new
  ones.
- Names are updated in English, Ukrainian and Russian, and the screens are titled after the new names.

### Language follows Minecraft
- ATA texts now use each player's Minecraft language, the same way SuperbWarfare WarZone and its addons do:
  Ukrainian or Russian when the game is set to it, English otherwise. Nothing depends on the server any more.
- Removed the separate ATA language setting (`config/ata-client.toml`, `/atalang`) and the server's
  `allowClientLanguage` option.

### Upgrade notes
- Network protocol 14 -> 15: server and all clients must update to 1.2.0-warzone together.

## 1.1.4-warzone

### Radar: one set of contact types
- Every contact now has one of four types, whatever mod it comes from:
  - **PLN** planes and airships (SBW, AshVehicles and other addons);
  - **HEL** helicopters;
  - **UAV** large drones: Shahed, Orlan and AshVehicles' MQ-9 Reaper and X-47B (found by name, no list needed);
  - **MSL** missiles: SBW and AshVehicles guided missiles (air-to-ground, anti-air, anti-tank) and the Iskander.
- The separate SHD / BAL / ORL tags and their legend rows are gone; the legend shows the four types.
- Small SBW drones and swarm drones are never shown.
- The altitude threshold (`radarMinAltitude`) now applies to every contact, the Iskander included.
- Missiles are found through SBW's missile base class where it exists, and by class name on older SBW
  builds and on AshVehicles 4.2, so all supported versions keep working. New list `radarMissileIds` for
  anything that is not detected.

### Radar range and route length
- New `radarRange` (default 1,500, up to 5,000 blocks). The radar map, range rings, zoom and redstone output
  follow it; past 1,536 blocks the terrain map gets coarser.
- Shahed and Orlan routes: default 10,000 blocks as before, now up to 30,000 (was 20,000). The route map
  grows with the longer of the two limits (coarser when bigger) and the route screens zoom out far enough to show it.
- Orlan flight time can go up to 180 min (was 120); about 60 min suits a 30,000-block route.
- Radar and route maps are rebuilt once after the update (their scale is now part of the cache).

### Damage on the SuperbWarfare scale
- Shahed hits like SBW's medium aerial bomb, the Mk-82: 650 @ 4 / 22 blocks (was 60 @ 3 / 14).
- Iskander hits like a heavy bomb: 1600 @ 6 / 40 blocks (was 160 @ 5 / 26). That matches the FAB-500 in
  the private AshVehicles WarZone build; for comparison, upstream SBW's Mk-84 is 1300 / 32.
- With SBW installed both are in SBW's `superbwarfare:aerial_bomb` entity tag, so each vehicle applies its own
  aerial-bomb armour modifier to them, the same one it uses for SBW's bombs. The multipliers come from the
  installed SBW and its addons (the private SuperbWarfare WarZone build uses its own, lower values), so how
  many hits a vehicle takes depends on the pack.
- Existing `ata-common.toml` files are updated once: values still at the old defaults (damage, Iskander
  radii) move to the new defaults, changed values stay.

### Iskander flight sound
- A new looping roar by **Kalitovskiy** follows the Iskander from launch to impact and can be heard up to
  3,000 blocks away, fading with distance and keeping its direction. It ends with the speed-of-sound delay, together with the
  explosion.

### SuperbWarfare air defense
- SBW's small cannon shells and its laser tower delete any projectile they touch that SBW doesn't know how to
  damage, so an Iskander could vanish mid-flight. That covers SBW's LAV-AD and addon anti-air vehicles that
  fire SBW shells (for example the Gepard and Pantsir in AshVehicles and the Typhoon in MCSP). The Iskander now ignores that and keeps flying; it
  still can't be locked.
- An Orlan hit that way is shot down with smoke and fire instead of disappearing silently.

### With SuperbWarfare installed
- Recipes use SBW parts:
  - Shahed: steel, a fuse, 3 high-energy explosives, a motor and a propeller;
  - Iskander: steel, a seeker, 3 high-energy explosives and 3 missile engines;
  - Orlan-10 (new recipe): steel, a seeker, a motor and a propeller.
  Without SBW the old vanilla Shahed and Iskander recipes apply.
- Shahed and Iskander explosions play SBW's three huge-explosion sounds (close, far, very far), mixed by ATA
  according to distance: they keep their direction, carry up to 1,024 / 1,536 blocks and arrive with the
  speed-of-sound delay. Without SBW, or on an SBW build that lacks those sounds, the old ATA sounds play.
- Shahed, Orlan and Iskander register with SBW's long-range entity tracking, so SBW radars and the tactical
  map show them. That tracking exists in SBW 0.8.9.1 and later; on older versions this step is skipped.
  SBW only tracks entity types in its `scan_white_list` server setting, so ATA adds its three entries there by
  itself; nothing has to be configured.

### Missiles against Shahed and Orlan
- Guided missiles locked on a Shahed or Orlan (Igla, Stinger, SBW and AshVehicles anti-air missiles) now
  detonate when they pass within 4 blocks of it and bring it down. Before, a fast missile often slipped
  past the small drone between two ticks, lost the target and flew on upwards. Works with any SBW version
  whose missiles keep a target id.

### Shahed and Orlan launch
- On launch the drone pointed the opposite way for a moment and then flipped round: its starting heading
  used a different angle convention from the one used in flight. Launched from the terminal it always
  started facing south. Both now start facing the way they fly.
- The Shahed's first moments in flight no longer jitter: the client predicts the climb in the launch
  direction instead of straight up.
- The 5-second countdown on the Shahed launcher played a furnace crackle, which sounded like static. It
  now plays the drone's own engine (the one it will fly with), spinning up in pitch and volume until it
  takes off.

### Radar map fix
- In loaded chunks (the square around players) the map showed the block under the surface, so grass looked
  like dirt and water was one block shallower. It now shows the top block. Radar and route maps rebuild once;
  terrain tiles fix themselves as players pass by, or right away with `/ata terrain rescan`.

### Upgrade notes
- Network protocol 13 -> 14: server and all clients must update to 1.1.4-warzone together.

## 1.1.3-warzone

### Newer SuperbWarfare and AshVehicles versions
- Fixed a crash when placing (or looking at) the radar with SuperbWarfare 0.8.9 or newer. SBW 0.8.9 moved
  the FuMO 25 model files, so the radar asked GeckoLib for a model that no longer existed.
  - The radar now finds the FuMO 25 model wherever the installed SBW keeps it (0.8.7–0.8.8 and 0.8.9+
    layouts), and spins the right part of it in both.
  - If the model can't be found or loaded (for example in a future SBW version), the radar uses the
    original ATA block instead, with a normal hitbox outline, and the log says why. The game doesn't crash.
- Radar detection works with SBW 0.8.8+, where SBW removed its aircraft interfaces. Vehicles are classified
  by their SBW vehicle type: airplanes and airships are **PLN**, helicopters **HEL**, drones **UAV**.
  This also covers AshVehicles 4.3+ aircraft. Tanks, APCs, AA, cars, artillery and boats are never shown.
  The `radarPlaneIds` / `radarHelicopterIds` / `radarDroneIds` / `radarExcludedIds` lists still override it.
- Lock-on with newer versions: SBW 0.8.8+ seekers and AshVehicles 4.3+ missiles lock Shahed and Orlan through
  the same `destroyable_projectile` tag as before; the Iskander stays unlockable.
- Checked against SBW 0.8.8, 0.8.8-hotfix, 0.8.8-final, 0.8.9-snapshot, 0.8.9, 0.8.9-final, 0.8.9.1, 0.8.9.2
  and AshVehicles 4.3, 4.5, 4.6. Behaviour with the private WarZone patched builds (SBW 0.8.7.1, AshVehicles 4.2)
  is unchanged.

### Iskander range
- Fixed the Iskander terminal and target settings screens ignoring the server's `iskanderMinDistance` and
  `iskanderMaxRange`. They read the player's own local config (default 1000–5000), so a server with
  `iskanderMaxRange = 10000` still showed "1000–5000" and refused targets past 5000. The server now sends
  its values to every player on join, and again whenever `ata-common.toml` is edited while the server runs
  (the route limits are re-sent the same way).

### Sound
- New second Shahed engine sound by **Kalitovskiy**. Each Shahed picks one of the two engine sounds at
  random when it is launched and keeps it for the whole flight (also after a save/reload); every player
  hears the same one.

### Interface
- The "Scanning: x%" / "Queued" / "Loading" map indicator on the radar and on the Shahed and Orlan route
  screens moved from the centre of the map to its bottom edge, on a dark plate, so it no longer sits under
  the launcher, radar and route markers.

### Upgrade notes
- Network protocol 12 -> 13: server and all clients must update to 1.1.3-warzone together.

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
