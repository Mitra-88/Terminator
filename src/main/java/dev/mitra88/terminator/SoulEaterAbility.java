package dev.mitra88.terminator;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
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
        Player attacker;

        if (event.getDamager() instanceof Player player) {
            attacker = player;
        } else if (event.getDamager() instanceof Arrow arrow) {
            if (!(arrow.getShooter() instanceof Player shooter)) return;
            if (!arrow.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY)) {
                return;
            }
            attacker = shooter;
        } else {
            return;
        }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        ItemMeta meta = Terminator.terminatorMeta(weapon);
        if (meta == null) return;

        boolean isKillingBlow = isFatalHit(event);
        boolean isCriticalHit = event.isCritical();

        if (!isCriticalHit && !isKillingBlow) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean metaChanged = isCriticalHit && consumeStoredStrength(event, pdc);

        if (isKillingBlow) {
            metaChanged |= storeStrengthFromKill(event, pdc);
        }

        if (metaChanged) {
            weapon.setItemMeta(meta);
        }
    }

    private boolean storeStrengthFromKill(EntityDamageByEntityEvent event, PersistentDataContainer pdc) {
        if (!(event.getEntity() instanceof Monster)) {
            return false;
        }

        double mobDamage = event.getFinalDamage();
        if (mobDamage <= 0) {
            return false;
        }

        pdc.set(strengthKey, PersistentDataType.DOUBLE, mobDamage * DAMAGE_MULTIPLIER);
        return true;
    }

    private boolean consumeStoredStrength(EntityDamageByEntityEvent event, PersistentDataContainer pdc) {
        Double storedStrength = pdc.get(strengthKey, PersistentDataType.DOUBLE);
        if (storedStrength == null || storedStrength <= 0) {
            return false;
        }

        event.setDamage(event.getDamage() + storedStrength);
        pdc.remove(strengthKey);
        return true;
    }

    private boolean isFatalHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return false;
        }
        return victim.getHealth() - event.getFinalDamage() <= 0.0;
    }
}
