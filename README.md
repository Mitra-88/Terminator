# Terminator

Paper plugin that adds the Terminator from Hypixel SkyBlock (yes, the M7 one) to your server.

It's a shortbow: shoots instantly, no drawing, 3 arrows per click. Every 3rd landed hit charges Salvation, a beam that pierces up to 5 mobs and always crits. Killing a mob stores its soul in the bow (Soul Eater), and your next crit unleashes it for bonus damage. If you've used one on Hypixel, you already know exactly how it plays.

Wiki: [Terminator](https://hypixelskyblock.minecraft.wiki/w/Terminator)

## Setup

For the supported Minecraft versions, check the [Modrinth page](https://modrinth.com/plugin/terminator) or the [latest release on GitHub](https://github.com/Mitra-88/Terminator) both always show the current one.

Drop the jar in `plugins/`, restart. The config generates at `plugins/Terminator/config.yml` and everything in it is commented.

After that, config changes don't need a restart: edit, save, `/terminatorreload`.
## Getting one

No crafting recipe you weren't going to craft it anyway. Right?

```
/giveterminator [player]
```

No argument gives one to yourself. Note that `terminator.give` is enabled for **everyone** by default. If you don't want players handing themselves mythics, set `default: false` on it in `paper-plugin.yml` or override it in your permissions' plugin.

## Commands & permissions

| Command                    | Description                             | Permission          | Default     |
|----------------------------|-----------------------------------------|---------------------|-------------|
| `/giveterminator [player]` | Gives the bow to you or another player. | `terminator.give`   | All players |
| `/terminatorreload`        | Reloads `config.yml` without a restart. | `terminator.reload` | Ops         |

## Config

Everything's documented in `config.yml` itself. The options you'll actually touch:

| Option                               | Default              | What it does                                  |
|--------------------------------------|----------------------|-----------------------------------------------|
| `shooting.arrow-damage-min` / `-max` | 20000 / 50000        | Each arrow deals random damage in this range. |
| `shooting.shoot-cooldown-ms`         | 200                  | Minimum delay between shots.                  |
| `shooting.side-spread-degrees`       | 10                   | Angle between the 3 arrows.                   |
| `shooting.shoot-sound`               | `ENTITY_ARROW_SHOOT` | Sound played on each shot.                    |
| `salvation.hits-required`            | 3                    | Arrow hits needed to charge the beam.         |
| `salvation.beam-damage`              | 50000                | Damage dealt to each enemy the beam hits.     |
| `salvation.beam-max-pierce`          | 5                    | Max enemies the beam can hit.                 |
| `salvation.beam-distance`            | 32                   | Beam range in blocks.                         |

The Terminator's name and lore are fully configurable (`item.display-name`, `item.lore`) and support [MiniMessage](https://docs.advntr.dev/minimessage/format.html) formatting. 
That's where the stars and colors in the default item come from.

## Credits & license
Icon: [Furfsky](https://furfsky.net/)
AGPL-3.0, see [LICENSE](LICENSE).
