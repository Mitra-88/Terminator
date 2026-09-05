package dev.mitra88.terminator;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class SoulEaterAbility implements Listener {

    private static final double SOUL_MULTIPLIER = 10.0;

    private final NamespacedKey soulKey;

    public SoulEaterAbility(Plugin plugin) {
        this.soulKey = new NamespacedKey(plugin, "soul_eater_strength");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        ItemMeta meta = Terminator.terminatorMeta(weapon);
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean metaChanged = false;

        if (isCriticalHit(event)) {
            metaChanged |= consumeStoredSoul(event, pdc);
        }

        if (isFatalHit(event) && event.getEntity() instanceof Monster monster) {
            metaChanged |= storeSoul(pdc, monster);
        }

        if (metaChanged) {
            weapon.setItemMeta(meta);
        }
    }

    private static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Arrow arrow && arrow.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY) && arrow.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    private static boolean isCriticalHit(EntityDamageByEntityEvent event) {
        return event.isCritical() || (event.getDamager() instanceof Arrow arrow && arrow.isCritical());
    }

    private boolean consumeStoredSoul(EntityDamageByEntityEvent event, PersistentDataContainer pdc) {
        Double storedSoul = pdc.get(soulKey, PersistentDataType.DOUBLE);
        if (storedSoul == null || storedSoul <= 0.0) return false;

        event.setDamage(event.getDamage() + storedSoul);
        pdc.remove(soulKey);
        return true;
    }

    private boolean storeSoul(PersistentDataContainer pdc, Monster monster) {
        AttributeInstance attackDamage = monster.getAttribute(Attribute.ATTACK_DAMAGE);
        double soul = (attackDamage != null ? attackDamage.getValue() : 0.0) * SOUL_MULTIPLIER;

        if (soul > 0.0) {
            Double storedSoul = pdc.get(soulKey, PersistentDataType.DOUBLE);
            if (storedSoul != null && storedSoul == soul) return false;

            pdc.set(soulKey, PersistentDataType.DOUBLE, soul);
            return true;
        }

        if (pdc.has(soulKey)) {
            pdc.remove(soulKey);
            return true;
        }
        return false;
    }

    private static boolean isFatalHit(EntityDamageByEntityEvent event) {
        return event.getEntity() instanceof LivingEntity victim && victim.getHealth() - event.getFinalDamage() <= 0.0;
    }
}