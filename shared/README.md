# shared

Code that is written once and compiled into several modules. None of these folders has a build of its own: each
module adds the folders it needs to its source set (see its `build.gradle`), so the classes are compiled (and
checked) once per Minecraft version or loader. Put new code in the widest folder where it compiles; never copy a
file between versions.

| Folder | Compiled into |
|---|---|
| `mc/all/` | every `common` module (vanilla Minecraft code and resources for all versions) |
| `mc/1.20.1-1.21.1/` | the `common` modules of 1.20.1 and 1.21.1 (old rendering, interpolation, NBT saving, annotation game tests) |
| `mc/1.21.1-26.3/` | the `common` modules of 1.21.1 and 26.3 (payload messages, some mixins) |
| `fabric/all/` | every Fabric module |
| `fabric/1.20.1-1.21.1/` | Fabric 1.20.1 and 1.21.1 |
| `fabric/1.21.1-26.3/` | Fabric 1.21.1 and 26.3 |
| `neoforge/all/` | every NeoForge module |

Which folders a version uses is listed in `versions/<version>/gradle.properties` (`shared_mc`, `shared_neoforge`,
`shared_fabric`, `shared_forge`); each folder has `java/` and optionally `resources/`.

Rule of thumb when adding a Minecraft version: copy a `versions/<version>/` folder, adjust its `gradle.properties`,
add the version to `settings.gradle` and port its modules; then move every file that ends up identical into the
matching folder here (or widen a range folder, or add one). Code that is the same in all versions but needs a
different Minecraft API should be split so that the version-independent part can live in `mc/all/` (or in `core`, if
it needs no Minecraft at all).

Small API differences (a renamed method, a changed constructor) do not need a copy per version: each
`common` module has the same compat classes with the same methods, and the shared code calls those.

- `de.mcpj.mc.compat.McCompat`: common/server calls (riding, teleport, packets, sounds, particles, building objects)
- `de.mcpj.mc.client.ClientCompat`: client calls (screen/HUD state, camera, input, `HudCanvas` for the GUI graphics)
- `de.mcpj.mc.gametest.GameTestCompat`: what the shared game tests need
- base classes for overridden Minecraft methods whose signatures differ: `JumperEntityCompat`, `DroneEntityCompat`,
  `SavedBlockEntity`, `ModEntityBlock`, `ModItem` (loader side: `SegmentEntityCompat`, `PartEntityCompat`,
  `FabricCompat`, `NeoForgeEnergy`, `NeoForgeClientServices`)

Shared code must not name a type that differs between versions (e.g. `ResourceLocation`/`Identifier`); pass the
compat method's result on directly instead.
