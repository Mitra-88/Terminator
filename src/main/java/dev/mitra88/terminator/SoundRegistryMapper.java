package dev.mitra88.terminator;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SoundRegistryMapper {

    private static final Map<String, Sound> SOUNDS = new HashMap<>();
    private static volatile boolean loaded = false;

    private SoundRegistryMapper() {}

    public static Sound get(String input, Sound fallback) {
        if (input == null || input.isBlank()) return fallback;

        String key = canonical(input);
        if (key.isEmpty()) return fallback;

        if (!loaded) load();

        return SOUNDS.getOrDefault(key, fallback);
    }

    private static synchronized void load() {
        if (loaded) return;

        for (Sound sound : Registry.SOUND_EVENT) {
            NamespacedKey key = Registry.SOUND_EVENT.getKey(sound);
            if (key == null) continue;

            put(key.getKey(), sound);
            put(key.toString(), sound);
        }
        loaded = true;
    }

    private static void put(String name, Sound sound) {
        SOUNDS.putIfAbsent(canonical(name), sound);
    }

    private static String canonical(String input) {
        String s = input.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(s.length());

        boolean separator = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.' || c == '_' || c == ':' || c == ' ') {
                if (!out.isEmpty()) separator = true;
            } else if (isKeyChar(c)) {
                if (separator) { out.append('_'); separator = false; }
                out.append(c);
            } else {
                return "";
            }
        }
        return out.toString();
    }

    private static boolean isKeyChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }
}
