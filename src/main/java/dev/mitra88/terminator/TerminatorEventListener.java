package dev.mitra88.terminator;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TerminatorEventListener implements Listener {

    private final TerminatorConfig config;

    private enum ClickSide { NONE, LEFT, RIGHT }

    private static class HoldState {
        ClickSide lastSide = ClickSide.NONE;
        long untilMs = 0L;
    }

    private final Map<UUID, HoldState> holdMap = new HashMap<>();
    private final Map<UUID, Long> shootCooldown = new HashMap<>();

    public TerminatorEventListener(Terminator ignoredPlugin, TerminatorConfig config) {
        this.config = config;
    }

    private static ClickSide sideFromAction(Action a) {
        return (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK)
                ? ClickSide.RIGHT
                : ClickSide.LEFT;
    }

    private boolean blockOppositeIfHolding(Player p, ClickSide incoming) {
        HoldState st = holdMap.computeIfAbsent(p.getUniqueId(), _ -> new HoldState());
        long now = System.currentTimeMillis();
        if (now >= st.untilMs) st.lastSide = ClickSide.NONE;
        return st.lastSide != ClickSide.NONE && st.lastSide != incoming;
    }

    private void setHold(Player p, ClickSide side) {
        HoldState st = holdMap.computeIfAbsent(p.getUniqueId(), _ -> new HoldState());
        st.lastSide = side;
        st.untilMs = System.currentTimeMillis() + config.holdWindowMs;
    }

    private static Vector dirFromYawPitch(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double x = -Math.cos(pitch) * Math.sin(yaw);
        double y = -Math.sin(pitch);
        double z =  Math.cos(pitch) * Math.cos(yaw);
        return new Vector(x, y, z).normalize();
    }

    private void shootArrow(Player player, Vector direction) {
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(direction.multiply(config.arrowVelocity));

        double min = Math.min(config.arrowDamageMin, config.arrowDamageMax);
        double max = Math.max(config.arrowDamageMin, config.arrowDamageMax);
        double damage = (min >= max) ? min : ThreadLocalRandom.current().nextDouble(min, max);
        arrow.setDamage(damage);

        arrow.getPersistentDataContainer().set(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE, (byte) 1);

        player.playSound(
                player.getLocation(),
                config.shootSound,
                config.shootSoundVolume,
                config.shootSoundPitch
        );
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (!meta.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) return;

        Action action = event.getAction();
        if (!config.clickActions.contains(action)) return;

        Player p = event.getPlayer();
        ClickSide incomingSide = sideFromAction(action);

        if (blockOppositeIfHolding(p, incomingSide)) {
            event.setCancelled(true);
            return;
        }

        long now = System.currentTimeMillis();
        long lastShoot = shootCooldown.getOrDefault(p.getUniqueId(), 0L);
        if (now - lastShoot < config.shootCooldownMs) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        shootCooldown.put(p.getUniqueId(), now);

        float yaw = p.getLocation().getYaw();
        float pitch = p.getLocation().getPitch();
        final float spread = config.sideSpreadDegrees;

        shootArrow(p, dirFromYawPitch(yaw, pitch));                     // center
        shootArrow(p, dirFromYawPitch(yaw + spread, pitch));    // right
        shootArrow(p, dirFromYawPitch(yaw - spread, pitch));    // left
        setHold(p, incomingSide);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) return;
        if (event.getHitBlock() != null) {
            arrow.remove();
            return;
        }
        if (event.getHitEntity() instanceof Enderman enderman) {
            enderman.damage(arrow.getDamage(), (Player) arrow.getShooter());
            arrow.remove();
        }
    }
}
