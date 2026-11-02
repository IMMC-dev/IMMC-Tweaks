# IMMC Tweaks

The bespoke plugin for server-side tweaks on IMMC: Survival. Each tweak module is independently toggle-able in the config.

## Tweak Modules
**No Anvil Limit**
- When enabled, the 'too expensive' limit on anvils is removed by capping the cost at 39 levels

**Better Bonemeal**
- When enabled, allows for bone-mealing plants not normally allowed by the Vanilla game. Currently this only includes small flowers
- Config allows for enabling/disabling individual plant groups, and excluding plants within each group

**Creeper Confetti**
- When enabled, when creepers explode they no longer destroy blocks, and instead have a chance to spawn a firework explosion (confetti)
- This firework is purely visual and deals no damage
- The firework explosion can be configured as follows:
    - Percentage chance to spawn confetti
    - Colours of confetti
    - Fade colours of confetti
    - Firework effect types to use as confetti

**No Crop Trample**
- When enabled players and mobs cannot trample on farmland to turn it into dirt

**Item Name Colour**
- When enabled it allows players to use the & colour and formatting codes, as well as hex codes, to add colours and formatting to item names
- Adds a command /itemnamecolour (/inc) to see the colour and formatting codes used to name the item a player is holding
- Output of /inc command is also displayed when placing an item with custom formatting in an anvil, since the anvil cannot inherit more than the plain text

**Sign Colour Name**
- When enabled it allows players to use the & colour and formatting codes, as well as hex codes, to add colours and formatting to signs
- Unlike item names, colour and formatting codes can be injected back onto the sign when it is edited, so no command is added for this module.


## Default Config
<details>
<summary>config.yml</summary>

```
# IMMC Tweaks configuration

no-anvil-limit:
  # Remove the too expensive anvil limit by capping the max level cost at 39 levels
  enabled: true


better-bonemeal:
  # Allows bonemealing plants not normall bone-mealable
  enabled: true

  flowers:
    # Allow bonemealing of small flowers like poppies or wither roses.
    enabled: true
      # Small flowers that can NOT be duplicated with bone meal, by their normal Minecraft name.
      # Leave as [] to allow every small flower. Example:
      #   excluded:
      #     - wither_rose
      #     - torchflower
    excluded: []


creeper-confetti:
  # Prevent creepers from destroying blocks, while releasing fireworks instead
  enabled: true

  # Percent chance (0-100) that an exploding creeper becomes confetti
  chance: 100

  # Colours the bursts are made of, as hex codes.
  # Hex codes MUST be in quotes, otherwise YAML treats the # as a comment.
  colours:
    - "#FF0000"   # red
    - "#FF9100"   # orange
    - "#FFE600"   # yellow
    - "#4DFF00"   # green
    - "#08FF94"   # mint
    - "#00EEFF"   # cyan
    - "#0044FF"   # blue
    - "#8000FF"   # purple
    - "#CC00FF"   # magenta

  # Colours the bursts fade into
  fade-colours:
    - "#000000"   # black

  # Each entry is one burst that goes off when a creeper explodes
  # Valid Types: BALL, BALL_LARGE, STAR, BURST or CREEPER
  # Flicker: whether to use the sparkle effect
  # Trail: whether to use the firework trail effect
  effects:
    - type: BALL
      flicker: true
      trail: false
    - type: BALL_LARGE
      flicker: true
      trail: false


no-crop-trample:
  # Prevent players and mobs from destroying farmland by jumping on it
  enabled: true


item-name-colour:
  # Allow use of '&' colour codes and '#' hex codes to add custom colours to item names
  enabled: true


sign-text-colour:
  # Allow use of '&' colour codes and '#' hex codes to add custom colours to sign text
  enabled: true
```

</details>

---
## To Do:
| Feature                 | Current Plugin used in IMMC: Survival                                                                                      | IMMC Tweaks Status |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------|--------------------|
| No Crop Trampling       | https://www.spigotmc.org/resources/simplenocroptrample.73609/                                                              | Done ✅            |
| No Anvil Limit          | None                                                                                                                       | Done ✅            |
| Coloured Item Names     | https://www.spigotmc.org/resources/anvilcolors.109155/                                                                     | Done ✅            |
| Coloured Sign Text      | https://www.spigotmc.org/resources/colored-signs.31676/                                                                    | Done ✅            |
| Creeper Confetti        | https://www.spigotmc.org/resources/creeperconfetti.85204/                                                                  | Done ✅            |
| Shulker Backpacks       | https://www.spigotmc.org/resources/eshulkerbox-let-players-interact-with-shulker-boxes-in-a-better-way-and-comfort.106156/ | To Do ⏰           |
| Bone Meal Small Flowers | https://www.spigotmc.org/resources/flowermeal.103720/                                                                      | Done ✅            |
