# Terminator

A Paper plugin that adds the **Terminator** shortbow from [Hypixel SkyBlock](https://hypixelskyblock.minecraft.wiki/w/Terminator) to your server.

## The bow at a glance

- **Shoots instantly** - just click.
- **Fires 3 arrows at once** in a spread.
- **Damages endermen** (normally immune to arrows).
- **Never breaks.**

It also has two abilities: **Salvation** (a piercing beam) and **Soul Eater** (stored kill power).

## For players

### Getting the bow
Run `/giveterminator` - everyone can use it by default.

### Shooting
- **Hold right-click** - automatic fire.
- **Spam left-click** - rapid fire.
- No arrows needed in your inventory.

### Salvation (the beam)
1. Land **3 arrow hits** on enemies. Your action bar shows progress:
   *Salvation: T1 → T2 → T3!*
2. **Left-click** to fire the beam.
3. It pierces up to **5 enemies** in a line and hits hard.
4. The charge resets - land 3 more hits to fire it again.

### Soul Eater
1. **Kill a monster** - the bow stores power from the killing blow (10× its damage).
2. Your **next critical hit** releases all that stored power as bonus damage.

## For admins

### Installation
1. For the supported Minecraft versions, check the [Modrinth page](https://modrinth.com/plugin/terminator) or the [latest release on GitHub](https://github.com/Mitra-88/Terminator) both always show the current one.
2. Drop the `.jar` into `plugins/`.
3. Restart the server.
4. (Optional) Edit `plugins/Terminator/config.yml`, then run `/terminatorreload`.

### Commands & permissions

| Command                    | Description                             | Permission          | Default     |
|----------------------------|-----------------------------------------|---------------------|-------------|
| `/giveterminator [player]` | Gives the bow to you or another player. | `terminator.give`   | All players |
| `/terminatorreload`        | Reloads `config.yml` without a restart. | `terminator.reload` | Ops         |

### Most-used config options

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

The bow's name and lore are fully configurable (`item.display-name`, `item.lore`) and support
[MiniMessage](https://docs.advntr.dev/minimessage/format.html) formatting.

## Credits & license
- Icon: [Furfsky](https://furfsky.net/)
- Licensed under the [GNU AGPL v3.0](LICENSE).
