/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  org.joml.Vector3d
 */
package dev.leo.treeoverrun.physics;

import dev.leo.treeoverrun.physics.PhysicsDeferredSync;
import dev.leo.treeoverrun.physics.TreeDebrisPlotHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

final class TreeSubLevelRemoval {
    private TreeSubLevelRemoval() {
    }

    static void safeRemove(ServerLevel level, ServerSubLevel subLevel) {
        if (subLevel.isRemoved()) {
            return;
        }
        PhysicsDeferredSync.cancel(subLevel.getUniqueId());
        TreeDebrisPlotHelper.unregister(subLevel);
        TreeSubLevelRemoval.wakeBeforeRemoval(level, subLevel);
        ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)level);
        if (container != null) {
            try {
                container.removeSubLevel((SubLevel)subLevel, SubLevelRemovalReason.REMOVED);
                return;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        subLevel.markRemoved();
    }

    private static void wakeBeforeRemoval(ServerLevel level, ServerSubLevel subLevel) {
        try {
            SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get((Level)level);
            if (physicsSystem == null) {
                return;
            }
            Vector3d position = subLevel.logicalPose().position();
            physicsSystem.wakeUpObjectsAt((int)Math.floor(position.x), (int)Math.floor(position.y), (int)Math.floor(position.z));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}

