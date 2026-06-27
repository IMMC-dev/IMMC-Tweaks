package net.immortalmc.tweaks;

import org.bukkit.plugin.java.JavaPlugin;

import net.immortalmc.tweaks.modules.NoAnvilLimit;

public class immcTweaks extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new NoAnvilLimit(), this);
        getLogger().info("IMMC Tweaks enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("IMMC Tweaks disabled.");
    }
}
