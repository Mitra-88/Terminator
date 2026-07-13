package dev.mitra88.terminator;

import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TerminatorEventListener implements Listener {

    private static final Set<Action> CLICK_ACTIONS = EnumSet.of(
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK,
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK
    );

    private static final float SIDE_SPREAD_DEGREES = 10f;
    private static final long HOLD_WINDOW_MS = 250;
    private static final long SHOOT_COOLDOWN_MS = 200;

    private final Terminator plugin;

    private enum ClickSide { NONE, LEFT, RIGHT }

    private static class HoldState {
        ClickSide lastSide = ClickSide.NONE;
        long untilMs = 0L;
    }

    private final Map<UUID, HoldState> holdMap = new HashMap<>();
    private final Map<UUID, Long> shootCooldown = new HashMap<>();

    public TerminatorEventListener(Terminator plugin) {
        this.plugin = plugin;
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
        st.untilMs = System.currentTimeMillis() + HOLD_WINDOW_MS;
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
        arrow.setCritical(true);
        arrow.setVelocity(direction.multiply(4));
        arrow.setDamage(ThreadLocalRandom.current().nextDouble(20000, 50000));
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (!meta.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) return;

        Action action = event.getAction();
        if (!CLICK_ACTIONS.contains(action)) return;

        Player p = event.getPlayer();
        ClickSide incomingSide = sideFromAction(action);

        if (blockOppositeIfHolding(p, incomingSide)) {
            event.setCancelled(true);
            return;
        }

        long now = System.currentTimeMillis();
        long lastShoot = shootCooldown.getOrDefault(p.getUniqueId(), 0L);
        if (now - lastShoot < SHOOT_COOLDOWN_MS) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        shootCooldown.put(p.getUniqueId(), now);

        float yaw = p.getLocation().getYaw();
        float pitch = p.getLocation().getPitch();

        new BukkitRunnable() {
            @Override
            public void run() {
                shootArrow(p, dirFromYawPitch(yaw, pitch));                          // center
                shootArrow(p, dirFromYawPitch(yaw + SIDE_SPREAD_DEGREES, pitch));    // right
                shootArrow(p, dirFromYawPitch(yaw - SIDE_SPREAD_DEGREES, pitch));    // left
                setHold(p, incomingSide);
            }
        }.runTask(plugin);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter) {
            for (Entity entity : arrow.getNearbyEntities(3, 3, 3)) {
                if (entity instanceof Enderman enderman) {
                    enderman.damage(arrow.getDamage(), shooter);
                    arrow.remove();
                    break;
                }
            }
        }
    }
}
