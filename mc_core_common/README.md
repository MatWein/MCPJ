# mc_core_common

Minecraft-facing code (and resources) that compiles **unchanged** against every supported Minecraft version and
loader. It has no build of its own: each `mc_<version>_common` module adds these folders to its source set, so the
classes are compiled (and checked) once per Minecraft version.

Rule of thumb when adding a Minecraft version: port `mc_<version>_common`, then move every file that ends up
identical in all versions here. Code that is the same in all versions but needs a different Minecraft API should be
split so that the version-independent part can live here (or in `core`, if it needs no Minecraft at all).

Small API differences (a renamed method, a changed constructor) do not need a copy per version: each
`mc_<version>_common` has the same compat classes with the same static methods, and the shared code calls those.

- `de.mcpj.mc.compat.McCompat`: common/server calls (riding, teleport, packets, sounds, particles)
- `de.mcpj.mc.client.ClientCompat`: client calls (screen/HUD state, camera, input, `HudCanvas` for the GUI graphics)
- `de.mcpj.mc.gametest.GameTestCompat`: what the shared game tests need

Shared code must not name a type that differs between versions (e.g. `ResourceLocation`/`Identifier`); pass the
compat method's result on directly instead.
