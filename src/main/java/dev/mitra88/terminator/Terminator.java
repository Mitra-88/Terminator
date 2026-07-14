package dev.mitra88.terminator;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Terminator extends JavaPlugin {

    public static final NamespacedKey TERMINATOR_KEY = new NamespacedKey("terminator", "terminator");

    private TerminatorConfig config;
    private TerminatorEventListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = new TerminatorConfig(this);
        listener = new TerminatorEventListener(config);
        getServer().getPluginManager().registerEvents(listener, this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register("giveterminator",
                    "Gives the Terminator item to the player.",
                    new TerminatorCommand(config, false));
            commands.register("terminatorreload",
                    "Reloads the Terminator configuration.",
                    new TerminatorCommand(config, true));
        });
    }

    @Override
    public void onDisable() {
        if (listener != null) listener.cleanup();
    }
}
