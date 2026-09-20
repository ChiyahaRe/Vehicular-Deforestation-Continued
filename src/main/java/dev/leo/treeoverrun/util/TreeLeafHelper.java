/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.LeavesBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.IntegerProperty
 *  net.minecraft.world.level.block.state.properties.Property
 */
package dev.leo.treeoverrun.util;

import dev.leo.treeoverrun.util.TreeBlockHelper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public final class TreeLeafHelper {
    private static final int FOREIGN_LOG_HORIZONTAL_RADIUS = 8;
    private static final int FOREIGN_LOG_VERTICAL_MIN = -3;
    private static final int FOREIGN_LOG_VERTICAL_MAX = 5;

    private TreeLeafHelper() {
    }

    public static LeafOwnershipContext buildOwnershipContext(BlockGetter level, Set<BlockPos> logBlocks) {
        if (logBlocks.isEmpty()) {
            return new LeafOwnershipContext(List.of());
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos log : logBlocks) {
            minX = Math.min(minX, log.getX());
            minY = Math.min(minY, log.getY());
            minZ = Math.min(minZ, log.getZ());
            maxX = Math.max(maxX, log.getX());
            maxY = Math.max(maxY, log.getY());
            maxZ = Math.max(maxZ, log.getZ());
        }
        minX -= 8;
        minY -= 3;
        minZ -= 8;
        maxX += 8;
        maxY += 5;
        maxZ += 8;
        ArrayList<BlockPos> foreignLogs = new ArrayList<BlockPos>();
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (logBlocks.contains(candidate) || !TreeBlockHelper.isTreeAnchor(level.getBlockState(candidate))) continue;
                    foreignLogs.add(candidate.immutable());
                }
            }
        }
        return new LeafOwnershipContext(foreignLogs);
    }

    public static Set<BlockPos> collectConnectedLeaves(BlockGetter level, Set<BlockPos> logBlocks, BlockPos anchor, int maxLeaves) {
        return TreeLeafHelper.collectConnectedLeaves(level, logBlocks, anchor, maxLeaves, TreeLeafHelper.buildOwnershipContext(level, logBlocks));
    }

    public static Set<BlockPos> collectConnectedLeaves(BlockGetter level, Set<BlockPos> logBlocks, BlockPos anchor, int maxLeaves, LeafOwnershipContext ownership) {
        if (maxLeaves <= 0 || logBlocks.isEmpty()) {
            return Set.of();
        }
        HashSet<BlockPos> visited = new HashSet<BlockPos>(logBlocks);
        ArrayDeque<BlockPos> frontier = new ArrayDeque<BlockPos>(logBlocks);
        HashSet<BlockPos> leaves = new HashSet<BlockPos>();
        while (!frontier.isEmpty() && leaves.size() < maxLeaves) {
            BlockPos prevPos = (BlockPos)frontier.remove();
            BlockState prevState = level.getBlockState(prevPos);
            int prevLeafDistance = TreeLeafHelper.hasDecayDistance(prevState) ? TreeLeafHelper.getLeafDistance(prevState) : 0;
            TreeLeafHelper.visitNeighbours(level, prevPos, visited, candidate -> {
                if (leaves.size() >= maxLeaves) {
                    return;
                }
                BlockState state = level.getBlockState(candidate);
                if (TreeLeafHelper.isPersistentLeaf(state)) {
                    return;
                }
                if (!TreeLeafHelper.isOwnedByLogCluster(candidate, logBlocks, ownership)) {
                    return;
                }
                if (TreeLeafHelper.isWithinNonDecayingLeafRadius(anchor, candidate, state) && visited.add((BlockPos)candidate)) {
                    leaves.add((BlockPos)candidate);
                    frontier.add((BlockPos)candidate);
                    return;
                }
                if (TreeLeafHelper.hasDecayDistance(state) && TreeLeafHelper.getLeafDistance(state) > prevLeafDistance && visited.add((BlockPos)candidate)) {
                    leaves.add((BlockPos)candidate);
                    frontier.add((BlockPos)candidate);
                }
            });
        }
        return leaves;
    }

    static boolean isOwnedByLogCluster(BlockPos leaf, Set<BlockPos> logBlocks, LeafOwnershipContext ownership) {
        double ownedDistSq = Double.MAX_VALUE;
        for (BlockPos log : logBlocks) {
            ownedDistSq = Math.min(ownedDistSq, leaf.distSqr((Vec3i)log));
        }
        double foreignDistSq = Double.MAX_VALUE;
        for (BlockPos foreignLog : ownership.foreignLogs()) {
            foreignDistSq = Math.min(foreignDistSq, leaf.distSqr((Vec3i)foreignLog));
        }
        return foreignDistSq == Double.MAX_VALUE || ownedDistSq <= foreignDistSq;
    }

    private static void visitNeighbours(BlockGetter level, BlockPos pos, Set<BlockPos> visited, Consumer<BlockPos> acceptor) {
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    BlockPos neighbor;
                    if (dx == 0 && dy == 0 && dz == 0 || visited.contains(neighbor = pos.offset(dx, dy, dz))) continue;
                    acceptor.accept(neighbor);
                }
            }
        }
    }

    private static boolean isPersistentLeaf(BlockState state) {
        return ((Boolean)state.getOptionalValue((Property)LeavesBlock.PERSISTENT).orElse(Boolean.FALSE)).booleanValue();
    }

    private static boolean isWithinNonDecayingLeafRadius(BlockPos anchor, BlockPos candidate, BlockState state) {
        int radius = TreeLeafHelper.nonDecayingLeafHorizontalRadius(state);
        if (radius < 0) {
            return false;
        }
        BlockPos offset = candidate.subtract((Vec3i)anchor);
        int horizontalDistance = Math.max(Math.abs(offset.getX()), Math.abs(offset.getZ()));
        return horizontalDistance <= radius && TreeBlockHelper.isLeaf(state);
    }

    private static int nonDecayingLeafHorizontalRadius(BlockState state) {
        if (state.is(Blocks.RED_MUSHROOM_BLOCK)) {
            return 2;
        }
        if (state.is(Blocks.BROWN_MUSHROOM_BLOCK)) {
            return 3;
        }
        if (state.is(BlockTags.WART_BLOCKS) || state.is(Blocks.WEEPING_VINES) || state.is(Blocks.WEEPING_VINES_PLANT)) {
            return 3;
        }
        return -1;
    }

    public static boolean hasDecayDistance(BlockState state) {
        if (!TreeBlockHelper.isLeaf(state)) {
            return false;
        }
        for (Property property : state.getProperties()) {
            if (!(property instanceof IntegerProperty) || !property.getName().equals("distance") || property == BlockStateProperties.STABILITY_DISTANCE) continue;
            return true;
        }
        return false;
    }

    public static int getLeafDistance(BlockState state) {
        IntegerProperty distanceProperty = LeavesBlock.DISTANCE;
        for (Property property : state.getProperties()) {
            if (!(property instanceof IntegerProperty)) continue;
            IntegerProperty integerProperty = (IntegerProperty)property;
            if (!property.getName().equals("distance")) continue;
            distanceProperty = integerProperty;
            break;
        }
        return (Integer)state.getValue((Property)distanceProperty);
    }

    public record LeafOwnershipContext(List<BlockPos> foreignLogs) {
    }
}

