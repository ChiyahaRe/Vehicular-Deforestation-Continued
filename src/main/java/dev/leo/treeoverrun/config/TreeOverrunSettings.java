/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.RandomSource
 */
package dev.leo.treeoverrun.config;

import net.minecraft.util.RandomSource;

public final class TreeOverrunSettings {
    public static final double SCAN_RADIUS = 3.5;
    public static final int WEAK_IMPACT_COOLDOWN_TICKS = 8;
    public static final double IMPACT_SPEED_MASS_EXPONENT = 0.5;
    public static final double MIN_EFFECTIVE_IMPACT_SPEED = 0.2;
    public static final int MIN_VERTICAL_LOG_SPAN = 2;
    public static final int MAX_LEAF_GATHER = 512;
    public static final int ASSEMBLY_START_DELAY_TICKS = 2;
    public static final int SUBLEVEL_CLIENT_WARMUP_TICKS = 0;
    public static final double LEAF_FRAGILE_TRIGGER_VELOCITY = 0.4;
    public static final double VEHICLE_MOMENTUM_PRESERVE = 0.65;
    private static boolean enabled = true;
    private static double minImpactSpeed = 0.5;
    private static double impactMassReference = 32.0;
    private static int maxTreeBlocks = 4096;
    private static int cooldownTicks = 20;
    private static int leavesPerLog = 16;
    private static int minTreeLeaves = 12;
    private static int debrisDecayChanceDenominator = 100;
    private static int maxAssemblyChunksPerTick = 4;
    private static boolean inheritVehicleVelocity = true;
    private static int chunkSize = 3;
    private static int leafChunkSize = 12;
    private static boolean dropTreesAsItems = false;
    private static double saplingReplantChance = 0.5;
    private static double debrisDropChance = 1.0;
    private static int maxActiveDebrisSubLevels = 0;

    private TreeOverrunSettings() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        TreeOverrunSettings.enabled = enabled;
    }

    public static double minImpactSpeed() {
        return minImpactSpeed;
    }

    public static void setMinImpactSpeed(double minImpactSpeed) {
        TreeOverrunSettings.minImpactSpeed = minImpactSpeed;
    }

    public static double impactMassReference() {
        return impactMassReference;
    }

    public static void setImpactMassReference(double impactMassReference) {
        TreeOverrunSettings.impactMassReference = Math.max(0.01, impactMassReference);
    }

    public static double requiredImpactSpeedForMass(double impactingMass) {
        double mass = Math.max(0.01, impactingMass);
        double reference = Math.max(0.01, impactMassReference);
        double scaled = minImpactSpeed * Math.pow(reference / mass, 0.5);
        return Math.max(0.2, scaled);
    }

    public static int maxTreeBlocks() {
        return maxTreeBlocks;
    }

    public static void setMaxTreeBlocks(int maxTreeBlocks) {
        TreeOverrunSettings.maxTreeBlocks = maxTreeBlocks;
    }

    public static int cooldownTicks() {
        return cooldownTicks;
    }

    public static void setCooldownTicks(int cooldownTicks) {
        TreeOverrunSettings.cooldownTicks = cooldownTicks;
    }

    public static int leavesPerLog() {
        return leavesPerLog;
    }

    public static void setLeavesPerLog(int leavesPerLog) {
        TreeOverrunSettings.leavesPerLog = leavesPerLog;
    }

    public static int minTreeLeaves() {
        return minTreeLeaves;
    }

    public static void setMinTreeLeaves(int minTreeLeaves) {
        TreeOverrunSettings.minTreeLeaves = minTreeLeaves;
    }

    public static int debrisDecayChanceDenominator() {
        return debrisDecayChanceDenominator;
    }

    public static void setDebrisDecayChanceDenominator(int debrisDecayChanceDenominator) {
        TreeOverrunSettings.debrisDecayChanceDenominator = Math.max(1, debrisDecayChanceDenominator);
    }

    public static int maxAssemblyChunksPerTick() {
        return maxAssemblyChunksPerTick;
    }

    public static void setMaxAssemblyChunksPerTick(int maxAssemblyChunksPerTick) {
        TreeOverrunSettings.maxAssemblyChunksPerTick = Math.max(1, maxAssemblyChunksPerTick);
    }

    public static boolean rollDebrisDecay(RandomSource random) {
        return random.nextInt(debrisDecayChanceDenominator) == 0;
    }

    public static boolean inheritVehicleVelocity() {
        return inheritVehicleVelocity;
    }

    public static void setInheritVehicleVelocity(boolean inheritVehicleVelocity) {
        TreeOverrunSettings.inheritVehicleVelocity = inheritVehicleVelocity;
    }

    public static int chunkSize() {
        return chunkSize;
    }

    public static void setChunkSize(int chunkSize) {
        TreeOverrunSettings.chunkSize = Math.max(1, chunkSize);
    }

    public static int leafChunkSize() {
        return leafChunkSize;
    }

    public static void setLeafChunkSize(int leafChunkSize) {
        TreeOverrunSettings.leafChunkSize = Math.max(1, leafChunkSize);
    }

    public static boolean dropTreesAsItems() {
        return dropTreesAsItems;
    }

    public static void setDropTreesAsItems(boolean dropTreesAsItems) {
        TreeOverrunSettings.dropTreesAsItems = dropTreesAsItems;
    }

    public static double saplingReplantChance() {
        return saplingReplantChance;
    }

    public static void setSaplingReplantChance(double saplingReplantChance) {
        TreeOverrunSettings.saplingReplantChance = Math.max(0.0, Math.min(1.0, saplingReplantChance));
    }

    public static double debrisDropChance() {
        return debrisDropChance;
    }

    public static void setDebrisDropChance(double debrisDropChance) {
        TreeOverrunSettings.debrisDropChance = Math.max(0.0, Math.min(1.0, debrisDropChance));
    }

    public static int maxActiveDebrisSubLevels() {
        return maxActiveDebrisSubLevels;
    }

    public static void setMaxActiveDebrisSubLevels(int maxActiveDebrisSubLevels) {
        TreeOverrunSettings.maxActiveDebrisSubLevels = Math.max(0, maxActiveDebrisSubLevels);
    }

    public static boolean rollDebrisDrop(RandomSource random) {
        return debrisDropChance >= 1.0 || random.nextDouble() < debrisDropChance;
    }
}

