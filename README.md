# MCPJ

MCPJ adds a flyable Puddle Jumper from *Stargate Atlantis* to Minecraft Forge 1.20.1 (Forge 47.4.16).
It works on its own; other mods such as Just Stargate Mod or Mekanism are optional.
About 10 blocks long, with cloak, shield, cargo hold, retractable drive pods, homing drones,
headlights and an energy system that works with any Forge Energy mod.

## Getting started

- Creative inventory, tab **Tools & Utilities**: *Puddle Jumper* and *Jumper Charging Pad*
- There are no crafting recipes yet.

Use the item on the ground and the jumper appears facing your view direction.
New jumpers start with 20 % energy.

## Controls

| Action | Key |
|---|---|
| Board | Right-click the jumper |
| Open cargo hold (outside) | Sneak + right-click |
| Open cargo hold (as pilot) | `I` |
| Forward / reverse | `W` / `S` (along the bow) |
| Turn | `A` / `D` (the camera keeps its direction) |
| Strafe | `Q` / `E` (replaces drop/inventory while piloting) |
| Climb / descend | `Space` / `Ctrl` (sprint key) |
| Look around | Mouse (moves the camera only) |
| Cycle target | `Tab` (nearest first, then the nearest not yet selected; after the last one: no target; after a 3 s pause it starts over) |
| Launch drone | Left click (flies to the selected target regardless of view; without target to the nearest hostile ahead) |
| Toggle cloak | `C` |
| Toggle shield | `V` |
| Toggle headlights | `L` (replaces advancements while piloting) |
| Extend / retract drive pods | `G` (otherwise automatic: retracted when landed, extended in the air, always extended when firing) |
| Leave | `Shift` |

All jumper keys can be rebound under *Options → Controls → MCPJ*.

## Features

**Flight.** The jumper is inert: it accelerates and turns gently and coasts. Without input it brakes and hovers in
place, even after the pilot leaves it mid-air. Boarding switches to third person, leaving restores the previous view.
The engine glows and trails ion plumes while airborne and is silent when landed. Edge guidance slides the ship past an edge it
barely touches (up to half a block sideways or up/down, `flight.edgeGuidance` in the server config), e.g. into a
stargate.

**Cloak.** Invisible to others apart from a very faint energy bubble, but still solid. The pilot sees the ship as a
faint glowing outline. Mobs lose track of the pilot. Launching a drone decloaks.

**Shield.** A gently wobbling energy bubble with a water look that absorbs all damage as long as there is energy;
hits make it flash.

**Targeting and drones.** Living entities and uncloaked jumpers within 64 blocks are marked (hostiles red,
jumpers cyan, others white; the selected target bold with name and distance). Drones home in on their target and
explode on impact (slightly weaker than a creeper, breaking blocks only with `mobGriefing`); the own jumper and its
passengers are never harmed.

**Hull.** 240 hull points (a diamond sword needs about 35 hits). A hostile drone destroys 60 % of the hull, so the
second one is fatal. There is no self-repair, and the hull takes damage in creative mode too. A jumper that is at
least half submerged loses 1 hull point per second without shield. Lava burns 8 hull points per second as soon as
any of it touches the hull, flowing lava included (e.g. a bucket poured over the ship); an active shield absorbs
this as long as there is energy. The jumper ignores knockback from explosions, hits, mobs and water currents.

**Passengers** cannot be hurt while seated; the hull takes the hits instead (only `/kill` and the void still apply).
Mobs that go for the pilot therefore attack the ship: melee mobs hit the hull once they stand next to it, arrows and
explosions hit it anyway (mobs never target players in creative mode, as usual).
When the hull is destroyed, the passengers are thrown out first and the jumper explodes (about as strong as TNT,
breaking blocks only with `mobGriefing`), which can still hit them. Its item and cargo drop afterwards (only the cargo
in creative mode).

**Collisions.** Brushing walls, light bumps and normal landings (even at full sink rate) are harmless. From about
8 m/s against an obstacle or 15 m/s downwards damage increases, up to at most **30 % of the hull** per impact
(full speed into a wall or a crash from great height). The shield absorbs impacts as well.

**Headlights.** Minecraft has no moving light sources, so the server places invisible vanilla light blocks
(`minecraft:light`, level 15) in the hull and ahead of the bow and removes them behind the ship. A light moves only
once the jumper is 2 blocks away and at most every 4 ticks; above 10 m/s only the bow light is used. Cloaked
jumpers stay dark.

**Cargo.** 54 slots. Dropped items close to the hull are collected automatically, like a player does.

