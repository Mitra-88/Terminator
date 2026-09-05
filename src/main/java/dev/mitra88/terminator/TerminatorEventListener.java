package dev.mitra88.terminator;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TerminatorEventListener implements Listener {

    private static final Component OFF_HAND_MESSAGE = Component.text("The Terminator only works in your main hand.", NamedTextColor.RED);
    private static final long OFF_HAND_NOTICE_COOLDOWN_MS = 5000L;

    private final TerminatorConfig config;
    private final SalvationBeamAbility salvationBeam;

    private enum ClickSide {LEFT, RIGHT}

    private static final class PlayerState {
        ClickSide lastSide;
        long holdUntilMs;
        long shootCooldownUntilMs;
    }

    private final Map<UUID, PlayerState> states = new HashMap<>();
    private final Map<UUID, Long> offHandNotice = new HashMap<>();

    public TerminatorEventListener(TerminatorConfig config, SalvationBeamAbility salvationBeam) {
        this.config = config;
        this.salvationBeam = salvationBeam;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (Terminator.terminatorMeta(event.getItem()) == null) return;

        if (event.getHand() != EquipmentSlot.HAND) {
            event.setCancelled(true);
            notifyOffHand(event.getPlayer());
            return;
        }

        Action action = event.getAction();
        if (!config.clickActions.contains(action)) return;

        event.setCancelled(true);
        event.getPlayer().clearActiveItem();

        handleTerminatorClick(event.getPlayer(), sideFromAction(action));
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (Terminator.terminatorMeta(event.getOffHandItem()) != null) {
            notifyOffHand(event.getPlayer());
        }
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

        double arrowDamage = arrowDamage(arrow);
        Entity hitEntity = event.getHitEntity();
        Player shooter = arrow.getShooter() instanceof Player player ? player : null;

        arrow.remove();

        if (hitEntity == null || shooter == null || !shooter.isOnline()) return;
        if (hitEntity.getUniqueId().equals(shooter.getUniqueId())) return;

        if (hitEntity instanceof LivingEntity living) {
            living.setNoDamageTicks(0);
        }

        if (hitEntity instanceof Enderman enderman) {
            Terminator.damageIgnoringHurtCooldown(enderman, shooter, arrowDamage, "arrow");
        }

        if (hitEntity instanceof LivingEntity target && !(target instanceof ArmorStand)) {
            salvationBeam.onArrowHit(shooter);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;

        Double damage = arrow.getPersistentDataContainer().get(Terminator.ARROW_DAMAGE_KEY, PersistentDataType.DOUBLE);
        if (damage == null) return;

        event.setDamage(damage);
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
        offHandNotice.remove(event.getPlayer().getUniqueId());
        salvationBeam.onQuit(event.getPlayer());
    }

    public void cleanup() {
        states.clear();
        offHandNotice.clear();
        salvationBeam.cleanup();
    }

    private void notifyOffHand(Player player) {
        long now = System.currentTimeMillis();
        Long lastNotice = offHandNotice.get(player.getUniqueId());
        if (lastNotice != null && now - lastNotice < OFF_HAND_NOTICE_COOLDOWN_MS) return;

        offHandNotice.put(player.getUniqueId(), now);
        player.sendMessage(OFF_HAND_MESSAGE);
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

        player.getWorld().playSound(player.getLocation(), config.shootSound, config.shootSoundVolume, config.shootSoundPitch);

        double volleyDamage = randomArrowDamage();
        shootArrow(player, dirFromYawPitch(yaw, pitch), volleyDamage);
        shootArrow(player, dirFromYawPitch(yaw + spread, pitch), volleyDamage);
        shootArrow(player, dirFromYawPitch(yaw - spread, pitch), volleyDamage);
    }

    private PlayerState touchState(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), _ -> new PlayerState());
    }

    private void shootArrow(Player player, Vector direction, double damage) {
        Arrow arrow = player.launchProjectile(Arrow.class, direction.clone().multiply(config.arrowVelocity));

        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        arrow.setCritical(config.criticalArrows);
        arrow.setDamage(damage);
        arrow.getPersistentDataContainer().set(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE, (byte) 1);
        arrow.getPersistentDataContainer().set(Terminator.ARROW_DAMAGE_KEY, PersistentDataType.DOUBLE, damage);
    }

    private static double arrowDamage(Arrow arrow) {
        Double damage = arrow.getPersistentDataContainer().get(Terminator.ARROW_DAMAGE_KEY, PersistentDataType.DOUBLE);
        return damage != null ? damage : arrow.getDamage();
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
