package net.immortalmc.tweaks;

import org.bukkit.plugin.java.JavaPlugin;

import net.immortalmc.tweaks.modules.ItemNameColour;
import net.immortalmc.tweaks.modules.NoAnvilLimit;
import net.immortalmc.tweaks.modules.NoCropTrample;

public class immcTweaks extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new NoAnvilLimit(), this);
        getLogger().info("NoAnvilLimit Loaded");
        getServer().getPluginManager().registerEvents(new NoCropTrample(), this);
        getLogger().info("NoCropTrample Loaded");
        getServer().getPluginManager().registerEvents(new ItemNameColour(this), this);
        getLogger().info("ItemNameColour Loaded"); 
        getLogger().info("IMMC Tweaks enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("IMMC Tweaks disabled.");
    }
}
