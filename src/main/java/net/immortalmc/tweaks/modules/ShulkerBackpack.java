package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

/**
 * Shulker Backpack module - opens shulker box inventory when right-clicked in inventory.
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
                    
                    // Get the display name from the shulker box item
                    String shulkerName = "";
                    if (meta.hasDisplayName()) {
                        shulkerName = meta.displayName().toString();
                    }
                    
                    // Create a preview inventory using the new API (Component title)
                    Inventory preview = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, Component.text(shulkerName));
                    preview.setContents(shulkerInventory.getContents());
                    
                    // Open the preview inventory for the player
                    player.openInventory(preview);
                    
                    plugin.getLogger().info("[ShulkerBackpack] Opened shulker box preview for " + player.getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[ShulkerBackpack] Failed to read shulker box: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // Check if the closed inventory is our shulker box preview
        Inventory topInv = player.getOpenInventory().getTopInventory();
        if (topInv.getType() != InventoryType.SHULKER_BOX) return;
        
        // The shulker box's inventory should match the preview inventory
        // If so, update the actual shulker box with the modified contents
        try {
            // Get the player's main hand item using the new API
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand == null || mainHand.getType() != SHULKER_BOX) return;
            
            ItemMeta mainMeta = mainHand.getItemMeta();
            if (!(mainMeta instanceof BlockStateMeta blockStateMeta)) return;
            
            Object blockState = blockStateMeta.getBlockState();
            if (!(blockState instanceof ShulkerBox shulkerBox)) return;
            
            // Update the shulker box with the preview inventory's contents
            shulkerBox.getInventory().setContents(topInv.getContents());
            blockStateMeta.setBlockState(shulkerBox);
            player.getInventory().setItemInMainHand(mainHand);
            
            plugin.getLogger().info("[ShulkerBackpack] Updated shulker box contents for " + player.getName());
        } catch (Exception e) {
            // Not our inventory, ignore
        }
    }
}
