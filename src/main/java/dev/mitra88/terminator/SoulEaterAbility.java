package dev.mitra88.terminator;

import org.bukkit.NamespacedKey;
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

    private static final double DAMAGE_MULTIPLIER = 10.0;

    private final NamespacedKey strengthKey;

    public SoulEaterAbility(Plugin plugin) {
        this.strengthKey = new NamespacedKey(plugin, "soul_eater_strength");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        ItemMeta meta = Terminator.terminatorMeta(weapon);
        if (meta == null) return;

        boolean criticalHit = isCriticalHit(event);
        if (!criticalHit && !isFatalHit(event)) return;

        double killingBlowDamage = event.getFinalDamage();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean metaChanged = criticalHit && consumeStoredStrength(event, pdc);

        if (isFatalHit(event) && event.getEntity() instanceof Monster) {
            metaChanged |= storeStrength(pdc, killingBlowDamage);
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

    private boolean storeStrength(PersistentDataContainer pdc, double damage) {
        if (damage <= 0.0) return false;
        pdc.set(strengthKey, PersistentDataType.DOUBLE, damage * DAMAGE_MULTIPLIER);
        return true;
    }

    private boolean consumeStoredStrength(EntityDamageByEntityEvent event, PersistentDataContainer pdc) {
        Double storedStrength = pdc.get(strengthKey, PersistentDataType.DOUBLE);
        if (storedStrength == null || storedStrength <= 0.0) return false;

        event.setDamage(event.getDamage() + storedStrength);
        pdc.remove(strengthKey);
        return true;
    }

    private static boolean isFatalHit(EntityDamageByEntityEvent event) {
        return event.getEntity() instanceof LivingEntity victim && victim.getHealth() - event.getFinalDamage() <= 0.0;
    }
}
