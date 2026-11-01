package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;
import net.immortalmc.tweaks.utils.SendChatMsg;
import net.immortalmc.tweaks.utils.ColourCodes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

public class ItemNameColour implements Listener, CommandExecutor {

    private final immcTweaks plugin; 

    public ItemNameColour(immcTweaks plugin) { 
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            SendChatMsg.send(sender, "This command can only be run by a player.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir() || !item.hasItemMeta()) {
            SendChatMsg.send(player, "You must be holding an item with a custom name.");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            SendChatMsg.send(player, Component.text("The item you're holding has no custom name."));
            return true;
        }

        Component displayName = meta.displayName();
        if (displayName == null) {
            SendChatMsg.send(player, Component.text("The item you're holding has no custom name."));
            return true;
        }

        SendChatMsg.send(player, buildClickableMessage(meta.displayName()));
        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null) return;

        String name = event.getView().getRenameText();
        if (name == null || name.isEmpty()) return;

        // Convert & codes and #rrggbb hex codes to a Component
        Component formatted = ColourCodes.parse(name);

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
                SendChatMsg.send(player, buildClickableMessage(meta.displayName()));
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

        SendChatMsg.send(player, buildClickableMessage(meta.displayName()));
    }

    public Component buildClickableMessage(Component component) {
        String codes = ColourCodes.toCodes(component);

        Component copyButton = Component.text("[Copy Name]")
            .color(TextColor.color(0xA0F1FB))
            .clickEvent(ClickEvent.copyToClipboard(codes))
            .hoverEvent(HoverEvent.showText(Component.text("Click to copy the full name to clipboard")));

        Component header = Component.text("Item Colour Codes:     ")
            .color(TextColor.color(0xFFFFFF))
            .append(copyButton);

        return header
            .append(Component.newline())
            .append(ColourCodes.buildCodeLine(codes));
    }
}