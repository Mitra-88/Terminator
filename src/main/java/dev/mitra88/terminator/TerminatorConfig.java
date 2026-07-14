package dev.mitra88.terminator;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.block.Action;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Arrays;
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

        this.shootSound = loadSound(
                cfg.getString("shooting.shoot-sound", "ENTITY_ARROW_SHOOT")
        );
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
        this.beamMaxDistance         = cfg.getDouble("salvation.beam-distance", 32.0);
        this.beamMaxPierce           = cfg.getInt("salvation.beam-max-pierce", 5);
        this.beamDamage              = cfg.getDouble("salvation.beam-damage", 50000.0);
        this.beamCooldownMs          = cfg.getLong("salvation.beam-cooldown-ms", 100L);
        this.beamParticlesPerMeter   = cfg.getDouble("salvation.beam-particles-per-meter", 2.0);
        this.beamRaySize             = cfg.getDouble("salvation.beam-ray-size", 0.5);
    }

    private static Set<Action> loadClickActions(List<String> raw) {
        Set<Action> actions = EnumSet.noneOf(Action.class);
        for (String s : raw) {
            if (s == null) continue;
            try {
                actions.add(Action.valueOf(s.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (actions.isEmpty()) {
            actions.add(Action.RIGHT_CLICK_AIR);
            actions.add(Action.RIGHT_CLICK_BLOCK);
            actions.add(Action.LEFT_CLICK_AIR);
            actions.add(Action.LEFT_CLICK_BLOCK);
        }
        return Collections.unmodifiableSet(actions);
    }

    private static Sound loadSound(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Sound.ENTITY_ARROW_SHOOT;
        }

        String normalized = name.trim().toLowerCase(Locale.ROOT);

        NamespacedKey key;
        if (normalized.contains(":")) {
            String[] parts = normalized.split(":", 2);
            key = new NamespacedKey(parts[0], parts[1]);
        } else {
            key = NamespacedKey.minecraft(normalized);
        }

        Sound sound = Registry.SOUNDS.get(key);
        return sound != null ? sound : Sound.ENTITY_ARROW_SHOOT;
    }

    private static Map<NamespacedKey, Integer> loadEnchantments(ConfigurationSection section) {
        Map<NamespacedKey, Integer> ench = new LinkedHashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String normalized = key.trim().toLowerCase(Locale.ROOT);
                NamespacedKey nk;
                if (normalized.contains(":")) {
                    String[] parts = normalized.split(":", 2);
                    nk = new NamespacedKey(parts[0], parts[1]);
                } else {
                    nk = NamespacedKey.minecraft(normalized);
                }
                ench.put(nk, section.getInt(key));
            }
        }
        if (ench.isEmpty()) {
            ench.put(NamespacedKey.minecraft("unbreaking"), 100);
        }
        return Collections.unmodifiableMap(ench);
    }

    private static Set<DataComponentType> loadHiddenComponents(List<String> raw) {
        Set<DataComponentType> components = new HashSet<>();
        for (String s : raw) {
            if (s == null) continue;
            String name = s.trim().toUpperCase(Locale.ROOT);
            try {
                Field f = DataComponentTypes.class.getField(name);
                Object value = f.get(null);
                if (value instanceof DataComponentType) {
                    components.add((DataComponentType) value);
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        if (components.isEmpty()) {
            components.addAll(Arrays.asList(
                    DataComponentTypes.UNBREAKABLE,
                    DataComponentTypes.ENCHANTMENTS,
                    DataComponentTypes.STORED_ENCHANTMENTS,
                    DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    DataComponentTypes.TRIM,
                    DataComponentTypes.DYED_COLOR
            ));
        }
        return Collections.unmodifiableSet(components);
    }
}
