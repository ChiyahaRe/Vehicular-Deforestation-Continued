/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.server.level.ServerLevel
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.leo.treeoverrun.compat;

import dev.leo.treeoverrun.compat.DebrisGroundContact;
import dev.leo.treeoverrun.compat.TreePhysicsCompat;
import dev.leo.treeoverrun.physics.TreeDebrisPlotHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;

final class TreeDebrisImpactCompat {
    static final double DUST_SPEED = 0.75;
    private static final Map<UUID, Double> LAST_SPEED = new ConcurrentHashMap<UUID, Double>();
    private static final Map<UUID, Long> LAST_DUST_TICK = new ConcurrentHashMap<UUID, Long>();

    private TreeDebrisImpactCompat() {
    }

    static void tick(SubLevelPhysicsSystem physicsSystem) {
        if (!TreePhysicsCompat.isLoaded()) {
            return;
        }
        ServerLevel level = physicsSystem.getLevel();
        TreeDebrisPlotHelper.forEachActiveDebris(level, debris -> {
            if (debris.isRemoved()) {
                TreePhysicsCompat.clearDebrisCompatState(debris.getUniqueId());
                return;
            }
            RigidBodyHandle handle = physicsSystem.getPhysicsHandle(debris);
            if (handle == null || !handle.isValid()) {
                return;
            }
            DebrisGroundContact.Contact contact = DebrisGroundContact.find(level, debris);
            if (contact == null) {
                return;
            }
            double speed = handle.getLinearVelocity(new Vector3d()).length();
            Double previous = LAST_SPEED.put(debris.getUniqueId(), speed);
            if (speed >= 0.75) {
                long lastDust;
                long now = level.getGameTime();
                if (now - (lastDust = LAST_DUST_TICK.getOrDefault(debris.getUniqueId(), Long.MIN_VALUE).longValue()) >= 4L) {
                    TreePhysicsCompat.spawnDebrisImpactPresentation(level, debris, contact.worldPoint(), contact.groundBlock(), speed);
                    LAST_DUST_TICK.put(debris.getUniqueId(), now);
                }
            } else if (previous != null && previous >= 2.0) {
                TreePhysicsCompat.tryPlayDebrisImpactSound(level, debris);
            }
        });
    }

    static void onDebrisRegistered(ServerLevel level, ServerSubLevel debris, Vector3dc inheritVelocity) {
        double speed;
        if (!TreePhysicsCompat.isLoaded()) {
            return;
        }
        DebrisGroundContact.Contact contact = DebrisGroundContact.find(level, debris);
        if (contact == null) {
            return;
        }
        double d = speed = inheritVelocity == null ? 0.0 : inheritVelocity.length();
        if (speed >= 0.75) {
            TreePhysicsCompat.spawnDebrisImpactPresentation(level, debris, contact.worldPoint(), contact.groundBlock(), speed);
        }
    }

    static void clearTracking(UUID debrisId) {
        LAST_SPEED.remove(debrisId);
        LAST_DUST_TICK.remove(debrisId);
    }
}

