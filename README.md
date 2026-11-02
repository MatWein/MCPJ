# Puddle Jumper – Minecraft-Mod (Forge 1.20.1 / 47.4.16)

Ein fliegbarer Puddle Jumper aus *Stargate Atlantis*, ca. 10,4 Blöcke lang, mit Tarnung,
Schutzschild, Laderaum, ausfahrbaren Antriebsgondeln und zielsuchenden Drohnen.

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
| Laderaum öffnen (als Pilot) | `I` |
| Vor / zurück | `W` / `S` (in Bugrichtung) |
| Drehen | `A` / `D` |
| Seitlich fliegen (ohne Drehung) | `Q` / `E` (im Jumper statt Wegwerfen/Inventar) |
| Steigen / Sinken | `Leertaste` / `Strg` (Sprint-Taste) |
| Umschauen | Maus (bewegt nur die Kamera; beim Drehen mit `A`/`D` bleibt die Kamera stehen) |
| Ziel wechseln | `Tab` (nächstgelegenes zuerst, dann immer das nächstgelegene noch nicht gewählte; nach dem letzten: kein Ziel; nach 3 s Pause beginnt es wieder beim nächsten) |
| Drohne abfeuern | Linksklick (fliegt zum gewählten Ziel, egal wohin man schaut; ohne Ziel zum nächsten Feind vor dem Schiff) |
| Tarnung an/aus | `C` |
| Schutzschild an/aus | `V` |
| Licht an/aus | `L` (im Jumper statt Fortschritte) |
| Antriebsgondeln aus/ein | `G` (sonst automatisch: gelandet eingefahren, in der Luft ausgefahren; beim Feuern immer aus) |
| Aussteigen | `Shift` |

Die Tasten `C`, `V`, `G`, `L`, `Tab`, `Q`/`E` und `I` lassen sich unter *Optionen → Steuerung → Puddle Jumper* ändern.

Der Jumper ist träge: Er beschleunigt und dreht gemächlich und rollt aus. Ohne Eingabe bremst er ab und
schwebt auf der Stelle – auch wenn man in der Luft aussteigt, bleibt er dort stehen. Die Düsen leuchten,
sobald er in der Luft ist, und ziehen Ionenfahnen, die mit dem Tempo länger werden; gelandet sind sie aus.
Beim Einsteigen schaltet die Kamera automatisch in die Außenansicht, beim Aussteigen zurück in die
vorherige Ansicht.
Getarnt ist das Schiff für andere unsichtbar, nur eine sehr dezente Energieblase verrät es; es bleibt
aber fest (man kann nicht hindurchlaufen). Der Pilot sieht sein Schiff als schwachen Leuchtumriss.
Mobs verlieren den Piloten aus den Augen.
Im Jumper werden alle Lebewesen und andere (ungetarnte) Puddle Jumper in 64 Blöcken Umkreis dezent
markiert – Feinde rot, Jumper cyan, sonst weiß; das gewählte Ziel kräftig mit Name und Entfernung. Drohnen explodieren beim Einschlag (etwas kleiner als ein Creeper,
Blockschaden nur bei aktivem `mobGriefing`); der eigene Jumper und Pilot bleiben verschont.
Das Triebwerk ist auch von außen zu hören, solange es läuft (in der Luft oder mit Pilot).
Der Jumper bekommt keinen Rückstoß: Explosionen, Treffer, Spieler, Mobs oder Wasserströmung verschieben ihn nicht
(Schaden nimmt er trotzdem, außer mit aktivem Schild).
Der Schutzschild legt eine sanft wabernde Energieblase mit verschwommenem Wasser-Effekt um den Jumper
und macht ihn unzerstörbar; Treffer lassen die Blase kurz aufleuchten.
Beim Abfeuern einer Drohne wird die Tarnung deaktiviert.
Mit `L` leuchtet der Jumper wie eine Fackel (Lichtstufe 15, in Rumpfmitte und vor dem Bug): Der Server setzt dafür
unsichtbare Vanilla-Lichtblöcke (`minecraft:light`) in Luft oder Wasser und entfernt sie beim Weiterfliegen wieder.
Um Licht-Updates zu sparen, wandert ein Licht erst ab 2 Blöcken Abstand und höchstens alle 4 Ticks mit; im schnellen
Flug (> 10 m/s) leuchtet nur das Bug-Licht. Getarnt bleibt das Licht aus.
Die Hülle hält 240 Punkte aus (Diamantschwert: ~35 Schläge, ~23 kritische) – auch im Kreativmodus. Ein
fremder Drohnentreffer kostet 60 %: die erste Drohne übersteht der Jumper knapp, die zweite zerstört ihn.
Eine Selbstreparatur gibt es nicht.
Unter Wasser ist alles träger: Der Jumper beschleunigt und dreht langsamer (max. ~5 m/s), Drohnen fliegen
halb so schnell. Ist er mindestens zur Hälfte untergetaucht, verliert er ohne Schild 1 Hüllenpunkt pro
Sekunde (HUD-Warnung). Beim Zerstören fallen Item und
Ladung heraus (im Kreativmodus nur die Ladung).

## Projektaufbau

```
assets/                               Original-Vorlage (OBJ + Textur), Basis für tools/build_model.py
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
- **Textur** in doppelter Auflösung im Oliv-Khaki der Serie, mit eingravierten
  Paneellinien und kupferfarbenem Fensterrahmen – Rumpf, Unterseite und Gondeln einheitlich
- **Drohne** (goldener, leuchtender Körper mit Schweifen) und ein gerendertes Item-Icon

Nach Änderungen: `python3 tools/build_model.py`, im Spiel neu laden mit `F3+T`.
