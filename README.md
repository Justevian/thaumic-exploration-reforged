# Thaumic Exploration: Reforged

**Русский** · [English](README.en.md)

Неофициальный порт **Thaumic Exploration 1.1-53** с Minecraft 1.7.10 на **Minecraft 1.21.1** для [Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged). Аддон добавляет свою вкладку с 31 исследованием в Таумономиконе. Доступны русский и английский языки.

## Что добавляет мод

- **Эссенция и алхимия.** Связанные банки, банка-утилизатор, вечная и огненная урны, тигель и жаровня душ.
- **Автоматизация.** Связанные сундуки с общим инвентарём для големов и воронок, автоматический сортировщик и репликатор.
- **Магическое снаряжение.** Янтарные и трансмутационные стержни, посох некроманта, наконечники странника и механика, сапоги метеора и кометы, кольца и зачарования.
- **Исследования.** Парящие свечи для стабилизации наполнения и мозговой резервуар, который извлекает фрагменты знаний из книг.

**Масштаб Таумономикона.** Наведите курсор на карту исследований и прокрутите колёсико. Начальный масштаб 80%, диапазон от 40% до 160%.

## Установка

1. Создайте профиль **Minecraft 1.21.1** с **NeoForge 21.1.256+**.
2. Скачайте JAR из [релиза для 1.21.1](https://github.com/Justevian/thaumic-exploration-reforged/releases/tag/v0.1.19-alpha%2B1.21.1).
3. Поместите его в папку `mods` вместе с **Thaumcraft 4: Reforged 1.1.3 для 1.21.1** и **TerraBlender 1.21.1-4.1.0.1** + **Curios 9.5.1+1.21.1**.

[Скачать Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged/releases/tag/1.21.1)

## Сборка из исходников

Установите JDK 21, положите `thaumcraft-4-reforged-1.1.3-1.21.1.jar` в папку `libs` и запустите `./gradlew build` или `gradlew.bat build` на Windows. Готовый JAR появится в `build/libs`.

## Авторы и благодарности

- **Flaxbeard** и **nekosune** — создание и поддержка [оригинального Thaumic Exploration](https://github.com/Flaxbeard/Thaumic_Exploration).
- **Azanor** — оригинальный Thaumcraft.
- **overseas** — [Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged).

В порте адаптированы механики, текстуры и модели оригинала. Оригинальный проект распространяется по [CC BY-NC-SA 4.0](https://www.curseforge.com/minecraft/mc-mods/thaumic-exploration/license).

Порт распространяется по [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/).

## Development checks

Place the 1.21.1 NeoForge versions of TerraBlender and Curios in `libs` as well as Thaumcraft. The ZIP and JAR contain no dependencies. This is an experimental backport of the 26.3 branch. Use a separate 1.21.1 world.

Use an isolated development world and accept the EULA in `run/eula.txt` before running the server checks:

```sh
./gradlew runServer -PportSmoke=true
./gradlew runServer -PportSmoke=true -PverifySavedStorage=true
```

For the client check, copy `run/world` into `run/saves/exploration-port-smoke` and run `./gradlew runClient -PclientSmoke=true`. On Windows use `gradlew.bat`. These checks exit automatically and are excluded from the JAR.
