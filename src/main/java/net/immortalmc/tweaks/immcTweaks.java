package net.immortalmc.tweaks;

import net.immortalmc.tweaks.modules.NoAnvilLimit;
import net.immortalmc.tweaks.modules.BetterBonemeal;
import net.immortalmc.tweaks.modules.CreeperConfetti;
import net.immortalmc.tweaks.modules.NoCropTrample;
import net.immortalmc.tweaks.modules.ItemNameColour;
import net.immortalmc.tweaks.modules.SignTextColour;
import net.immortalmc.tweaks.utils.SendChatMsg;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.List;

/**
 * IMMC Tweaks: a modular collection of gameplay tweaks for the IMMC: Survival server.
 *
 * Each tweak lives in its own class in the modules package and has its own section in config.yml with an "enabled" switch. To add a new tweak:
 *   1. Create the module class in the modules package
 *   2. Add a section with "enabled: true" to config.yml
 *   3. Add one registerModule(...) line in onEnable below
 * 
 * Module classes use upper camel case, such as NoCropTrample, whereas config keys use kebab case such as no-crop-trample.
 */
public class immcTweaks extends JavaPlugin {

    // Commands from enabled modules, collected as modules register so they can be logged together.
    private final List<String> activeCommands = new ArrayList<>();

    @Override
    public void onEnable() {
        // Copies config.yml out of the jar on first run (does nothing if it already exists)
        saveDefaultConfig();

        registerModule("no-crop-trample", "NoCropTrample", NoCropTrample::new);
        registerModule("sign-text-colour", "SignTextColour", SignTextColour::new);
        registerModule("no-anvil-limit", "NoAnvilLimit", NoAnvilLimit::new);
        registerModule("creeper-confetti", "CreeperConfetti", () -> new CreeperConfetti(this));
        registerModule("better-bonemeal", "BetterBonemeal", () -> new BetterBonemeal(this));
        registerModule("item-name-colour", "ItemNameColour", () -> new ItemNameColour(this), "itemnamecolour");

        logActiveCommands();
        getLogger().info("IMMC Tweaks enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("IMMC Tweaks disabled.");
    }

    /**
     * Registers a module's event listener if it is enabled in config.yml.
     *
     * @param configKey     the module's section in config.yml (its "enabled" setting is read)
     * @param displayName   the name shown in log messages, typically the same as the module class name
     * @param factory       creates the module. This is only called if the module is enabled
     * @param commands      names of the commands (from plugin.yml) that belong to this module
     */
    private void registerModule(String configKey, String displayName,
                                Supplier<? extends Listener> factory, String... commands) {
        if (!getConfig().getBoolean(configKey + ".enabled", true)) {
            // Even if a module is disabled, the commands stay listed in plugin.yml and are ussable in game
            // Rrather than throwing an error, have the command explain the module is disabled
            for (String name : commands) {
                PluginCommand command = getCommand(name);
                if (command == null) continue;

                command.setExecutor((sender, cmd, label, args) -> {
                    SendChatMsg.send(sender, "This command belongs to the " + displayName
                        + " module, which is currently disabled.");
                    return true;
                });
            }

            getLogger().info(displayName + ": Disabled");
            return;
        }

        Listener module = factory.get();
        getServer().getPluginManager().registerEvents(module, this);

        // Hook the module up to its commands, if it has any
        if (commands.length > 0) {
            if (module instanceof CommandExecutor executor) {
                for (String name : commands) {
                    PluginCommand command = getCommand(name);
                    if (command != null) {
                        command.setExecutor(executor);
                        activeCommands.add(describeCommand(command));
                    }
                }
            } else {
                getLogger().warning(displayName + ": has commands listed but does not implement CommandExecutor");
            }
        }

        getLogger().info(displayName + ": Enabled");
    }

    // Logs the commands that are active, e.g. "Commands: /itemnamecolour (/inc)". 
    private void logActiveCommands() {
        if (activeCommands.isEmpty()) {
            getLogger().info("Active Commands: none");
        } else {
            getLogger().info("Active Commands: " + String.join(", ", activeCommands));
        }
    }

    // A command's name with its aliases, e.g. "/itemnamecolour (/inc)".
    private String describeCommand(PluginCommand command) {
        String text = "/" + command.getName();

        List<String> aliases = new ArrayList<>();
        for (String alias : command.getAliases()) {
            aliases.add("/" + alias);
        }

        if (!aliases.isEmpty()) {
            text += " (" + String.join(", ", aliases) + ")";
        }
        return text;
    }
}