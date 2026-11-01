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
            getLogger().info("NoCropTrample Enabled");
        } else {
            getLogger().info("NoCropTrample Disabled");
        }

        if (getConfig().getBoolean("sign-text-colour.enabled", true)) {
            getServer().getPluginManager().registerEvents(new SignTextColour(), this);
            getLogger().info("SignTextColour Enabled");
        } else {
            getLogger().info("SignTextColour Disabled");
        }

        if (getConfig().getBoolean("item-name-colour.enabled", true)) {
            ItemNameColour itemNameColour = new ItemNameColour(this);
            getServer().getPluginManager().registerEvents(itemNameColour, this);
            getServer().getPluginCommand("itemnamecolour").setExecutor(itemNameColour);
            getLogger().info("ItemNameColour Enabled"); 
        } else {
            getLogger().info("ItemNameColour Disabled");
        }
        
        if (getConfig().getBoolean("no-anvil-limit.enabled", true)) {
            getServer().getPluginManager().registerEvents(new NoAnvilLimit(), this);
            getLogger().info("NoAnvilLimit Enabled");
        } else {
            getLogger().info("NoAnvilLimit Disabled");
        }

        if (getConfig().getBoolean("creeper-confetti.enabled", true)) {
            getServer().getPluginManager().registerEvents(new CreeperConfetti(this), this);
            getLogger().info("CreeperConfetti Enabled");
        } else {
            getLogger().info("CreeperConfetti Disabled");
        }

        getLogger().info("IMMC Tweaks enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("IMMC Tweaks disabled.");
    }
}