**Underwater.** Everything is slower: thrust and turning are halved and drones fly at half speed.

**Lava.** Thrust and turning drop to 30 %, the ship brakes harder and sinks slowly when the power fails.

**Portals and stargates.** The jumper travels as one unit: passengers stay seated, cargo, energy and hull are kept,
and the ship keeps its speed along its new heading. Seated passengers and the hull segments never travel on their
own. With *Just Stargate Mod* this also works for gates in the same dimension (JSG is optional; the integration
uses reflection). Flying into the receiving side of a gate or into a kawoosh destroys the ship, as with any entity.

## Energy

The jumper stores Forge Energy (FE); everything costs energy, in creative mode too.

| Consumer | Default |
|---|---|
| Capacity | 60 M FE (≈ 150 MJ in Mekanism) |
| Drive, landed | 0 |
| Drive, airborne | 250 FE/t hovering up to 1,500 FE/t at top speed (≈ 3 h 20 min hovering, ≈ 33 min full speed ≈ 40 km) |
| Drive underwater / in lava | × 1.5 / × 2 |
| Shield | 500 FE/t, × 2 underwater, × 3 in lava, plus 200 FE per absorbed hull point |
| Cloak | 400 FE/t, × 1.5 underwater, × 2 in lava |
| Under fire (2 s after a hit) | shield and cloak × 2 |
| Drone | 150,000 FE per launch |
| Headlights | 20 FE/t |

If the energy for an absorbed hit is missing, the shield collapses and the hit goes through. With an empty storage
every system fails (shield, cloak, lights, drive and controls) and the jumper **crashes**. The HUD shows charge,
consumption or charging power and the remaining time; below 10 % it blinks and an alarm sounds.

### Jumper Charging Pad

A block that accepts Forge Energy from every side (Mekanism, Thermal, Immersive Engineering, …; Mekanism joules
are converted automatically). A **landed** jumper charges from all pads directly below its hull with
10,000 FE/t per pad, so a larger landing field charges faster.

- off (dark) → powered (glowing runway lights) → charging (bright, light level 15)
- the landing arrow points in the direction the player faced when placing it
- a comparator reads the charge of the jumper currently charging

## Configuration

Gameplay values live in the server config `world/serverconfig/mcpj-server.toml` (synced to clients):
flight model, hull and impact damage, energy consumption, drones, targeting ranges, headlights, cargo pickup and
the charging pad. The client config `config/mcpj-client.toml` holds the camera distance and the low energy
warning threshold.

## Building

The project is split into modules so the game logic does not have to be rewritten for every Minecraft version
or mod loader:

| Module | Contents |
|---|---|
| `core/` | the game logic in plain Java 17, without any Minecraft or mod loader API; version-independent assets |
| `mc_1.20.1_common/` | vanilla Minecraft 1.20.1 code shared by both loaders (entities, blocks, rendering, HUD, input, network messages, game tests); compiled into each loader module, never shipped on its own |
| `mc_1.20.1_forge_47/` | Forge 47: registration, network transport, config, events, part entities, Forge Energy, Just Stargate Mod |
| `mc_1.20.1_fabric/` | Fabric: registration, network transport, TOML config with server sync, mixins, hull segments, Team Reborn Energy (optional) |
| `mc_core_common/` | Minecraft code and resources that compile unchanged against every supported version; no build of its own, each `mc_<version>_common` compiles it in |
| `mc_26.3_common/` | vanilla Minecraft 26.3 code shared by both loaders, including vanilla mixins both loaders use (camera, keys, attack, hiding the pilot, cloak) |
| `mc_26.3_neoforge/` | NeoForge: registration, payloads, config, events, part entities, energy capability |
| `mc_26.3_fabric/` | Fabric: registration, payloads, TOML config with server sync, hull segments, explosion mixin |

Each loader module builds its own jar: `mcpj-<version>+mc<minecraft version>-<loader>.jar` in `<module>/build/libs/`.
Gradle 9 runs on Java 25 (Gradle finds or downloads it, as well as Java 17 for the 1.20.1 modules). Build plugins: ModDevGradle (Forge 1.20.1 via `legacyforge`, NeoForge) and Fabric Loom.

