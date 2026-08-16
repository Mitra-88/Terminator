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
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TerminatorEventListener implements Listener {

    private final TerminatorConfig config;

    private enum ClickSide {LEFT, RIGHT}

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

    private static ClickSide sideFromAction(Action action) {
        return (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
                ? ClickSide.RIGHT
                : ClickSide.LEFT;
    }

    private static Vector dirFromYawPitch(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cosP = Math.cos(pitch);
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
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) {
            return;
        }

        Action action = event.getAction();
        if (!config.clickActions.contains(action)) return;

        Player player = event.getPlayer();
        int id = player.getEntityId();
        long now = System.currentTimeMillis();
        ClickSide side = sideFromAction(action);

        if (side == ClickSide.LEFT && hitCounter.get(id) >= config.salvationHitsRequired) {
            event.setCancelled(true);
            if (now - beamCooldown.get(id) < config.beamCooldownMs) {
                return;
            }

            beamCooldown.put(id, now);
            hitCounter.put(id, 0);
            fireSalvationBeam(player);
            return;
        }

        PlayerState state = states.computeIfAbsent(player.getUniqueId(), unused -> new PlayerState());

        if (now < state.holdUntilMs && state.lastSide != side) {
            event.setCancelled(true);
            return;
        }

        if (now < state.shootCooldownUntilMs) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        state.shootCooldownUntilMs = now + config.shootCooldownMs;
        state.lastSide = side;
        state.holdUntilMs = now + config.holdWindowMs;

        Location location = player.getLocation();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        float spread = config.sideSpreadDegrees;

        shootArrow(player, dirFromYawPitch(yaw, pitch));
        shootArrow(player, dirFromYawPitch(yaw + spread, pitch));
        shootArrow(player, dirFromYawPitch(yaw - spread, pitch));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;

        if (!(arrow.getShooter() instanceof Player shooter)) return;

        if (!arrow.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) {
            return;
        }

        if (event.getHitBlock() != null) {
            arrow.remove();
            return;
        }

        Entity hitEntity = event.getHitEntity();
        if (!(hitEntity instanceof LivingEntity target)) {
            arrow.remove();
            return;
        }

        if (target instanceof Enderman enderman) {
            enderman.damage(arrow.getDamage(), shooter);
        }

        arrow.remove();
        onSalvationHit(shooter);
    }

    private void onSalvationHit(Player player) {
        int id = player.getEntityId();
        int current = hitCounter.get(id);
        if (current >= config.salvationHitsRequired) return;

        int next = current + 1;
        hitCounter.put(id, next);

        Component message = switch (next) {
            case 1 -> AB_T1;
            case 2 -> AB_T2;
            default -> AB_T3;
        };
        player.sendActionBar(message);
    }

    private void fireSalvationBeam(Player player) {
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        Vector origin = eye.toVector();
        Vector direction = eye.getDirection();

        double maxDistance = Math.max(0.0, config.beamMaxDistance);
        double raySize = Math.max(0.0, config.beamRaySize);
        int maxPierce = Math.max(0, config.beamMaxPierce);

        RayTraceResult blockHit = world.rayTraceBlocks(
                eye, direction, maxDistance, FluidCollisionMode.NEVER, false
        );

        double limit = blockHit != null
                ? origin.distance(blockHit.getHitPosition())
                : maxDistance;

        spawnLavaTrail(player, origin, direction, limit);

        List<LivingEntity> targets = findBeamTargets(
                world, eye, origin,
                direction, limit, raySize,
                maxPierce, player
        );

        for (LivingEntity target : targets) {
            int oldMaxNoDamageTicks = target.getMaximumNoDamageTicks();

            target.setMaximumNoDamageTicks(0);
            target.setNoDamageTicks(0);
            target.damage(config.beamDamage, player);
            target.setMaximumNoDamageTicks(oldMaxNoDamageTicks);
        }
    }

    private List<LivingEntity> findBeamTargets(
            World world,
            Location eye,
            Vector origin,
            Vector direction,
            double limit,
            double raySize,
            int maxPierce,
            Player player
    ) {
        if (limit <= 0.0 || maxPierce <= 0) {
            return new ArrayList<>(0);
        }

        Vector end = origin.clone().add(direction.clone().multiply(limit));

        BoundingBox beamBox = new BoundingBox(
                Math.min(origin.getX(), end.getX()) - raySize,
                Math.min(origin.getY(), end.getY()) - raySize,
                Math.min(origin.getZ(), end.getZ()) - raySize,
                Math.max(origin.getX(), end.getX()) + raySize,
                Math.max(origin.getY(), end.getY()) + raySize,
                Math.max(origin.getZ(), end.getZ()) + raySize
        );

        Collection<Entity> nearby = world.getNearbyEntities(beamBox);
        if (nearby.isEmpty()) {
            return new ArrayList<>(0);
        }

        List<LivingEntity> candidates = new ArrayList<>();

        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living instanceof ArmorStand) continue;
            if (living.getEntityId() == player.getEntityId()) continue;

            BoundingBox entityBox = expandBox(living.getBoundingBox(), raySize);

            if (entityBox.rayTrace(origin, direction, limit) != null) {
                candidates.add(living);
            }
        }

        if (candidates.isEmpty()) {
            return candidates;
        }

        Location tempLoc = new Location(world, 0, 0, 0);

        candidates.sort((e1, e2) -> {
            e1.getLocation(tempLoc);
            double dist1 = tempLoc.distanceSquared(eye);

            e2.getLocation(tempLoc);
            double dist2 = tempLoc.distanceSquared(eye);

            return Double.compare(dist1, dist2);
        });

        if (candidates.size() <= maxPierce) {
            return candidates;
        }

        List<LivingEntity> finalTargets = new ArrayList<>(maxPierce);

        for (int i = 0; i < maxPierce; i++) {
            finalTargets.add(candidates.get(i));
        }

        return finalTargets;
    }

    private static BoundingBox expandBox(BoundingBox box, double amount) {
        if (amount <= 0.0) return box;

        return new BoundingBox(
                box.getMinX() - amount,
                box.getMinY() - amount,
                box.getMinZ() - amount,
                box.getMaxX() + amount,
                box.getMaxY() + amount,
                box.getMaxZ() + amount
        );
    }

    private void spawnLavaTrail(Player player, Vector origin, Vector direction, double length) {
        if (length <= 0.0) return;

        int count = Math.max(1, (int) (length * config.beamParticlesPerMeter));
        double step = length / count;

        double x = origin.getX();
        double y = origin.getY();
        double z = origin.getZ();

        double dx = direction.getX() * step;
        double dy = direction.getY() * step;
        double dz = direction.getZ() * step;

        Location location = new Location(player.getWorld(), x, y, z);

        for (int i = 0; i < count; i++) {
            player.spawnParticle(Particle.DRIPPING_LAVA, location, 1, 0.02, 0.02, 0.02, 0.0);

            x += dx;
            y += dy;
            z += dz;

            location.setX(x);
            location.setY(y);
            location.setZ(z);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        int id = player.getEntityId();

        hitCounter.remove(id);
        beamCooldown.remove(id);
        states.remove(player.getUniqueId());
    }

    public void cleanup() {
        hitCounter.clear();
        beamCooldown.clear();
        states.clear();
    }
}
