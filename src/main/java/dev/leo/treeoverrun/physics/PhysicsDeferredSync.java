/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.server.level.ServerLevel
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.leo.treeoverrun.physics;

import dev.leo.treeoverrun.TreeOverrunSublevels;
import dev.leo.treeoverrun.compat.TreePhysicsCompat;
import dev.leo.treeoverrun.physics.TreeDebrisHelper;
import dev.leo.treeoverrun.physics.TreeDebrisPlotHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class PhysicsDeferredSync {
    private static final int BREAK_DELAY_TICKS = 1;
    private static final Map<UUID, PendingStatsSync> PENDING_STATS = new ConcurrentHashMap<UUID, PendingStatsSync>();
    private static final Map<UUID, PendingBreak> PENDING_BREAKS = new ConcurrentHashMap<UUID, PendingBreak>();
    private static boolean warnedStatsContainerMissing;
    private static boolean warnedBreaksContainerMissing;

    private PhysicsDeferredSync() {
    }

    public static void queueStatsSync(ServerSubLevel subLevel, Vector3dc vehicleVelocity) {
        PENDING_STATS.put(subLevel.getUniqueId(), new PendingStatsSync(vehicleVelocity));
    }

    public static void queueBreak(ServerSubLevel subLevel) {
        PENDING_BREAKS.putIfAbsent(subLevel.getUniqueId(), new PendingBreak(1));
    }

    public static void cancel(UUID subLevelId) {
        PENDING_STATS.remove(subLevelId);
        PENDING_BREAKS.remove(subLevelId);
    }

    public static void flush(ServerLevel level) {
        PhysicsDeferredSync.flushStatsSyncs(level);
        PhysicsDeferredSync.flushBreaks(level);
    }

    private static void flushStatsSyncs(ServerLevel level) {
        if (PENDING_STATS.isEmpty()) {
            return;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(container instanceof ServerSubLevelContainer)) {
            if (!warnedStatsContainerMissing) {
                warnedStatsContainerMissing = true;
                TreeOverrunSublevels.LOGGER.info("Tree debris registration is waiting for a server sublevel container: pending={}", (Object)PENDING_STATS.size());
            }
            return;
        }
        ServerSubLevelContainer serverContainer = container;
        warnedStatsContainerMissing = false;
        Iterator<Map.Entry<UUID, PendingStatsSync>> iterator = PENDING_STATS.entrySet().iterator();
        while (iterator.hasNext()) {
            ServerSubLevel serverSubLevel;
            Map.Entry<UUID, PendingStatsSync> entry = iterator.next();
            SubLevel subLevel = serverContainer.getSubLevel(entry.getKey());
            if (!(subLevel instanceof ServerSubLevel) || (serverSubLevel = (ServerSubLevel)subLevel).isRemoved()) {
                iterator.remove();
                continue;
            }
            PendingStatsSync pending = entry.getValue();
            TreeDebrisPlotHelper.registerTreeDebris(serverSubLevel);
            if (pending.inheritVelocity() != null) {
                TreePhysicsCompat.onDebrisRegistered(level, serverSubLevel, (Vector3dc)pending.inheritVelocity());
            }
            iterator.remove();
        }
    }

    private static void flushBreaks(ServerLevel level) {
        if (PENDING_BREAKS.isEmpty()) {
            return;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(container instanceof ServerSubLevelContainer)) {
            if (!warnedBreaksContainerMissing) {
                warnedBreaksContainerMissing = true;
                TreeOverrunSublevels.LOGGER.info("Tree debris removal is waiting for a server sublevel container: pending={}", (Object)PENDING_BREAKS.size());
            }
            return;
        }
        ServerSubLevelContainer serverContainer = container;
        warnedBreaksContainerMissing = false;
        Iterator<Map.Entry<UUID, PendingBreak>> iterator = PENDING_BREAKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingBreak> entry = iterator.next();
            if (entry.getValue().ticksRemaining() > 1) {
                entry.setValue(new PendingBreak(entry.getValue().ticksRemaining() - 1));
                continue;
            }
            SubLevel subLevel = serverContainer.getSubLevel(entry.getKey());
            if (!(subLevel instanceof ServerSubLevel)) continue;
            ServerSubLevel serverSubLevel = (ServerSubLevel)subLevel;
            if (serverSubLevel.isRemoved()) {
                iterator.remove();
                continue;
            }
            iterator.remove();
            TreeDebrisHelper.breakIntoItemsAndRemove(level, serverSubLevel);
        }
    }

    public static void flushQueuedBreaks(ServerLevel level) {
        PhysicsDeferredSync.flushBreaks(level);
    }

    public static int pendingBreakCount() {
        return PENDING_BREAKS.size();
    }

    private record PendingStatsSync(Vector3d inheritVelocity) {
        PendingStatsSync(Vector3dc velocity) {
            this(velocity == null ? null : new Vector3d(velocity));
        }
    }

    private record PendingBreak(int ticksRemaining) {
    }
}

