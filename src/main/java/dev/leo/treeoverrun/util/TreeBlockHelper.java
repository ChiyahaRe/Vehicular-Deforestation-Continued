/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.leo.treeoverrun.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class TreeBlockHelper {
    private static final TagKey<Block> TREE_ATTACHMENTS = TagKey.create((ResourceKey)Registries.BLOCK, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"tree_overrun_sublevels", (String)"tree_attachments"));

    private TreeBlockHelper() {
    }

    public static boolean isTreeBlock(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    public static boolean isLeaf(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    public static boolean isTreeAnchor(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    public static boolean isTreeAttachment(BlockState state) {
        return state.is(TREE_ATTACHMENTS) || state.is(BlockTags.BEEHIVES) || state.is(BlockTags.CLIMBABLE) || state.is(BlockTags.WOOL_CARPETS) || state.is(Blocks.MOSS_CARPET) || state.is(Blocks.COCOA) || state.is(Blocks.SNOW);
    }

    public static boolean canConnectTreeBlocks(BlockPos fromPos, BlockState fromState, BlockPos toPos, BlockState toState, Direction direction) {
        return fromState.is(BlockTags.LOGS) && toState.is(BlockTags.LOGS);
    }
}

