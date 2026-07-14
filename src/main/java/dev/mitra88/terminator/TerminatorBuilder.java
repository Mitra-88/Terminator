package dev.mitra88.terminator;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TerminatorBuilder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static Component mm(String input) {
        return MM.deserialize(input).decoration(TextDecoration.ITALIC, false);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static ItemStack giveTerminator(TerminatorConfig config) {
        ItemStack terminatorBow = new ItemStack(config.material);
        ItemMeta meta = terminatorBow.getItemMeta();
        if (meta == null) return terminatorBow;

        meta.displayName(mm(config.displayName));

        List<Component> lore = new ArrayList<>(config.lore.size());
        for (String line : config.lore) {
            lore.add(line.isEmpty() ? Component.empty() : mm(line));
        }
        meta.lore(lore);

        for (Map.Entry<NamespacedKey, Integer> entry : config.enchantments.entrySet()) {
            Enchantment ench = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .get(entry.getKey());
            if (ench != null) {
                meta.addEnchant(ench, entry.getValue(), true);
            }
        }

        meta.setUnbreakable(config.unbreakable);
        terminatorBow.setItemMeta(meta);

        // HOPEFULLY THIS DOESN'T BREAK! lmao
        if (!config.hiddenTooltipComponents.isEmpty()) {
            DataComponentType[] hidden =
                    config.hiddenTooltipComponents.toArray(new DataComponentType[0]);
            terminatorBow.setData(
                    DataComponentTypes.TOOLTIP_DISPLAY,
                    TooltipDisplay.tooltipDisplay()
                            .addHiddenComponents(hidden)
                            .build()
            );
        }

        meta = terminatorBow.getItemMeta();
        meta.getPersistentDataContainer().set(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE, (byte) 1);
        terminatorBow.setItemMeta(meta);

        return terminatorBow;
    }
}