```bash
./gradlew build                                                  # all modules and jars
./gradlew :core:test                                             # fast unit tests of the game logic (no Minecraft)

./gradlew :mc_1.20.1_forge_47:runClient                          # Forge development client
./gradlew :mc_1.20.1_forge_47:runClient -PjoinServer=localhost   # ... joining a server directly
./gradlew :mc_1.20.1_forge_47:runGameTestServer                  # Forge game tests (headless, fresh world each run)
./gradlew :mc_1.20.1_forge_47:runGameTestServer -Pstandalone     # the same without any other mod

./gradlew :mc_1.20.1_fabric:runClient                            # Fabric development client
./gradlew :mc_1.20.1_fabric:runServer                            # Fabric development server
./gradlew :mc_1.20.1_fabric:runGametest                          # Fabric game tests (the same 36 tests)

./gradlew :mc_26.3_neoforge:runClient                            # Minecraft 26.3: NeoForge client, server, game tests
./gradlew :mc_26.3_neoforge:runServer
./gradlew :mc_26.3_neoforge:runGameTestServer
./gradlew :mc_26.3_fabric:runClient                              # Minecraft 26.3: Fabric client, server, game tests
./gradlew :mc_26.3_fabric:runServer
./gradlew :mc_26.3_fabric:runGametest
```

Minecraft 26.3 game tests use the vanilla registry: `McpjGameTests` (mc_26.3_common) lists all tests with their time
limits, both loaders register the functions and the build generates the test instances
(`data/mcpj/test_instance/*.json`) from that list.

`mc_1.20.1_forge_47/run-server/` is a regular Forge server for multiplayer tests (start with `run.sh` there after
copying the Forge jar into its `mods/`). The Forge build also loads every other jar in that `mods/` folder
(deobfuscated) into its development runs. Its `runClient` additionally gets Embeddium and Oculus (shaders) from the
Modrinth maven; they are client-only and therefore copied to `run/mods` instead of the shared classpath. Versions are
set in `mc_1.20.1_forge_47/gradle.properties`. None of these mods is required: `-Pstandalone` leaves them all out.

On Fabric the settings live in `config/mcpj-server.toml` and `config/mcpj-client.toml` (same keys as on Forge); a
server sends its settings to joining players. The only required dependency is the Fabric API.

## Project layout

```
core/src/main/java/de/mcpj/core/       game logic without Minecraft or a mod loader (plain Java 17)
  config/                              all settings (ConfigBuilder: implemented per platform)
  jumper/                              the ship: flight, energy, hull, shield/cloak/lights, pods, weapons, animation;
                                       JumperHost/JumperSync are what a platform provides
  drone/  pad/                         homing drone, charging pad
  targeting/  hud/                     target cycling, HUD content (lines, warnings, colors)
  render/                              OBJ mesh, shield bubble, ion plumes, attitude (VertexSink: drawn by the platform)
  math/  util/                         vectors, boxes, units, save data
core/src/test/                         unit tests of the game logic (no Minecraft)
mc_1.20.1_common/src/main/java/de/mcpj/mc/
  entity/  block/  item/               entities and blocks hosting the core logic (loader hooks are abstract)
  client/                              rendering, HUD, input, camera, sounds
  network/  rules/  platform/          messages, gameplay rules, McpjPlatform (what a loader provides)
  gametest/                            the game test cases (registered by each loader)
mc_core_common/src/main/java/de/mcpj/mc/  the part of the packages above that is identical in all Minecraft versions
                                       (version differences via McCompat / ClientCompat / GameTestCompat)
mc_26.3_common/src/main/java/de/mcpj/mc/  as mc_1.20.1_common, plus registry/McpjObjects (builds all objects) and mixin/
mc_26.3_neoforge/src/main/java/de/mcpj/neoforge/  registry, entity (part entities), block (energy), network, config, event, client
mc_26.3_fabric/src/main/java/de/mcpj/fabric/      registry, entity (segments), block, network, config, client, mixin
mc_1.20.1_fabric/src/main/java/de/mcpj/fabric/
  registry/  entity/  block/  network/ config/  compat/  client/  gametest/
  mixin/                               hooks Fabric has no events for (segments, cloak, camera, keys, ...)
mc_1.20.1_forge_47/src/main/java/de/mcpj/forge/
  registry/  entity/  block/           registration, part entity segments, Forge Energy
  network/  config/  client/           SimpleChannel, Forge config adapter, Forge client events
  event/  compat/                      Forge events (rules, travel, id migration), Just Stargate Mod
  gametest/                            game test registration (Forge annotations)
tools/                                 texture, test structure and preview scripts
```

### Model

`models/entity/puddle_jumper.obj` and the texture atlas `textures/entity/puddle_jumper.png` are checked in as they
are; there is no generator for them anymore. The atlas regions used by the Java code (ion plume) are documented
in `IonPlumeRenderer`. `tools/preview.py` renders preview images of the current model.
