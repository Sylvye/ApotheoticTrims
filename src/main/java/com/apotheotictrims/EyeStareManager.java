package com.apotheotictrims;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class EyeStareManager {
    private static final int EFFECT_TICKS = 10;
    private final EyeEffects effects;
    private final Map<UUID, Player> viewerTargets = new HashMap<>();
    private final Map<UUID, TargetState> targets = new HashMap<>();

    EyeStareManager() {
        this(new PotionEyeEffects());
    }

    EyeStareManager(EyeEffects effects) {
        this.effects = effects;
    }

    void update(Player viewer, Player target, boolean particles) {
        UUID viewerId = viewer.getUniqueId();
        if (viewerTargets.get(viewerId) != target) {
            stopViewing(viewerId);
            if (target == null) return;
            viewerTargets.put(viewerId, target);
        }
        if (target == null) return;
        TargetState state = targets.computeIfAbsent(target.getUniqueId(), ignored -> new TargetState(target));
        state.viewers.put(viewerId, viewer.isSneaking());
        refresh(state, EyeEffect.WEAKNESS);
        refresh(state, EyeEffect.GLOWING);
        updateSlowness(state);
        if (particles && state.viewers.keySet().iterator().next().equals(viewerId))
            target.getWorld().spawnParticle(Particle.GLOW, target.getLocation().add(0, 1, 0),
                    2, .25, .45, .25, 0);
    }

    void clear(Player player) {
        stopViewing(player.getUniqueId());
        TargetState state = targets.get(player.getUniqueId());
        if (state == null) return;
        for (UUID viewerId : Set.copyOf(state.viewers.keySet())) stopViewing(viewerId);
    }

    void clearViewer(Player player) {
        stopViewing(player.getUniqueId());
    }

    void clear() {
        for (UUID viewerId : Set.copyOf(viewerTargets.keySet())) stopViewing(viewerId);
    }

    private void refresh(TargetState state, EyeEffect effect) {
        if (effects.refresh(state.target, effect)) state.owned.add(effect);
    }

    private void updateSlowness(TargetState state) {
        if (state.viewers.containsValue(true)) refresh(state, EyeEffect.SLOWNESS);
        else release(state, EyeEffect.SLOWNESS);
    }

    private void release(TargetState state, EyeEffect effect) {
        if (state.owned.remove(effect)) effects.clear(state.target, effect);
    }

    private void stopViewing(UUID viewerId) {
        Player target = viewerTargets.remove(viewerId);
        if (target == null) return;
        TargetState state = targets.get(target.getUniqueId());
        if (state == null) return;
        state.viewers.remove(viewerId);
        if (state.viewers.isEmpty()) {
            targets.remove(target.getUniqueId());
            for (EyeEffect effect : EyeEffect.values()) release(state, effect);
        } else updateSlowness(state);
    }

    enum EyeEffect { WEAKNESS, GLOWING, SLOWNESS }

    interface EyeEffects {
        boolean refresh(Player target, EyeEffect effect);
        void clear(Player target, EyeEffect effect);
    }

    private static final class TargetState {
        final Player target;
        final Map<UUID, Boolean> viewers = new HashMap<>();
        final EnumSet<EyeEffect> owned = EnumSet.noneOf(EyeEffect.class);

        TargetState(Player target) { this.target = target; }
    }

    private static final class PotionEyeEffects implements EyeEffects {
        @Override public boolean refresh(Player target, EyeEffect effect) {
            PotionEffectType type = type(effect);
            PotionEffect current = target.getPotionEffect(type);
            if (current != null && !replaceable(current)) return false;
            return target.addPotionEffect(new PotionEffect(type, EFFECT_TICKS, 0, false, false, true), false);
        }

        @Override public void clear(Player target, EyeEffect effect) {
            PotionEffectType type = type(effect);
            PotionEffect current = target.getPotionEffect(type);
            if (current != null && replaceable(current)) target.removePotionEffect(type);
        }

        private PotionEffectType type(EyeEffect effect) {
            return switch (effect) {
                case WEAKNESS -> PotionEffectType.WEAKNESS;
                case GLOWING -> PotionEffectType.GLOWING;
                case SLOWNESS -> PotionEffectType.SLOWNESS;
            };
        }

        private boolean replaceable(PotionEffect effect) {
            return effect.getAmplifier() == 0 && effect.getDuration() <= EFFECT_TICKS;
        }
    }
}
