# Puddle Jumper – Minecraft-Mod (Forge 1.20.1 / 47.4.16)

Ein fliegbarer Puddle Jumper aus *Stargate Atlantis*, ca. 8 Blöcke lang, mit Tarnung,
Heckklappe samt Laderaum und zielsuchenden Drohnen.

## Bauen & Starten

```bash
./gradlew build            # Mod-JAR -> build/libs/puddlejumper-1.0.0.jar
./gradlew runClient        # Entwicklungs-Client
./gradlew runGameTestServer  # automatische Tests (headless)
```

Benötigt Java 17 (wird von Gradle bei Bedarf automatisch geladen).

## Bekommen

- Kreativ: Reiter **Werkzeuge & Hilfsmittel**
- Rezept (Werkbank):

  | | | |
  |---|---|---|
  | Eisenblock | Getöntes Glas | Eisenblock |
  | Diamant | Redstoneblock | Diamant |
  | Eisenblock | Truhe | Eisenblock |

Item auf den Boden setzen → der Jumper erscheint in Blickrichtung.

## Bedienung

| Aktion | Taste |
|---|---|
| Einsteigen | Rechtsklick auf den Jumper |
| Laderaum öffnen (von außen) | Schleichen + Rechtsklick (Heckklappe öffnet sich automatisch) |
| Laderaum öffnen (als Pilot) | Inventar-Taste (`E`) |
| Vor / zurück | `W` / `S` (fliegt in Blickrichtung, auch auf- und abwärts) |
| Seitlich | `A` / `D` |
| Steigen / Sinken | `Leertaste` / `Strg` (Sprint-Taste) |
| Drehen | Maus – der Bug folgt der Blickrichtung |
| Drohne abfeuern | Linksklick (zielt auf anvisiertes Wesen, sonst nächster Feind vor dem Schiff) |
| Tarnung an/aus | `C` |
| Heckklappe auf/zu | `H` |
| Antriebsgondeln aus/ein | `G` (fahren bei hoher Geschwindigkeit und beim Feuern auch automatisch aus) |
| Aussteigen | `Shift` |

Die Tasten `C`, `H`, `G` lassen sich unter *Optionen → Steuerung → Puddle Jumper* ändern.

Ohne Eingabe bremst der Jumper ab und schwebt auf der Stelle. Ohne Pilot sinkt er langsam zu Boden.
Getarnt ist der Jumper für andere Spieler unsichtbar, und Mobs verlieren den Piloten aus den Augen.
Beim Abfeuern einer Drohne wird die Tarnung deaktiviert. Zerstört wird er wie ein Boot, nur deutlich
robuster; dabei fallen Item und Ladung heraus.

## Projektaufbau

```
assets/                               Original-Vorlage (OBJ, Textur, Sound)
tools/build_model.py                  erzeugt das verbesserte Modell + Texturatlas + Drohne + Item-Icon
tools/preview.py                      Software-Renderer für Vorschaubilder (python3 tools/preview.py out.png [--open])
tools/make_test_structure.py          GameTest-Plattform
src/main/java/de/matthias/puddlejumper/
  entity/PuddleJumperEntity.java      Flugphysik, Multipart-Hitbox, Laderaum, Tarnung, Drohnenstart
  entity/PuddleJumperPart.java        Bug-/Heck-Hitbox
  entity/DroneEntity.java             zielsuchende Drohne
  client/ObjModel.java                eigener OBJ-Loader/-Renderer (Gruppen, *_glow leuchtet)
  client/PuddleJumperRenderer.java    Animationen (Klappe, Gondeln, Neigung, Tarnung)
  client/…                            HUD, Tasten, Kamera, Triebwerkssound
  test/JumperGameTests.java           GameTests
```

### Modell

`tools/build_model.py` nimmt `assets/puddlejumper.obj` als Basis und ergänzt:

- **Heckklappe** als eigenes Teil (Rampe, klappt um die Unterkante auf), mit Innenseite und Kanten
- **Innenraum**: Innenhülle, Boden, Pilotensitze, Konsole mit Display, Schott mit Durchgang, Seitenbänke
- **Antriebsgondeln** links/rechts mit leuchtenden Düsen, Streben und gelben Drohnen-Ports
- **Textur** in doppelter Auflösung, mit Metallstruktur, kühlerem „Antiker“-Farbton und getöntem Cockpitglas
- **Drohne** (goldener, leuchtender Körper mit Schweifen) und ein gerendertes Item-Icon

Nach Änderungen: `python3 tools/build_model.py`, im Spiel neu laden mit `F3+T`.
