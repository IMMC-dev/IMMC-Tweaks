package net.immortalmc.tweaks.modules;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * Stops farmland from being trampled into dirt when players or mobs jump or fall on it.
 * The farmland never changes, so any crops on it are kept too.
 *
 * Config: no-crop-trample.enabled (the on/off switch is handled in immcTweaks).
 */
public class NoCropTrample implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTrample(EntityChangeBlockEvent event) {
        // Trampling is an entity turning farmland into dirt. 
        // Checking the result as well leaves other changes alone.
        // I don't think these other changes exist, but it future proofs this module in case one is ever added.
        if (event.getBlock().getType() == Material.FARMLAND && event.getTo() == Material.DIRT) {
            event.setCancelled(true);
        }
    }
}