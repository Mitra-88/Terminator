package dev.mitra88.terminator;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class Terminator extends JavaPlugin {

    public static final NamespacedKey TERMINATOR_KEY = new NamespacedKey("terminator", "terminator");
    public static final NamespacedKey ARROW_DAMAGE_KEY = new NamespacedKey("terminator", "arrow_damage");

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

    public static void damageIgnoringHurtCooldown(LivingEntity target, Entity attacker, double amount, String damageTypeKey) {
        if (amount <= 0.0) return;

        DamageType type = resolveDamageType(damageTypeKey);
        DamageSource source = DamageSource.builder(type).withCausingEntity(attacker).withDirectEntity(attacker).build();

        int previousMaxNoDamageTicks = target.getMaximumNoDamageTicks();
        try {
            target.setMaximumNoDamageTicks(0);
            target.setNoDamageTicks(0);
            target.damage(amount, source);
        } finally {
            target.setMaximumNoDamageTicks(previousMaxNoDamageTicks);
        }
    }

    private static DamageType resolveDamageType(String key) {
        Registry<DamageType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE);
        DamageType type = registry.get(NamespacedKey.minecraft(key));
        if (type == null) {
            type = registry.get(NamespacedKey.minecraft("generic"));
        }
        return Objects.requireNonNull(type, "Damage type '" + key + "' is not registered");
    }
}
