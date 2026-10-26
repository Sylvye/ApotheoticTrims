package com.apotheotictrims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.function.Predicate;

/** Sneak charging and invisibility with particles and Grungler-style equipment masking. */
public final class SilenceManager {
    private final SettingsManager settings;
    private final Visibility visibility;
    private final Map<UUID, State> states = new HashMap<>();
    private long tick;

    SilenceManager(SettingsManager settings) { this(settings, new PotionVisibility()); }

    SilenceManager(SettingsManager settings, Visibility visibility) {
        this.settings = settings;
        this.visibility = visibility;
    }

    void tick(Collection<? extends Player> players, Predicate<Player> eligible) {
        tick++;
        for (Player player : players) {
            if (!eligible.test(player) || !player.isSneaking() || player.isDead()) {
                clear(player);
                continue;
            }
            State state = states.computeIfAbsent(player.getUniqueId(), ignored -> new State(player));
            int required = Math.max(1, (int) Math.ceil(settings.value(TrimAbility.SILENCE, "charge-seconds") * 20));
            if (!state.hidden && ++state.charged >= required) {
                state.hidden = true;
                visibility.start(player, tick);
                message(player, "Silence: Invisible");
                refreshEquipment();
            }
            if (state.hidden) {
                visibility.refresh(player, tick);
            } else if (state.charged == 1 || state.charged % 5 == 0) {
                message(player, String.format(Locale.ROOT, "Silence: %.1f / %.1fs",
                        state.charged / 20.0, required / 20.0));
                if (settings.particles()) player.getWorld().spawnParticle(Particle.SMOKE,
                        player.getLocation().add(0, 1, 0), 2, .25, .4, .25, .005);
            }
        }
        // As in Grungler, repeat client equipment overrides to cover normal equipment packets.
        if (tick % 2 == 0) refreshEquipment();
    }

    public void clear(Player player) {
        State state = states.remove(player.getUniqueId());
        if (state == null) return;
        if (state.hidden) {
            visibility.stop(player, tick);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(player)) visibility.equipment(viewer, player, false);
            }
        }
        if (settings.actionBar()) player.sendActionBar(Component.empty());
    }

    void clear() {
        for (State state : List.copyOf(states.values())) clear(state.player);
    }

    public void mask(Player viewer, Player hidden) {
        State state = states.get(hidden.getUniqueId());
        if (state != null && state.hidden && !viewer.equals(hidden)) visibility.equipment(viewer, hidden, true);
    }

    public void refreshEquipment() {
        if (states.values().stream().noneMatch(state -> state.hidden)) return;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (State state : states.values()) if (state.hidden) mask(viewer, state.player);
        }
    }

    public void potionChanged(Player player, EntityPotionEffectEvent event) {
        State state = states.get(player.getUniqueId());
        if (state != null && state.hidden) visibility.potionChanged(player, event, tick);
    }

    private void message(Player player, String text) {
        if (settings.actionBar()) player.sendActionBar(Component.text(text, NamedTextColor.AQUA));
    }

    private static final class State {
        final Player player;
        int charged;
        boolean hidden;
        State(Player player) { this.player = player; }
    }

    interface Visibility {
        void start(Player player, long tick);
        void refresh(Player player, long tick);
        void stop(Player player, long tick);
        void equipment(Player viewer, Player subject, boolean hidden);
        default void potionChanged(Player player, EntityPotionEffectEvent event, long tick) {}
    }

    static final class PotionVisibility implements Visibility {
        private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        private final Map<UUID, SavedEffect> saved = new HashMap<>();
        private final PotionEffectType type;
        private boolean modifying;

        PotionVisibility() { this(PotionEffectType.INVISIBILITY); }
        PotionVisibility(PotionEffectType type) { this.type = type; }

        @Override public void start(Player player, long tick) {
            remember(player, player.getPotionEffect(type), tick);
        }

        @Override public void refresh(Player player, long tick) {
            modifying = true;
            try {
                PotionEffect current = player.getPotionEffect(type);
                if (current != null && !owned(current)) player.removePotionEffect(type);
                player.addPotionEffect(new PotionEffect(type, 3, 0, false, true, false));
            } finally { modifying = false; }
        }

        @Override public void stop(Player player, long tick) {
            SavedEffect previous = saved.remove(player.getUniqueId());
            modifying = true;
            try {
                PotionEffect current = player.getPotionEffect(type);
                if (current != null && !owned(current)) return;
                if (current != null) player.removePotionEffect(type);
                if (previous != null) {
                    PotionEffect effect = previous.effect;
                    int remaining = effect.isInfinite() ? -1 : (int) Math.max(0, previous.expiry - tick);
                    if (remaining != 0) player.addPotionEffect(new PotionEffect(effect.getType(), remaining,
                            effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
                }
            } finally { modifying = false; }
        }

        @Override public void potionChanged(Player player, EntityPotionEffectEvent event, long tick) {
            if (modifying || event.isCancelled() || event.getModifiedType() != type) return;
            if (event.getAction() == EntityPotionEffectEvent.Action.CHANGED && !event.isOverride()) return;
            remember(player, event.getNewEffect(), tick);
        }

        private void remember(Player player, PotionEffect effect, long tick) {
            if (effect == null) saved.remove(player.getUniqueId());
            else saved.put(player.getUniqueId(), new SavedEffect(effect, tick + Math.max(0, effect.getDuration())));
        }

        private static boolean owned(PotionEffect effect) {
            return effect.getAmplifier() == 0 && effect.getDuration() > 0 && effect.getDuration() <= 3
                    && !effect.isAmbient() && effect.hasParticles() && !effect.hasIcon();
        }

        @Override public void equipment(Player viewer, Player subject, boolean hidden) {
            Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : SLOTS) {
                ItemStack item = hidden ? null : subject.getInventory().getItem(slot);
                equipment.put(slot, item == null ? ItemStack.empty() : item);
            }
            viewer.sendEquipmentChange(subject, equipment);
        }

        private record SavedEffect(PotionEffect effect, long expiry) {}
    }
}
