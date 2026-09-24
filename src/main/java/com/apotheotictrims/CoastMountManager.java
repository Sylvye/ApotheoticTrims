package com.apotheotictrims;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

final class CoastMountManager {
    private final SettingsManager settings;
    private final NamespacedKey speedKey;
    private final Map<UUID, AppliedMount> applied = new HashMap<>();

    CoastMountManager(ApotheoticTrimsPlugin plugin, SettingsManager settings) {
        this(settings, new NamespacedKey(plugin, "coast_mount_speed"));
    }

    CoastMountManager(SettingsManager settings, NamespacedKey speedKey) {
        this.settings = settings;
        this.speedKey = speedKey;
    }

    void tick(Collection<? extends Player> players, Predicate<Player> hasCoast) {
        Map<UUID, RidingMount> current = collect(players, hasCoast);

        applied.entrySet().removeIf(entry -> {
            RidingMount ride = current.get(entry.getKey());
            AppliedMount old = entry.getValue();
            if (ride != null && ride.entity() == old.entity() && ride.category() == old.category()) return false;
            removeModifier(old);
            return true;
        });
        for (RidingMount ride : current.values()) {
            if (ride.entity() instanceof LivingEntity living) {
                Attribute attributeType = ride.category() == CoastMountCategory.HAPPY_GHAST
                        ? Attribute.FLYING_SPEED : Attribute.MOVEMENT_SPEED;
                AttributeInstance attribute = living.getAttribute(attributeType);
                if (attribute == null && attributeType == Attribute.FLYING_SPEED) {
                    attributeType = Attribute.MOVEMENT_SPEED;
                    attribute = living.getAttribute(attributeType);
                }
                if (attribute == null) continue;
                AppliedMount old = applied.get(living.getUniqueId());
                AttributeModifier existing = attribute.getModifier(speedKey);
                double amount = ride.multiplier() - 1;
                if (old != null && old.attribute() != attributeType) removeModifier(old);
                if (existing != null && existing.getAmount() == amount && old != null
                        && old.attribute() == attributeType) continue;
                if (existing != null) attribute.removeModifier(existing);
                attribute.addTransientModifier(new AttributeModifier(speedKey, amount,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1));
                applied.put(living.getUniqueId(), new AppliedMount(living, ride.category(), attributeType));
            }
        }
    }

    Map<UUID, RidingMount> collect(Collection<? extends Player> players, Predicate<Player> hasCoast) {
        Map<UUID, RidingMount> current = new HashMap<>();
        for (Player player : players) {
            if (!hasCoast.test(player)) continue;
            Entity mount = player.getVehicle();
            if (mount == null || !mount.isValid()) continue;
            CoastMountCategory category = CoastMountCategory.of(mount);
            if (category == null) continue;
            double multiplier = settings.value(TrimAbility.COAST, category.settingKey());
            current.merge(mount.getUniqueId(), new RidingMount(mount, category, multiplier),
                    (first, second) -> first.multiplier() >= second.multiplier() ? first : second);
        }
        return current;
    }

    void clear() {
        applied.values().forEach(this::removeModifier);
        applied.clear();
    }

    private void removeModifier(AppliedMount old) {
        if (!(old.entity() instanceof LivingEntity living) || !living.isValid() || old.attribute() == null) return;
        AttributeInstance attribute = living.getAttribute(old.attribute());
        if (attribute != null) attribute.removeModifier(speedKey);
    }

    record RidingMount(Entity entity, CoastMountCategory category, double multiplier) {}
    private record AppliedMount(Entity entity, CoastMountCategory category, Attribute attribute) {}
}
