/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback$CollisionResult
 *  dev.ryanhcode.sable.physics.callback.FragileBlockCallback
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.state.BlockState
 *  org.joml.Vector3d
 */
package dev.leo.treeoverrun.compat;

import dev.leo.treeoverrun.compat.TreePhysicsCompat;
import dev.leo.treeoverrun.physics.TreeLogCollisionCallback;
import dev.leo.treeoverrun.physics.TreeOverrunHandler;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.physics.callback.FragileBlockCallback;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

final class ChainedFragileBlockCallback
extends FragileBlockCallback {
    private final FragileBlockCallback primary;
    private final FragileBlockCallback secondary;

    private ChainedFragileBlockCallback(FragileBlockCallback primary, FragileBlockCallback secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    static ChainedFragileBlockCallback of(FragileBlockCallback primary, FragileBlockCallback secondary) {
        return new ChainedFragileBlockCallback(primary, secondary);
    }

    public double getTriggerVelocity() {
        return this.primary.getTriggerVelocity();
    }

    public boolean shouldTriggerFor(BlockState state) {
        return this.primary.shouldTriggerFor(state) || this.secondary.shouldTriggerFor(state);
    }

    public BlockSubLevelCollisionCallback.CollisionResult sable$onCollision(BlockPos pos, BlockPos pos2, Vector3d pos1, double impactVelocity) {
        ServerLevel level;
        ServerSubLevel debris;
        SubLevelPhysicsSystem physicsSystem;
        double previousImpactVelocity = TreeLogCollisionCallback.currentImpactVelocity;
        TreeLogCollisionCallback.currentImpactVelocity = impactVelocity;
        try {
        if (TreePhysicsCompat.isLoaded() && (physicsSystem = SubLevelPhysicsSystem.currentlySteppingSystem) != null && (debris = TreeOverrunHandler.resolveDebrisAt(level = physicsSystem.getLevel(), pos, physicsSystem)) != null) {
            TreePhysicsCompat.handleDebrisImpactCompat(level, debris, pos1, impactVelocity);
            return BlockSubLevelCollisionCallback.CollisionResult.NONE;
        }
        return super.sable$onCollision(pos, pos2, pos1, impactVelocity);
        }
        finally {
            TreeLogCollisionCallback.currentImpactVelocity = previousImpactVelocity;
        }
    }

    public BlockSubLevelCollisionCallback.CollisionResult onHit(ServerLevel level, BlockPos pos, BlockState state, Vector3d impactPos) {
        BlockSubLevelCollisionCallback.CollisionResult primaryResult = this.primary.onHit(level, pos, state, impactPos);
        SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.currentlySteppingSystem;
        if (physicsSystem != null && physicsSystem.getLevel() == level && TreeOverrunHandler.resolveDebrisAt(level, pos, physicsSystem) != null) {
            return primaryResult;
        }
        if (primaryResult != TreeOverrunHandler.passThroughCollision() && TreePhysicsCompat.impactSpeedAt(level, pos) >= 2.0) {
            this.secondary.onHit(level, pos, state, impactPos);
        }
        return primaryResult;
    }
}

