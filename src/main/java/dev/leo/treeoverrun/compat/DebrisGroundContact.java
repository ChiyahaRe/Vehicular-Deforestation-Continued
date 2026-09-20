/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.companion.math.BoundingBox3i
 *  dev.ryanhcode.sable.companion.math.BoundingBox3ic
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.plot.LevelPlot
 *  dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 */
package dev.leo.treeoverrun.compat;

import dev.leo.treeoverrun.physics.PlotBlockAccess;
import dev.leo.treeoverrun.util.TreeBlockHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

final class DebrisGroundContact {
    private DebrisGroundContact() {
    }

    @Nullable
    static Contact find(ServerLevel level, ServerSubLevel debris) {
        ServerLevelPlot plot = debris.getPlot();
        if (plot == null) {
            return null;
        }
        BoundingBox3ic bounds = plot.getBoundingBox();
        if (bounds == null || bounds == BoundingBox3i.EMPTY) {
            return null;
        }
        Contact best = null;
        double lowestWorldY = Double.MAX_VALUE;
        int stepX = Math.max(1, (bounds.maxX() - bounds.minX()) / 2);
        int stepZ = Math.max(1, (bounds.maxZ() - bounds.minZ()) / 2);
        for (int x = bounds.minX(); x <= bounds.maxX(); x += stepX) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z += stepZ) {
                for (int y = bounds.minY(); y <= bounds.maxY(); ++y) {
                    BlockPos plotPos = new BlockPos(x, y, z);
                    BlockState plotState = PlotBlockAccess.getBlockState((LevelPlot)plot, plotPos);
                    if (plotState.isAir() || !TreeBlockHelper.isTreeBlock(plotState)) continue;
                    Vec3 worldCenter = debris.logicalPose().transformPosition(Vec3.atCenterOf((Vec3i)plotPos));
                    BlockPos below = BlockPos.containing((double)worldCenter.x, (double)(worldCenter.y - 0.55), (double)worldCenter.z);
                    BlockState ground = level.getBlockState(below);
                    if (ground.isAir() || !ground.isFaceSturdy((BlockGetter)level, below, Direction.UP) || !(worldCenter.y < lowestWorldY)) continue;
                    lowestWorldY = worldCenter.y;
                    best = new Contact(new Vector3d(worldCenter.x, worldCenter.y, worldCenter.z), ground);
                }
            }
        }
        return best;
    }

    record Contact(Vector3d worldPoint, BlockState groundBlock) {
    }
}

