package net.immortalmc.tweaks.modules;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.view.AnvilView;

/**
 * Removes the anvil's "Too Expensive!" limit.
 *
 * Vanilla refuses any anvil operation costing 40 levels or more. This module:
 *   1. Raises the server-side limit so those operations are no longer blocked.
 *   2. Caps the cost at 39 levels. The client shows "Too Expensive!" whenever it sees a cost
 *      of 40 or more, regardless of what the server allows, so the cost is held just below
 *      that. The cost shown is also the cost charged.
 *
 * The result is that anything that would have been too expensive simply costs 39 levels.
 *
 * Config: no-anvil-limit.enabled (the on/off switch is handled in immcTweaks).
 */
public class NoAnvilLimit implements Listener {

    /** The highest cost the client will display without showing "Too Expensive!". */
    private static final int MAX_COST = 39;

    // The priority here is set to HIGHEST to ensure that this runs last and sees the cost after every other module has had its say
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilView view = event.getView();

        // Lift the server-side limit for this anvil
        view.setMaximumRepairCost(Integer.MAX_VALUE);

        // Keep the cost low enough that the client doesn't show "Too Expensive!"
        if (view.getRepairCost() > MAX_COST) {
            view.setRepairCost(MAX_COST);
        }
    }
}