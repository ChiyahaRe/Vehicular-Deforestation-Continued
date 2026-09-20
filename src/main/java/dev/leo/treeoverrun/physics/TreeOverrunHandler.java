/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback$CollisionResult
 *  dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle
 *  dev.ryanhcode.sable.companion.math.BoundingBox3d
 *  dev.ryanhcode.sable.companion.math.BoundingBox3dc
 *  dev.ryanhcode.sable.companion.math.BoundingBox3i
 *  dev.ryanhcode.sable.companion.math.BoundingBox3ic
 *  dev.ryanhcode.sable.companion.math.JOMLConversion
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.leo.treeoverrun.physics;

import dev.leo.treeoverrun.config.TreeOverrunSettings;
import dev.leo.treeoverrun.physics.TreeAssembly;
import dev.leo.treeoverrun.physics.TreeAssemblyQueue;
import dev.leo.treeoverrun.physics.TreeDebrisHelper;
import dev.leo.treeoverrun.physics.TreeDebrisPlotHelper;
import dev.leo.treeoverrun.physics.TreeLeafDecayQueue;
import dev.leo.treeoverrun.physics.TreeSubLevelRemoval;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class TreeOverrunHandler {
    private static final BlockSubLevelCollisionCallback.CollisionResult PASS_THROUGH_COLLISION = new BlockSubLevelCollisionCallback.CollisionResult(JOMLConversion.ZERO, true);
    private static final Long2LongOpenHashMap COOLDOWNS = new Long2LongOpenHashMap();
    private static final Long2LongOpenHashMap WEAK_IMPACT_COOLDOWNS = new Long2LongOpenHashMap();
    private static final Long2LongOpenHashMap UPROOT_CLAIMS = new Long2LongOpenHashMap();
    private static final int EXPIRY_PRUNE_INTERVAL_TICKS = 200;
    private static long nextExpiryPruneTick = Long.MIN_VALUE;

    private TreeOverrunHandler() {
    }

    public static void onPostPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
    }

    public static void onServerLevelTick(ServerLevel level) {
        TreeOverrunHandler.pruneExpiredPositionState(level);
        TreeAssemblyQueue.process(level);
        TreeLeafDecayQueue.process(level);
    }

    private static void pruneExpiredPositionState(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (nextExpiryPruneTick != Long.MIN_VALUE && gameTime < nextExpiryPruneTick) {
            return;
        }
        nextExpiryPruneTick = gameTime + 200L;
        TreeOverrunHandler.removeExpired(COOLDOWNS, gameTime);
        TreeOverrunHandler.removeExpired(WEAK_IMPACT_COOLDOWNS, gameTime);
        TreeOverrunHandler.removeExpired(UPROOT_CLAIMS, gameTime);
    }

    private static void removeExpired(Long2LongOpenHashMap expiries, long gameTime) {
        expiries.long2LongEntrySet().removeIf(entry -> entry.getLongValue() <= gameTime);
    }

    public static void onPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
    }

    public static boolean canTriggerAt(ServerLevel level, BlockPos anchor) {
        long gameTime = level.getGameTime();
        long key = anchor.asLong();
        if (TreeOverrunHandler.isUnderActiveUproot(level, anchor)) {
            return false;
        }
        if (gameTime < WEAK_IMPACT_COOLDOWNS.getOrDefault(key, Long.MIN_VALUE)) {
            return false;
        }
        long cooldownUntil = COOLDOWNS.getOrDefault(key, Long.MIN_VALUE);
        if (gameTime < cooldownUntil) {
            return false;
        }
        COOLDOWNS.put(key, gameTime + (long)TreeOverrunSettings.cooldownTicks());
        return true;
    }

    public static boolean canCheckCollision(ServerLevel level, BlockPos anchor) {
        long key;
        long gameTime = level.getGameTime();
        return gameTime >= WEAK_IMPACT_COOLDOWNS.getOrDefault(key = anchor.asLong(), Long.MIN_VALUE) && gameTime >= COOLDOWNS.getOrDefault(key, Long.MIN_VALUE);
    }

    public static void markWeakImpact(ServerLevel level, BlockPos pos) {
        WEAK_IMPACT_COOLDOWNS.put(pos.asLong(), level.getGameTime() + 8L);
    }

    public static BlockSubLevelCollisionCallback.CollisionResult passThroughCollision() {
        return PASS_THROUGH_COLLISION;
    }

    public static void enqueueTreeImpact(ServerLevel level, BlockPos impactOrigin, Vector3dc vehicleImpactVelocity, UUID impactingVehicleId) {
        TreeAssemblyQueue.enqueueImpact(level, impactOrigin, vehicleImpactVelocity, vehicleImpactVelocity.length(), impactingVehicleId);
    }

    public static void enqueueTreeImpact(ServerLevel level, BlockPos impactOrigin, double impactSpeed, UUID impactingVehicleId) {
        TreeAssemblyQueue.enqueueImpact(level, impactOrigin, (Vector3dc)new Vector3d(), impactSpeed, impactingVehicleId);
    }

    static boolean isUnderActiveUproot(ServerLevel level, BlockPos pos) {
        return level.getGameTime() < UPROOT_CLAIMS.getOrDefault(pos.asLong(), Long.MIN_VALUE);
    }

    public static boolean isTreeClaimedForUproot(ServerLevel level, BlockPos pos) {
        return TreeOverrunHandler.isUnderActiveUproot(level, pos);
    }

    static void claimTreeBlocks(ServerLevel level, Iterable<TreeAssembly.TreeBlock> blocks) {
        long claimUntil = level.getGameTime() + (long)TreeOverrunHandler.uprootClaimDurationTicks();
        for (TreeAssembly.TreeBlock block : blocks) {
            UPROOT_CLAIMS.put(block.pos().asLong(), claimUntil);
        }
    }

    private static int uprootClaimDurationTicks() {
        return 440;
    }

    public static boolean isTreeDebrisSubLevel(ServerSubLevel subLevel) {
        return TreeOverrunHandler.isTreeDebrisSubLevel(null, subLevel);
    }

    public static boolean isTreeDebrisSubLevel(ServerLevel level, ServerSubLevel subLevel) {
        if (subLevel == null || subLevel.isRemoved()) {
            return false;
        }
        if (!TreeDebrisHelper.mightBeTreeDebris(subLevel)) {
            return false;
        }
        return TreeDebrisPlotHelper.isMarkedTreeDebris(subLevel);
    }

    public static void safeRemoveSubLevel(ServerLevel level, ServerSubLevel subLevel) {
        TreeSubLevelRemoval.safeRemove(level, subLevel);
    }

    public static ServerSubLevel findImpactingVehicle(SubLevelPhysicsSystem physicsSystem, BlockPos pos, double minSpeed) {
        if (SubLevelPhysicsSystem.IN_PHYSICS_STEP) {
            return null;
        }

        AABB box = new AABB(pos).inflate(3.5);
        BoundingBox3d query = new BoundingBox3d(box);
        for (SubLevel subLevel : physicsSystem.queryIntersecting((BoundingBox3dc)query)) {
            Vector3d velocity;
            ServerSubLevel serverSubLevel;
            RigidBodyHandle handle;
            if (!(subLevel instanceof ServerSubLevel) || (handle = physicsSystem.getPhysicsHandle(serverSubLevel = (ServerSubLevel)subLevel)) == null || !handle.isValid() || (velocity = handle.getLinearVelocity(new Vector3d())).lengthSquared() < minSpeed * minSpeed || !TreeOverrunHandler.looksLikeVehicle(serverSubLevel)) continue;
            return serverSubLevel;
        }
        return null;
    }

    public static ServerSubLevel findImpactingDebrisSubLevel(SubLevelPhysicsSystem physicsSystem, ServerLevel level, BlockPos pos, double minSpeed) {
        if (SubLevelPhysicsSystem.IN_PHYSICS_STEP) {
            return null;
        }

        AABB box = new AABB(pos).inflate(3.5);
        BoundingBox3d query = new BoundingBox3d(box);
        for (SubLevel subLevel : physicsSystem.queryIntersecting((BoundingBox3dc)query)) {
            Vector3d velocity;
            RigidBodyHandle handle;
            ServerSubLevel serverSubLevel;
            if (!(subLevel instanceof ServerSubLevel) || !TreeOverrunHandler.isTreeDebrisSubLevel(level, serverSubLevel = (ServerSubLevel)subLevel) || (handle = physicsSystem.getPhysicsHandle(serverSubLevel)) == null || !handle.isValid() || (velocity = handle.getLinearVelocity(new Vector3d())).lengthSquared() < minSpeed * minSpeed) continue;
            return serverSubLevel;
        }
        return null;
    }

    @Nullable
    public static ServerSubLevel resolveDebrisAt(ServerLevel level, BlockPos pos, SubLevelPhysicsSystem physicsSystem) {
        ServerSubLevel containedDebris;
        SubLevel containing = Sable.HELPER.getContaining((Level)level, (Vec3i)pos);
        if (containing instanceof ServerSubLevel && TreeOverrunHandler.isTreeDebrisSubLevel(level, containedDebris = (ServerSubLevel)containing)) {
            return containedDebris;
        }
        ServerSubLevel activeNearby = TreeDebrisPlotHelper.findActiveDebrisNear(level, pos);
        if (activeNearby != null) {
            return activeNearby;
        }
        return TreeOverrunHandler.findImpactingDebrisSubLevel(physicsSystem, level, pos, 0.0);
    }

    private static boolean looksLikeVehicle(ServerSubLevel subLevel) {
        ServerLevelPlot plot = subLevel.getPlot();
        BoundingBox3ic bounds = plot.getBoundingBox();
        if (bounds == null || bounds == BoundingBox3i.EMPTY) {
            return false;
        }
        int volume = (bounds.maxX() - bounds.minX() + 1) * (bounds.maxY() - bounds.minY() + 1) * (bounds.maxZ() - bounds.minZ() + 1);
        return volume >= 12 && volume <= 8192;
    }
}

