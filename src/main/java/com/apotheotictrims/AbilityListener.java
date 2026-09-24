package com.apotheotictrims;

import io.papermc.paper.event.entity.WardenAngerChangeEvent;
import io.papermc.paper.event.player.PlayerShieldDisableEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AbilityListener implements Listener {
    private final ApotheoticTrimsPlugin plugin;
    private final SettingsManager settings;
    private final AbilityManager abilities;
    private final Map<RetaliationKey, Long> retaliation = new HashMap<>();

    public AbilityListener(ApotheoticTrimsPlugin plugin, SettingsManager settings, AbilityManager abilities) {
        this.plugin = plugin;
        this.settings = settings;
        this.abilities = abilities;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player credited = creditedPlayer(event);
        if (event.getEntity() instanceof Enemy enemy && credited != null && abilities.has(credited, TrimAbility.SNOUT)) {
            retaliation.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
            long duration = Math.round(settings.value(TrimAbility.SNOUT, "retaliation-seconds") * 1000);
            retaliation.put(new RetaliationKey(enemy.getUniqueId(), credited.getUniqueId()), System.currentTimeMillis() + duration);
            if (enemy instanceof Mob mob) mob.setTarget(credited);
        }

        if (event.getEntity() instanceof Player victim && abilities.has(victim, TrimAbility.SILENCE)
                && isInstantDamageSource(event.getDamageSource().getDirectEntity())) {
            event.setCancelled(true);
            return;
        }

        if (credited != null && abilities.has(credited, TrimAbility.SENTRY)
                && event.getDamager() instanceof Projectile && !(event.getDamager() instanceof ThrownPotion)) {
            event.setDamage(event.getDamage() * settings.value(TrimAbility.SENTRY, "damage-multiplier"));
        }

        if (event.getEntity() instanceof LivingEntity target && event.getDamager() instanceof Player player
                && abilities.has(player, TrimAbility.BOLT)) {
            int threshold = settings.intValue(TrimAbility.BOLT, "combo-hits");
            long timeout = Math.round(settings.value(TrimAbility.BOLT, "timeout-seconds") * 1000);
            ComboTracker.Result result = abilities.combos().hit(player.getUniqueId(), System.currentTimeMillis(), threshold, timeout);
            if (result.triggered()) {
                abilities.actionBar(player, Component.text("Bolt combo! ⚡"));
                abilities.feedback(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Particle.ELECTRIC_SPARK);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!target.isValid() || target.isDead()) return;
                    LightningStrike lightning = target.getWorld().strikeLightningEffect(target.getLocation());
                    DamageSource source = DamageSource.builder(DamageType.LIGHTNING_BOLT)
                            .withDirectEntity(lightning)
                            .withCausingEntity(player)
                            .withDamageLocation(target.getLocation())
                            .build();
                    target.damage(settings.value(TrimAbility.BOLT, "lightning-damage"), source);
                });
            } else {
                abilities.actionBar(player, Component.text("Bolt combo: " + result.count() + "/" + threshold));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Enemy) || !(event.getTarget() instanceof Player player)
                || !abilities.has(player, TrimAbility.SNOUT)) return;
        RetaliationKey key = new RetaliationKey(event.getEntity().getUniqueId(), player.getUniqueId());
        Long until = retaliation.get(key);
        if (until == null || until < System.currentTimeMillis()) {
            retaliation.remove(key);
            event.setCancelled(true);
            event.setTarget(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGenericGameEvent(GenericGameEvent event) {
        if (event.getEntity() instanceof Player player && abilities.has(player, TrimAbility.WARD)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockReceiveGameEvent(BlockReceiveGameEvent event) {
        if (event.getEntity() instanceof Player player && abilities.has(player, TrimAbility.WARD)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWardenAnger(WardenAngerChangeEvent event) {
        if (event.getNewAnger() > event.getOldAnger() && event.getTarget() instanceof Player player
                && abilities.has(player, TrimAbility.WARD)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWardenTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Warden warden && event.getTarget() instanceof Player player
                && abilities.has(player, TrimAbility.WARD)) {
            event.setCancelled(true);
            event.setTarget(null);
            warden.clearAnger(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShieldDisabled(PlayerShieldDisableEvent event) {
        Player player = event.getPlayer();
        if (!abilities.has(player, TrimAbility.FLOW)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            double radius = settings.value(TrimAbility.FLOW, "radius");
            double horizontal = settings.value(TrimAbility.FLOW, "horizontal-velocity");
            double vertical = settings.value(TrimAbility.FLOW, "vertical-velocity");
            for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(player.getLocation(), radius)) {
                if (entity == player || entity instanceof Player other && other.getGameMode() == GameMode.SPECTATOR) continue;
                Vector away = entity.getLocation().toVector().subtract(player.getLocation().toVector());
                away.setY(0);
                if (away.lengthSquared() < .0001) away = new Vector(1, 0, 0);
                entity.setVelocity(away.normalize().multiply(horizontal).setY(vertical));
            }
            abilities.feedback(player, Sound.ENTITY_WIND_CHARGE_WIND_BURST, Particle.GUST);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotem(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player) || !abilities.has(player, TrimAbility.VEX)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            int duration = Math.max(1, (int) Math.round(settings.value(TrimAbility.VEX, "duration-seconds") * 20));
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration,
                    settings.intValue(TrimAbility.VEX, "strength-level") - 1, false, true, true), true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration,
                    settings.intValue(TrimAbility.VEX, "speed-level") - 1, false, true, true), true);
            abilities.feedback(player, Sound.ITEM_TOTEM_USE, Particle.TOTEM_OF_UNDYING);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID && abilities.has(player, TrimAbility.SPIRE)) {
            event.setCancelled(true);
            wrapFromVoid(player);
        } else if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            boolean spireProtected = abilities.spireFallProtection().remove(player.getUniqueId());
            boolean wildProtected = abilities.consumeWildLandingProtection(player);
            if (spireProtected || wildProtected) event.setCancelled(true);
        }
    }

    private void wrapFromVoid(Player player) {
        World world = player.getWorld();
        Location current = player.getLocation();
        int x = current.getBlockX();
        int z = current.getBlockZ();
        int y = world.getMaxHeight() - 2;
        int bottom = Math.max(world.getMinHeight(), y - 32);
        while (y > bottom && (!world.getBlockAt(x, y, z).isPassable() || !world.getBlockAt(x, y + 1, z).isPassable())) y--;
        Location destination = new Location(world, current.getX(), y, current.getZ(), current.getYaw(), current.getPitch());
        Vector velocity = player.getVelocity();
        if (player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            abilities.spireFallProtection().add(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> player.setVelocity(velocity));
            abilities.feedback(player, Sound.BLOCK_PORTAL_TRAVEL, Particle.PORTAL);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player player && event.getNewEffect() != null
                && abilities.has(player, TrimAbility.SILENCE)
                && SilenceEffects.isBlocked(event.getNewEffect().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if ((player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
                && abilities.has(player, TrimAbility.WILD)) {
            event.setCancelled(true);
            abilities.performDoubleJump(player);
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { cleanup(event.getPlayer()); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { cleanup(event.getPlayer()); }
    @EventHandler public void onEntityDeath(EntityDeathEvent event) {
        UUID dead = event.getEntity().getUniqueId();
        retaliation.entrySet().removeIf(entry -> entry.getKey().mob().equals(dead));
    }
    @EventHandler public void onChangedWorld(PlayerChangedWorldEvent event) {
        abilities.clearWildLandingProtection(event.getPlayer());
        scheduleRefresh(event.getPlayer());
    }
    @EventHandler public void onRespawn(PlayerRespawnEvent event) { scheduleRefresh(event.getPlayer()); }
    @EventHandler public void onBreak(PlayerItemBreakEvent event) { scheduleRefresh(event.getPlayer()); }
    @EventHandler public void onSwap(PlayerSwapHandItemsEvent event) { scheduleRefresh(event.getPlayer()); }
    @EventHandler public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) scheduleRefresh(player);
    }
    @EventHandler public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) scheduleRefresh(player);
    }
    @EventHandler public void onInteract(PlayerInteractEvent event) { scheduleRefresh(event.getPlayer()); }

    private void cleanup(Player player) {
        abilities.cleanupPlayer(player);
        retaliation.entrySet().removeIf(entry -> entry.getKey().player().equals(player.getUniqueId()));
    }

    private void scheduleRefresh(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) abilities.refresh(player);
        });
    }

    private Player creditedPlayer(EntityDamageByEntityEvent event) {
        Entity causing = event.getDamageSource().getCausingEntity();
        if (causing instanceof Player player) return player;
        if (causing instanceof Tameable tameable && tameable.getOwner() instanceof Player player) return player;
        Entity damager = event.getDamager();
        if (damager instanceof Player player) return player;
        if (damager instanceof Tameable tameable && tameable.getOwner() instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
            if (shooter instanceof Tameable tameable && tameable.getOwner() instanceof Player player) return player;
        }
        return null;
    }

    private boolean isInstantDamageSource(Entity direct) {
        if (direct instanceof ThrownPotion potion) {
            return potion.getEffects().stream().anyMatch(effect -> effect.getType().equals(PotionEffectType.INSTANT_DAMAGE));
        }
        if (direct instanceof Arrow arrow) {
            return arrow.getBasePotionType() == PotionType.HARMING || arrow.getBasePotionType() == PotionType.STRONG_HARMING
                    || arrow.getCustomEffects().stream().anyMatch(effect -> effect.getType().equals(PotionEffectType.INSTANT_DAMAGE));
        }
        if (direct instanceof AreaEffectCloud cloud) {
            return cloud.getBasePotionType() == PotionType.HARMING || cloud.getBasePotionType() == PotionType.STRONG_HARMING
                    || cloud.getCustomEffects().stream().anyMatch(effect -> effect.getType().equals(PotionEffectType.INSTANT_DAMAGE));
        }
        return false;
    }

    private record RetaliationKey(UUID mob, UUID player) {}
}
