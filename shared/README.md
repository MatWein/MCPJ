# shared

Code that is written once and compiled into several modules. None of these folders has a build of its own: each
module adds the folders it needs to its source set (see its `build.gradle`), so the classes are compiled (and
checked) once per Minecraft version or loader. Put new code in the widest folder where it compiles; never copy a
file between versions.

| Folder | Compiled into |
|---|---|
| `mc_all/` | every `mc_<version>_common` (vanilla Minecraft code and resources for all versions) |
| `mc_1.20.1-1.21.1/` | `mc_1.20.1_common`, `mc_1.21.1_common` (old rendering, interpolation, NBT saving, annotation game tests) |
| `mc_1.21.1-26.3/` | `mc_1.21.1_common`, `mc_26.3_common` (payload messages, some mixins) |
| `fabric/` | every Fabric module |
| `fabric_1.20.1-1.21.1/` | Fabric 1.20.1 and 1.21.1 |
| `fabric_1.21.1-26.3/` | Fabric 1.21.1 and 26.3 |
| `neoforge/` | every NeoForge module |

Rule of thumb when adding a Minecraft version: port `mc_<version>_common` and the loader modules, then move every
file that ends up identical into the matching folder here (or widen a range folder, or add one). Code that is the
same in all versions but needs a different Minecraft API should be split so that the version-independent part can
live in `mc_all/` (or in `core`, if it needs no Minecraft at all).

Small API differences (a renamed method, a changed constructor) do not need a copy per version: each
`mc_<version>_common` has the same compat classes with the same methods, and the shared code calls those.

- `de.mcpj.mc.compat.McCompat`: common/server calls (riding, teleport, packets, sounds, particles, building objects)
- `de.mcpj.mc.client.ClientCompat`: client calls (screen/HUD state, camera, input, `HudCanvas` for the GUI graphics)
- `de.mcpj.mc.gametest.GameTestCompat`: what the shared game tests need
- base classes for overridden Minecraft methods whose signatures differ: `JumperEntityCompat`, `DroneEntityCompat`,
  `SavedBlockEntity`, `ModEntityBlock`, `ModItem` (loader side: `SegmentEntityCompat`, `PartEntityCompat`,
  `FabricCompat`, `NeoForgeEnergy`, `NeoForgeClientServices`)

Shared code must not name a type that differs between versions (e.g. `ResourceLocation`/`Identifier`); pass the
compat method's result on directly instead.
