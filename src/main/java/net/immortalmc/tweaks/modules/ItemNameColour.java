package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.immcTweaks;
import net.immortalmc.tweaks.utils.ColourCodes;
import net.immortalmc.tweaks.utils.SendChatMsg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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

/**
 * Lets players use colour codes (& codes and #rrggbb hex) when renaming items in an anvil,
 * and shows them the codes of an existing name so they can edit it.
 *
 *   - Anvil renaming: codes typed in the rename box become real colours on the item.
 *   - Placing a named item in an anvil: the player is sent its name as clickable codes.
 *   - /itemnamecolour: sends the same message for the item in the player's main hand.
 *
 * The anvil's rename box only ever contains plain text, which is why the codes have to be
 * sent to the player in chat rather than appearing in the box itself.
 *
 * Config: item-name-colour.enabled (the on/off switch is handled in immcTweaks).
 */
public class ItemNameColour implements Listener, CommandExecutor {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final TextColor COPY_BUTTON_COLOUR = TextColor.color(0xA0F1FB);

    // The anvil's left input slot id, where the item being renamed goes.
    private static final int LEFT_INPUT_SLOT = 0;

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

          if (!sendNameCodes(player, player.getInventory().getItemInMainHand(), false)) {
            SendChatMsg.send(player, "You must be holding an item with a custom name.");
        }
        return true;
    }

    // Turns the codes typed in the anvil's rename box into a coloured name on the result item. 
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null) return;

        // Grab what the player has typed in the rename box
        String name = event.getView().getRenameText();
        if (name == null || name.isEmpty()) return;

        // The rename box starts out filled with the item's current name as plain text. If it hasn't been changed, leave the result alone so the item keeps its existing colours.
        // For example when the item is only being repaired or enchanted, the item name should not be changed or stripped or formatting.
        Component currentName = getCustomName(event.getInventory().getFirstItem());
        if (currentName != null && name.equals(PLAIN.serialize(currentName))) return;

        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;

        meta.displayName(ColourCodes.parse(name));
        result.setItemMeta(meta);
        event.setResult(result);
    }

    // When a named item is placed in the anvil, sends the player the codes of its name.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAnvilSlotFilled(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory anvil)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Handle shift-clicking.
        // On shift-click the game picks the slot, so wait until the click has finished and then look at what ended up in the left input slot.
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // If the left slot is already occupied, nothing new lands there
            ItemStack leftSlot = anvil.getItem(LEFT_INPUT_SLOT);
            if (leftSlot != null && !leftSlot.getType().isAir()) return;

            plugin.getServer().getScheduler().runTask(plugin,
                () -> sendNameCodes(player, anvil.getItem(LEFT_INPUT_SLOT), true));
            return;
        }

        // Handle regular item placement
        if (event.getRawSlot() != LEFT_INPUT_SLOT) return;
        sendNameCodes(player, event.getCursor(), true);
    }

    // The item's custom name, or null if it's empty or has no custom name.
    private Component getCustomName(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        ItemMeta meta = item.getItemMeta();
        return meta == null ? null : meta.displayName();
    }

    // Check if the item name actually has custom formatting.
    // Returns true if the name has any colour or formatting, i.e. its codes differ from its plain text.
    private boolean hasFormatting(Component name) {
        return !ColourCodes.toCodes(name).equals(PLAIN.serialize(name));
    }

    /**
     * Sends the player the text and codes of the item's custom name.
     *
     * @param onlyIfFormatted   if true, this function stays quiet when the name has no colour or formatting. 
     *                          A plain name already shows up in full in the anvil's rename box, making this redundant.
     * 
     * @return                  Returns false if the item has no custom name at all.
     *                          Returns true otherwise, even if nothing was sent because the name was plain, so the command can tell the player the item has no name
     */
    private boolean sendNameCodes(Player player, ItemStack item, boolean onlyIfFormatted) {
        Component name = getCustomName(item);
        if (name == null) return false;

        if (!onlyIfFormatted || hasFormatting(name)) {
            SendChatMsg.send(player, buildClickableMessage(name));
        }
        return true;
    }

    /**
     * Builds the two-line message:
     *   Item Colour Codes:     [Copy Name]
     *   <the name's codes, each one highlighted and click-to-copy>
     * The copy button is on the first line so a long name can't push it onto a wrapped line.
     */
    private Component buildClickableMessage(Component name) {
        String codes = ColourCodes.toCodes(name);

        Component copyButton = Component.text("[Copy Name]")
            .color(COPY_BUTTON_COLOUR)
            .clickEvent(ClickEvent.copyToClipboard(codes))
            .hoverEvent(HoverEvent.showText(Component.text("Click to copy the full name to clipboard")));

        return Component.text("Item Colour Codes:     ")
            .append(copyButton)
            .append(Component.newline())
            .append(ColourCodes.buildCodeLine(codes));
    }
}