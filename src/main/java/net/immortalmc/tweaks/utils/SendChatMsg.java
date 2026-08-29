package net.immortalmc.tweaks.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.command.CommandSender;

public class SendChatMsg {
    private static final TextColor PREFIX_COLOR = TextColor.color(0xCC0000);
    private static final TextColor BRACKET_COLOR = TextColor.color(0x5CADB6);

    private static final Component PREFIX = Component.text("[", BRACKET_COLOR)
        .append(Component.text("IMMC Tweaks", PREFIX_COLOR).decorate(TextDecoration.BOLD))
        .append(Component.text("] ", BRACKET_COLOR))
        .append(Component.text("").color(TextColor.color(0xFFFFFF)));

    public static void send(CommandSender sender, Component message) {
        sender.sendMessage(PREFIX.append(Component.empty().color(TextColor.color(0xFFFFFF)).append(message)));
    }

    public static void send(CommandSender sender, String message) {
        send(sender, Component.text(message));
    }
}
