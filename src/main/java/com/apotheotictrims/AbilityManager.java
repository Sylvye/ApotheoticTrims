package com.apotheotictrims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class AbilityManager {
    private final ApotheoticTrimsPlugin plugin;
    private final SettingsManager settings;
    private final TrimDetector detector = new TrimDetector();
    private final ComboTracker comboTracker = new ComboTracker();
    private final Map<UUID, TrimAbility> active = new HashMap<>();
    private final Set<UUID> grantedFlight = new HashSet<>();
    private final Set<UUID> wildUsed = new HashSet<>();
    private final Map<UUID, Long> lastWildJump = new HashMap<>();
    private final Set<UUID> spireFallProtection = new HashSet<>();
    private final EyeChargeTracker eyeCharges = new EyeChargeTracker();
    private final CoastMountManager coastMounts;
    private final NamespacedKey duneModifierKey;
    private BukkitTask task;
    private BukkitTask wildTask;
    private BukkitTask coastTask;
    private BukkitTask eyeTask;

    public AbilityManager(ApotheoticTrimsPlugin plugin, SettingsManager settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.duneModifierKey = new NamespacedKey(plugin, "dune_knockback_resistance");
        this.coastMounts = new CoastMountManager(plugin, settings);
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 10L);
        wildTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (has(player, TrimAbility.WILD)) updateWild(player);
            }
        }, 1L, 1L);
        coastTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> coastMounts.tick(Bukkit.getOnlinePlayers(),
                player -> has(player, TrimAbility.COAST)), 1L, 1L);
        eyeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (has(player, TrimAbility.EYE)) updateEye(player);
            }
        }, 1L, 5L);
    }

    public void stop() {
        if (task != null) task.cancel();
        if (wildTask != null) wildTask.cancel();
        if (coastTask != null) coastTask.cancel();
        if (eyeTask != null) eyeTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) cleanup(player, active.get(player.getUniqueId()));
        active.clear();
        comboTracker.clear();
        wildUsed.clear();
        lastWildJump.clear();
        spireFallProtection.clear();
        coastMounts.clear();
        eyeCharges.clear();
    }

    public Optional<TrimAbility> active(Player player) {
        TrimAbility ability = active.get(player.getUniqueId());
        return ability != null && settings.enabled(ability) && player.hasPermission("apotheotictrims.use")
                ? Optional.of(ability) : Optional.empty();
    }

    public boolean has(Player player, TrimAbility ability) { return active(player).orElse(null) == ability; }
    public ComboTracker combos() { return comboTracker; }
    public Set<UUID> spireFallProtection() { return spireFallProtection; }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
        coastMounts.tick(Bukkit.getOnlinePlayers(), player -> has(player, TrimAbility.COAST));
    }

    public void refresh(Player player) {
        TrimAbility detected = player.hasPermission("apotheotictrims.use")
                ? detector.detect(player).filter(settings::enabled).orElse(null) : null;
        UUID id = player.getUniqueId();
        TrimAbility previous = active.get(id);
        if (previous != detected) {
            cleanup(player, previous);
            if (detected == null) {
                active.remove(id);
            } else {
                active.put(id, detected);
                if (detected == TrimAbility.WILD) updateWild(player);
                playActivationFeedback(player, detected);
            }
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
            if (spireFallProtection.contains(player.getUniqueId()) && (player.isOnGround() || player.isInWater())) {
                spireFallProtection.remove(player.getUniqueId());
            }
            TrimAbility ability = active.get(player.getUniqueId());
            if (ability == null) continue;
            switch (ability) {
                case TIDE -> {
                    Location location = player.getLocation();
                    boolean inRain = location.getWorld().hasStorm()
                            && location.getWorld().isClearWeather() == false
                            && location.getBlock().getLightFromSky() > 0
                            && location.getWorld().getHighestBlockYAt(location) <= location.getBlockY();
                    if (player.isInWater() || location.getBlock().getType() == Material.BUBBLE_COLUMN || inRain) {
                        effect(player, PotionEffectType.DOLPHINS_GRACE, settings.intValue(ability, "dolphins-grace-level"));
                        effect(player, PotionEffectType.RESISTANCE, settings.intValue(ability, "resistance-level"));
                    }
                }
                case COAST -> { }
                case DUNE -> applyDune(player);
                case WILD -> { }
                case RIB -> {
                    if (player.getFireTicks() > 0) {
                        int level = settings.intValue(ability, "regeneration-level");
                        PotionEffect current = player.getPotionEffect(PotionEffectType.REGENERATION);
                        if (regenerationNeedsRefresh(current, level)) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
                                    60, level - 1, false, false, true), false);
                        }
                    }
                }
                case WARD -> quietWardens(player);
                case EYE -> { }
                case SILENCE -> SilenceEffects.types().forEach(player::removePotionEffect);
                case WAYFINDER -> effect(player, PotionEffectType.SPEED, settings.intValue(ability, "speed-level"));
                case RAISER -> effect(player, PotionEffectType.JUMP_BOOST, settings.intValue(ability, "jump-boost-level"));
                case SHAPER -> effect(player, PotionEffectType.HASTE, settings.intValue(ability, "haste-level"));
                case HOST -> {
                    effect(player, PotionEffectType.HERO_OF_THE_VILLAGE, settings.intValue(ability, "hero-level"));
                    effect(player, PotionEffectType.LUCK, settings.intValue(ability, "luck-level"));
                }
                default -> { }
            }
        }
    }

    private void effect(Player player, PotionEffectType type, int level) {
        effect(player, type, level, 30);
    }

    private void effect(Player player, PotionEffectType type, int level, int duration) {
        PotionEffect current = player.getPotionEffect(type);
        if (current != null && current.getAmplifier() > level - 1) return;
        player.addPotionEffect(new PotionEffect(type, duration, level - 1, false, false, true), false);
    }

    static boolean regenerationNeedsRefresh(PotionEffect current, int level) {
        return current == null || current.getAmplifier() < level - 1
                || current.getAmplifier() == level - 1 && current.getDuration() <= 10;
    }

    private void applyDune(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (attribute == null) return;
        double amount = settings.value(TrimAbility.DUNE, "knockback-resistance");
        AttributeModifier current = attribute.getModifier(duneModifierKey);
        if (current != null && current.getAmount() == amount) return;
        if (current != null) attribute.removeModifier(current);
        attribute.addTransientModifier(new AttributeModifier(duneModifierKey, amount, AttributeModifier.Operation.ADD_NUMBER));
    }

    private void removeDune(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (attribute != null) attribute.removeModifier(duneModifierKey);
    }

    private void updateWild(Player player) {
        UUID id = player.getUniqueId();
        if (player.isOnGround() || player.isInWater() || player.isClimbing()) wildUsed.remove(id);
        boolean mode = player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
        long cooldown = Math.round(settings.value(TrimAbility.WILD, "cooldown-seconds") * 1000);
        boolean cooled = System.currentTimeMillis() - lastWildJump.getOrDefault(id, 0L) >= cooldown;
        boolean eligible = mode && !player.isGliding() && !player.isSwimming() && !player.isInsideVehicle()
                && !wildUsed.contains(id) && cooled;
        if (eligible && !player.getAllowFlight()) {
            player.setAllowFlight(true);
            grantedFlight.add(id);
        } else if (!eligible && grantedFlight.remove(id)) {
            player.setAllowFlight(false);
        }
    }

    public boolean performDoubleJump(Player player) {
        if (!has(player, TrimAbility.WILD)) return false;
        UUID id = player.getUniqueId();
        if (wildUsed.contains(id) || player.isGliding() || player.isSwimming() || player.isInsideVehicle()) return false;
        long cooldown = Math.round(settings.value(TrimAbility.WILD, "cooldown-seconds") * 1000);
        if (System.currentTimeMillis() - lastWildJump.getOrDefault(id, 0L) < cooldown) return false;
        wildUsed.add(id);
        lastWildJump.put(id, System.currentTimeMillis());
        player.setFlying(false);
        if (grantedFlight.remove(id)) player.setAllowFlight(false);
        var velocity = player.getVelocity();
        velocity.setY(settings.value(TrimAbility.WILD, "vertical-velocity"));
        player.setVelocity(velocity);
        feedback(player, Sound.ENTITY_BREEZE_JUMP, Particle.CLOUD);
        return true;
    }

    public boolean ownsWildFlight(Player player) {
        return grantedFlight.contains(player.getUniqueId());
    }

    private void updateEye(Player player) {
        long tick = Bukkit.getCurrentTick();
        Player target = EyeTargeting.findTarget(player);
        long stareTicks = Math.round(settings.value(TrimAbility.EYE, "stare-seconds") * 20);
        if (eyeCharges.stare(player.getUniqueId(), target == null ? null : target.getUniqueId(), tick, stareTicks)) {
            int duration = Math.max(1, (int) Math.round(settings.value(TrimAbility.EYE, "stare-effect-seconds") * 20));
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0, false, false, true), false);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration,
                    settings.intValue(TrimAbility.EYE, "slowness-level") - 1, false, false, true), false);
            feedback(player, Sound.ENTITY_ENDER_EYE_LAUNCH, Particle.GLOW);
        }
        long sneakTicks = Math.round(settings.value(TrimAbility.EYE, "sneak-seconds") * 20);
        if (!eyeCharges.sneak(player.getUniqueId(), player.isSneaking(), tick, sneakTicks)) return;
        double radius = settings.value(TrimAbility.EYE, "radius");
        int duration = Math.max(1, (int) Math.round(settings.value(TrimAbility.EYE, "reveal-seconds") * 20));
        for (Player nearby : player.getWorld().getNearbyPlayers(player.getLocation(), radius)) {
            if (nearby != player && player.canSee(nearby))
                nearby.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0, false, false, true), false);
        }
        feedback(player, Sound.ENTITY_ENDER_EYE_LAUNCH, Particle.GLOW);
    }

    private void quietWardens(Player player) {
        for (Warden warden : player.getWorld().getNearbyEntitiesByType(Warden.class, player.getLocation(), 64)) {
            if (warden.getTarget() == player) warden.setTarget(null);
            if (warden.getAnger(player) > 0) warden.clearAnger(player);
        }
    }

    public void feedback(Player player, Sound sound, Particle particle) {
        if (settings.sounds()) player.playSound(player.getLocation(), sound, 0.7f, 1.15f);
        if (settings.particles()) player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 12, .35, .45, .35, .02);
    }

    public void boltTriggerFeedback(Player player) {
        actionBar(player, Component.text("Bolt combo! ⚡"));
        feedback(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Particle.ELECTRIC_SPARK);
    }

    private void playActivationFeedback(Player player, TrimAbility ability) {
        ActivationFeedback activation = switch (ability) {
            case TIDE -> new ActivationFeedback(Sound.ENTITY_DOLPHIN_SPLASH, Particle.BUBBLE);
            case COAST -> new ActivationFeedback(Sound.BLOCK_NOTE_BLOCK_CHIME, Particle.HAPPY_VILLAGER);
            case DUNE -> new ActivationFeedback(Sound.BLOCK_SAND_BREAK, Particle.DUST_PLUME);
            case WILD -> new ActivationFeedback(Sound.ENTITY_BREEZE_JUMP, Particle.CLOUD);
            case SENTRY -> new ActivationFeedback(Sound.ENTITY_ARROW_SHOOT, Particle.CRIT);
            case SNOUT -> new ActivationFeedback(Sound.ENTITY_HOGLIN_AMBIENT, Particle.ANGRY_VILLAGER);
            case BOLT -> new ActivationFeedback(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Particle.ELECTRIC_SPARK);
            case FLOW -> new ActivationFeedback(Sound.ENTITY_WIND_CHARGE_WIND_BURST, Particle.GUST);
            case RIB -> new ActivationFeedback(Sound.ENTITY_BLAZE_SHOOT, Particle.FLAME);
            case WARD -> new ActivationFeedback(Sound.BLOCK_SCULK_SENSOR_CLICKING, Particle.SCULK_CHARGE_POP);
            case VEX -> new ActivationFeedback(Sound.ENTITY_VEX_AMBIENT, Particle.ENCHANT);
            case SPIRE -> new ActivationFeedback(Sound.BLOCK_END_PORTAL_SPAWN, Particle.PORTAL);
            case EYE -> new ActivationFeedback(Sound.ENTITY_ENDER_EYE_LAUNCH, Particle.GLOW);
            case SILENCE -> new ActivationFeedback(Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, Particle.SMOKE);
            case WAYFINDER -> new ActivationFeedback(Sound.ENTITY_HORSE_STEP, Particle.CLOUD);
            case RAISER -> new ActivationFeedback(Sound.ENTITY_SLIME_JUMP, Particle.CLOUD);
            case SHAPER -> new ActivationFeedback(Sound.BLOCK_BEACON_ACTIVATE, Particle.ENCHANT);
            case HOST -> new ActivationFeedback(Sound.EVENT_RAID_HORN, Particle.HAPPY_VILLAGER);
        };
        feedback(player, activation.sound(), activation.particle());
        actionBar(player, Component.text(ability.displayName() + " trim ability active"));
    }

    public void actionBar(Player player, Component message) {
        if (settings.actionBar()) player.sendActionBar(message.color(NamedTextColor.AQUA));
    }

    public void cleanupPlayer(Player player) {
        cleanup(player, active.remove(player.getUniqueId()));
        comboTracker.clear(player.getUniqueId());
        spireFallProtection.remove(player.getUniqueId());
        eyeCharges.clear(player.getUniqueId());
    }

    public void clearEye(Player player) { eyeCharges.clear(player.getUniqueId()); }

    private void cleanup(Player player, TrimAbility previous) {
        UUID id = player.getUniqueId();
        removeDune(player);
        if (grantedFlight.remove(id) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        wildUsed.remove(id);
        if (previous == TrimAbility.BOLT) comboTracker.clear(id);
        if (previous == TrimAbility.SPIRE) spireFallProtection.remove(id);
        if (previous == TrimAbility.EYE) eyeCharges.clear(id);
    }

    private record ActivationFeedback(Sound sound, Particle particle) {}
}
