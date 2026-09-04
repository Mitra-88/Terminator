package dev.mitra88.terminator;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
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
import java.util.logging.Logger;

public final class TerminatorConfig {

    private static final Map<Enchantment, Integer> HARDCODED_ENCHANTMENTS = buildEnchantments();

    private static Map<Enchantment, Integer> buildEnchantments() {
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        enchantments.put(Enchantment.UNBREAKING, 100);
        return Collections.unmodifiableMap(enchantments);
    }

    private static final List<String> HIDDEN_COMPONENT_KEYS = List.of(
            "minecraft:enchantments",
            "minecraft:jukebox_playable",
            "minecraft:painting/variant",
            "minecraft:map_id",
            "minecraft:fireworks",
            "minecraft:attribute_modifiers",
            "minecraft:unbreakable",
            "minecraft:written_book_content",
            "minecraft:banner_patterns",
            "minecraft:trim",
            "minecraft:potion_contents",
            "minecraft:dyed_color",
            "minecraft:charged_projectiles"
    );

    private static final Set<Action> SUPPORTED_ACTIONS = Collections.unmodifiableSet(EnumSet.of(
            Action.LEFT_CLICK_AIR,
            Action.LEFT_CLICK_BLOCK,
            Action.RIGHT_CLICK_AIR,
            Action.RIGHT_CLICK_BLOCK
    ));

    public float sideSpreadDegrees;
    public long holdWindowMs;
    public long shootCooldownMs;
    public double arrowVelocity;
    public double arrowDamageMin;
    public double arrowDamageMax;
    public boolean criticalArrows;
    public Set<Action> clickActions;
    public Sound shootSound;
    public float shootSoundVolume;
    public float shootSoundPitch;

    public Material material;
    public String displayName;
    public List<String> lore;
    public boolean unbreakable;
    public Map<Enchantment, Integer> enchantments;
    public Set<DataComponentType> hiddenTooltipComponents;

    public int salvationHitsRequired;
    public double beamMaxDistance;
    public int beamMaxPierce;
    public double beamDamage;
    public long beamCooldownMs;
    public double beamParticlesPerMeter;
    public double beamRaySize;

    private final JavaPlugin plugin;

