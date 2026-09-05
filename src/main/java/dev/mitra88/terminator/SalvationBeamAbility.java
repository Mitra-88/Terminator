package dev.mitra88.terminator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SalvationBeamAbility {

    private static final int MAX_PARTICLES = 256;
    private static final double LAVA_DENSITY_FACTOR = 0.75;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final TerminatorConfig config;

    private final Map<UUID, Integer> hitCounter = new HashMap<>();
    private final Map<UUID, Long> beamCooldown = new HashMap<>();

    public SalvationBeamAbility(TerminatorConfig config) {
        this.config = config;
    }

    public void onArrowHit(Player shooter) {
        UUID id = shooter.getUniqueId();
        int hits = hitCounter.getOrDefault(id, 0);
        if (hits >= config.salvationHitsRequired) return;

        hits++;
        hitCounter.put(id, hits);
        shooter.sendActionBar(progressMessage(hits));
    }

    public boolean tryFireBeam(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (hitCounter.getOrDefault(id, 0) < config.salvationHitsRequired
                || now - beamCooldown.getOrDefault(id, 0L) < config.beamCooldownMs) {
            return false;
        }

        beamCooldown.put(id, now);
        hitCounter.remove(id);
        fireSalvationBeam(player);
        return true;
    }

    public void onQuit(Player player) {
        hitCounter.remove(player.getUniqueId());
        beamCooldown.remove(player.getUniqueId());
    }

    public void cleanup() {
        hitCounter.clear();
        beamCooldown.clear();
    }

    private Component progressMessage(int hits) {
        String tier = hits >= config.salvationHitsRequired ? "<light_purple><bold>T" + hits + "!" : (hits == 1 ? "<yellow>T" : "<gold>T") + hits;
        return MM.deserialize("<dark_gray>Salvation: " + tier).decoration(TextDecoration.ITALIC, false);
    }

    private void fireSalvationBeam(Player player) {
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        Vector origin = eye.toVector();
        Vector direction = eye.getDirection();

        world.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, 0.3f, 2.0f);

        double maxDistance = Math.max(0.0, config.beamMaxDistance);
        double raySize = Math.max(0.0, config.beamRaySize);
        int maxPierce = Math.max(0, config.beamMaxPierce);

        RayTraceResult blockHit = world.rayTraceBlocks(eye, direction, maxDistance, FluidCollisionMode.NEVER, false);
        double limit = blockHit != null ? origin.distance(blockHit.getHitPosition()) : maxDistance;

        spawnLavaTrail(world, origin, direction, limit);

        for (LivingEntity target : findBeamTargets(world, eye, origin, direction, limit, raySize, maxPierce, player)) {
            Terminator.damageIgnoringHurtCooldown(target, player, config.beamDamage, "player");
        }
    }

    private List<LivingEntity> findBeamTargets(World world, Location eye, Vector origin, Vector direction,
                                               double limit, double raySize, int maxPierce, Player player) {
        if (limit <= 0.0 || maxPierce <= 0) {
            return List.of();
        }

        Vector end = direction.clone().multiply(limit).add(origin);
        BoundingBox beamBox = new BoundingBox(
                Math.min(origin.getX(), end.getX()) - raySize,
                Math.min(origin.getY(), end.getY()) - raySize,
                Math.min(origin.getZ(), end.getZ()) - raySize,
                Math.max(origin.getX(), end.getX()) + raySize,
                Math.max(origin.getY(), end.getY()) + raySize,
                Math.max(origin.getZ(), end.getZ()) + raySize);

        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(beamBox)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living instanceof ArmorStand || living.getEntityId() == player.getEntityId()) continue;

            if (living.getBoundingBox().expand(raySize, raySize, raySize).rayTrace(origin, direction, limit) != null) {
                candidates.add(living);
            }
        }

        candidates.sort(Comparator.comparingDouble(living -> living.getLocation().distanceSquared(eye)));
        return candidates.size() <= maxPierce ? candidates : new ArrayList<>(candidates.subList(0, maxPierce));
    }

    private void spawnLavaTrail(World world, Vector origin, Vector direction, double length) {
        if (length <= 0.0) return;

        int maxPerType = Math.max(1, MAX_PARTICLES / 2);
        int count = (int) Math.clamp(length * config.beamParticlesPerMeter * LAVA_DENSITY_FACTOR, 1.0, maxPerType);
        double halfStep = length / count / 2.0;

        Vector step = direction.clone().multiply(halfStep);
        Location location = origin.toLocation(world);
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.BLACK, 0.5f);

        for (int i = 0; i < count; i++) {
            world.spawnParticle(Particle.DRIPPING_LAVA, location, 1, 0.02, 0.02, 0.02, 0.0);
            location.add(step);
            world.spawnParticle(Particle.DUST, location, 1, 0.02, 0.02, 0.02, 0.0, blackDust);
            location.add(step);
        }
    }
}