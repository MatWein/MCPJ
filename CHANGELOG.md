# Changelog

## 1.1.0 (2026-11-08)

Changes since 1.0.0.

### Added

- **Puddle Jumper Key**: a key card that is paired with a jumper and locks it on the first right-click; further
  right-clicks unlock or lock the ship. A locked jumper can only be boarded, and its cargo hold only opened, by someone
  carrying its key. Locking throws out passengers without the key, and a paired jumper does not accept a second key.
- **Finding your jumper**: while you carry a paired key, its jumper is marked with a yellow frame on your screen, even
  when cloaked, within tracking range (160 blocks at most).
- **Navigation**: each jumper has a memory of up to 64 named waypoints, managed by anyone on board in the navigation
  console (`N`). A compass band in the pilot's HUD shows the heading, the destination, its distance, height difference
  and the arrival time.
- **Creative tab**: all items and blocks of the mod are now in their own *MCPJ* tab instead of *Tools & Utilities*.

### Changed

- Charging pads now repair the hull of a landed jumper once its battery is full (`chargingPad.fullRepairEnergy`,
  0 turns it off).
- The Jumper Projector can also be started by right-clicking its top and confirming the question. The eight lights on
  top show the build progress, and with `projector.requirePower = true` the outline disappears while there is no
  energy.
- First person view in the jumper hides the own ship and looks straight ahead along the bow.

### Development

- The project is grouped by Minecraft version (`versions/<version>/`, `shared/`), the Gradle logic moved into convention
  plugins (`build-logic/`). Module names stay `mc_<version>_<loader>`, except Forge: `:mc_1.20.1_forge`.
