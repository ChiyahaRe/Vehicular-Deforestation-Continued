/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.fml.ModContainer
 *  net.neoforged.fml.ModList
 *  net.neoforged.fml.common.Mod
 *  net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
 *  net.neoforged.neoforge.common.NeoForge
 *  net.neoforged.neoforge.event.tick.LevelTickEvent$Post
 */
package dev.leo.treeoverrun.neoforge;

import dev.leo.treeoverrun.TreeOverrunSublevels;
import dev.leo.treeoverrun.block.TreeOverrunBlocks;
import dev.leo.treeoverrun.compat.TreePhysicsCompat;
import dev.leo.treeoverrun.neoforge.TreeOverrunBlockRegistration;
import dev.leo.treeoverrun.neoforge.config.TreeOverrunConfig;
import dev.leo.treeoverrun.physics.PhysicsDeferredSync;
import dev.leo.treeoverrun.physics.TreeDebrisPlotHelper;
import dev.leo.treeoverrun.physics.TreeOverrunHandler;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@Mod(value="tree_overrun_sublevels")
public final class TreeOverrunNeoForge {
    public TreeOverrunNeoForge(IEventBus modBus, ModContainer modContainer) {
        TreeOverrunBlockRegistration.register(modBus);
        modBus.addListener(TreeOverrunConfig::onLoad);
        modBus.addListener(TreeOverrunConfig::onReload);
        TreeOverrunConfig.register(modContainer);
        modBus.addListener(TreeOverrunNeoForge::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(TreeOverrunNeoForge::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            PhysicsDeferredSync.flush(serverLevel);
            TreeOverrunHandler.onServerLevelTick(serverLevel);
            PhysicsDeferredSync.flush(serverLevel);
            TreeDebrisPlotHelper.bonusRandomTicks(serverLevel);
            PhysicsDeferredSync.flushQueuedBreaks(serverLevel);
            SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get((Level)serverLevel);
            if (physicsSystem != null) {
                TreePhysicsCompat.tickDebrisImpacts(physicsSystem);
            }
        }
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            TreeOverrunBlocks.bindTreeDebrisCore((Block)TreeOverrunBlockRegistration.TREE_DEBRIS_CORE.get());
            TreePhysicsCompat.bindLoaderPresent(ModList.get().isLoaded("treephysics"));
            TreeOverrunSublevels.init();
        });
    }
}

