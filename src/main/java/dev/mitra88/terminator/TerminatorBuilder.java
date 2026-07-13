package dev.mitra88.terminator;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;


public class TerminatorBuilder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static Component mm(String input) {
        return MM.deserialize(input).decoration(TextDecoration.ITALIC, false);
    }

    public static ItemStack giveTerminator() {
        ItemStack terminatorBow = new ItemStack(Material.BOW);
        ItemMeta meta = terminatorBow.getItemMeta();
        if (meta == null) return terminatorBow;

        meta.displayName(mm("<light_purple>Precise Terminator <gold>✪✪✪✪<red>➎"));

        List<Component> lore = List.of(
                mm("<gray>Gear Score: <light_purple>1042 <dark_gray>(4163)"),
                mm("<gray>Damage: <red>374 <yellow>(+30) <dark_gray>(+1,958.53)"),
                mm("<gray>Strength: <red>110 <yellow>(+30) <gold>[+5] <blue>(+20) <dark_gray>(+599.55)"),
                mm("<gray>Crit Chance: <red>+68% <blue>(+60%) <dark_gray>(+104.04%)"),
                mm("<gray>Crit Damage: <red>+280% <dark_gray>(+1,456.05%)"),
                mm("<gray>Bonus Attack Speed: <red>+44% <dark_gray>(+61.2%)"),
                mm("<gray>Shot Cooldown: <green>0.2s"),
                Component.empty(),
                mm("<light_purple><bold>Soul Eater V <blue>Chance V, Cubism VI"),
                mm("<blue>Dragon Hunter V, Dragon Tracer V, Flame II"),
                mm("<blue>Impaling III, Infinite Quiver X, Overload V"),
                mm("<blue>Piercing I, Power VII, Punch II"),
                mm("<blue>Snipe IV, Vicious V, Gravity VI, Tabasco III"),
                Component.empty(),
                mm("<gray>Shoots <aqua>3 <gray>arrows at once."),
                mm("<gray>Can damage enderman."),
                Component.empty(),
                mm("<red>Divides your <blue>☣ Crit Chance <red>by 4!"),
                Component.empty(),
                mm("<gold>Ability: Salvation <yellow><bold>LEFT CLICK"),
                mm("<gray>Can be casted after landing <gold><bold>3 <gray>hits."),
                mm("<gray>Shoot a beam, penetrating up to <yellow>5"),
                mm("<gray>enemies."),
                mm("<gray>The beam always crits."),
                mm("<dark_gray>Soulflow Cost: <dark_aqua>1⸎"),
                Component.empty(),
                mm("<light_purple>Shortbow: Instantly shoots!"),
                Component.empty(),
                mm("<light_purple><bold><obfuscated>A</obfuscated> MYTHIC DUNGEON BOW <obfuscated>A</obfuscated>")
        );
        meta.lore(lore);

        Enchantment unbreaking = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft("unbreaking"));
        if (unbreaking != null) {
            meta.addEnchant(unbreaking, 100, true);
        }

        meta.setUnbreakable(true);
        terminatorBow.setItemMeta(meta);

        //noinspection UnstableApiUsage ! HOPEFULLY THIS DOESNT BREAK
        terminatorBow.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                TooltipDisplay.tooltipDisplay()
                        .addHiddenComponents(
                                DataComponentTypes.UNBREAKABLE,
                                DataComponentTypes.ENCHANTMENTS,
                                DataComponentTypes.STORED_ENCHANTMENTS,
                                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                                DataComponentTypes.TRIM,
                                DataComponentTypes.DYED_COLOR
                        )
                        .build());

        meta = terminatorBow.getItemMeta();
        meta.getPersistentDataContainer().set(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE, (byte) 1);
        terminatorBow.setItemMeta(meta);

        return terminatorBow;
    }
}
