package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Shulker Backpack module - simplified debug version.
 * Just detects right-clicking in inventory and logs it.
 */
public class ShulkerBackpack implements Listener {
    private final immcTweaks plugin;
    private boolean enabled;

    private static final Material SHULKER_BOX = Material.SHULKER_BOX;

    public ShulkerBackpack(immcTweaks plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("shulker-backpack.enabled", true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.getOpenInventory() == null) return;

        // Only handle right-click events (PICKUP_HALF)
        if (!event.getAction().toString().equals("PICKUP_HALF")) return;

        // Check if the item being clicked (in the slot) is a shulker box
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != SHULKER_BOX) return;

        // Log when shulker box is right-clicked
        plugin.getLogger().info("[ShulkerBackpack] DEBUG: Player " + player.getName() + " right-clicked a SHULKER_BOX!");
    }
}
