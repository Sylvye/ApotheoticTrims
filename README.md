# ApotheoticTrims

ApotheoticTrims is a Paper 26.2 plugin that grants one special ability when a player wears helmet, chestplate, leggings, and boots with the same armor trim pattern. Trim materials may differ. All settings are managed in game; no file editing or reload is required.

## Requirements and installation

- Paper 26.2 build 129 or newer compatible build
- Java 25

Build with `./gradlew build` (`gradlew.bat build` on Windows), then copy `build/libs/ApotheoticTrims-1.0.0.jar` into the server's `plugins` directory and restart the server.

## Commands

| Command | Purpose |
| --- | --- |
| `/trims` | Open the trim catalog and show the active set. |
| `/trims info [trim]` | Open or print an ability's details. |
| `/trims status [player]` | Show the active ability. Inspecting others requires admin permission. |
| `/trims settings` | Open the admin editor. |
| `/trims set <trim\|feedback> <setting> <value>` | Set an exact value. |
| `/trims toggle <trim>` | Enable or disable an ability. |
| `/trims reset <trim\|all>` | Restore defaults. |
| `/trims help` | Show command help. |

In an ability screen, left/right click changes a number by its small step and shift-left/right uses its coarse step. Click **Enter exact value** and type a number in chat; type `cancel` to abort. Prompts expire after 60 seconds.

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `apotheotictrims.use` | Everyone | Activate trim abilities. |
| `apotheotictrims.info` | Everyone | Open the catalog and inspect abilities. |
| `apotheotictrims.admin` | Operators | Change global settings and inspect other players. |

## Abilities

- **Tide:** Dolphin's Grace and Resistance in water.
- **Coast:** Luck.
- **Dune:** Knockback resistance.
- **Wild:** One flight-key double jump per airborne cycle with protected landing.
- **Sentry:** Player-shot damaging projectiles deal 1.3x damage.
- **Snout:** Hostile mobs ignore the wearer until the wearer, their projectile, or their pet attacks that mob.
- **Bolt:** A 10-hit timed melee combo calls non-incendiary, vanilla-typed lightning for 16 damage.
- **Flow:** A disabled shield repels nearby living entities.
- **Rib:** Fire Resistance.
- **Ward:** Suppresses the wearer's sculk vibrations and Warden anger/targeting.
- **Vex:** A used totem grants Strength and Speed for 90 seconds by default.
- **Spire:** Void falls wrap to world height and the resulting fall damage is cancelled.
- **Eye:** Sneaking makes visible nearby players glow.
- **Silence:** Blocks the configured list of negative effects, including Instant Damage.
- **Wayfinder:** Speed II.
- **Raiser:** Jump Boost.
- **Shaper:** Haste II.
- **Host:** Hero of the Village V.

## Verification

Run `./gradlew test`. Before deploying broadly, verify on a test Paper server: each full set and mixed-set deactivation; Ward with sensors, shriekers, and a Warden; Wild in survival and creative; Flow after an axe shield disable; Silence with commands, splash/lingering potions, and tipped arrows; and multiplayer Snout/Eye behavior.
