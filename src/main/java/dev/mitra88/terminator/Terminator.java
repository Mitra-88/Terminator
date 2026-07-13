package dev.mitra88.terminator;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class Terminator extends JavaPlugin {

    public static NamespacedKey TERMINATOR_KEY;

    @Override
    public void onEnable() {
        TERMINATOR_KEY = new NamespacedKey(this, "terminator");
        getServer().getPluginManager().registerEvents(new TerminatorEventListener(this), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    "giveterminator",
                    "Gives the Terminator item to the player.",
                    new TerminatorCommand()
            );
        });
    }
}