package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.utils.ColourCodes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import io.papermc.paper.event.player.PlayerOpenSignEvent;

import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.List;

/**
 * Lets players use colour codes (& codes and #rrggbb hex) on signs.
 *
 * The sign editor only ever shows plain text, so editing an already-coloured sign would normally lose its colours. 
 * This module gets around that with a pair of handlers:
 *      1. onSignOpen:      when the editor opens, the player is shown the sign with its colours written out as codes.
 *      2. onSignChange:    when the sign is saved, the codes are turned back into colours.
 *
 * Config: sign-text-colour.enabled (the on/off switch is handled in immcTweaks).
 */
public class SignTextColour implements Listener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    // Step 1: show the editing player the sign's codes instead of its colours. 
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignOpen(PlayerOpenSignEvent event) {
        // getState() gives us a copy, so changing it does NOT change the real sign
        Sign virtualSign = (Sign) event.getSign().getBlock().getState();
        SignSide side = virtualSign.getSide(event.getSide());

        List<Component> lines = side.lines();
        for (int i = 0; i < lines.size(); i++) {
            side.line(i, Component.text(ColourCodes.toCodes(lines.get(i))));
        }

        // Send the codes version to the editing player only, so everyone else still sees the real sign
        event.getPlayer().sendBlockUpdate(virtualSign.getLocation(), virtualSign);
    }

    // Step 2: turn the codes the player typed back into colours when the sign is saved.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        List<Component> lines = event.lines();

        for (int i = 0; i < lines.size(); i++) {
            // What the player typed, as plain text (any existing colours are ignored)
            String text = PLAIN.serialize(lines.get(i));
            if (text.isEmpty()) continue;

            event.line(i, ColourCodes.parse(text));
        }
    }
}