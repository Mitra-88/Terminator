package dev.mitra88.terminator;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TerminatorBuilder {

    public static ItemStack giveTerminator() {
        ItemStack terminatorBow = new ItemStack(Material.BOW);
        ItemMeta meta = terminatorBow.getItemMeta();

        Objects.requireNonNull(meta).setDisplayName(ColorUtils.color("&dPrecise Terminator &6✪✪✪✪&c➎"));

        List<String> lore = Arrays.asList(
                ColorUtils.color("&7Gear Score: &d1042 &8(4163)"),
                ColorUtils.color("&7Damage: &c374 &e(+30) &8(+1,958.53)"),
                ColorUtils.color("&7Strength: &c110 &e(+30) &6[+5] &9(+20) &8(+599.55)"),
                ColorUtils.color("&7Crit Chance: &c+68% &9(+60%) &8(+104.04%)"),
                ColorUtils.color("&7Crit Damage: &c+280% &8(+1,456.05%)"),
                ColorUtils.color("&7Bonus Attack Speed: &c+44% &8(+61.2%)"),
                ColorUtils.color("&7Shot Cooldown: &a0.2s"),
                "",
                ColorUtils.color("&d&lSoul Eater V &9Chance V, Cubism VI"),
                ColorUtils.color("&9Dragon Hunter V, Dragon Tracer V, Flame II"),
                ColorUtils.color("&9Impaling III, Infinite Quiver X, Overload V"),
                ColorUtils.color("&9Piercing I, Power VII, Punch II"),
                ColorUtils.color("&9Snipe IV, Vicious V, Gravity VI, Tabasco III"),
                "",
                ColorUtils.color("&7Shoots &b3 &7arrows at once."),
                ColorUtils.color("&7Can damage enderman."),
                "",
                ColorUtils.color("&cDivides your &9☣ Crit Chance &cby 4!"),
                "",
                ColorUtils.color("&6Ability: Salvation &e&lLEFT CLICK"),
                ColorUtils.color("&7Can be casted after landing &6&l3 &7hits."),
                ColorUtils.color("&7Shoot a beam, penetrating up to &e5"),
                ColorUtils.color("&7enemies."),
                ColorUtils.color("&7The beam always crits."),
                ColorUtils.color("&8Soulflow Cost: &3&l1⸎"),
                "",
                ColorUtils.color("&dShortbow: Instantly shoots!"),
                "",
                ColorUtils.color("&d&l&kA&a &d&lMYTHIC DUNGEON BOW &kA")
        );

        meta.setLore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 100, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        terminatorBow.setItemMeta(meta);

        return terminatorBow;
    }
}
