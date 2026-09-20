/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 */
package dev.leo.treeoverrun.physics;

import dev.leo.treeoverrun.compat.TreePhysicsCompat;
import dev.leo.treeoverrun.config.TreeOverrunSettings;
import dev.leo.treeoverrun.physics.PhysicsDeferredSync;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class TreeDebrisPlotHelper {
    public static final String DEBRIS_USER_TAG = "tree_overrun_debris";
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> EXPIRING = ConcurrentHashMap.newKeySet();

    private TreeDebrisPlotHelper() {
    }

    public static boolean isMarkedTreeDebris(ServerSubLevel subLevel) {
        CompoundTag tag = subLevel.getUserDataTag();
        return tag != null && tag.getBoolean(DEBRIS_USER_TAG);
    }

    public static boolean isExpiring(ServerSubLevel subLevel) {
        return EXPIRING.contains(subLevel.getUniqueId());
    }

    private static void markExpiring(ServerSubLevel subLevel) {
        EXPIRING.add(subLevel.getUniqueId());
    }

    public static void unregister(ServerSubLevel subLevel) {
        UUID id = subLevel.getUniqueId();
        ACTIVE.remove(id);
        EXPIRING.remove(id);
        TreePhysicsCompat.clearDebrisCompatState(id);
    }

    public static int activeCount() {
        return ACTIVE.size();
    }

    public static void forEachActiveDebris(ServerLevel level, Consumer<ServerSubLevel> action) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(container instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer serverContainer = container;
        for (UUID id : new ArrayList<UUID>(ACTIVE)) {
            SubLevel subLevel = serverContainer.getSubLevel(id);
            if (!(subLevel instanceof ServerSubLevel)) continue;
            ServerSubLevel serverSubLevel = (ServerSubLevel)subLevel;
            action.accept(serverSubLevel);
        }
    }

    @Nullable
    public static ServerSubLevel findActiveDebrisNear(ServerLevel level, BlockPos worldPos) {
        ServerSubLevel containing = TreeDebrisPlotHelper.resolveSubLevel(level, worldPos);
        if (containing != null && TreeDebrisPlotHelper.isMarkedTreeDebris(containing)) {
            return containing;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(container instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer serverContainer = container;
        ServerSubLevel nearest = null;
        double nearestDistanceSq = 9.0;
        for (UUID id : ACTIVE) {
            double dz;
            double dy;
            ServerSubLevel serverSubLevel;
            SubLevel subLevel = serverContainer.getSubLevel(id);
            if (!(subLevel instanceof ServerSubLevel) || (serverSubLevel = (ServerSubLevel)subLevel).isRemoved() || !TreeDebrisPlotHelper.isMarkedTreeDebris(serverSubLevel)) continue;
            Vector3d center = serverSubLevel.logicalPose().position();
            double dx = center.x - ((double)worldPos.getX() + 0.5);
            double distanceSq = dx * dx + (dy = center.y - ((double)worldPos.getY() + 0.5)) * dy + (dz = center.z - ((double)worldPos.getZ() + 0.5)) * dz;
            if (!(distanceSq < nearestDistanceSq)) continue;
            nearestDistanceSq = distanceSq;
            nearest = serverSubLevel;
        }
        return nearest;
    }

    @Nullable
    public static ServerSubLevel resolveSubLevel(ServerLevel level, BlockPos worldPos) {
        ServerSubLevel serverSubLevel;
        SubLevel subLevel = Sable.HELPER.getContaining((Level)level, (Vec3i)worldPos);
        if (subLevel instanceof ServerSubLevel && !(serverSubLevel = (ServerSubLevel)subLevel).isRemoved()) {
            return serverSubLevel;
        }
        return null;
    }

    public static void registerTreeDebris(ServerSubLevel subLevel) {
        CompoundTag tag = subLevel.getUserDataTag();
        if (tag == null) {
            tag = new CompoundTag();
        }
        tag.putBoolean(DEBRIS_USER_TAG, true);
        subLevel.setUserDataTag(tag);
        ACTIVE.add(subLevel.getUniqueId());
        EXPIRING.remove(subLevel.getUniqueId());
    }

    public static void bonusRandomTicks(ServerLevel level) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(container instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer serverContainer = container;
        TreeDebrisPlotHelper.rehydrateActiveDebris(serverContainer);
        if (ACTIVE.isEmpty()) {
            return;
        }
        for (UUID id : new ArrayList<UUID>(ACTIVE)) {
            SubLevel subLevel = serverContainer.getSubLevel(id);
            if (!(subLevel instanceof ServerSubLevel)) continue;
            ServerSubLevel serverSubLevel = (ServerSubLevel)subLevel;
            if (serverSubLevel.isRemoved() || !TreeDebrisPlotHelper.isMarkedTreeDebris(serverSubLevel)) {
                ACTIVE.remove(id);
                EXPIRING.remove(id);
                continue;
            }
            if (TreeDebrisPlotHelper.isExpiring(serverSubLevel)) {
                PhysicsDeferredSync.queueBreak(serverSubLevel);
                continue;
            }
            if (!TreeOverrunSettings.rollDebrisDecay(level.getRandom())) continue;
            TreeDebrisPlotHelper.markExpiring(serverSubLevel);
            PhysicsDeferredSync.queueBreak(serverSubLevel);
        }
    }

    private static int rehydrateActiveDebris(ServerSubLevelContainer container) {
        int rehydrated = 0;
        for (ServerSubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel == null || subLevel.isRemoved() || !TreeDebrisPlotHelper.isMarkedTreeDebris(subLevel) || !ACTIVE.add(subLevel.getUniqueId())) continue;
            ++rehydrated;
        }
        return rehydrated;
    }
}

