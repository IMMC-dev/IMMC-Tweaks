package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemNameColour implements Listener {

    private final immcTweaks plugin; 

    public ItemNameColour(immcTweaks plugin) { 
        this.plugin = plugin;
    }

    // Handles & codes like &c, &l etc.
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null) return;

        String name = event.getView().getRenameText();
        if (name == null || name.isEmpty()) return;

        // Convert & codes and #rrggbb hex codes to a Component
        Component formatted = parseColors(name);

        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;

        meta.displayName(formatted);
        result.setItemMeta(meta);
        event.setResult(result);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAnvilSlotFilled(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            AnvilInventory anvil = (AnvilInventory) event.getInventory();
            
            // If slot 0 is currently empty, an item is being added to it
            // If slot 0 already has an item, the player is removing it
            ItemStack currentSlot0 = anvil.getItem(0);
            if (currentSlot0 != null && !currentSlot0.getType().isAir()) return;

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                ItemStack item = anvil.getItem(0);
                if (item == null || !item.hasItemMeta()) return;
                ItemMeta meta = item.getItemMeta();
                if (meta == null || !meta.hasDisplayName()) return;
                Component displayName = meta.displayName();
                if (displayName == null) return;
                player.sendMessage(Component.text("Item Colour Codes: ")
                    .append(Component.text(componentToColorCodes(displayName))));
            });
            return;
        }

        // Only care about slot 0 (left input)
        if (event.getRawSlot() != 0) return;

        ItemStack item = event.getCursor();
        if (item == null || item.getType().isAir()) return; // removed the getCurrentItem() fallback

        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        Component displayName = meta.displayName();
        if (displayName == null) return;

        String colorCodes = componentToColorCodes(displayName);
        player.sendMessage(Component.text("Item Colour Codes: ")
            .append(Component.text(colorCodes)));
    }

    private String componentToColorCodes(Component component) {
        // Serialize back to MiniMessage, then convert hex tags to # format
        // and convert named colours to & codes
        String miniMessage = MiniMessage.miniMessage().serialize(component);

        // Replace <color:#rrggbb> back to #rrggbb
        String withHex = miniMessage.replaceAll(
            "<color:(#[A-Fa-f0-9]{6})>",
            "$1"
        );

        // Remove closing </color> tags
        withHex = withHex.replace("</color>", "");

        // Convert MiniMessage named formatting back to & codes
        withHex = withHex
            .replace("<black>", "&0")
            .replace("<dark_blue>", "&1")
            .replace("<dark_green>", "&2")
            .replace("<dark_aqua>", "&3")
            .replace("<dark_red>", "&4")
            .replace("<dark_purple>", "&5")
            .replace("<gold>", "&6")
            .replace("<gray>", "&7")
            .replace("<dark_gray>", "&8")
            .replace("<blue>", "&9")
            .replace("<green>", "&a")
            .replace("<aqua>", "&b")
            .replace("<red>", "&c")
            .replace("<light_purple>", "&d")
            .replace("<yellow>", "&e")
            .replace("<white>", "&f")
            .replace("<bold>", "&l")
            .replace("</bold>", "")
            .replace("<italic>", "&o")
            .replace("</italic>", "")
            .replace("<underlined>", "&n")
            .replace("</underlined>", "")
            .replace("<strikethrough>", "&m")
            .replace("</strikethrough>", "")
            .replace("<obfuscated>", "&k")
            .replace("</obfuscated>", "")
            .replace("<reset>", "&r");

        return withHex;
    }

    private Component parseColors(String input) {
        // First handle & legacy codes
        Component result = LEGACY.deserialize(input);
        String reEncoded = MiniMessage.miniMessage().serialize(result);

        // Then replace #rrggbb with MiniMessage hex tags and parse
        String withHex = reEncoded.replaceAll(
            "#([A-Fa-f0-9]{6})",
            "<color:#$1>"
        );

        return MiniMessage.miniMessage().deserialize(withHex);
    }
}