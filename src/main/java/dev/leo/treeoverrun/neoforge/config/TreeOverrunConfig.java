/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.neoforged.fml.ModContainer
 *  net.neoforged.fml.config.IConfigSpec
 *  net.neoforged.fml.config.ModConfig$Type
 *  net.neoforged.fml.event.config.ModConfigEvent$Loading
 *  net.neoforged.fml.event.config.ModConfigEvent$Reloading
 *  net.neoforged.neoforge.common.ModConfigSpec
 *  net.neoforged.neoforge.common.ModConfigSpec$BooleanValue
 *  net.neoforged.neoforge.common.ModConfigSpec$Builder
 *  net.neoforged.neoforge.common.ModConfigSpec$DoubleValue
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 */
package dev.leo.treeoverrun.neoforge.config;

import dev.leo.treeoverrun.config.TreeOverrunSettings;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class TreeOverrunConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER.translation("tree_overrun_sublevels.configuration.enabled").define("enabled", true);
    public static final ModConfigSpec.DoubleValue MIN_IMPACT_SPEED;
    public static final ModConfigSpec.DoubleValue IMPACT_MASS_REFERENCE;
    public static final ModConfigSpec.IntValue COOLDOWN_TICKS;
    public static final ModConfigSpec.BooleanValue INHERIT_VEHICLE_VELOCITY;
    public static final ModConfigSpec.IntValue MIN_TREE_LEAVES;
    public static final ModConfigSpec.IntValue LEAVES_PER_LOG;
    public static final ModConfigSpec.IntValue MAX_TREE_BLOCKS;
    public static final ModConfigSpec.IntValue CHUNK_SIZE;
    public static final ModConfigSpec.IntValue LEAF_CHUNK_SIZE;
    public static final ModConfigSpec.IntValue DEBRIS_DECAY_CHANCE_DENOMINATOR;
    public static final ModConfigSpec.IntValue DEBRIS_DROP_CHANCE;
    public static final ModConfigSpec.BooleanValue DROP_TREES_AS_ITEMS;
    public static final ModConfigSpec.IntValue MAX_ASSEMBLY_CHUNKS_PER_TICK;
    public static final ModConfigSpec.IntValue MAX_ACTIVE_DEBRIS_SUBLEVELS;
    public static final ModConfigSpec.DoubleValue SAPLING_REPLANT_CHANCE;
    public static final ModConfigSpec SPEC;

    private TreeOverrunConfig() {
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, (IConfigSpec)SPEC);
    }

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            TreeOverrunConfig.apply();
        }
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            TreeOverrunConfig.apply();
        }
    }

    private static void apply() {
        TreeOverrunSettings.setEnabled((Boolean)ENABLED.get());
        TreeOverrunSettings.setMinImpactSpeed((Double)MIN_IMPACT_SPEED.get());
        TreeOverrunSettings.setImpactMassReference((Double)IMPACT_MASS_REFERENCE.get());
        TreeOverrunSettings.setCooldownTicks((Integer)COOLDOWN_TICKS.get());
        TreeOverrunSettings.setInheritVehicleVelocity((Boolean)INHERIT_VEHICLE_VELOCITY.get());
        TreeOverrunSettings.setMinTreeLeaves((Integer)MIN_TREE_LEAVES.get());
        TreeOverrunSettings.setLeavesPerLog((Integer)LEAVES_PER_LOG.get());
        TreeOverrunSettings.setMaxTreeBlocks((Integer)MAX_TREE_BLOCKS.get());
        TreeOverrunSettings.setChunkSize((Integer)CHUNK_SIZE.get());
        TreeOverrunSettings.setLeafChunkSize((Integer)LEAF_CHUNK_SIZE.get());
        TreeOverrunSettings.setDebrisDecayChanceDenominator((Integer)DEBRIS_DECAY_CHANCE_DENOMINATOR.get());
        TreeOverrunSettings.setDebrisDropChance((double)((Integer)DEBRIS_DROP_CHANCE.get()).intValue() / 100.0);
        TreeOverrunSettings.setDropTreesAsItems((Boolean)DROP_TREES_AS_ITEMS.get());
        TreeOverrunSettings.setMaxAssemblyChunksPerTick((Integer)MAX_ASSEMBLY_CHUNKS_PER_TICK.get());
        TreeOverrunSettings.setMaxActiveDebrisSubLevels((Integer)MAX_ACTIVE_DEBRIS_SUBLEVELS.get());
        TreeOverrunSettings.setSaplingReplantChance((Double)SAPLING_REPLANT_CHANCE.get());
    }

    static {
        BUILDER.translation("tree_overrun_sublevels.configuration.impact").push("impact");
        MIN_IMPACT_SPEED = BUILDER.translation("tree_overrun_sublevels.configuration.min_impact_speed").defineInRange("minImpactSpeed", 0.5, 0.1, 64.0);
        IMPACT_MASS_REFERENCE = BUILDER.translation("tree_overrun_sublevels.configuration.impact_mass_reference").defineInRange("impactMassReference", 32.0, 0.01, 4096.0);
        COOLDOWN_TICKS = BUILDER.translation("tree_overrun_sublevels.configuration.cooldown_ticks").defineInRange("cooldownTicks", 20, 0, 200);
        INHERIT_VEHICLE_VELOCITY = BUILDER.translation("tree_overrun_sublevels.configuration.inherit_vehicle_velocity").define("inheritVehicleVelocity", true);
        BUILDER.pop();
        BUILDER.translation("tree_overrun_sublevels.configuration.tree_detection").push("treeDetection");
        MIN_TREE_LEAVES = BUILDER.translation("tree_overrun_sublevels.configuration.min_tree_leaves").defineInRange("minTreeLeaves", 12, 0, 512);
        LEAVES_PER_LOG = BUILDER.translation("tree_overrun_sublevels.configuration.leaves_per_log").defineInRange("leavesPerLog", 16, 0, 256);
        MAX_TREE_BLOCKS = BUILDER.translation("tree_overrun_sublevels.configuration.max_tree_blocks").defineInRange("maxTreeBlocks", 4096, 16, 65536);
        BUILDER.pop();
        BUILDER.translation("tree_overrun_sublevels.configuration.debris").push("debris");
        CHUNK_SIZE = BUILDER.translation("tree_overrun_sublevels.configuration.chunk_size").defineInRange("chunkSize", 3, 1, 64);
        LEAF_CHUNK_SIZE = BUILDER.translation("tree_overrun_sublevels.configuration.leaf_chunk_size").defineInRange("leafChunkSize", 12, 1, 64);
        DEBRIS_DECAY_CHANCE_DENOMINATOR = BUILDER.translation("tree_overrun_sublevels.configuration.debris_decay_chance_denominator").defineInRange("debrisDecayChanceDenominator", 100, 1, 100000);
        DEBRIS_DROP_CHANCE = BUILDER.translation("tree_overrun_sublevels.configuration.debris_drop_chance").defineInRange("debrisDropChance", 100, 0, 100);
        BUILDER.pop();
        BUILDER.translation("tree_overrun_sublevels.configuration.performance").push("performance");
        DROP_TREES_AS_ITEMS = BUILDER.translation("tree_overrun_sublevels.configuration.drop_trees_as_items").define("dropTreesAsItems", false);
        MAX_ACTIVE_DEBRIS_SUBLEVELS = BUILDER.translation("tree_overrun_sublevels.configuration.max_active_debris_sublevels").defineInRange("maxActiveDebrisSubLevels", 200, 0, 4096);
        MAX_ASSEMBLY_CHUNKS_PER_TICK = BUILDER.translation("tree_overrun_sublevels.configuration.max_assembly_chunks_per_tick").defineInRange("maxAssemblyChunksPerTick", 4, 1, 64);
        BUILDER.pop();
        BUILDER.translation("tree_overrun_sublevels.configuration.world").push("world");
        SAPLING_REPLANT_CHANCE = BUILDER.translation("tree_overrun_sublevels.configuration.sapling_replant_chance").defineInRange("saplingReplantChance", 0.5, 0.0, 1.0);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}

