/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback$CollisionResult
 *  dev.ryanhcode.sable.physics.callback.FragileBlockCallback
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  org.joml.Vector3d
 */
package dev.leo.treeoverrun.physics;

import dev.leo.treeoverrun.physics.TreeOverrunHandler;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.physics.callback.FragileBlockCallback;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;

public final class TreeLeafFragileCallback
extends FragileBlockCallback {
    public static final TreeLeafFragileCallback INSTANCE = new TreeLeafFragileCallback();

    private TreeLeafFragileCallback() {
    }

    public double getTriggerVelocity() {
        return 0.4;
    }

    public BlockSubLevelCollisionCallback.CollisionResult sable$onCollision(BlockPos pos, BlockPos pos2, Vector3d pos1, double impactVelocity) {
        ServerLevel level;
        SubLevelPhysicsSystem system = SubLevelPhysicsSystem.currentlySteppingSystem;
        if (system != null && TreeOverrunHandler.isTreeClaimedForUproot(level = system.getLevel(), pos)) {
            return TreeOverrunHandler.passThroughCollision();
        }
        return super.sable$onCollision(pos, pos2, pos1, impactVelocity);
    }
}

