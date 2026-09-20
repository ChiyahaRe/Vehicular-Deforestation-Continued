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
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package dev.leo.treeoverrun.physics;

import dev.leo.treeoverrun.block.TreeOverrunBlocks;
import dev.leo.treeoverrun.config.TreeOverrunSettings;
import dev.leo.treeoverrun.physics.PlotBlockAccess;
import dev.leo.treeoverrun.physics.TreeOverrunHandler;
import dev.leo.treeoverrun.util.TreeBlockHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class TreeDebrisHelper {
    private TreeDebrisHelper() {
    }

    public static boolean mightBeTreeDebris(ServerSubLevel subLevel) {
        if (subLevel == null || subLevel.isRemoved()) {
            return false;
        }
        ServerLevelPlot plot = subLevel.getPlot();
        if (plot == null) {
            return false;
        }
        BoundingBox3ic bounds = plot.getBoundingBox();
        if (bounds == null || bounds == BoundingBox3i.EMPTY) {
            return false;
        }
        return TreeDebrisHelper.plotAabbCellCount(bounds) <= TreeDebrisHelper.maxPlotAabbCells();
    }

    public static boolean isTreeDebris(ServerSubLevel subLevel) {
        if (!TreeDebrisHelper.mightBeTreeDebris(subLevel)) {
            return false;
        }
        ServerLevelPlot plot = subLevel.getPlot();
        if (plot.getBlockEntityActors().iterator().hasNext()) {
            return false;
        }
        BoundingBox3ic bounds = plot.getBoundingBox();
        int maxBlocks = Math.max(TreeOverrunSettings.chunkSize(), TreeOverrunSettings.leafChunkSize());
        int nonAir = 0;
        for (int x = bounds.minX(); x <= bounds.maxX(); ++x) {
            for (int y = bounds.minY(); y <= bounds.maxY(); ++y) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); ++z) {
                    BlockState state = PlotBlockAccess.getBlockState((LevelPlot)plot, new BlockPos(x, y, z));
                    if (state.isAir() || TreeOverrunBlocks.isTreeDebrisCore(state)) continue;
                    if (!TreeBlockHelper.isTreeBlock(state)) {
                        return false;
                    }
                    if (++nonAir <= maxBlocks) continue;
                    return false;
                }
            }
        }
        return nonAir > 0;
    }

    public static void breakIntoItemsAndRemove(ServerLevel level, ServerSubLevel subLevel) {
        if (subLevel.isRemoved()) {
            return;
        }
        TreeDebrisHelper.dropPlotContentsAsItems(subLevel, level, level.random);
        TreeDebrisHelper.clearWorldTreeBlocksFromPlot(subLevel, level);
        TreeOverrunHandler.safeRemoveSubLevel(level, subLevel);
    }

    private static int maxPlotAabbCells() {
        int maxBlocks = Math.max(TreeOverrunSettings.chunkSize(), TreeOverrunSettings.leafChunkSize());
        return maxBlocks * 6;
    }

    private static int plotAabbCellCount(BoundingBox3ic bounds) {
        int dx = bounds.maxX() - bounds.minX() + 1;
        int dy = bounds.maxY() - bounds.minY() + 1;
        int dz = bounds.maxZ() - bounds.minZ() + 1;
        return dx * dy * dz;
    }

    private static DropResult dropPlotContentsAsItems(ServerSubLevel subLevel, ServerLevel level, RandomSource random) {
        ServerLevelPlot plot = subLevel.getPlot();
        BoundingBox3ic bounds = plot.getBoundingBox();
        if (bounds == null || bounds == BoundingBox3i.EMPTY) {
            return new DropResult(0, 0);
        }
        int blocks = 0;
        int drops = 0;
        for (int x = bounds.minX(); x <= bounds.maxX(); ++x) {
            for (int y = bounds.minY(); y <= bounds.maxY(); ++y) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); ++z) {
                    BlockPos plotPos = new BlockPos(x, y, z);
                    BlockState state = PlotBlockAccess.getBlockState((LevelPlot)plot, plotPos);
                    if (state.isAir() || TreeOverrunBlocks.isTreeDebrisCore(state)) continue;
                    ++blocks;
                    if (!TreeOverrunSettings.rollDebrisDrop(random)) continue;
                    Vec3 worldCenter = subLevel.logicalPose().transformPosition(Vec3.atCenterOf((Vec3i)plotPos));
                    Block.dropResources((BlockState)state, (Level)level, (BlockPos)BlockPos.containing((Position)worldCenter));
                    ++drops;
                }
            }
        }
        return new DropResult(blocks, drops);
    }

    private static void clearWorldTreeBlocksFromPlot(ServerSubLevel subLevel, ServerLevel level) {
        ServerLevelPlot plot = subLevel.getPlot();
        BoundingBox3ic bounds = plot.getBoundingBox();
        if (bounds == null || bounds == BoundingBox3i.EMPTY) {
            return;
        }
        for (int x = bounds.minX(); x <= bounds.maxX(); ++x) {
            for (int y = bounds.minY(); y <= bounds.maxY(); ++y) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); ++z) {
                    Vec3 worldCenter;
                    BlockPos worldPos;
                    BlockState current;
                    BlockPos plotPos = new BlockPos(x, y, z);
                    if (!TreeBlockHelper.isTreeBlock(PlotBlockAccess.getBlockState((LevelPlot)plot, plotPos)) || (current = level.getBlockState(worldPos = BlockPos.containing((Position)(worldCenter = subLevel.logicalPose().transformPosition(Vec3.atCenterOf((Vec3i)plotPos)))))).isAir() || !TreeBlockHelper.isTreeBlock(current)) continue;
                    level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private record DropResult(int blocks, int drops) {
    }
}

