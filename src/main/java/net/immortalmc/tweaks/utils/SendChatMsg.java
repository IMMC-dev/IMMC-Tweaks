package net.immortalmc.tweaks.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.command.CommandSender;

/**
 * Sends chat messages with the standard plugin prefix: [IMMC Tweaks] message
 *
 * Use this for every message sent to a player or the console, so the style stays consistent.
 */
public final class SendChatMsg {

    private static final TextColor PREFIX_COLOR = TextColor.color(0xCC0000);
    private static final TextColor BRACKET_COLOR = TextColor.color(0x5CADB6);

    // "[IMMC Tweaks] " prefix with colours and formatting
    private static final Component PREFIX = Component.text("[", BRACKET_COLOR)
        .append(Component.text("IMMC Tweaks", PREFIX_COLOR).decorate(TextDecoration.BOLD))
        .append(Component.text("] ", BRACKET_COLOR));

    private SendChatMsg() {}

    // Sends a message that already has its own styling (colours, click events, etc.).
    public static void send(CommandSender sender, Component message) {
        // The message goes inside a white wrapper, so any part of it without its own colour
        // is white instead of inheriting the bracket colour from the prefix.
        Component body = Component.empty().color(NamedTextColor.WHITE).append(message);
        sender.sendMessage(PREFIX.append(body));
    }

    // Sends plain text in white.
    public static void send(CommandSender sender, String message) {
        send(sender, Component.text(message));
    }
}