    public TerminatorConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        ConfigReader cfg = new ConfigReader(plugin.getConfig(), plugin.getLogger());
        readShooting(cfg);
        readItem(cfg);
        readSalvation(cfg);
    }

    private void readShooting(ConfigReader cfg) {
        sideSpreadDegrees = (float) cfg.decimal("shooting.side-spread-degrees", 10.0);
        holdWindowMs = cfg.ms("shooting.hold-window-ms", 200);
        shootCooldownMs = cfg.ms("shooting.shoot-cooldown-ms", 200);
        arrowVelocity = cfg.decimal("shooting.arrow-velocity", 4.0);
        arrowDamageMin = cfg.decimal("shooting.arrow-damage-min", 20000.0);
        arrowDamageMax = cfg.decimal("shooting.arrow-damage-max", 50000.0);
        criticalArrows = cfg.bool("shooting.critical-arrows");
        clickActions = loadClickActions(cfg.list("shooting.click-actions"));
        shootSound = loadSound(cfg.string("shooting.shoot-sound", "ENTITY_ARROW_SHOOT"));
        shootSoundVolume = (float) cfg.decimal("shooting.shoot-sound-volume", 1.0);
        shootSoundPitch = (float) cfg.decimal("shooting.shoot-sound-pitch", 1.0);
    }

    private void readItem(ConfigReader cfg) {
        material = loadMaterial(cfg.string("item.material", "BOW"));
        displayName = cfg.string("item.display-name", "<light_purple>Precise Terminator <gold>✪✪✪✪<red>➎");
        lore = Collections.unmodifiableList(cfg.list("item.lore"));
        unbreakable = cfg.bool("item.unbreakable");
        enchantments = HARDCODED_ENCHANTMENTS;
        hiddenTooltipComponents = resolveHiddenComponents();
    }

    private void readSalvation(ConfigReader cfg) {
        salvationHitsRequired = cfg.whole("salvation.hits-required", 3, 1);
        beamMaxDistance = cfg.decimal("salvation.beam-distance", 32.0);
        beamMaxPierce = cfg.whole("salvation.beam-max-pierce", 5, 0);
        beamDamage = cfg.decimal("salvation.beam-damage", 50000.0);
        beamCooldownMs = cfg.ms("salvation.beam-cooldown-ms", 100);
        beamParticlesPerMeter = cfg.decimal("salvation.beam-particles-per-meter", 2.0);
        beamRaySize = cfg.decimal("salvation.beam-ray-size", 0.5);
    }

    private Set<DataComponentType> resolveHiddenComponents() {
        Registry<DataComponentType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DATA_COMPONENT_TYPE);
        Set<DataComponentType> components = new HashSet<>(HIDDEN_COMPONENT_KEYS.size());

        for (String name : HIDDEN_COMPONENT_KEYS) {
            NamespacedKey key = NamespacedKey.fromString(name);
            DataComponentType type = key != null ? registry.get(key) : null;
            if (type != null) {
                components.add(type);
            } else {
                plugin.getLogger().fine("[Terminator] data component '" + name + "' does not exist on this server version - skipped.");
            }
        }
        return Collections.unmodifiableSet(components);
    }

    private Material loadMaterial(String raw) {
        Material material = Material.matchMaterial(raw);
        if (material == null || material.isAir() || !material.isItem()) {
            warn("item.material", "'" + raw + "' is not a usable item material - using BOW.");
            return Material.BOW;
        }
        return material;
    }

    private Sound loadSound(String raw) {
        Sound sound = SoundRegistryMapper.get(raw, null);
        if (sound != null) return sound;

        warn("shooting.shoot-sound", "'" + raw + "' is not a registered sound - using ENTITY_ARROW_SHOOT.");
        return Sound.ENTITY_ARROW_SHOOT;
    }

    private Set<Action> loadClickActions(List<String> raw) {
        Set<Action> actions = EnumSet.noneOf(Action.class);
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) continue;

            Action action = parseAction(entry);
            if (action == null || !SUPPORTED_ACTIONS.contains(action)) {
                warn("shooting.click-actions", "'" + entry + "' is not a supported click action "
                        + "(LEFT_CLICK_AIR, LEFT_CLICK_BLOCK, RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK) - skipped.");
                continue;
            }
            actions.add(action);
        }
        return actions.isEmpty() ? SUPPORTED_ACTIONS : Collections.unmodifiableSet(actions);
    }

    private static Action parseAction(String input) {
        try {
            return Action.valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    private void warn(String path, String detail) {
        plugin.getLogger().warning(path + ": " + detail);
    }

    private record ConfigReader(FileConfiguration cfg, Logger log) {

        double decimal(String path, double def) {
            if (!cfg.isSet(path)) return def;
            if (!cfg.isInt(path) && !cfg.isDouble(path) && !cfg.isLong(path)) {
                warn(path, "expected a number, found '" + cfg.get(path) + "' - using " + def + ".");
                return def;
            }
            return clamp(cfg.getDouble(path, def), 0.0, path);
        }

        int whole(String path, int def, int min) {
            if (!cfg.isSet(path)) return def;
            if (!cfg.isInt(path)) {
                warn(path, "expected a whole number, found '" + cfg.get(path) + "' - using " + def + ".");
                return def;
            }
            return (int) clamp(cfg.getInt(path, def), min, path);
        }

        long ms(String path, long def) {
            if (!cfg.isSet(path)) return def;
            if (!cfg.isInt(path) && !cfg.isLong(path)) {
                warn(path, "expected a whole number, found '" + cfg.get(path) + "' - using " + def + ".");
                return def;
            }
            return (long) clamp(cfg.getLong(path, def), 0L, path);
        }

        boolean bool(String path) {
            if (!cfg.isSet(path)) return true;
            if (!cfg.isBoolean(path)) {
                warn(path, "expected true or false, found '" + cfg.get(path) + "' - using " + true + ".");
                return true;
            }
            return cfg.getBoolean(path, true);
        }

        String string(String path, String def) {
            return cfg.getString(path, def);
        }

        List<String> list(String path) {
            return cfg.getStringList(path);
        }

        private double clamp(double value, double min, String path) {
            if (value >= min) return value;
            warn(path, value + " is below the minimum " + min + " - raised to " + min + ".");
            return min;
        }

        private void warn(String path, String detail) {
            log.warning(path + ": " + detail);
        }
    }
}
