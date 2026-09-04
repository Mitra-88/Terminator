package dev.mitra88.terminator;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TerminatorEventListener implements Listener {

    private final TerminatorConfig config;
    private final SalvationBeamAbility salvationBeam;

    private enum ClickSide {LEFT, RIGHT}

    private static final class PlayerState {
        ClickSide lastSide;
        long holdUntilMs;
        long shootCooldownUntilMs;
    }

    private final Map<UUID, PlayerState> states = new HashMap<>();

    public TerminatorEventListener(TerminatorConfig config, SalvationBeamAbility salvationBeam) {
        this.config = config;
        this.salvationBeam = salvationBeam;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (Terminator.terminatorMeta(event.getItem()) == null) return;

        Action action = event.getAction();
        if (!config.clickActions.contains(action)) return;

        event.setCancelled(true);
        event.getPlayer().clearActiveItem();

        handleTerminatorClick(event.getPlayer(), sideFromAction(action));
    }

    @EventHandler
    public void onPrePlayerAttack(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();

        if (Terminator.terminatorMeta(player.getInventory().getItemInMainHand()) == null) return;
        if (!isLeftClickEnabled()) return;

        event.setCancelled(true);
        handleTerminatorClick(player, ClickSide.LEFT);
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player && Terminator.terminatorMeta(event.getBow()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY)) return;

        double arrowDamage = arrow.getDamage();
        Entity hitEntity = event.getHitEntity();
        Player shooter = arrow.getShooter() instanceof Player player ? player : null;

        arrow.remove();

        if (hitEntity == null) return;
        if (shooter == null || !shooter.isOnline()) return;

        if (hitEntity instanceof Enderman enderman) {
            enderman.damage(arrowDamage, shooter);
        }

        if (hitEntity instanceof LivingEntity target && !(target instanceof ArmorStand)) {
            salvationBeam.onArrowHit(shooter);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArrowSelfDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (arrow.getShooter() != victim) return;
        if (!arrow.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY)) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
        salvationBeam.onQuit(event.getPlayer());
    }

    public void cleanup() {
        states.clear();
        salvationBeam.cleanup();
    }

    private boolean isLeftClickEnabled() {
        return config.clickActions.contains(Action.LEFT_CLICK_AIR) || config.clickActions.contains(Action.LEFT_CLICK_BLOCK);
    }

    private void handleTerminatorClick(Player player, ClickSide side) {
        long now = System.currentTimeMillis();

        if (side == ClickSide.LEFT && salvationBeam.tryFireBeam(player)) {
            touchState(player).shootCooldownUntilMs = now + config.shootCooldownMs;
            return;
        }

        PlayerState state = touchState(player);

        if (now < state.holdUntilMs && state.lastSide != side) return;
        if (now <= state.shootCooldownUntilMs) return;

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

    private PlayerState touchState(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), _ -> new PlayerState());
    }

    private void shootArrow(Player player, Vector direction) {
        Arrow arrow = player.launchProjectile(Arrow.class, direction.clone().multiply(config.arrowVelocity));

        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        arrow.setCritical(config.criticalArrows);
        arrow.setDamage(randomArrowDamage());
        arrow.getPersistentDataContainer().set(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE, (byte) 1);

        player.playSound(player.getLocation(), config.shootSound, config.shootSoundVolume, config.shootSoundPitch);
    }

    private double randomArrowDamage() {
        double min = Math.min(config.arrowDamageMin, config.arrowDamageMax);
        double max = Math.max(config.arrowDamageMin, config.arrowDamageMax);
        return min >= max ? min : ThreadLocalRandom.current().nextDouble(min, max);
    }

    private static ClickSide sideFromAction(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK ? ClickSide.RIGHT : ClickSide.LEFT;
    }

    private static Vector dirFromYawPitch(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cosPitch = Math.cos(pitch);
        return new Vector(-cosPitch * Math.sin(yaw), -Math.sin(pitch), cosPitch * Math.cos(yaw));
    }
}
