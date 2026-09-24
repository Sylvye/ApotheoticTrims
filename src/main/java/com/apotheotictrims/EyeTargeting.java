package com.apotheotictrims;

import org.bukkit.GameMode;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

final class EyeTargeting {
    private EyeTargeting() {}

    static Player findTarget(Player viewer) {
        Location eyes = viewer.getEyeLocation();
        Vector look = eyes.getDirection();
        Vector origin = eyes.toVector();
        Player best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Player candidate : viewer.getWorld().getPlayers()) {
            if (candidate == viewer || candidate.getGameMode() == GameMode.SPECTATOR
                    || !viewer.canSee(candidate)) continue;
            BoundingBox box = candidate.getBoundingBox();
            double centerDistance = box.getCenter().distance(origin);
            double leeway = .12 * Math.max(0, 1 - centerDistance / 24);
            RayTraceResult hit = box.clone().expand(leeway).rayTrace(origin, look, centerDistance + 2);
            if (hit == null) continue;
            double distance = hit.getHitPosition().distance(origin);
            if (distance >= bestDistance) continue;
            RayTraceResult block = viewer.getWorld().rayTraceBlocks(eyes, look, distance,
                    FluidCollisionMode.NEVER, true);
            if (block != null && block.getHitPosition().distance(origin) < distance - .001) continue;
            best = candidate;
            bestDistance = distance;
        }
        return best;
    }
}
