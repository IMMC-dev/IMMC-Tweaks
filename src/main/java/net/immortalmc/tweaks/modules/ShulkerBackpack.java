package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Shulker Backpack module - simplified debug version.
 * Reads shulker box contents when right-clicked in inventory.
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

        // Try to get the shulker box contents from BlockStateMeta
        try {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta instanceof BlockStateMeta blockStateMeta) {
                Object blockState = blockStateMeta.getBlockState();
                if (blockState instanceof ShulkerBox shulkerBox) {
                    Inventory shulkerInventory = shulkerBox.getInventory();
                    
                    // Cancel the event to prevent picking up the shulker box
                    event.setCancelled(true);
                    
                    // Log the shulker box contents
                    plugin.getLogger().info("[ShulkerBackpack] Player " + player.getName() 
                        + " right-clicked a shulker box. Contents:");
                    
                    ItemStack[] contents = shulkerInventory.getContents();
                    for (int i = 0; i < contents.length; i++) {
                        ItemStack item = contents[i];
                        if (item != null) {
                            String displayName = null;
                            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                                displayName = item.getItemMeta().displayName().toString();
                            }
                            plugin.getLogger().info("  Slot " + i + ": " + 
                                (displayName != null ? displayName : "unknown") + 
                                " x" + item.getAmount());
                        } else {
                            plugin.getLogger().info("  Slot " + i + ": empty");
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[ShulkerBackpack] Failed to read shulker box: " + e.getMessage());
        }
    }
}
