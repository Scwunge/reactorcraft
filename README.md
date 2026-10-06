# ReactorCraft (1.21.1 NeoForge port)

Nuclear power for RotaryCraft: fission reactors with fuel rods, control rods, coolant and steam turbines; breeder, thorium
and pebble-bed reactors; radiation and nuclear waste; and fusion with plasma, magnets and injectors.

This is a port of **ReactorCraft** by **Reika Kalseki** to Minecraft **1.21.1** on **NeoForge**, ported with permission.
It needs the 1.21.1 port of RotaryCraft: <https://github.com/Scwunge/rotarycraft>.

## Status

Work in progress.

- Done: the reactor heat model, ores and world generation, ingots, fluorite, crafting parts, fluids and canisters, with the original art.
- Next: processing machines, the fission reactor and turbines, radiation and the hazmat suit, then breeder, thorium, pebble-bed and fusion reactors.

## Server owners

Everything gameplay-related is in the server config (`serverconfig/reactorcraft-server.toml`). Meltdowns, radiation and hot
reactor parts only change the world when the `mobGriefing` game rule allows it, act in the name of the player who placed
the machine so claim mods apply, and each has its own switch to turn it off.

## Building

Put the RotaryCraft port's jar in `libs/` (see `rotarycraft_jar` in `gradle.properties`), then:

```
./gradlew build
```

Needs Java 21. The jar ends up in `build/libs`.

## Licence

MIT, see [LICENSE](LICENSE). Original ReactorCraft and DragonAPI by Reika Kalseki.
