/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.particle.ParticleEngine
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.HitResult
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.fml.IExtensionPoint
 *  net.neoforged.fml.ModContainer
 *  net.neoforged.fml.common.Mod
 *  net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions
 *  net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent
 *  net.neoforged.neoforge.client.gui.ConfigurationScreen
 *  net.neoforged.neoforge.client.gui.IConfigScreenFactory
 */
package dev.leo.treeoverrun.neoforge;

import dev.leo.treeoverrun.neoforge.TreeOverrunBlockRegistration;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value="tree_overrun_sublevels", dist={Dist.CLIENT})
public final class TreeOverrunNeoForgeClient {
    public TreeOverrunNeoForgeClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (IExtensionPoint)((IConfigScreenFactory)ConfigurationScreen::new));
        modBus.addListener(TreeOverrunNeoForgeClient::onRegisterClientExtensions);
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(new IClientBlockExtensions(){

            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
                return true;
            }

            public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
                return true;
            }
        }, new Block[]{(Block)TreeOverrunBlockRegistration.TREE_DEBRIS_CORE.get()});
    }
}

