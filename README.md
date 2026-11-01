# Puddle Jumper – Minecraft-Mod (Forge 1.20.1 / 47.4.16)

Ein fliegbarer Puddle Jumper aus *Stargate Atlantis*, ca. 8 Blöcke lang, mit Tarnung,
Laderaum, ausfahrbaren Antriebsgondeln und zielsuchenden Drohnen.

## Bauen & Starten

```bash
./gradlew build            # Mod-JAR -> build/libs/puddlejumper-1.0.0.jar
./gradlew runClient        # Entwicklungs-Client
./gradlew runServer        # Entwicklungs-Server (Verzeichnis run-server/)
./gradlew runClient -PjoinServer=localhost   # Client verbindet direkt zum Server
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
| Laderaum öffnen (von außen) | Schleichen + Rechtsklick |
| Laderaum öffnen (als Pilot) | Inventar-Taste (`E`) |
| Vor / zurück | `W` / `S` (in Bugrichtung) |
| Drehen | `A` / `D` |
| Steigen / Sinken | `Leertaste` / `Strg` (Sprint-Taste) |
| Umschauen | Maus (bewegt nur die Kamera; die Kamera dreht beim Kurven mit) |
| Drohne abfeuern | Linksklick (zielt auf anvisiertes Wesen, sonst nächster Feind vor dem Schiff) |
| Tarnung an/aus | `C` |
| Antriebsgondeln aus/ein | `G` (sonst automatisch: gelandet eingefahren, in der Luft ausgefahren; beim Feuern immer aus) |
| Aussteigen | `Shift` |

Die Tasten `C` und `G` lassen sich unter *Optionen → Steuerung → Puddle Jumper* ändern.

Der Jumper ist träge: Er beschleunigt und dreht gemächlich und rollt aus. Ohne Eingabe bremst er ab und
schwebt auf der Stelle – auch wenn man in der Luft aussteigt, bleibt er dort stehen. Die Düsen leuchten,
sobald er in der Luft ist, und ziehen Ionenfahnen, die mit dem Tempo länger werden; gelandet sind sie aus.
Beim Einsteigen schaltet die Kamera automatisch in die Außenansicht, beim Aussteigen zurück in die
vorherige Ansicht.
Getarnt ist der Jumper für andere Spieler nur noch als schwacher Schimmer zu sehen, bleibt aber fest
(man kann nicht hindurchlaufen). Mobs verlieren den Piloten aus den Augen.
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
  client/PuddleJumperRenderer.java    Animationen (Gondeln, Neigung, Tarnung, Ionenfahnen)
  client/…                            HUD, Tasten, Kamera, Triebwerkssound
  test/JumperGameTests.java           GameTests
```

### Modell

`tools/build_model.py` nimmt `assets/puddlejumper.obj` als Basis und ergänzt:

- **Antriebsgondeln** links/rechts (halbe Schiffslänge, mit Lamellen), je zwei blaue Düsenfenster,
  Ausleger und gelbe Drohnen-Ports
- **Textur** in doppelter Auflösung im Oliv-Khaki der Serie (Vorlage: `assets/bilder/`), mit eingravierten
  Paneellinien und kupferfarbenem Fensterrahmen – Rumpf, Unterseite und Gondeln einheitlich
- **Drohne** (goldener, leuchtender Körper mit Schweifen) und ein gerendertes Item-Icon

Nach Änderungen: `python3 tools/build_model.py`, im Spiel neu laden mit `F3+T`.
