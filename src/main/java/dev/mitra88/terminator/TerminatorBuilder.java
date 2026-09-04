package dev.mitra88.terminator;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class TerminatorBuilder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static Component mm(String input) {
        return MM.deserialize(input).decoration(TextDecoration.ITALIC, false);
    }

    public static ItemStack build(TerminatorConfig config) {
        ItemStack bow = new ItemStack(config.material);
        ItemMeta meta = bow.getItemMeta();

        meta.displayName(mm(config.displayName));

        if (!config.lore.isEmpty()) {
            List<Component> lore = new ArrayList<>(config.lore.size());
            for (String line : config.lore) {
                lore.add(line.isEmpty() ? Component.empty() : mm(line));
            }
            meta.lore(lore);
        }

        meta.setUnbreakable(config.unbreakable);
        meta.getPersistentDataContainer().set(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE, (byte) 1);
        bow.setItemMeta(meta);
        bow.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        if (!config.hiddenTooltipComponents.isEmpty()) {
            bow.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                    TooltipDisplay.tooltipDisplay().addHiddenComponents(config.hiddenTooltipComponents.toArray(new DataComponentType[0])).build());
        }

        return bow;
    }
}
