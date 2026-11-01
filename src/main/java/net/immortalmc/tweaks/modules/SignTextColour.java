package net.immortalmc.tweaks.modules;

import net.immortalmc.tweaks.utils.ColourCodes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import io.papermc.paper.event.player.PlayerOpenSignEvent;

import java.util.List;

public class SignTextColour implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onSignOpen(PlayerOpenSignEvent event) {
            // getState() gives us a copy, so changing it does NOT change the real sign
            Sign virtualSign = (Sign) event.getSign().getBlock().getState();
            SignSide side = virtualSign.getSide(event.getSide());

            List<Component> lines = side.lines();
            for (int i = 0; i < lines.size(); i++) {
                side.line(i, Component.text(ColourCodes.toCodes(lines.get(i))));
            }

            // Send the codes version to the editing player only
            event.getPlayer().sendBlockUpdate(virtualSign.getLocation(), virtualSign);
        }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onSignChange(SignChangeEvent event) {
            List<Component> lines = event.lines();

            for (int i = 0; i < lines.size(); i++) {
                // Get exactly what the player typed, as plain text
                String text = PlainTextComponentSerializer.plainText().serialize(lines.get(i));
                if (text.isEmpty()) continue;

                // Convert & codes and #rrggbb codes into a coloured Component
                event.line(i, ColourCodes.parse(text));
            }
        }
}
