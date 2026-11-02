package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;

import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lets bone meal be used on plants that vanilla doesn't allow it on. 
 * Using bone meal on one drops a copy of the plant, like bone meal on a tall flower does.
 * Works for both players and dispensers.
 *
 * Plants are organised into groups. 
 * Each group has its own "enabled" switch and an "excluded" list of blocks to leave alone in config.yml:
 *
 *   better-bonemeal:
 *     enabled: true          # master switch, handled in immcTweaks
 *     flowers:
 *       enabled: true
 *       excluded: []         # e.g. wither_rose
 *
 * To add a new group, add a loadGroup(...) line in the constructor and a matching config section.
 * Groups assume a plant drops the same material as its block, which is true for flowers.
 */
public class BetterBonemeal implements Listener {

    private static final String SECTION = "better-bonemeal";
    private static final String PREFIX = "BetterBonemeal: ";

    // How many effect particles to spawn. 
    // Vanilla's bone meal effect uses 15 particles.
    private static final int EFFECT_PARTICLES = 15;
    // How far the sparkles spread around the plant, in blocks.
    private static final double SPARKLE_SPREAD = 0.3;

    // Function to setup a plant group.
    // This is a group of plants that can be duplicated, e.g. "flowers". Its settings come from its config section.
    private record PlantGroup(String name, Set<Material> blocks, boolean enabled, Set<Material> excluded) {
        boolean matches(Material type) {
            return enabled && blocks.contains(type) && !excluded.contains(type);
        }
    }

    private final immcTweaks plugin;
    private final List<PlantGroup> groups = new ArrayList<>();

    public BetterBonemeal(immcTweaks plugin) {
        this.plugin = plugin;

        // To add a new kind of plant add a line here and a matching section in config.yml
        loadGroup("flowers", Tag.SMALL_FLOWERS.getValues());
        logEnabledGroups();
    }

    // Reads better-bonemeal.<name> from the config and registers the group.
    private void loadGroup(String name, Set<Material> blocks) {
        String path = SECTION + "." + name;
        boolean enabled = plugin.getConfig().getBoolean(path + ".enabled", true);

        Set<Material> excluded = new HashSet<>();
        for (String entry : plugin.getConfig().getStringList(path + ".excluded")) {
            Material material = Material.matchMaterial(entry);

            if (material == null || !blocks.contains(material)) {
                plugin.getLogger().warning(PREFIX + "'" + entry + "' is not part of '" + name + "', ignoring it.");
                continue;
            }

            excluded.add(material);
        }

        if (!excluded.isEmpty()) {
            plugin.getLogger().info(PREFIX + "Excluding " + excluded.size() + " " + name + " from duplication.");
        }

        groups.add(new PlantGroup(name, blocks, enabled, excluded));
    }

    // Logs which plant types are enabled, so it's clear what the config produced. 
    private void logEnabledGroups() {
        List<String> enabledNames = new ArrayList<>();
        for (PlantGroup group : groups) {
            if (group.enabled()) enabledNames.add(group.name());
        }

        if (enabledNames.isEmpty()) {
            plugin.getLogger().info(PREFIX + "No plant types enabled.");
        } else {
            plugin.getLogger().info(PREFIX + "Enabled for " + String.join(", ", enabledNames));
        }
    }

    // Handle a player right-clicking a plant while holding bone meal.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerBoneMeal(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        EquipmentSlot hand = event.getHand();
        if (block == null || item == null || hand == null) return;
        if (item.getType() != Material.BONE_MEAL) return;
        if (!canDuplicate(block)) return;

        // Disable for adventure mode players
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.ADVENTURE) return;

        // Since vanilla does nothing with this click, we cancel it and completely take over its function
        event.setCancelled(true);

        duplicate(block);

        // Show the arm swing, since the client doesn't predict one for this
        if (hand == EquipmentSlot.HAND) {
            player.swingMainHand();
        } else {
            player.swingOffHand();
        }

        // Use up one bone meal when in Survival mode
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.getInventory().getItem(hand).subtract();
        }
    }

    // Handle a dispenser firing bone meal at a plant.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispenserBoneMeal(BlockDispenseEvent event) {
        if (event.getItem().getType() != Material.BONE_MEAL) return;

        // Ensure the block is a dispenser, and not another block with this event (e.g. dropper)
        Block dispenserBlock = event.getBlock();
        if (dispenserBlock.getType() != Material.DISPENSER) return;

        // What block is the dispenser is facing
        Block target = dispenserBlock.getRelative(((Directional) dispenserBlock.getBlockData()).getFacing());
        if (!canDuplicate(target)) return;

        // Since vanilla does nothing useful with this plant, cancel it and take over this dispense
        event.setCancelled(true);

        ItemStack boneMeal = event.getItem().clone();
        boneMeal.setAmount(1);

        // We need to reduce the bone meal count inside the dispneser.
        // The dispenser writes its inventory back after this event, so removing the bone meal the same tick could be undone.
        // So we wait for the next game tick, then reduce the count.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!(dispenserBlock.getState() instanceof Dispenser dispenser)) return;
            if (!canDuplicate(target)) return;

            // Only duplicate plant if a bone meal was really taken out of the dispenser
            if (dispenser.getInventory().removeItem(boneMeal).isEmpty()) {
                duplicate(target);
            }
        });
    }

    // Returns true if this block belongs to an enabled plant group and hasn't been excluded in the config.
    private boolean canDuplicate(Block block) {
        Material type = block.getType();

        for (PlantGroup group : groups) {
            if (group.matches(type)) return true;
        }
        return false;
    }

    // Drops a copy of the plant and plays the bone meal effect. Shared by the player and dispenser handlers. 
    private void duplicate(Block plant) {
        World world = plant.getWorld();
        Location center = plant.getLocation().add(0.5, 0.5, 0.5);

        world.dropItemNaturally(center, new ItemStack(plant.getType()));
        world.playEffect(plant.getLocation(), Effect.BONE_MEAL_USE, EFFECT_PARTICLES);

        // Vanilla's effect doesn't spawn particles for these plants, so add the green sparkles ourselves
        world.spawnParticle(Particle.HAPPY_VILLAGER, center, EFFECT_PARTICLES,
            SPARKLE_SPREAD, SPARKLE_SPREAD, SPARKLE_SPREAD, 0);
    }
}