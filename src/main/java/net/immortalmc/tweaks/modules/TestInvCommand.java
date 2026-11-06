package net.immortalmc.tweaks.modules;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;

public class TestInvCommand implements Listener, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            player.openInventory(Bukkit.createInventory(null, InventoryType.SHULKER_BOX, Component.text("test")));
        }
        return true;
    }
}