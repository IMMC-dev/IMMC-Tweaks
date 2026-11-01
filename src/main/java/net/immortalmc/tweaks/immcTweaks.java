package net.immortalmc.tweaks;

import net.immortalmc.tweaks.modules.NoAnvilLimit;
import net.immortalmc.tweaks.modules.BetterBonemeal;
import net.immortalmc.tweaks.modules.CreeperConfetti;
import net.immortalmc.tweaks.modules.NoCropTrample;
import net.immortalmc.tweaks.modules.ItemNameColour;
import net.immortalmc.tweaks.modules.SignTextColour;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

/**
 * IMMC Tweaks: a modular collection of gameplay tweaks for the IMMC: Survival server.
 *
 * Each tweak lives in its own class in the modules package and has its own section in config.yml with an "enabled" switch. To add a new tweak:
 *   1. Create the module class in the modules package
 *   2. Add a section with "enabled: true" to config.yml
 *   3. Add one registerModule(...) line in onEnable below
 * 
 * Module classes use upper camel case, such as NoCropTrample, whereas config keys use lower kebab case such as no-crop-trample.
 */
public class immcTweaks extends JavaPlugin {

    @Override
    public void onEnable() {
        // Copies config.yml out of the jar on first run (does nothing if it already exists)
        saveDefaultConfig();

        registerModule("no-anvil-limit", "NoAnvilLimit", NoAnvilLimit::new);
        registerModule("better-bonemeal", "BetterBonemeal", () -> new BetterBonemeal(this));
        registerModule("creeper-confetti", "CreeperConfetti", () -> new CreeperConfetti(this));
        registerModule("no-crop-trample", "NoCropTrample", NoCropTrample::new);
        registerModule("sign-text-colour", "SignTextColour", SignTextColour::new);

        // This module also owns a command, so needs a more involved registration
        registerModule("item-name-colour", "ItemNameColour", () -> {
            ItemNameColour module = new ItemNameColour(this);
            getCommand("itemnamecolour").setExecutor(module);
            return module;
        });

        getLogger().info("IMMC Tweaks enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("IMMC Tweaks disabled.");
    }

    /**
     * Registers a module's event listener if it is enabled in config.yml.
     *
     * @param configKey   the module's section in config.yml (its "enabled" setting is read)
     * @param displayName the name shown in log messages, typically the same as the module class name
     * @param factory     creates the module. This is only called if the module is enabled
     */
    private void registerModule(String configKey, String displayName, Supplier<Listener> factory) {
        if (!getConfig().getBoolean(configKey + ".enabled", true)) {
            getLogger().info(displayName + ": Disabled");
            return;
        }

        getServer().getPluginManager().registerEvents(factory.get(), this);
        getLogger().info(displayName + ": Enabled");
    }
}