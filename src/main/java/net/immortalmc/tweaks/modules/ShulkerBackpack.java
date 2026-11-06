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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import net.kyori.adventure.text.Component;

import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Shulker Backpack module - opens shulker box inventory when right-clicked in inventory.
 */
public class ShulkerBackpack implements Listener {
    private static final Material SHULKER_BOX = Material.SHULKER_BOX;
    private final NamespacedKey SHULKER_TAG_KEY;

    private final immcTweaks plugin;
    private final Map<Player, Integer> activeShulkerSlots = new HashMap<>();
    private final Map<Player, Inventory> openPreviews = new HashMap<>();

    public ShulkerBackpack(immcTweaks plugin) {
        this.plugin = plugin;
        this.SHULKER_TAG_KEY = new NamespacedKey(plugin, "shulker_backpack_tag");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // While a preview is open, protect the shulker it came from
        Integer slot = activeShulkerSlots.get(player);
        if (slot != null) {
            boolean clickedTagged = event.getClickedInventory() == player.getInventory()
                    && event.getSlot() == slot;
            boolean swappedWithTagged =
                    (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() == slot)
                    || (event.getClick() == ClickType.SWAP_OFFHAND && slot == 40);

            if (clickedTagged || swappedWithTagged) {
                event.setCancelled(true);
                return;
            }
        }

        // Only handle right-clicks
        if (event.getAction() != InventoryAction.PICKUP_HALF) return;

        // Only the player's own inventory
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || clickedInventory.getType() != InventoryType.PLAYER) return;

        // Only shulker boxes
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != SHULKER_BOX) return;
        if (!(clickedItem.getItemMeta() instanceof BlockStateMeta)) return;

        // Never pick the shulker up with the cursor
        event.setCancelled(true);

        // One preview at a time
        if (openPreviews.containsKey(player)) return;

        int targetSlot = event.getSlot();
        Bukkit.getScheduler().runTask(plugin, () -> openPreview(player, targetSlot));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClickPreview(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory preview = openPreviews.get(player);
        if (preview == null) return;

        // Sync whenever interacting with preview or player inventory while preview is open
        if (event.getInventory() == preview || event.getInventory() == player.getInventory()) {
            Bukkit.getScheduler().runTask(plugin, () -> syncPreviewToShulker(player));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory preview = openPreviews.get(player);
        if (preview == null) return;

        // Sync when dragging items in preview or player inventory
        if (event.getInventory() == preview || event.getInventory() == player.getInventory()) {
            Bukkit.getScheduler().runTask(plugin, () -> syncPreviewToShulker(player));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory preview = openPreviews.get(player);
        if (preview == null || event.getInventory() != preview) return;

        openPreviews.remove(player);
        Integer slot = activeShulkerSlots.remove(player);

        if (slot != null) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null
                    && item.getType() == SHULKER_BOX
                    && item.getItemMeta() instanceof BlockStateMeta bsm
                    && bsm.getBlockState() instanceof ShulkerBox box) {

                // Remove the tag if the interact path added one
                bsm.getPersistentDataContainer().remove(SHULKER_TAG_KEY);
                item.setItemMeta(bsm);
            }
        }

        preview.clear();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;

        EquipmentSlot hand = event.getHand();
        if (hand == null) return;

        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();

        int slot;
        if (hand == EquipmentSlot.HAND) {
            slot = inventory.getHeldItemSlot();
        } else {
            // Main hand has priority over off hand
            if (inventory.getItemInMainHand().getType() == SHULKER_BOX) return;
            slot = 40;
        }

        openPreview(player, slot);
    }

    private void openPreview(Player player, int slot) {
        // One preview at a time
        if (openPreviews.containsKey(player)) return;

        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType() != SHULKER_BOX) return;
        if (!(item.getItemMeta() instanceof BlockStateMeta bsm)) return;
        if (!(bsm.getBlockState() instanceof ShulkerBox box)) return;

        Component title = bsm.hasDisplayName() ? bsm.displayName() : Component.text("Shulker Box");

        Inventory preview = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, title);
        preview.setContents(box.getInventory().getContents());

        // openInventory returns null if another plugin cancelled the open
        if (player.openInventory(preview) == null) return;

        // Register only once the preview is actually open
        openPreviews.put(player, preview);
        activeShulkerSlots.put(player, slot);

        plugin.getLogger().info("[ShulkerBackpack] Opened shulker for " + player.getName() + " from slot " + slot);
    }

    private void syncPreviewToShulker(Player player) {
        // Only sync if the preview is still open and tagged
        if (!openPreviews.containsKey(player) || !activeShulkerSlots.containsKey(player)) return;

        Inventory preview = openPreviews.get(player);
        if (preview == null) return;

        Integer slot = activeShulkerSlots.get(player);
        if (slot == null) return;

        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType() != SHULKER_BOX) return;
        if (!(item.getItemMeta() instanceof BlockStateMeta bsm)) return;
        if (!(bsm.getBlockState() instanceof ShulkerBox box)) return;

        // Copy preview contents back to shulker
        box.getInventory().setContents(preview.getContents());
        bsm.setBlockState(box);

        // Remove the tag if the interact path added one
        bsm.getPersistentDataContainer().remove(SHULKER_TAG_KEY);

        item.setItemMeta(bsm);
    }
}
