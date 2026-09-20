/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 */
package dev.leo.treeoverrun.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class TreeOverrunBlocks {
    public static final ResourceLocation TREE_DEBRIS_CORE_ID = ResourceLocation.fromNamespaceAndPath((String)"tree_overrun_sublevels", (String)"tree_debris_core");
    @Nullable
    private static Block treeDebrisCore;

    private TreeOverrunBlocks() {
    }

    public static void bindTreeDebrisCore(Block block) {
        treeDebrisCore = block;
    }

    public static Block treeDebrisCore() {
        if (treeDebrisCore != null) {
            return treeDebrisCore;
        }
        return (Block)BuiltInRegistries.BLOCK.get(TREE_DEBRIS_CORE_ID);
    }

    public static boolean isTreeDebrisCore(BlockState state) {
        return state.is(TreeOverrunBlocks.treeDebrisCore());
    }
}

