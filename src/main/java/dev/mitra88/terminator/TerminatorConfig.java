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
        Logger log = plugin.getLogger();
        readShooting(cfg, log);
        readItem(cfg, log);
        readSalvation(cfg);
    }

    private void readShooting(ConfigReader cfg, Logger log) {
        sideSpreadDegrees = (float) cfg.decimal("shooting.side-spread-degrees", 10.0);
        holdWindowMs      = cfg.ms("shooting.hold-window-ms", 200);
        shootCooldownMs   = cfg.ms("shooting.shoot-cooldown-ms", 200);
        arrowVelocity     = cfg.decimal("shooting.arrow-velocity", 4.0);
        arrowDamageMin    = cfg.decimal("shooting.arrow-damage-min", 20000.0);
        arrowDamageMax    = cfg.decimal("shooting.arrow-damage-max", 50000.0);
        clickActions      = loadClickActions(cfg.list("shooting.click-actions"), log);

        shootSound        = loadSound(cfg.string("shooting.shoot-sound", "ENTITY_ARROW_SHOOT"), log);
        shootSoundVolume  = (float) cfg.decimal("shooting.shoot-sound-volume", 1.0);
        shootSoundPitch   = (float) cfg.decimal("shooting.shoot-sound-pitch", 1.0);
    }

    private void readItem(ConfigReader cfg, Logger log) {
        material                = loadMaterial(cfg.string("item.material", "BOW"), log);
        displayName             = cfg.string("item.display-name", "<light_purple>Precise Terminator <gold>✪✪✪✪<red>➎");
        lore                    = Collections.unmodifiableList(cfg.list("item.lore"));
        unbreakable             = cfg.bool();

        enchantments            = HARDCODED_ENCHANTMENTS;
        hiddenTooltipComponents = resolveHiddenComponents(log);
    }

    private void readSalvation(ConfigReader cfg) {
        salvationHitsRequired = cfg.whole("salvation.hits-required", 3, 1);
        beamMaxDistance       = cfg.decimal("salvation.beam-distance", 32.0);
        beamMaxPierce         = cfg.whole("salvation.beam-max-pierce", 5, 0);
        beamDamage            = cfg.decimal("salvation.beam-damage", 50000.0);
        beamCooldownMs        = cfg.ms("salvation.beam-cooldown-ms", 100);
        beamParticlesPerMeter = cfg.decimal("salvation.beam-particles-per-meter", 2.0);
        beamRaySize           = cfg.decimal("salvation.beam-ray-size", 0.5);
    }

    private static Set<DataComponentType> resolveHiddenComponents(Logger log) {
        Registry<DataComponentType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DATA_COMPONENT_TYPE);
        Set<DataComponentType> components = new HashSet<>(HIDDEN_COMPONENT_KEYS.size());

        for (String name : HIDDEN_COMPONENT_KEYS) {
            NamespacedKey key = NamespacedKey.fromString(name);
            DataComponentType type = key != null ? registry.get(key) : null;
            if (type != null) {
                components.add(type);
            } else {
                log.fine("[Terminator] data component '" + name + "' does not exist on this server version - skipped.");
            }
        }
        return Collections.unmodifiableSet(components);
    }

    private static Material loadMaterial(String raw, Logger log) {
        Material material = Material.matchMaterial(raw);
        if (material == null) {
            warn(log, "item.material", "'" + raw + "' is not a material - using BOW.");
            return Material.BOW;
        }
        return material;
    }

    private static Sound loadSound(String raw, Logger log) {
        Sound sound = SoundRegistryMapper.get(raw, null);
        if (sound == null) {
            NamespacedKey fallbackKey = Registry.SOUND_EVENT.getKey(Sound.ENTITY_ARROW_SHOOT);
            warn(log, "shooting.shoot-sound", "'" + raw + "' is not a registered sound - using "
                    + (fallbackKey != null ? fallbackKey : "the default sound") + " instead.");
            return Sound.ENTITY_ARROW_SHOOT;
        }
        return sound;
    }

    private static Set<Action> loadClickActions(List<String> raw, Logger log) {
        Set<Action> actions = EnumSet.noneOf(Action.class);
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) continue;
            Action action = parseEnum(entry);
            if (action != null) {
                actions.add(action);
            } else {
                warn(log, "shooting.click-actions", "'" + entry + "' is not a click action "
                        + "(LEFT_CLICK_AIR, LEFT_CLICK_BLOCK, RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK) - skipped.");
            }
        }
        if (actions.isEmpty()) {
            actions = EnumSet.of(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK, Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK);
        }
        return Collections.unmodifiableSet(actions);
    }

    private static void warn(Logger log, String path, String detail) {
        log.warning(path + ": " + detail);
    }

    private static Action parseEnum(String input) {
        try {
            return Action.valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    private record ConfigReader(FileConfiguration cfg, Logger log) {

        double decimal(String path, double def) {
            if (!cfg.isSet(path)) return def;
            if (!cfg.isInt(path) && !cfg.isDouble(path) && !cfg.isLong(path)) {
                warn(log, path, "expected a number, found '" + cfg.get(path) + "' - using " + def + ".");
                return def;
            }
            return clamp(cfg.getDouble(path, def), 0.0, path);
        }

        int whole(String path, int def, int min) {
            if (!cfg.isSet(path)) return def;
            if (!cfg.isInt(path)) {
                warn(log, path, "particles-per-meter must be a whole number, found '" + cfg.get(path) + "' - using " + def + ".");
                return def;
            }
            return (int) clamp(cfg.getInt(path, def), min, path);
        }

        long ms(String path, long def) {
            if (!cfg.isSet(path)) return def;
            if (!cfg.isInt(path) && !cfg.isLong(path)) {
                warn(log, path, "expected a whole number, found '" + cfg.get(path) + "' - using " + def + ".");
                return def;
            }
            return (long) clamp(cfg.getLong(path, def), (long) 0, path);
        }

        private double clamp(double value, double min, String path) {
            if (value >= min) return value;
            warn(log, path, value + " is below the minimum " + min + " - raised to " + min + ".");
            return min;
        }

        String string(String path, String def) {
            return cfg.getString(path, def);
        }

        List<String> list(String path) {
            return cfg.getStringList(path);
        }

        boolean bool() {
            return cfg.getBoolean("item.unbreakable", true);
        }
    }
}
