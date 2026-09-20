/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper
 *  dev.ryanhcode.sable.sublevel.plot.LevelPlot
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 */
package dev.leo.treeoverrun.physics;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class PlotBlockAccess {
    private PlotBlockAccess() {
    }

    public static BlockState getBlockState(LevelPlot plot, BlockPos plotPos) {
        LevelChunk chunk = plot.getChunk(plot.toLocal(new ChunkPos(plotPos)));
        if (chunk == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return chunk.getBlockState(plotPos);
    }

    public static boolean placeBlockWithNotify(ServerLevel level, LevelPlot plot, BlockPos plotPos, BlockState state) {
        if (!PlotBlockAccess.getBlockState(plot, plotPos).isAir()) {
            return false;
        }
        LevelChunk chunk = plot.getChunk(plot.toLocal(new ChunkPos(plotPos)));
        if (chunk == null) {
            return false;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        chunk.setBlockState(plotPos, state, true);
        SubLevelAssemblyHelper.markAndNotifyBlock((Level)level, (BlockPos)plotPos, (LevelChunk)chunk, (BlockState)air, (BlockState)state, (int)3, (int)512);
        level.sendBlockUpdated(plotPos, air, state, 3);
        return PlotBlockAccess.getBlockState(plot, plotPos).is(state.getBlock()) && level.getBlockState(plotPos).is(state.getBlock());
    }
}

