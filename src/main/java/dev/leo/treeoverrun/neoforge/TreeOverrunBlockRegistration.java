/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredBlock
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  net.neoforged.neoforge.registries.DeferredRegister$Blocks
 */
package dev.leo.treeoverrun.neoforge;

import dev.leo.treeoverrun.block.TreeDebrisCoreBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TreeOverrunBlockRegistration {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks((String)"tree_overrun_sublevels");
    public static final DeferredBlock<TreeDebrisCoreBlock> TREE_DEBRIS_CORE = BLOCKS.register("tree_debris_core", TreeDebrisCoreBlock::new);

    private TreeOverrunBlockRegistration() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}

