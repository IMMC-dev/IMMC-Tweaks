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
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
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
    private final Map<Player, org.bukkit.inventory.Inventory> previewInventories = new HashMap<>(); // Track preview inventories
    private final Map<Player, Integer> taggedHand = new HashMap<>(); // Track which hand the outdoor shulker is in (-1 = main hand, -2 = off hand)

    public ShulkerBackpack(immcTweaks plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("shulker-backpack.enabled", true);
        this.SHULKER_TAG_KEY = new NamespacedKey(plugin, "shulker_backpack_tag");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Cancel clicks on the tagged slot (player inventory only)
        Integer playerTaggedSlot = taggedSlots.get(player);
        if (playerTaggedSlot != null
                && event.getClickedInventory() == player.getInventory()
                && playerTaggedSlot == event.getSlot()) {
            event.setCancelled(true);
            return;
        }

        // Only handle right-clicks
        if (event.getAction() != InventoryAction.PICKUP_HALF) return;

        // Only the player's own inventory
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || clickedInventory.getType() != InventoryType.PLAYER) return;

        // Only shulker boxes
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != SHULKER_BOX) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (!(meta instanceof BlockStateMeta)) return;

        // Cancel the pickup, then open the real preview one tick later
        event.setCancelled(true);

        final int slot = event.getSlot();
        taggedSlots.put(player, slot);
        Bukkit.getScheduler().runTask(plugin, () ->
                openShulkerBackpackPreview(player, clickedItem, slot, meta));
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player)) return;

    // Ignore closes that aren't our preview
    Inventory preview = previewInventories.get(player);
    if (preview == null || event.getInventory() != preview) return;

    previewInventories.remove(player);
    Integer slot = taggedSlots.remove(player);

    if (slot != null) {
        ItemStack item = player.getInventory().getItem(slot);
        if (item != null
                && item.getType() == Material.SHULKER_BOX
                && item.getItemMeta() instanceof BlockStateMeta bsm
                && bsm.getBlockState() instanceof ShulkerBox box) {

            // Save the preview contents back into the shulker
            box.getInventory().setContents(preview.getContents());
            bsm.setBlockState(box);

            // Remove the tag if the interact path added one
            bsm.getPersistentDataContainer().remove(SHULKER_TAG_KEY);

            item.setItemMeta(bsm);
        }
    }

    preview.clear();
}
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR) return;
        
        // Check if player is holding a shulker box in main hand or off hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        ItemStack shulkerInHand = null;
        if (mainHand != null && mainHand.getType() == SHULKER_BOX) {
            shulkerInHand = mainHand;
        } else if (offHand != null && offHand.getType() == SHULKER_BOX) {
            shulkerInHand = offHand;
        }
        
        if (shulkerInHand == null) return;
        
        // Get the player's inventory to find the slot number
        Inventory playerInventory = player.getInventory();
        int slotNumber = -1;
        
        // Check hotbar slots (0-8) - check mainhand first
        if (shulkerInHand == mainHand) {
            for (int i = 0; i < 9; i++) {
                ItemStack slotItem = playerInventory.getItem(i);
                if (slotItem != null && slotItem.isSimilar(shulkerInHand)) {
                    slotNumber = i;
                    break;
                }
            }
        }
        
        // If not in hotbar, check offhand slot (40)
        if (slotNumber == -1 && shulkerInHand == offHand) {
            ItemStack offHandItem = playerInventory.getItem(40);
            if (offHandItem != null && offHandItem.isSimilar(shulkerInHand)) {
                slotNumber = 40;
            }
        }
        
        if (slotNumber == -1) {
            plugin.getLogger().warning("[ShulkerBackpack] Could not find slot number for shulker box in inventory");
            return;
        }
        
        // Tag the shulker box in the actual inventory slot
        ItemStack itemInSlot = playerInventory.getItem(slotNumber);
        ItemMeta slotMeta = itemInSlot.getItemMeta();
        if (slotMeta != null) {
            org.bukkit.persistence.PersistentDataContainer container = slotMeta.getPersistentDataContainer();
            container.set(SHULKER_TAG_KEY, PersistentDataType.BYTE, (byte) 1);
            slotMeta.getPersistentDataContainer().set(SHULKER_TAG_KEY, PersistentDataType.BYTE, (byte) 1);
            itemInSlot.setItemMeta(slotMeta);
        }
        
        // Store the actual slot number
        taggedSlots.put(player, slotNumber);
        
        // Get the shulker box item's meta
        ItemMeta itemMeta = shulkerInHand.getItemMeta();
        
        // Open the shulker backpack preview
        openShulkerBackpackPreview(player, shulkerInHand, slotNumber, itemMeta);
    }
    
    private void openShulkerBackpackPreview(Player player, ItemStack shulkerItem, int slotNumber, ItemMeta meta) {
        // Get the display name from the shulker box item
        String shulkerName = "Shulker Box";
        Component title = null;
        if (meta.hasDisplayName()) {
            title = meta.displayName();
        }
        
        // Try to get the shulker box contents from BlockStateMeta
        try {
            if (meta instanceof BlockStateMeta blockStateMeta) {
                Object blockState = blockStateMeta.getBlockState();
                if (blockState instanceof ShulkerBox shulkerBox) {
                    Inventory shulkerInventory = shulkerBox.getInventory();
                    
                    // Create a preview inventory using the new API (Component title)
                    Inventory preview = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, title != null ? title : Component.text(shulkerName));
                    preview.setContents(shulkerInventory.getContents());
                    
                    // Open the preview inventory for the player
                    player.openInventory(preview);
                    
                    // Store the preview inventory to access it later
                    previewInventories.put(player, preview);
                    
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
                    
                    String handName = "hotbar";
                    if (slotNumber == 40) handName = "off hand";
                    plugin.getLogger().info("[ShulkerBackpack] Tagged shulker for " + player.getName() + " - Name: " + (title != null ? title.toString() : shulkerName) + " - Slot: " + slotNumber + " (" + handName + ") - Contents:\n" + contents.toString());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[ShulkerBackpack] Failed to read shulker box: " + e.getMessage());
        }
    }
}
