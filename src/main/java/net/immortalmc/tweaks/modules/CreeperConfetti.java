package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;

import org.bukkit.NamespacedKey;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.entity.Firework;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CreeperConfetti implements Listener {

    private static final String PREFIX = "CreeperConfetti: ";

    private final immcTweaks plugin;
    private final NamespacedKey confettiKey;   // <-- new field

    private int chance = 0;
    private List<FireworkEffect> effects = new ArrayList<>();

    public CreeperConfetti(immcTweaks plugin) {
        this.plugin = plugin;
        this.confettiKey = new NamespacedKey(plugin, "confetti_firework");   // <-- new line
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("creeper-confetti");
        if (section == null) {
            plugin.getLogger().warning(PREFIX + "No 'creeper-confetti' section found in config.yml, tweak disabled.");
            return;
        }

        // Chance is a percentage, so keep it between 0 and 100
        chance = Math.max(0, Math.min(100, section.getInt("chance", 100)));

        List<Color> colors = parseColors(section.getStringList("colors"), "colors");
        List<Color> fadeColors = parseColors(section.getStringList("fade-colors"), "fade-colors");

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
                    + "', skipping it. Valid types: BALL, BALL_LARGE, STAR, BURST, CREEPER");
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

    private boolean readBoolean(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value instanceof Boolean b && b;
    }


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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConfettiDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework firework
            && firework.getPersistentDataContainer().has(confettiKey)) {
            event.setCancelled(true);
        }
    }
}