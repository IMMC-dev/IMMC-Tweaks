package net.immortalmc.tweaks;

import org.bukkit.plugin.java.JavaPlugin;

import net.immortalmc.tweaks.modules.NoCropTrample;
import net.immortalmc.tweaks.modules.SignTextColour;
import net.immortalmc.tweaks.modules.ItemNameColour;
import net.immortalmc.tweaks.modules.NoAnvilLimit;
import net.immortalmc.tweaks.modules.CreeperConfetti;

public class immcTweaks extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        if (getConfig().getBoolean("no-crop-trample.enabled", true)) {
            getServer().getPluginManager().registerEvents(new NoCropTrample(), this);
            getLogger().info("NoCropTrample Loaded");
        }

        if (getConfig().getBoolean("sign-text-colour.enabled", true)) {
            getServer().getPluginManager().registerEvents(new SignTextColour(), this);
            getLogger().info("SignTextColour Loaded");
        }

        if (getConfig().getBoolean("item-name-colour.enabled", true)) {
            ItemNameColour itemNameColour = new ItemNameColour(this);
            getServer().getPluginManager().registerEvents(itemNameColour, this);
            getServer().getPluginCommand("itemnamecolour").setExecutor(itemNameColour);
            getLogger().info("ItemNameColour Loaded"); 
        }
        
        if (getConfig().getBoolean("no-anvil-limit.enabled", true)) {
            getServer().getPluginManager().registerEvents(new NoAnvilLimit(), this);
            getLogger().info("NoAnvilLimit Loaded");
        }

        if (getConfig().getBoolean("creeper-confetti.enabled", true)) {
            getServer().getPluginManager().registerEvents(new CreeperConfetti(this), this);
            getLogger().info("CreeperConfetti Loaded");
        }
        getLogger().info("IMMC Tweaks enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("IMMC Tweaks disabled.");
    }
}
