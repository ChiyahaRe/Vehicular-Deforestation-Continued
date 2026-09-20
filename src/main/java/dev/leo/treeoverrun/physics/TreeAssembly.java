/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.leo.treeoverrun.physics;

import dev.leo.treeoverrun.config.TreeOverrunSettings;
import dev.leo.treeoverrun.util.TreeBlockHelper;
import dev.leo.treeoverrun.util.TreeLeafHelper;
import dev.ryanhcode.sable.Sable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class TreeAssembly {
    private static final int LEAF_GATHER_FLOOR = 64;

    private TreeAssembly() {
    }

    public static TreePlan computeTreePlan(ServerLevel level, BlockPos impactOrigin) {
        TreeLeafHelper.LeafOwnershipContext leafOwnership;
        if (!TreeOverrunSettings.enabled()) {
            return null;
        }
        BlockState impactState = level.getBlockState(impactOrigin);
        if (!TreeBlockHelper.isTreeAnchor(impactState)) {
            return null;
        }
        if (Sable.HELPER.getContaining((Level)level, (Vec3i)impactOrigin) != null) {
            return null;
        }
        Set<BlockPos> logBlocks = TreeAssembly.collectDiagonallyConnectedLogs(level, impactOrigin);
        if (logBlocks.isEmpty()) {
            return null;
        }
        if (!TreeAssembly.hasNaturalTreeShape(logBlocks)) {
            return null;
        }
        int assemblyLeafCap = Math.max(0, logBlocks.size() * TreeOverrunSettings.leavesPerLog());
        int gatherCap = Math.min(512, Math.max(assemblyLeafCap, 64));
        Set<BlockPos> allLeaves = TreeLeafHelper.collectConnectedLeaves((BlockGetter)level, logBlocks, impactOrigin, gatherCap, leafOwnership = TreeLeafHelper.buildOwnershipContext((BlockGetter)level, logBlocks));
        if (allLeaves.size() < TreeOverrunSettings.minTreeLeaves()) {
            return null;
        }
        HashSet<BlockPos> assemblyLeaves = new HashSet<BlockPos>();
        if (assemblyLeafCap > 0) {
            ArrayList<BlockPos> byProximity = new ArrayList<BlockPos>(allLeaves);
            byProximity.sort(Comparator.comparingInt(pos -> pos.distManhattan((Vec3i)impactOrigin)));
            for (BlockPos leaf : byProximity) {
                assemblyLeaves.add(leaf);
                if (assemblyLeaves.size() < assemblyLeafCap) continue;
                break;
            }
        }
        HashSet<BlockPos> decayLeaves = new HashSet<BlockPos>(allLeaves);
        decayLeaves.removeAll(assemblyLeaves);
        List<TreeBlock> blocks = TreeAssembly.snapshotTreeBlocks(level, logBlocks, assemblyLeaves, impactOrigin);
        if (blocks.isEmpty()) {
            return null;
        }
        Set<BlockPos> attachments = TreeAssembly.collectTreeAttachments(level, logBlocks, allLeaves);
        BlockPos basePos = TreeAssembly.findStumpPos(logBlocks, impactOrigin);
        BlockState baseLogState = basePos == null ? null : level.getBlockState(basePos);
        return new TreePlan(blocks, logBlocks.size(), assemblyLeaves.size(), decayLeaves, attachments, basePos, baseLogState);
    }

    private static Set<BlockPos> collectTreeAttachments(ServerLevel level, Set<BlockPos> logBlocks, Set<BlockPos> leaves) {
        HashSet<BlockPos> treeBlocks = new HashSet<BlockPos>(logBlocks);
        treeBlocks.addAll(leaves);
        HashSet<BlockPos> attachments = new HashSet<BlockPos>();
        for (BlockPos treePos : treeBlocks) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = treePos.relative(dir);
                if (treeBlocks.contains(neighbor) || attachments.contains(neighbor) || !TreeBlockHelper.isTreeAttachment(level.getBlockState(neighbor))) continue;
                attachments.add(neighbor);
            }
        }
        return attachments;
    }

    private static BlockPos findStumpPos(Set<BlockPos> logBlocks, BlockPos impactOrigin) {
        BlockPos best = null;
        long bestKey = Long.MAX_VALUE;
        for (BlockPos log : logBlocks) {
            int dx = log.getX() - impactOrigin.getX();
            int dz = log.getZ() - impactOrigin.getZ();
            long horizontalDistSq = (long)dx * (long)dx + (long)dz * (long)dz;
            long key = ((long)log.getY() << 32) + horizontalDistSq;
            if (key >= bestKey) continue;
            bestKey = key;
            best = log;
        }
        return best;
    }

    private static Set<BlockPos> collectDiagonallyConnectedLogs(ServerLevel level, BlockPos origin) {
        int maxBlocks = Math.max(1, TreeOverrunSettings.maxTreeBlocks());
        HashSet<BlockPos> visited = new HashSet<BlockPos>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<BlockPos>();
        HashSet<BlockPos> logs = new HashSet<BlockPos>();
        visited.add(origin);
        frontier.add(origin);
        while (!frontier.isEmpty()) {
            BlockPos current = (BlockPos)frontier.remove();
            if (!TreeBlockHelper.isTreeAnchor(level.getBlockState(current))) continue;
            logs.add(current);
            if (logs.size() > maxBlocks) {
                return Set.of();
            }
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dy = -1; dy <= 1; ++dy) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        BlockPos neighbor;
                        if (dx == 0 && dy == 0 && dz == 0 || !visited.add(neighbor = current.offset(dx, dy, dz)) || !TreeBlockHelper.isTreeAnchor(level.getBlockState(neighbor))) continue;
                        frontier.add(neighbor);
                    }
                }
            }
        }
        return logs;
    }

    private static List<TreeBlock> snapshotTreeBlocks(ServerLevel level, Set<BlockPos> logBlocks, Set<BlockPos> leafBlocks, BlockPos impactPos) {
        HashSet<BlockPos> allBlocks = new HashSet<BlockPos>(logBlocks);
        allBlocks.addAll(leafBlocks);
        ArrayList<BlockPos> sorted = new ArrayList<BlockPos>(allBlocks);
        sorted.sort(Comparator.comparingInt(pos -> pos.distManhattan((Vec3i)impactPos)));
        ArrayList<TreeBlock> blocks = new ArrayList<TreeBlock>();
        for (BlockPos pos2 : sorted) {
            BlockState state = level.getBlockState(pos2);
            if (!TreeBlockHelper.isTreeBlock(state)) continue;
            blocks.add(new TreeBlock(pos2, state));
        }
        return blocks;
    }

    private static boolean hasNaturalTreeShape(Set<BlockPos> logBlocks) {
        if (logBlocks.size() < 2) {
            return false;
        }
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BlockPos log : logBlocks) {
            minY = Math.min(minY, log.getY());
            maxY = Math.max(maxY, log.getY());
        }
        return maxY - minY >= 2;
    }

    public record TreePlan(List<TreeBlock> blocks, int logCount, int leafCount, Set<BlockPos> decayLeaves, Set<BlockPos> attachments, BlockPos basePos, BlockState baseLogState) {
    }

    public record TreeBlock(BlockPos pos, BlockState state) {
    }
}

