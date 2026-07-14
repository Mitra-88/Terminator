package dev.mitra88.terminator;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public final class TerminatorEventListener implements Listener {

    private final TerminatorConfig config;

    private enum ClickSide { LEFT, RIGHT }

    private static final class PlayerState {
        ClickSide lastSide;
        long holdUntilMs;
        long shootCooldownUntilMs;
    }

    private static final Component AB_T1;
    private static final Component AB_T2;
    private static final Component AB_T3;
    static {
        MiniMessage mm = MiniMessage.miniMessage();
        AB_T1 = mm.deserialize("<dark_gray>Salvation: <yellow>T1").decoration(TextDecoration.ITALIC, false);
        AB_T2 = mm.deserialize("<dark_gray>Salvation: <gold>T2").decoration(TextDecoration.ITALIC, false);
        AB_T3 = mm.deserialize("<dark_gray>Salvation: <light_purple><bold>T3!").decoration(TextDecoration.ITALIC, false);
    }

    private final Int2IntOpenHashMap hitCounter = new Int2IntOpenHashMap();
    private final Int2LongOpenHashMap beamCooldown = new Int2LongOpenHashMap();
    private final Map<UUID, PlayerState> states = new HashMap<>();

    public TerminatorEventListener(TerminatorConfig config) {
        this.config = config;
        hitCounter.defaultReturnValue(0);
        beamCooldown.defaultReturnValue(0L);
    }

    private static ClickSide sideFromAction(Action a) {
        return (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK)
                ? ClickSide.RIGHT
                : ClickSide.LEFT;
    }

    private static Vector dirFromYawPitch(float yawDeg, float pitchDeg) {
        double yaw   = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cosP  = Math.cos(pitch);
        return new Vector(-cosP * Math.sin(yaw), -Math.sin(pitch), cosP * Math.cos(yaw));
    }

    private void shootArrow(Player player, Vector direction) {
        Arrow arrow = player.getWorld().spawnArrow(
                player.getEyeLocation(), direction,
                (float) config.arrowVelocity, 0f);
        arrow.setShooter(player);

        double min = Math.min(config.arrowDamageMin, config.arrowDamageMax);
        double max = Math.max(config.arrowDamageMin, config.arrowDamageMax);
        arrow.setDamage((min >= max) ? min : ThreadLocalRandom.current().nextDouble(min, max));

        arrow.getPersistentDataContainer().set(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE, (byte) 1);

        player.playSound(player.getLocation(), config.shootSound, config.shootSoundVolume, config.shootSoundPitch);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) return;

        Action action = event.getAction();
        if (!config.clickActions.contains(action)) return;

        Player p = event.getPlayer();
        int id = p.getEntityId();
        long now = System.currentTimeMillis();
        ClickSide side = sideFromAction(action);

        if (side == ClickSide.LEFT && hitCounter.get(id) >= config.salvationHitsRequired) {
            if (now - beamCooldown.get(id) < config.beamCooldownMs) {
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            beamCooldown.put(id, now);
            hitCounter.put(id, 0);
            fireSalvationBeam(p);
            return;
        }

        PlayerState st = states.computeIfAbsent(p.getUniqueId(), _ -> new PlayerState());

        if (now < st.holdUntilMs && st.lastSide != side) {
            event.setCancelled(true);
            return;
        }

        if (now < st.shootCooldownUntilMs) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        st.shootCooldownUntilMs = now + config.shootCooldownMs;
        st.lastSide = side;
        st.holdUntilMs = now + config.holdWindowMs;

        Location loc = p.getLocation();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        final float spread = config.sideSpreadDegrees;

        shootArrow(p, dirFromYawPitch(yaw, pitch));                      // center
        shootArrow(p, dirFromYawPitch(yaw + spread, pitch));     // right
        shootArrow(p, dirFromYawPitch(yaw - spread, pitch));     // left
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) return;

        if (event.getHitBlock() != null) {
            arrow.remove();
            return;
        }

        Entity hitEnt = event.getHitEntity();
        if (!(hitEnt instanceof LivingEntity target)) {
            arrow.remove();
            return;
        }

        if (!(arrow.getShooter() instanceof Player shooter)) {
            arrow.remove();
            return;
        }

        if (target instanceof Enderman enderman) {
            enderman.damage(arrow.getDamage(), shooter);
        }

        arrow.remove();
        onSalvationHit(shooter);
    }

    private void onSalvationHit(Player p) {
        int id = p.getEntityId();
        int current = hitCounter.get(id);
        if (current >= config.salvationHitsRequired) return;

        int next = current + 1;
        hitCounter.put(id, next);

        Component msg = switch (next) {
            case 1 -> AB_T1;
            case 2 -> AB_T2;
            default -> AB_T3;
        };
        p.sendActionBar(msg);
    }

    private void fireSalvationBeam(Player player) {
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        Vector origin = eye.toVector();
        Vector dir = eye.getDirection();

        final double maxDist = config.beamMaxDistance;
        final double raySize = config.beamRaySize;
        final int maxPierce = config.beamMaxPierce;

        RayTraceResult blockHit = world.rayTraceBlocks(eye, dir, maxDist, FluidCollisionMode.NEVER, false);
        final double limit = blockHit != null ? origin.distance(blockHit.getHitPosition()) : maxDist;

        spawnLavaTrail(world, origin, dir, limit);

        final double ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        final double dx = dir.getX(), dy = dir.getY(), dz = dir.getZ();
        double cx = ox, cy = oy, cz = oz;

        final Location cursorLoc = new Location(world, ox, oy, oz);
        final Set<Entity> alreadyHit = new HashSet<>(maxPierce);
        final Predicate<Entity> filter = e -> e != player
                && e instanceof LivingEntity
                && !(e instanceof ArmorStand)
                && !alreadyHit.contains(e);

        for (int pierced = 0; pierced < maxPierce; pierced++) {
            double traveled = (cx - ox) * dx + (cy - oy) * dy + (cz - oz) * dz;
            if (traveled >= limit) break;

            cursorLoc.setX(cx);
            cursorLoc.setY(cy);
            cursorLoc.setZ(cz);

            RayTraceResult r = world.rayTraceEntities(cursorLoc, dir, limit - traveled, raySize, filter);
            if (r == null) break;
            Entity ent = r.getHitEntity();
            if (ent == null) break;

            alreadyHit.add(ent);
            LivingEntity le = (LivingEntity) ent;

            int oldMax = le.getMaximumNoDamageTicks();
            le.setMaximumNoDamageTicks(0);
            le.setNoDamageTicks(0);
            le.damage(config.beamDamage, player);
            le.setMaximumNoDamageTicks(oldMax);

            Vector hitPos = r.getHitPosition();
            cx = hitPos.getX() + dx * 0.5;
            cy = hitPos.getY() + dy * 0.5;
            cz = hitPos.getZ() + dz * 0.5;
        }
    }

    private void spawnLavaTrail(World world, Vector origin, Vector dir, double length) {
        int count = Math.max(1, (int) (length * config.beamParticlesPerMeter));
        double step = length / count;
        double x = origin.getX(), y = origin.getY(), z = origin.getZ();
        double dx = dir.getX() * step, dy = dir.getY() * step, dz = dir.getZ() * step;
        for (int i = 0; i < count; i++) {
            world.spawnParticle(Particle.DRIPPING_LAVA, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
            x += dx; y += dy; z += dz;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        int id = p.getEntityId();
        hitCounter.remove(id);
        beamCooldown.remove(id);
        states.remove(p.getUniqueId());
    }

    public void cleanup() {
        hitCounter.clear();
        beamCooldown.clear();
        states.clear();
    }
}
