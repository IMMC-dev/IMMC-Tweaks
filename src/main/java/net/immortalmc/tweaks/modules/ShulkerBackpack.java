package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Shulker Backpack module - opens shulker box inventory when right-clicked in inventory.
 */
public class ShulkerBackpack implements Listener {
    private final immcTweaks plugin;
    private boolean enabled;

    private static final Material SHULKER_BOX = Material.SHULKER_BOX;
    private final NamespacedKey SHULKER_TAG_KEY;
    private final Map<Player, Integer> taggedSlots = new HashMap<>(); // Track which slot was tagged

    public ShulkerBackpack(immcTweaks plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("shulker-backpack.enabled", true);
        this.SHULKER_TAG_KEY = new NamespacedKey(plugin, "shulker_backpack_tag");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Simple debug: log any click and which slot
        Integer playerTaggedSlot = taggedSlots.get(player);
        boolean isTagged = (playerTaggedSlot != null && playerTaggedSlot == event.getSlot());
        plugin.getLogger().info("[ShulkerBackpack] Player " + player.getName() + " clicked slot " + event.getSlot() + " - Current item: " + (event.getCurrentItem() != null ? event.getCurrentItem().getType().name() : "null") + " - tagged: " + isTagged);
        
        // Cancel the click if it's the tagged slot
        if (isTagged) {
            event.setCancelled(true);
        }
        
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

                    // Clear the cursor to prevent phantom items
                    event.setCursor(null);

                    // Get the display name from the shulker box item
                    String shulkerName = "Shulker Box";
                    Component title = null;
                    if (meta.hasDisplayName()) {
                        title = meta.displayName();
                    }

                    // Create a preview inventory using the new API (Component title)
                    Inventory preview = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, title != null ? title : Component.text(shulkerName));
                    preview.setContents(shulkerInventory.getContents());

                    // Open the preview inventory for the player
                    player.openInventory(preview);

                    // Tag the shulker box to prevent it from being moved or dropped
                    // Create a copy to avoid modifying the original item in inventory
                    ItemStack taggedShulker = clickedItem.clone();
                    ItemMeta taggedMeta = taggedShulker.getItemMeta();
                    if (taggedMeta != null) {
                        org.bukkit.persistence.PersistentDataContainer container = taggedMeta.getPersistentDataContainer();
                        container.set(SHULKER_TAG_KEY, PersistentDataType.BYTE, (byte) 1);
                        taggedShulker.setItemMeta(taggedMeta);
                    }

                    // Store the slot number where the shulker was tagged
                    taggedSlots.put(player, event.getSlot());

                    // Debug output
                    StringBuilder contents = new StringBuilder();
                    for (int i = 0; i < shulkerInventory.getSize(); i++) {
                        ItemStack item = shulkerInventory.getItem(i);
                        if (item != null) {
                            contents.append(i).append(": ").append(item.getType()).append(" (").append(item.getAmount()).append(")\n");
                        } else {
                            contents.append(i).append(": empty\n");
                        }
                    }
                    
                    plugin.getLogger().info("[ShulkerBackpack] Tagged shulker for " + player.getName() + " - Name: " + (title != null ? title.toString() : shulkerName) + " - Slot: " + event.getSlot() + " - Contents:\n" + contents.toString());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[ShulkerBackpack] Failed to read shulker box: " + e.getMessage());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        Integer taggedSlot = taggedSlots.remove(player);
        if (taggedSlot != null) {
            plugin.getLogger().info("[ShulkerBackpack] Inventory close event fired for player: " + player.getName() + ", taggedSlot: " + taggedSlot);

            // Remove the tag
            ItemStack item = player.getInventory().getItem(taggedSlot);
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getPersistentDataContainer().has(SHULKER_TAG_KEY, PersistentDataType.BYTE)) {
                    meta.getPersistentDataContainer().remove(SHULKER_TAG_KEY);
                    item.setItemMeta(meta);
                }
            }
        } else {
            plugin.getLogger().info("[ShulkerBackpack] Inventory close event fired for player: " + player.getName() + ", but no tagged slot found");
        }
        
        // Check if player has any remaining tagged slots
        plugin.getLogger().info("[ShulkerBackpack] Player " + player.getName() + " has " + taggedSlots.size() + " tagged slot(s) remaining: " + taggedSlots.values());
    }
}
