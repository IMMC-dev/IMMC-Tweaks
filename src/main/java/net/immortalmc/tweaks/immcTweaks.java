package net.immortalmc.tweaks;

import org.bukkit.plugin.java.JavaPlugin;

import net.immortalmc.tweaks.modules.NoCropTrample;
import net.immortalmc.tweaks.modules.SignTextColour;
import net.immortalmc.tweaks.modules.ItemNameColour;
import net.immortalmc.tweaks.modules.NoAnvilLimit;

public class immcTweaks extends JavaPlugin {

    @Override
    public void onEnable() {
        
        getServer().getPluginManager().registerEvents(new NoCropTrample(), this);
        getLogger().info("NoCropTrample Loaded");
        getServer().getPluginManager().registerEvents(new SignTextColour(), this);
        getLogger().info("SignTextColour Loaded");
        ItemNameColour itemNameColour = new ItemNameColour(this);
        getServer().getPluginManager().registerEvents(itemNameColour, this);
        getServer().getPluginCommand("itemnamecolour").setExecutor(itemNameColour);
        getLogger().info("ItemNameColour Loaded"); 
        getServer().getPluginManager().registerEvents(new NoAnvilLimit(), this);
        getLogger().info("NoAnvilLimit Loaded");
        getLogger().info("IMMC Tweaks enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("IMMC Tweaks disabled.");
    }
}
