package dev.mitra88.terminator;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class Terminator extends JavaPlugin {

    public static NamespacedKey TERMINATOR_KEY;
    private TerminatorConfig config;
    private TerminatorEventListener listener;

    @Override
    public void onEnable() {
        TERMINATOR_KEY = new NamespacedKey(this, "terminator");
        saveDefaultConfig();

        config = new TerminatorConfig(this);
        listener = new TerminatorEventListener(this, config);

        getServer().getPluginManager().registerEvents(listener, this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            commands.register(
                    "giveterminator",
                    "Gives the Terminator item to the player.",
                    new TerminatorCommand(this, config, false)
            );

            commands.register(
                    "terminatorreload",
                    "Reloads the Terminator configuration.",
                    new TerminatorCommand(this, config, true)
            );
        });
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.cleanup();
        }

        getLogger().info("Terminator has been disabled.");
    }
}
