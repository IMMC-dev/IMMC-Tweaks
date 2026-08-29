package net.immortalmc.tweaks.modules;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemNameColour implements Listener {

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