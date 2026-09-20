package dev.leo.treeoverrun.physics;

import dev.leo.treeoverrun.config.TreeOverrunSettings;
import dev.leo.treeoverrun.util.TreeBlockHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.physics.callback.FragileBlockCallback;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public final class TreeLogCollisionCallback
extends FragileBlockCallback {
    public static final TreeLogCollisionCallback INSTANCE = new TreeLogCollisionCallback();

    // Sable gives sable$onCollision the impact speed and the block of the *other* body, but does
    // not pass either down to onHit. Stash them so onHit never has to ask the physics scene:
    // any call back into Rapier while Rapier3D.step() is running deadlocks the server thread.
    public static double currentImpactVelocity;
    public static BlockPos currentOtherBlockPos;

    private TreeLogCollisionCallback() {
    }

    public double getTriggerVelocity() {
        return 0.01;
    }

    public boolean shouldTriggerFor(BlockState state) {
        return TreeOverrunSettings.enabled() && TreeBlockHelper.isTreeAnchor(state);
    }

    public BlockSubLevelCollisionCallback.CollisionResult sable$onCollision(BlockPos pos, BlockPos otherHitBlockPos, Vector3d impactPos, double impactVelocity) {
        double previousVelocity = currentImpactVelocity;
        BlockPos previousOther = currentOtherBlockPos;
        currentImpactVelocity = impactVelocity;
        currentOtherBlockPos = otherHitBlockPos;
        try {
            return super.sable$onCollision(pos, otherHitBlockPos, impactPos, impactVelocity);
        }
        finally {
            currentImpactVelocity = previousVelocity;
            currentOtherBlockPos = previousOther;
        }
    }

    public BlockSubLevelCollisionCallback.CollisionResult onHit(ServerLevel level, BlockPos pos, BlockState state, Vector3d impactPos) {
        if (TreeOverrunHandler.isTreeClaimedForUproot(level, pos)) {
            return TreeOverrunHandler.passThroughCollision();
        }
        // Debris resolution without touching the physics scene: both of these read the plot grid
        // and the cached logical pose only.
        ServerSubLevel debrisAtPos = TreeDebrisPlotHelper.resolveSubLevel(level, pos);
        if (debrisAtPos != null && TreeOverrunHandler.isTreeDebrisSubLevel(level, debrisAtPos)) {
            return BlockSubLevelCollisionCallback.CollisionResult.NONE;
        }
        if (TreeDebrisPlotHelper.findActiveDebrisNear(level, pos) != null) {
            return BlockSubLevelCollisionCallback.CollisionResult.NONE;
        }
        // The colliding body is identified from the other hit block, which lands inside that
        // sub-level's plot. No broad-phase query and no velocity read needed.
        ServerSubLevel vehicle = TreeLogCollisionCallback.resolveImpactingSubLevel(level);
        if (vehicle == null) {
            return BlockSubLevelCollisionCallback.CollisionResult.NONE;
        }
        if (TreeOverrunHandler.isTreeDebrisSubLevel(level, vehicle)) {
            return BlockSubLevelCollisionCallback.CollisionResult.NONE;
        }
        // MassData.getMass() is a plain field read, so the mass-scaled threshold can be applied
        // here. Deciding it now matters: returning NONE lets the vehicle bounce off the trunk,
        // while passThrough makes it phase through, so the verdict cannot be deferred.
        double speed = currentImpactVelocity;
        MassData massTracker = vehicle.getMassTracker();
        double mass = massTracker == null ? 0.01 : Math.max(0.01, massTracker.getMass());
        if (speed < TreeOverrunSettings.requiredImpactSpeedForMass(mass)) {
            TreeOverrunHandler.markWeakImpact(level, pos);
            return BlockSubLevelCollisionCallback.CollisionResult.NONE;
        }
        if (!TreeOverrunHandler.canTriggerAt(level, pos)) {
            return BlockSubLevelCollisionCallback.CollisionResult.NONE;
        }
        // Only the inherited velocity is left to resolve, and that needs the rigid body, so it
        // happens after the physics step in TreeAssemblyQueue.processPendingImpacts.
        TreeOverrunHandler.enqueueTreeImpact(level, pos, speed, vehicle.getUniqueId());
        return TreeOverrunHandler.passThroughCollision();
    }

    private static ServerSubLevel resolveImpactingSubLevel(ServerLevel level) {
        BlockPos otherPos = currentOtherBlockPos;
        if (otherPos == null) {
            return null;
        }
        SubLevel containing = Sable.HELPER.getContaining((Level)level, (Vec3i)otherPos);
        if (containing instanceof ServerSubLevel) {
            ServerSubLevel serverSubLevel = (ServerSubLevel)containing;
            return serverSubLevel.isRemoved() ? null : serverSubLevel;
        }
        return null;
    }
}
