package com.apotheotictrims;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

final class EyeTargeting {
    private static final double MIN_DOT = Math.cos(Math.toRadians(6));

    private EyeTargeting() {}

    static Player findTarget(Player viewer) {
        Location eyes = viewer.getEyeLocation();
        Vector look = eyes.getDirection();
        Player best = null;
        double bestAngle = MIN_DOT;
        for (Player candidate : viewer.getWorld().getPlayers()) {
            if (candidate == viewer || candidate.getGameMode() == GameMode.SPECTATOR
                    || !viewer.canSee(candidate)) continue;
            Vector difference = candidate.getEyeLocation().toVector().subtract(eyes.toVector());
            if (difference.lengthSquared() < .01) continue;
            double angle = look.dot(difference.normalize());
            if (angle >= bestAngle && viewer.hasLineOfSight(candidate)) {
                best = candidate;
                bestAngle = angle;
            }
        }
        return best;
    }
}
