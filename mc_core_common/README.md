# mc_core_common

Minecraft-facing code (and resources) that compiles **unchanged** against every supported Minecraft version and
loader. It has no build of its own: each `mc_<version>_common` module adds these folders to its source set, so the
classes are compiled (and checked) once per Minecraft version.

Rule of thumb when adding a Minecraft version: port `mc_<version>_common`, then move every file that ends up
identical in all versions here. Code that is the same in all versions but needs a different Minecraft API should be
split so that the version-independent part can live here (or in `core`, if it needs no Minecraft at all).
