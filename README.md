# Nitro Client (Fabric 1.21.4)

Standalone Fabric client mod with a Krypton-style click GUI.

## Build
1. Install JDK 21.
2. Open the folder in IntelliJ IDEA (it imports the Gradle project), or run `gradle wrapper --gradle-version 8.12` once and then `./gradlew build`.
3. Take `build/libs/nitro-client-1.0.0.jar` and put it in your `mods` folder together with
   Fabric Loader 0.16+, **Fabric API 0.119.2+1.21.4** and Minecraft **1.21.4**.

## Use
- **Right Shift**: open the GUI. Left click = toggle, right click = settings, middle click = set a keybind
  (Esc/Backspace clears it). Drag panel headers to move them. Click a text setting to type; Ctrl+V pastes.
- **Anchor macro:** right-click an anchor, or hold **G** (rebind in Controls under "Nitro Client").
- **CLIENT panel:** toggle the arraylist and the top-left watermark separately, and type your own watermark text.
- Settings and keybinds are saved to `config/nitro.json`.

## Modules
| Module | What it does |
|---|---|
| Anchor macro | Right-click (or hold G) on a respawn anchor: charges it with glowstone from your hotbar, switches to the "Explode slot", detonates. Overworld/End only. |
| Crystal optimizer | Removes an end crystal on your client the moment you hit it (no waiting on the server). |
| Auto totem | Keeps a totem in the offhand (delay, health threshold). |
| Hover totem | Hover a totem in any inventory screen to move it to the offhand (and optionally a hotbar slot). |
| Auto inv totem | While your inventory is open, equips a totem and restocks a chosen hotbar slot. |
| Storage ESP | Boxes chests, trapped chests, ender chests, barrels, shulkers through walls. |
| Player ESP | Boxes (and optional tracers to) players through walls. |
| Free cam | Detached camera; WASD / jump / sneak to fly, mouse to look. |
| Oresim | Predicts ores from the world seed (see below). |
| Chunk finder | Flags chunks with sideways deepslate, off-geode amethyst clusters, long vine columns not hanging from leaves/logs, and fully grown kelp (age 24+). Chunks with enough signs get an orange box, coloured blocks, and a chat alert. |

## Oresim
1. Enable it, right click it, click the **Seed** field and type or paste the seed (numbers or text).
2. Pick ores to show. Diamond is on by default.
3. Stand somewhere with loaded chunks (ideally underground) and toggle **Calibrate now**. It tests index
   offsets -4..4 against real coal/iron/copper nearby and sets **Index offset** if one clearly matches.
   If it says inconclusive, the seed or version is probably wrong.

Limits: overworld only; needs the exact seed and a vanilla world; mined or carved blocks look like
"exposed to air" and can hide predicted ore; emerald and extra badlands gold indices are approximate.

## Notes
- Mappings are Yarn 1.21.4+build.8. Other versions need small API changes (Input/PlayerInput, camera, render events).
- This code was not compiled or run in the environment I wrote it in. If Gradle reports an error, send me the message.
- Multiplayer anticheats will flag most of this and many servers ban for it.
