package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Makes exploding creepers go off as a firework ("confetti") instead of destroying blocks.
 * Players and mobs nearby are still hurt and knocked back exactly as by a normal creeper explosion, and the normal explosion sound and puff still play alongside the firework.
 *
 * Config (creeper-confetti):
 *      chance:         percent chance (0-100) that a creeper becomes confetti; otherwise it explodes normally
 *      colors:         hex colours the bursts are made of, e.g. "#FF0000" (must be in quotes)
 *      fade-colors:    hex colours the bursts fade into
 *      effects:        the bursts to set off; each has a type, flicker and trail
 *
 * If the mobGriefing gamerule is off, the server doesn't fire the explosion event this module relies on, so there is no confetti in that case.
 */
public class CreeperConfetti implements Listener {

    private static final String SECTION = "creeper-confetti";
    private static final String PREFIX = "CreeperConfetti: ";

    private final immcTweaks plugin;

    // Marks fireworks spawned by this module, so their damage can be cancelled.
    private final NamespacedKey confettiKey;

    private final List<FireworkEffect> effects = new ArrayList<>();
    private int chance = 0;

    public CreeperConfetti(immcTweaks plugin) {
        this.plugin = plugin;
        this.confettiKey = new NamespacedKey(plugin, "confetti_firework");
        loadConfig();
    }

    // Reads the config. Anything invalid is skipped with a warning rather than stopping the plugin. 
    private void loadConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(SECTION);
        if (section == null) {
            plugin.getLogger().warning(PREFIX + "No '" + SECTION + "' section found in config.yml, tweak disabled.");
            return;
        }

        // Chance is a percentage, so keep it between 0 and 100
        int configuredChance = section.getInt("chance", 100);
        chance = Math.max(0, Math.min(100, configuredChance));
        if (chance != configuredChance) {
            plugin.getLogger().warning(PREFIX + "'chance' must be between 0 and 100, using "
                + chance + " instead of " + configuredChance + ".");
        }

        List<Color> colors = parseColors(section.getStringList("colours"), "colours");
        List<Color> fadeColors = parseColors(section.getStringList("fade-colours"), "fade-colours");

        // A firework burst can't be built without at least one colour
        if (colors.isEmpty()) {
            plugin.getLogger().warning(PREFIX + "No valid colours found in 'colors', tweak disabled.");
            return;
        }

        for (Map<?, ?> entry : section.getMapList("effects")) {
            FireworkEffect.Type type;
            try {
                type = FireworkEffect.Type.valueOf(String.valueOf(entry.get("type")).toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(PREFIX + "Unknown effect type '" + entry.get("type")
                    + "', skipping it. Valid types: " + Arrays.toString(FireworkEffect.Type.values()));
                continue;
            }

            effects.add(FireworkEffect.builder()
                .with(type)
                .flicker(readBoolean(entry, "flicker"))
                .trail(readBoolean(entry, "trail"))
                .withColor(colors)
                .withFade(fadeColors)
                .build());
        }

        if (effects.isEmpty()) {
            plugin.getLogger().warning(PREFIX + "No valid effects found, creeper explosions will be unchanged.");
            return;
        }

        plugin.getLogger().info(PREFIX + "Loaded " + effects.size() + " effect(s) with a "
            + chance + "% chance.");
    }

    // Converts hex codes like "#FF0000" into colours, skipping (and warning about) invalid ones. 
    private List<Color> parseColors(List<String> hexCodes, String label) {
        List<Color> colors = new ArrayList<>();

        for (String hex : hexCodes) {
            String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
            try {
                if (cleaned.length() != 6) throw new NumberFormatException();
                colors.add(Color.fromRGB(Integer.parseInt(cleaned, 16)));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning(PREFIX + "Invalid colour '" + hex + "' in '" + label
                    + "', skipping it. Use a hex code like \"#FF0000\".");
            }
        }

        return colors;
    }

    // Reads a true/false setting from an effect entry, treating a missing or invalid value as false.
    private boolean readBoolean(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value instanceof Boolean b && b;
    }

    // Handle a creeper exploding, and sometimes turn it into confetti.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreeperExplode(EntityExplodeEvent event) {
        if (event.getEntityType() != EntityType.CREEPER) return;
        if (effects.isEmpty()) return;

        // Roll the chance. If it fails, the creeper explodes normally.
        if (ThreadLocalRandom.current().nextInt(100) >= chance) return;

        // Remove every block from the explosion so nothing is destroyed.
        // Damage and knockback to entities are not part of this list, so they still happen.
        event.blockList().clear();

        Location location = event.getLocation();
        location.getWorld().spawn(location, Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffects(effects);
            firework.setFireworkMeta(meta);

            // Mark this firework as ours so we can recognise it later
            firework.getPersistentDataContainer().set(confettiKey, PersistentDataType.BYTE, (byte) 1);
        }).detonate();
    }

    // The confetti is for show only, so its own damage is cancelled.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConfettiDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework firework
            && firework.getPersistentDataContainer().has(confettiKey)) {
            event.setCancelled(true);
        }
    }
}