/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.leo.treeoverrun.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class TreeDebrisCoreBlock
extends Block {
    public TreeDebrisCoreBlock() {
        super(BlockBehaviour.Properties.of().noCollission().noLootTable().noOcclusion().instabreak().replaceable());
    }

    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}

