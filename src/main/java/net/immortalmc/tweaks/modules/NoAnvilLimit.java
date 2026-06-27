package net.immortalmc.tweaks.modules;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.view.AnvilView;

public class NoAnvilLimit implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilView view = event.getView();
        view.setMaximumRepairCost(Integer.MAX_VALUE);

        if (view.getRepairCost() > 39) {
            view.setRepairCost(39);
        }

        if (event.getResult() == null) {
            event.setResult(view.getTopInventory().getFirstItem());
        }
    }
}