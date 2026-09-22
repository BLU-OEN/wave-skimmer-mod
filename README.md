# Wave Skimmer

A NeoForge 1.21.1 mod (All The Mods 10 compatible) that adds a black-and-white floating generator. It sits on the surface of water and turns waves into Forge Energy (FE).

## Recipe
```
Iron      Iron      Iron
Trapdoor  Trapdoor  Trapdoor
Iron      Redstone  Iron
```
Any trapdoor works, including wooden or iron ones.

## Behaviour
- **Placement:** aim at a still water source, the same way you place a lily pad. The skimmer floats on top. If the water under it is removed, it pops off and drops as an item.
- **Power:** a base of 40 FE/t, scaled by:
  - the wave cycle (50–100%)
  - how much of the 5×5 patch of water underneath is open water
  - ×1.5 in ocean biomes
  - ×1.25 in rain, or ×1.75 in thunderstorms
- **Buffer and output:** stores 100k FE and pushes up to 1,000 FE/t through every face except the bottom. Hook cables into the side port or the engine's top port.
- **Status:** right-click it to see stored energy and the current generation rate.
- **Effects:** it bobs and rolls on the swell. Bubbles flow under the hull from front to back, churn rises from beneath it, and a wake sprays out of the tail.
- **Config:** every number above is adjustable in `config/waveskimmer-common.toml`.

## Build
```
gradlew build
```
Gradle must run on JDK 17 or 21. Set `JAVA_HOME` if your default Java is newer. The jar is written to `build/libs/`.

## Tweaking the look
Edit `tools/generate_assets.py` (textures and model shapes), then run `python tools/generate_assets.py`.
