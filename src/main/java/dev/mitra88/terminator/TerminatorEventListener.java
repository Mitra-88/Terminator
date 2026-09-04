package dev.mitra88.terminator;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    private static ClickSide sideFromAction(Action action) {
        return (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) ? ClickSide.RIGHT : ClickSide.LEFT;
    }

    private static Vector dirFromYawPitch(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cosP = Math.cos(pitch);
        return new Vector(-cosP * Math.sin(yaw), -Math.sin(pitch), cosP * Math.cos(yaw));
    }

    private void shootArrow(Player player, Vector direction) {
        Vector velocity = direction.clone().multiply(config.arrowVelocity);
        Arrow arrow = player.launchProjectile(Arrow.class, velocity);

        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

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
        ClickSide side = sideFromAction(action);

        event.setCancelled(true);
        event.setUseItemInHand(PlayerInteractEvent.Result.DENY);
        player.clearActiveItem();

        handleTerminatorClick(player, side);
    }

    @EventHandler
    public void onPrePlayerAttack(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) {
            return;
        }

        if (!isLeftClickEnabled()) return;

        event.setCancelled(true);
        handleTerminatorClick(player, ClickSide.LEFT);
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack bow = event.getBow();
        if (bow == null || !bow.hasItemMeta()) return;
        ItemMeta meta = bow.getItemMeta();
        if (!meta.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!shooter.isOnline()) return;
        if (!arrow.getPersistentDataContainer().has(Terminator.TERMINATOR_KEY, PersistentDataType.BYTE)) {
            return;
        }

        if (event.getHitBlock() != null) {
            arrow.remove();
            return;
        }

        if (!(event.getHitEntity() instanceof LivingEntity target)) {
            arrow.remove();
            return;
        }

        if (target instanceof ArmorStand) {
            arrow.remove();
            return;
        }

        if (target instanceof Enderman enderman) {
            enderman.damage(arrow.getDamage(), shooter);
        }

        arrow.remove();
        salvationBeam.onArrowHit(shooter);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        salvationBeam.onQuit(player);
        states.remove(player.getUniqueId());
    }

    public void cleanup() {
        salvationBeam.cleanup();
        states.clear();
    }

    private boolean isLeftClickEnabled() {
        return config.clickActions.contains(Action.LEFT_CLICK_AIR)
                || config.clickActions.contains(Action.LEFT_CLICK_BLOCK);
    }

    private void handleTerminatorClick(Player player, ClickSide side) {
        if (side == ClickSide.LEFT && salvationBeam.canFireBeam(player)) {
            salvationBeam.tryFireBeam(player);
            return;
        }

        long now = System.currentTimeMillis();
        PlayerState state = states.computeIfAbsent(player.getUniqueId(), _ -> new PlayerState());

        if (now < state.holdUntilMs && state.lastSide != side) {
            return;
        }

        if (now < state.shootCooldownUntilMs) {
            return;
        }

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
}
