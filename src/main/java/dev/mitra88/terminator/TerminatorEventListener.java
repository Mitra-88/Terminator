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

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TerminatorEventListener implements Listener {

    private final TerminatorConfig config;

    private enum ClickSide { NONE, LEFT, RIGHT }

    private static final class HoldState {
        ClickSide lastSide = ClickSide.NONE;
        long untilMs = 0L;
    }

    private static final Component AB_T1;
    private static final Component AB_T2;
    private static final Component AB_T3;
    static {
        MiniMessage mm = MiniMessage.miniMessage();
        AB_T1 = mm.deserialize("<dark_gray>Salvation: <yellow>T1")
                .decoration(TextDecoration.ITALIC, false);
        AB_T2 = mm.deserialize("<dark_gray>Salvation: <gold>T2")
                .decoration(TextDecoration.ITALIC, false);
        AB_T3 = mm.deserialize("<dark_gray>Salvation: <light_purple><bold>T3!")
                .decoration(TextDecoration.ITALIC, false);
    }

    private final Int2IntOpenHashMap  hitCounter    = new Int2IntOpenHashMap();
    private final Int2LongOpenHashMap beamCooldown  = new Int2LongOpenHashMap();

    private final Map<UUID, HoldState> holdMap       = new HashMap<>();
    private final Map<UUID, Long>      shootCooldown = new HashMap<>();

    public TerminatorEventListener(Terminator ignoredPlugin, TerminatorConfig config) {
        this.config = config;
        hitCounter.defaultReturnValue(0);
        beamCooldown.defaultReturnValue(0L);
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

        arrow.getPersistentDataContainer().set(
                Terminator.TERMINATOR_KEY, PersistentDataType.BYTE, (byte) 1);

        player.playSound(player.getLocation(),
                config.shootSound, config.shootSoundVolume, config.shootSoundPitch);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (!meta.getPersistentDataContainer()
                .has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) return;

        Action action = event.getAction();
        if (!config.clickActions.contains(action)) return;

        Player p = event.getPlayer();
        ClickSide incomingSide = sideFromAction(action);
        int id = p.getEntityId();
        long now = System.currentTimeMillis();

        if (incomingSide == ClickSide.LEFT
                && hitCounter.get(id) >= config.salvationHitsRequired) {

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

        if (blockOppositeIfHolding(p, incomingSide)) {
            event.setCancelled(true);
            return;
        }
        if (now - shootCooldown.getOrDefault(p.getUniqueId(), 0L) < config.shootCooldownMs) {
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
        if (!arrow.getPersistentDataContainer()
                .has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) return;

        if (event.getHitBlock() != null) {
            arrow.remove();
            return;
        }

        Entity hitEnt = event.getHitEntity();
        if (!(hitEnt instanceof LivingEntity target)) {
            arrow.remove();
            return;
        }

        if (target instanceof Enderman enderman && arrow.getShooter() instanceof Player shooter) {
            enderman.damage(arrow.getDamage(), shooter);
        }

        arrow.remove();

        if (arrow.getShooter() instanceof Player shooter) {
            onSalvationHit(shooter);
        }
    }

    private void onSalvationHit(Player p) {
        int id = p.getEntityId();
        int current = hitCounter.get(id);
        if (current >= config.salvationHitsRequired) {
            return;
        }
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
        Vector dir = eye.getDirection().clone().normalize();
        World world = eye.getWorld();
        Vector originVec = eye.toVector();

        final double maxDist = config.beamMaxDistance;
        final double raySize = config.beamRaySize;
        final int    maxPierce = config.beamMaxPierce;

        RayTraceResult blockHit = world.rayTraceBlocks(
                eye, dir, maxDist, FluidCollisionMode.NEVER, false);

        double wallDist = maxDist;
        if (blockHit != null) {
            blockHit.getHitPosition();
            wallDist = originVec.distance(blockHit.getHitPosition());
        }

        spawnLavaTrail(world, originVec, dir, Math.min(maxDist, wallDist));

        Set<Entity> alreadyHit = new HashSet<>(maxPierce);
        Vector cursor = originVec.clone();
        int pierced = 0;
        double traveled = 0.0;
        double limit = Math.min(maxDist, wallDist);

        while (pierced < maxPierce && traveled < limit) {
            double remaining = limit - traveled;
            Location cursorLoc = cursor.toLocation(world);

            RayTraceResult r = world.rayTraceEntities(
                    cursorLoc, dir, remaining, raySize,
                    e -> e != player
                            && !alreadyHit.contains(e)
                            && e instanceof LivingEntity
                            && !(e instanceof ArmorStand));

            if (r == null || r.getHitEntity() == null) {
                break;
            }

            Entity ent = r.getHitEntity();
            alreadyHit.add(ent);

            // Apply damage, bypassing i-frames per design.
            if (ent instanceof LivingEntity le) {
                int oldMax = le.getMaximumNoDamageTicks();
                le.setMaximumNoDamageTicks(0);
                le.setNoDamageTicks(0);
                le.damage(config.beamDamage, player);
                le.setMaximumNoDamageTicks(oldMax);
            }

            pierced++;

            Vector hitPos = r.getHitPosition();
            cursor = hitPos.clone().add(dir.clone().multiply(0.5));
            traveled = originVec.distance(cursor);
        }
    }

    private void spawnLavaTrail(World world, Vector originVec, Vector dir, double length) {
        int count = Math.max(1, (int) (length * config.beamParticlesPerMeter));
        double step = length / count;
        Vector stepVec = dir.clone().multiply(step);
        Location cur = originVec.toLocation(world);
        for (int i = 0; i < count; i++) {
            world.spawnParticle(Particle.DRIPPING_LAVA, cur, 1,
                    0.02, 0.02, 0.02, 0.0);
            cur.add(stepVec);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        int id = event.getPlayer().getEntityId();
        hitCounter.remove(id);
        beamCooldown.remove(id);
        holdMap.remove(event.getPlayer().getUniqueId());
        shootCooldown.remove(event.getPlayer().getUniqueId());
    }
}
