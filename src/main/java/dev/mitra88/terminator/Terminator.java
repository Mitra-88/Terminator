package dev.mitra88.terminator;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class Terminator extends JavaPlugin {

    public static final NamespacedKey TERMINATOR_KEY = new NamespacedKey("terminator", "terminator");

    private TerminatorConfig config;
    private TerminatorEventListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = new TerminatorConfig(this);

        SalvationBeamAbility salvationBeam = new SalvationBeamAbility(config);
        listener = new TerminatorEventListener(config, salvationBeam);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(new SoulEaterAbility(this), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register("giveterminator", "Gives the Terminator item to the player.", new TerminatorCommand(config, listener, false));
            commands.register("terminatorreload", "Reloads the Terminator configuration.", new TerminatorCommand(config, listener, true));
        });
    }

    @Override
    public void onDisable() {
        if (listener != null) listener.cleanup();
    }

    @Nullable
    public static ItemMeta terminatorMeta(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(TERMINATOR_KEY) ? meta : null;
    }
}
