# Thaumic Exploration: Reforged

[Русский](README.md) · **English**

An unofficial port of **Thaumic Exploration 1.1-53** from Minecraft 1.7.10 to **Minecraft 1.21.1** for [Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged). The add-on has its own Thaumonomicon tab with 31 research entries in English and Russian.

## Features

- **Essentia and alchemy.** Bound jars, the trash jar, Everfull and Everburn Urns, the Crucible of Souls, and the Soul Brazier.
- **Automation.** Bound chests with shared inventories accessible to golems and hoppers, the automatic sorter, and the replicator.
- **Magical gear.** Amber and transmutative rods, the necromancer staff, Sojourner and Mechanist caps, Meteor and Comet Boots, rings, and enchantments.
- **Research.** Floating candles stabilize infusions, and the Think Tank extracts knowledge fragments from books.

## Installation

1. Create a **Minecraft 1.21.1** profile with **NeoForge 21.1.256+**.
2. Download the JAR from the [1.21.1 release](https://github.com/Justevian/thaumic-exploration-reforged/releases/tag/v0.1.18-alpha%2B1.21.1).
3. Put it in the `mods` folder alongside **Thaumcraft 4: Reforged 1.1.3 for 1.21.1** and **TerraBlender 1.21.1-4.1.0.1** + **Curios 9.5.1+1.21.1**.

[Download Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged/releases/tag/1.21.1)

## Building from source

Install JDK 21, place `thaumcraft-4-reforged-1.1.3-1.21.1.jar` in `libs`, and run `./gradlew build` or `gradlew.bat build` on Windows. The resulting JAR is in `build/libs`.

## Credits

- **Flaxbeard** and **nekosune** — creation and maintenance of the [original Thaumic Exploration](https://github.com/Flaxbeard/Thaumic_Exploration).
- **Azanor** — original Thaumcraft.
- **overseas** — [Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged).

This port adapts the original mechanics, textures, and models. The original project is licensed under [CC BY-NC-SA 4.0](https://www.curseforge.com/minecraft/mc-mods/thaumic-exploration/license).

The port is distributed under [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/).

## Development checks

Place the 1.21.1 NeoForge versions of TerraBlender and Curios in `libs` as well as Thaumcraft. The ZIP and JAR contain no dependencies. This is an experimental backport of the 26.3 branch. Use a separate 1.21.1 world.

Use an isolated development world and accept the EULA in `run/eula.txt` before running the server checks:

```sh
./gradlew runServer -PportSmoke=true
./gradlew runServer -PportSmoke=true -PverifySavedStorage=true
```

For the client check, copy `run/world` into `run/saves/exploration-port-smoke` and run `./gradlew runClient -PclientSmoke=true`. On Windows use `gradlew.bat`. These checks exit automatically and are excluded from the JAR.
