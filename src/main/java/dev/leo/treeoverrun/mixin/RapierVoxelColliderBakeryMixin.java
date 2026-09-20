/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback
 *  dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback
 *  dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderBakery
 *  net.minecraft.world.level.block.LeavesBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package dev.leo.treeoverrun.mixin;

import dev.leo.treeoverrun.compat.TreePhysicsCompat;
import dev.leo.treeoverrun.physics.TreeLeafFragileCallback;
import dev.leo.treeoverrun.physics.TreeLogCollisionCallback;
import dev.leo.treeoverrun.util.TreeBlockHelper;
import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderBakery;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={RapierVoxelColliderBakery.class}, remap=false)
public abstract class RapierVoxelColliderBakeryMixin {
    @Redirect(method={"buildPhysicsDataForBlock"}, at=@At(value="INVOKE", target="Ldev/ryanhcode/sable/api/block/BlockWithSubLevelCollisionCallback;sable$getCallback(Lnet/minecraft/world/level/block/state/BlockState;)Ldev/ryanhcode/sable/api/physics/callback/BlockSubLevelCollisionCallback;"), remap=false)
    private BlockSubLevelCollisionCallback treeoverrun$resolveCallback(BlockState childState) {
        if (childState.getBlock() instanceof LeavesBlock) {
            if (TreePhysicsCompat.isLoaded()) {
                return BlockWithSubLevelCollisionCallback.sable$getCallback((BlockState)childState);
            }
            return TreeLeafFragileCallback.INSTANCE;
        }
        if (TreeBlockHelper.isTreeAnchor(childState)) {
            return TreePhysicsCompat.chainedLogCallback((BlockSubLevelCollisionCallback)TreeLogCollisionCallback.INSTANCE);
        }
        return BlockWithSubLevelCollisionCallback.sable$getCallback((BlockState)childState);
    }
}

