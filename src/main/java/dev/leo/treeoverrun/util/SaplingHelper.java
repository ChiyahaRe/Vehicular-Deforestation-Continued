/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 */
package dev.leo.treeoverrun.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class SaplingHelper {
    private static final int MAX_LEAF_LOOT_ROLLS = 64;
    private static final Map<Block, Optional<BlockState>> LEAF_SAPLING_CACHE = new ConcurrentHashMap<Block, Optional<BlockState>>();

    private SaplingHelper() {
    }

    @Nullable
    public static BlockState saplingFromLeafDrops(ServerLevel level, @Nullable BlockState leafState, BlockPos pos) {
        if (leafState == null) {
            return null;
        }
        Optional<BlockState> cached = LEAF_SAPLING_CACHE.get(leafState.getBlock());
        if (cached != null) {
            return cached.orElse(null);
        }
        BlockState found = SaplingHelper.rollLeafForSapling(level, leafState, pos);
        LEAF_SAPLING_CACHE.put(leafState.getBlock(), Optional.ofNullable(found));
        return found;
    }

    @Nullable
    private static BlockState rollLeafForSapling(ServerLevel level, BlockState leafState, BlockPos pos) {
        for (int roll = 0; roll < 64; ++roll) {
            List drops = Block.getDrops((BlockState)leafState, (ServerLevel)level, (BlockPos)pos, null);
            for (Object dropped : drops) {
            ItemStack stack = (ItemStack)dropped;
                BlockItem blockItem;
                Item item = stack.getItem();
                if (!(item instanceof BlockItem) || !(blockItem = (BlockItem)item).getBlock().defaultBlockState().is(BlockTags.SAPLINGS)) continue;
                return blockItem.getBlock().defaultBlockState();
            }
        }
        return null;
    }

    @Nullable
    public static BlockState saplingFor(@Nullable BlockState logState) {
        if (logState == null) {
            return null;
        }
        ResourceLocation logId = BuiltInRegistries.BLOCK.getKey(logState.getBlock());
        String base = SaplingHelper.baseWoodName(logId.getPath());
        if (base.isEmpty()) {
            return null;
        }
        Object saplingPath = switch (base) {
            case "mangrove" -> "mangrove_propagule";
            case "crimson", "warped" -> base + "_fungus";
            default -> base + "_sapling";
        };
        Block sapling = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath((String)logId.getNamespace(), (String)saplingPath)).orElse(null);
        if (sapling == null || sapling == Blocks.AIR) {
            return null;
        }
        return sapling.defaultBlockState();
    }

    private static String baseWoodName(String logPath) {
        String name = logPath;
        if (name.startsWith("stripped_")) {
            name = name.substring("stripped_".length());
        }
        for (String suffix : new String[]{"_log", "_wood", "_stem", "_hyphae"}) {
            if (!name.endsWith(suffix)) continue;
            return name.substring(0, name.length() - suffix.length());
        }
        return "";
    }
}

