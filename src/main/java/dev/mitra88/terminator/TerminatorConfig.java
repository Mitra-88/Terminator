package dev.mitra88.terminator;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.block.Action;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class TerminatorConfig {

    public float sideSpreadDegrees;
    public long holdWindowMs;
    public long shootCooldownMs;
    public double arrowVelocity;
    public double arrowDamageMin;
    public double arrowDamageMax;
    public Set<Action> clickActions;
    public Sound shootSound;
    public float shootSoundVolume;
    public float shootSoundPitch;

    public Material material;
    public String displayName;
    public List<String> lore;
    public boolean unbreakable;
    public Map<NamespacedKey, Integer> enchantments;
    public Set<DataComponentType> hiddenTooltipComponents;

    public int   salvationHitsRequired;
    public double beamMaxDistance;
    public int    beamMaxPierce;
    public double beamDamage;
    public long   beamCooldownMs;
    public double beamParticlesPerMeter;
    public double beamRaySize;

    public TerminatorConfig(JavaPlugin plugin) {
        reload(plugin);
    }

    public void reload(JavaPlugin plugin) {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        this.sideSpreadDegrees = (float) cfg.getDouble("shooting.side-spread-degrees", 10.0);
        this.holdWindowMs = cfg.getLong("shooting.hold-window-ms", 250L);
        this.shootCooldownMs = cfg.getLong("shooting.shoot-cooldown-ms", 200L);
        this.arrowVelocity = cfg.getDouble("shooting.arrow-velocity", 4.0);
        this.arrowDamageMin = cfg.getDouble("shooting.arrow-damage-min", 20000.0);
        this.arrowDamageMax = cfg.getDouble("shooting.arrow-damage-max", 50000.0);
        this.clickActions = loadClickActions(cfg.getStringList("shooting.click-actions"));

        this.shootSound = loadSound(cfg.getString("shooting.shoot-sound", "ENTITY_ARROW_SHOOT"));
        this.shootSoundVolume = (float) cfg.getDouble("shooting.shoot-sound-volume", 1.0);
        this.shootSoundPitch = (float) cfg.getDouble("shooting.shoot-sound-pitch", 1.0);

        Material mat = Material.matchMaterial(cfg.getString("item.material", "BOW"));
        this.material = (mat != null) ? mat : Material.BOW;
        this.displayName = cfg.getString(
                "item.display-name",
                "<light_purple>Precise Terminator <gold>✪✪✪✪<red>➎"
        );
        this.lore = Collections.unmodifiableList(cfg.getStringList("item.lore"));
        this.unbreakable = cfg.getBoolean("item.unbreakable", true);
        this.enchantments = loadEnchantments(cfg.getConfigurationSection("item.enchantments"));
        this.hiddenTooltipComponents = loadHiddenComponents(
                cfg.getStringList("item.tooltip-hidden-components")
        );

        this.salvationHitsRequired   = cfg.getInt("salvation.hits-required", 3);
        // Bukkit ray tracing throws IllegalArgumentException on negative inputs
        this.beamMaxDistance         = Math.max(0.0, cfg.getDouble("salvation.beam-distance", 32.0));
        this.beamMaxPierce           = Math.max(0, cfg.getInt("salvation.beam-max-pierce", 5));
        this.beamDamage              = cfg.getDouble("salvation.beam-damage", 50000.0);
        this.beamCooldownMs          = cfg.getLong("salvation.beam-cooldown-ms", 100L);
        this.beamParticlesPerMeter   = Math.max(0.0, cfg.getDouble("salvation.beam-particles-per-meter", 2.0));
        this.beamRaySize             = Math.max(0.0, cfg.getDouble("salvation.beam-ray-size", 0.5));
    }

    private static Set<Action> loadClickActions(List<String> raw) {
        Set<Action> actions = EnumSet.noneOf(Action.class);
        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            try {
                actions.add(Action.valueOf(s.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (actions.isEmpty()) {
            actions.addAll(EnumSet.allOf(Action.class));
        }
        return Collections.unmodifiableSet(actions);
    }

    private static Sound loadSound(String name) {
        if (name == null || name.isBlank()) {
            return Sound.ENTITY_ARROW_SHOOT;
        }
        try {
            NamespacedKey key = NamespacedKey.fromString(name.trim().toLowerCase(Locale.ROOT));
            if (key == null) return Sound.ENTITY_ARROW_SHOOT;
            Sound sound = Registry.SOUNDS.get(key);
            return sound != null ? sound : Sound.ENTITY_ARROW_SHOOT;
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_ARROW_SHOOT;
        }
    }

    private static Map<NamespacedKey, Integer> loadEnchantments(ConfigurationSection section) {
        Map<NamespacedKey, Integer> ench = new LinkedHashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int level = section.getInt(key);
                if (level < 1) continue; // Level 0 or negative removes enchantments in modern Minecraft
                try {
                    NamespacedKey nk = NamespacedKey.fromString(key.trim().toLowerCase(Locale.ROOT));
                    if (nk != null) {
                        ench.put(nk, level);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (ench.isEmpty()) {
            ench.put(NamespacedKey.minecraft("unbreaking"), 100);
        }
        return Collections.unmodifiableMap(ench);
    }

    private static Set<DataComponentType> loadHiddenComponents(List<String> raw) {
        Set<DataComponentType> components = new HashSet<>();
        Registry<DataComponentType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DATA_COMPONENT_TYPE);

        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            try {
                NamespacedKey key = NamespacedKey.fromString(s.trim().toLowerCase(Locale.ROOT));
                if (key == null) continue;
                DataComponentType type = registry.get(key);
                if (type != null) {
                    components.add(type);
                }
            } catch (IllegalArgumentException ignored) {}
        }

        if (components.isEmpty()) {
            components.add(DataComponentTypes.UNBREAKABLE);
            components.add(DataComponentTypes.ENCHANTMENTS);
            components.add(DataComponentTypes.STORED_ENCHANTMENTS);
            components.add(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            components.add(DataComponentTypes.TRIM);
            components.add(DataComponentTypes.DYED_COLOR);
        }
        return Collections.unmodifiableSet(components);
    }
}
