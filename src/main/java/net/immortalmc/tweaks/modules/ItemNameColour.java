package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;
import net.immortalmc.tweaks.utils.SendChatMsg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    // Handles & codes like &c, &l etc.
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be run by a player."));
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

    public String componentToColorCodes(Component component) {
        String miniMessage = MiniMessage.miniMessage().serialize(component);

        String withHex = miniMessage.replaceAll("<color:(#[A-Fa-f0-9]{6})>", "$1");
        withHex = withHex.replaceAll("<(#[A-Fa-f0-9]{6})>", "$1");
        withHex = withHex.replace("</color>", "");
        withHex = withHex
            .replace("<black>", "&0").replace("<dark_blue>", "&1")
            .replace("<dark_green>", "&2").replace("<dark_aqua>", "&3")
            .replace("<dark_red>", "&4").replace("<dark_purple>", "&5")
            .replace("<gold>", "&6").replace("<gray>", "&7")
            .replace("<dark_gray>", "&8").replace("<blue>", "&9")
            .replace("<green>", "&a").replace("<aqua>", "&b")
            .replace("<red>", "&c").replace("<light_purple>", "&d")
            .replace("<yellow>", "&e").replace("<white>", "&f")
            .replace("<bold>", "&l").replace("</bold>", "")
            .replace("<italic>", "&o").replace("</italic>", "")
            .replace("<underlined>", "&n").replace("</underlined>", "")
            .replace("<strikethrough>", "&m").replace("</strikethrough>", "")
            .replace("<obfuscated>", "&k").replace("</obfuscated>", "")
            .replace("<reset>", "&r");

        return withHex;
    }

    public Component buildClickableMessage(Component component) {
        String codes = componentToColorCodes(component);

        Component copyButton = Component.text("[Copy Name]")
            .color(TextColor.color(0xA0F1FB))
            .clickEvent(ClickEvent.copyToClipboard(codes))
            .hoverEvent(HoverEvent.showText(Component.text("Click to copy the full name to clipboard")));

        Component header = Component.text("Item Colour Codes:                   ")
            .color(TextColor.color(0xFFFFFF))
            .append(copyButton);

        Component nameLine = Component.empty();
        int i = 0;

        while (i < codes.length()) {
            if (i < codes.length() - 1 && codes.charAt(i) == '&') {
                char code = Character.toLowerCase(codes.charAt(i + 1));
                String token = "&" + codes.charAt(i + 1);
                TextColor tokenColor = legacyCodeToColor(code);
                TextDecoration decoration = legacyCodeToDecoration(code);

                Component tokenComponent = Component.text(token)
                    .color(tokenColor != null ? tokenColor : TextColor.color(0xFFFFFF))
                    .clickEvent(ClickEvent.copyToClipboard(token))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy " + token + " to clipboard")));

                if (decoration != null) {
                    tokenComponent = tokenComponent.decorate(decoration);
                }

                nameLine = nameLine.append(tokenComponent);
                i += 2;
            } else if (i + 7 <= codes.length() && codes.charAt(i) == '#') {
                String hex = codes.substring(i, i + 7);
                try {
                    TextColor tokenColor = TextColor.fromHexString(hex);
                    nameLine = nameLine.append(Component.text(hex)
                        .color(tokenColor != null ? tokenColor : TextColor.color(0xFFFFFF))
                        .clickEvent(ClickEvent.copyToClipboard(hex))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to copy " + hex + " to clipboard"))));
                    i += 7;
                } catch (Exception e) {
                    nameLine = nameLine.append(Component.text(String.valueOf(codes.charAt(i))));
                    i++;
                }
            } else {
                nameLine = nameLine.append(Component.text(String.valueOf(codes.charAt(i)))
                    .color(TextColor.color(0xFFFFFF)));
                i++;
            }
        }

        return header.append(Component.newline()).append(nameLine);
    }

    private TextColor legacyCodeToColor(char code) {
        return switch (code) {
            case '0' -> TextColor.color(0x000000);
            case '1' -> TextColor.color(0x0000AA);
            case '2' -> TextColor.color(0x00AA00);
            case '3' -> TextColor.color(0x00AAAA);
            case '4' -> TextColor.color(0xAA0000);
            case '5' -> TextColor.color(0xAA00AA);
            case '6' -> TextColor.color(0xFFAA00);
            case '7' -> TextColor.color(0xAAAAAA);
            case '8' -> TextColor.color(0x555555);
            case '9' -> TextColor.color(0x5555FF);
            case 'a' -> TextColor.color(0x55FF55);
            case 'b' -> TextColor.color(0x55FFFF);
            case 'c' -> TextColor.color(0xFF5555);
            case 'd' -> TextColor.color(0xFF55FF);
            case 'e' -> TextColor.color(0xFFFF55);
            case 'f' -> TextColor.color(0xFFFFFF);
            default -> null;
        };
    }

    private TextDecoration legacyCodeToDecoration(char code) {
        return switch (code) {
            case 'l' -> TextDecoration.BOLD;
            case 'm' -> TextDecoration.STRIKETHROUGH;
            case 'n' -> TextDecoration.UNDERLINED;
            case 'o' -> TextDecoration.ITALIC;
            default -> null;
        };
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