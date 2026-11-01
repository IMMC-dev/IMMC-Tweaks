package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;

import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.event.block.BlockDispenseEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BetterBonemeal implements Listener {

    private static final String SECTION = "better-bonemeal";
    private static final String PREFIX = "BetterBonemeal: ";

    /** One kind of plant that can be duplicated, e.g. "flowers". Its settings come from its config section. */
    private record PlantGroup(String name, Set<Material> blocks, boolean enabled, Set<Material> excluded) {
        boolean matches(Material type) {
            return enabled && blocks.contains(type) && !excluded.contains(type);
        }
    }

    private final immcTweaks plugin;
    private final List<PlantGroup> groups = new ArrayList<>();

    public BetterBonemeal(immcTweaks plugin) {
        this.plugin = plugin;

        // To add a new kind of plant: add a line here and a matching section in config.yml
        loadGroup("flowers", Tag.SMALL_FLOWERS.getValues());
        logEnabledGroups();
    }

    /** Reads better-bonemeal.<name> from the config and registers the group. */
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

    /** Logs which plant types ended up enabled, so it's clear what the config produced. */
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerBoneMeal(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        EquipmentSlot hand = event.getHand();
        if (block == null || item == null || hand == null) return;
        if (item.getType() != Material.BONE_MEAL) return;
        if (!canDuplicate(block)) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.ADVENTURE) return;

        // Vanilla does nothing with this click, so we take it over
        event.setCancelled(true);

        duplicate(block);

        // Show the arm swing, since the client doesn't predict one for this
        if (hand == EquipmentSlot.HAND) {
            player.swingMainHand();
        } else {
            player.swingOffHand();
        }

        // Use up one bone meal, like vanilla does outside creative mode
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.getInventory().getItem(hand).subtract();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispenserBoneMeal(BlockDispenseEvent event) {
        if (event.getItem().getType() != Material.BONE_MEAL) return;

        // Other blocks (e.g. droppers) fire this event too, so make sure it's a dispenser
        Block dispenserBlock = event.getBlock();
        if (dispenserBlock.getType() != Material.DISPENSER) return;

        // The block the dispenser is facing
        Block target = dispenserBlock.getRelative(((Directional) dispenserBlock.getBlockData()).getFacing());
        if (!canDuplicate(target)) return;

        // Take over this dispense, since vanilla does nothing useful with this plant
        event.setCancelled(true);

        ItemStack boneMeal = event.getItem().clone();
        boneMeal.setAmount(1);

        // Finish next tick. The dispenser writes its inventory back after this event,
        // so removing the bone meal right now could be undone.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!(dispenserBlock.getState() instanceof Dispenser dispenser)) return;
            if (!canDuplicate(target)) return;

            // Only duplicate if a bone meal was really taken out of the dispenser
            if (dispenser.getInventory().removeItem(boneMeal).isEmpty()) {
                duplicate(target);
            }
        });
    }

    /** True if this block belongs to an enabled plant group and hasn't been excluded in the config. */
    private boolean canDuplicate(Block block) {
        Material type = block.getType();

        for (PlantGroup group : groups) {
            if (group.matches(type)) return true;
        }
        return false;
    }

    /** Drops a copy of the plant and plays the bone meal effect. Shared by the player and dispenser handlers. */
    private void duplicate(Block plant) {
        World world = plant.getWorld();
        Location center = plant.getLocation().add(0.5, 0.5, 0.5);

        world.dropItemNaturally(center, new ItemStack(plant.getType()));
        world.playEffect(plant.getLocation(), Effect.BONE_MEAL_USE, 15);

        // Vanilla's effect doesn't spawn particles for these plants, so add the green sparkles ourselves
        world.spawnParticle(Particle.HAPPY_VILLAGER, center, 15, 0.3, 0.3, 0.3, 0);
    }
}