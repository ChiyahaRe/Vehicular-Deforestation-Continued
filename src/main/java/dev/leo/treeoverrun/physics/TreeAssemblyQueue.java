/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper
 *  dev.ryanhcode.sable.api.physics.mass.MassData
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.companion.math.BoundingBox3i
 *  dev.ryanhcode.sable.companion.math.BoundingBox3ic
 *  dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.leo.treeoverrun.physics;

import dev.leo.treeoverrun.TreeOverrunSublevels;
import dev.leo.treeoverrun.config.TreeOverrunSettings;
import dev.leo.treeoverrun.physics.PhysicsDeferredSync;
import dev.leo.treeoverrun.physics.TreeAssembly;
import dev.leo.treeoverrun.physics.TreeLeafDecayQueue;
import dev.leo.treeoverrun.physics.TreeOverrunHandler;
import dev.leo.treeoverrun.physics.TreeSubLevelRemoval;
import dev.leo.treeoverrun.util.SaplingHelper;
import dev.leo.treeoverrun.util.TreeBlockHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

final class TreeAssemblyQueue {
    private static final List<PendingTreeImpact> PENDING_IMPACTS = new ArrayList<PendingTreeImpact>();
    private static final List<PendingTreeAssembly> PENDING_TREES = new ArrayList<PendingTreeAssembly>();

    private TreeAssemblyQueue() {
    }

    static void enqueueImpact(ServerLevel level, BlockPos impactOrigin, Vector3dc vehicleImpactVelocity, double impactSpeed, UUID impactingVehicleId) {
        if (TreeOverrunHandler.isUnderActiveUproot(level, impactOrigin)) {
            return;
        }
        for (PendingTreeImpact queued : PENDING_IMPACTS) {
            if (queued.level() != level || !(queued.impactOrigin().distSqr((Vec3i)impactOrigin) <= 16.0)) continue;
            return;
        }
        PENDING_IMPACTS.add(new PendingTreeImpact(level, impactOrigin, new Vector3d(vehicleImpactVelocity), impactSpeed, impactingVehicleId));
    }

    static void process(ServerLevel level) {
        TreeAssemblyQueue.processPendingImpacts(level);
        TreeAssemblyQueue.assemblePendingTrees(level);
    }

    private static void processPendingImpacts(ServerLevel level) {
        int impactsProcessed = 0;
        boolean maxImpactsPerTick = true;
        Iterator<PendingTreeImpact> it = PENDING_IMPACTS.iterator();
        while (it.hasNext() && impactsProcessed < 1) {
            PendingTreeImpact pending = it.next();
            if (pending.level() != level) {
                it.remove();
                continue;
            }
            Vector3d vehicleVelocity = TreeAssemblyQueue.resolveImpactingVehicle(level, pending);
            if (vehicleVelocity == null) {
                it.remove();
                continue;
            }
            TreeAssembly.TreePlan plan = TreeAssembly.computeTreePlan(level, pending.impactOrigin());
            if (plan == null || plan.blocks().isEmpty() || TreeAssemblyQueue.planOverlapsActiveUproot(level, plan.blocks()) || TreeAssemblyQueue.overlapsPendingAssembly(level, plan.blocks())) {
                it.remove();
                continue;
            }
            TreeOverrunHandler.claimTreeBlocks(level, plan.blocks());
            TreeAssemblyQueue.playInitialTreeBreakSound(level, pending.impactOrigin(), plan.blocks());
            TreeAssemblyQueue.breakTreeAttachments(level, plan.attachments());
            BlockState replantSapling = TreeAssemblyQueue.rollReplantSapling(level, plan);
            if (TreeOverrunSettings.dropTreesAsItems() || TreeAssemblyQueue.shouldDropAsItems(level)) {
                TreeAssemblyQueue.forceClearWorldTreeBlocks(level, plan.blocks().stream().map(TreeAssembly.TreeBlock::pos).toList());
                TreeAssemblyQueue.dropTreeBlocksAsItems(level, plan.blocks());
                TreeLeafDecayQueue.scheduleOwnedLeafDecay(level, plan.decayLeaves());
                TreeAssemblyQueue.plantSaplingIfPossible(level, plan.basePos(), replantSapling);
            } else {
                TreeAssemblyQueue.enqueueWholeTree(level, plan.blocks(), plan.decayLeaves(), (Vector3dc)vehicleVelocity, plan.basePos(), replantSapling);
            }
            it.remove();
            ++impactsProcessed;
        }
    }

    /**
     * Runs on the server thread after the physics step, so reading the rigid body is safe here.
     * Returns the velocity the debris should inherit, or null when the impact must be discarded.
     */
    private static Vector3d resolveImpactingVehicle(ServerLevel level, PendingTreeImpact pending) {
        Vector3d recorded = pending.vehicleImpactVelocity();
        if (recorded.lengthSquared() > 0.0) {
            return recorded;
        }
        UUID vehicleId = pending.impactingVehicleId();
        if (vehicleId == null) {
            return null;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)level);
        if (container == null) {
            return new Vector3d();
        }
        SubLevel subLevel = container.getSubLevel(vehicleId);
        if (!(subLevel instanceof ServerSubLevel)) {
            return new Vector3d();
        }
        ServerSubLevel vehicle = (ServerSubLevel)subLevel;
        if (vehicle.isRemoved()) {
            return new Vector3d();
        }
        if (!TreeOverrunSettings.inheritVehicleVelocity()) {
            return new Vector3d();
        }
        SubLevelPhysicsSystem system = SubLevelPhysicsSystem.get((Level)level);
        if (system == null) {
            return new Vector3d();
        }
        RigidBodyHandle handle = system.getPhysicsHandle(vehicle);
        if (handle == null || !handle.isValid()) {
            return new Vector3d();
        }
        return handle.getLinearVelocity(new Vector3d());
    }

    private static boolean planOverlapsActiveUproot(ServerLevel level, List<TreeAssembly.TreeBlock> blocks) {
        for (TreeAssembly.TreeBlock block : blocks) {
            if (!TreeOverrunHandler.isUnderActiveUproot(level, block.pos())) continue;
            return true;
        }
        return false;
    }

    private static boolean overlapsPendingAssembly(ServerLevel level, List<TreeAssembly.TreeBlock> blocks) {
        for (PendingTreeAssembly pending : PENDING_TREES) {
            if (pending.level != level) continue;
            for (TreeAssembly.TreeBlock block : blocks) {
                if (!pending.blockPositions.contains(block.pos())) continue;
                return true;
            }
        }
        return false;
    }

    private static boolean shouldDropAsItems(ServerLevel level) {
        int limit = TreeOverrunSettings.maxActiveDebrisSubLevels();
        if (limit <= 0) {
            return false;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)level);
        return container != null && container.getLoadedCount() >= limit;
    }

    private static void playInitialTreeBreakSound(ServerLevel level, BlockPos impactOrigin, List<TreeAssembly.TreeBlock> planBlocks) {
        if (planBlocks.isEmpty()) {
            return;
        }
        BlockState soundState = level.getBlockState(impactOrigin);
        if (!TreeBlockHelper.isTreeAnchor(soundState)) {
            soundState = planBlocks.get(0).state();
        }
        SoundEvent sound = soundState.getSoundType().getBreakSound();
        float pitch = 0.9f + level.getRandom().nextFloat() * 0.2f;
        level.playSound(null, impactOrigin, sound, SoundSource.BLOCKS, 1.0f, pitch);
    }

    private static void enqueueWholeTree(ServerLevel level, List<TreeAssembly.TreeBlock> blocks, Set<BlockPos> decayLeaves, Vector3dc vehicleImpactVelocity, BlockPos plantBasePos, BlockState plantSaplingState) {
        if (blocks.isEmpty()) {
            return;
        }
        long readyAt = level.getGameTime() + 2L;
        PENDING_TREES.add(new PendingTreeAssembly(level, List.copyOf(blocks), Set.copyOf(decayLeaves), new Vector3d(vehicleImpactVelocity), readyAt, plantBasePos, plantSaplingState));
    }

    private static void assemblePendingTrees(ServerLevel level) {
        long gameTime = level.getGameTime();
        int chunksAssembledThisTick = 0;
        int maxChunksPerTick = TreeOverrunSettings.maxAssemblyChunksPerTick();
        Iterator<PendingTreeAssembly> iterator = PENDING_TREES.iterator();
        while (iterator.hasNext()) {
            PendingTreeAssembly pending = iterator.next();
            if (pending.level != level || gameTime < pending.readyAtTick) continue;
            if (!pending.prepareSnapshots()) {
                iterator.remove();
                continue;
            }
            if (chunksAssembledThisTick >= maxChunksPerTick) continue;
            int endClusterIndex = Math.min(pending.nextClusterIndex + (maxChunksPerTick - chunksAssembledThisTick), pending.clusters.size());
            TreeAssemblyQueue.restoreWorldTreeBlocks(level, TreeAssemblyQueue.clustersToPositions(pending.clusters, pending.nextClusterIndex, endClusterIndex), pending.snapshots);
            while (pending.nextClusterIndex < endClusterIndex && chunksAssembledThisTick < maxChunksPerTick) {
                TreeAssemblyQueue.processAssemblyCluster(level, pending);
                ++chunksAssembledThisTick;
            }
            if (pending.nextClusterIndex < pending.clusters.size()) continue;
            TreeAssemblyQueue.forceClearWorldTreeBlocks(level, pending.snapshots.keySet());
            TreeAssemblyQueue.plantSaplingIfPossible(level, pending.plantBasePos, pending.plantSaplingState);
            if (pending.createdChunks > 0 || pending.droppedLeafClusters > 0) {
                TreeLeafDecayQueue.scheduleOwnedLeafDecay(level, pending.decayLeaves);
            }
            iterator.remove();
        }
    }

    private static boolean clusterHasMass(ServerLevel level, Set<BlockPos> cluster, Map<BlockPos, BlockState> snapshots) {
        for (BlockPos pos : cluster) {
            BlockState state = snapshots.get(pos);
            if (state == null || state.isAir() || !(PhysicsBlockPropertyHelper.getMass((BlockGetter)level, (BlockPos)pos, (BlockState)state) > 0.0)) continue;
            return true;
        }
        return false;
    }

    private static void processAssemblyCluster(ServerLevel level, PendingTreeAssembly pending) {
        Set<BlockPos> cluster;
        if (TreeAssemblyQueue.clusterHasOnlyLeaves(cluster = pending.clusters.get(pending.nextClusterIndex++), pending.snapshots) && level.getRandom().nextBoolean()) {
            TreeAssemblyQueue.forceClearWorldTreeBlocks(level, cluster);
            ++pending.droppedLeafClusters;
            return;
        }
        if (TreeAssemblyQueue.clusterHasMass(level, cluster, pending.snapshots)) {
            if (TreeAssemblyQueue.assembleChunk(level, cluster, pending.snapshots, (Vector3dc)pending.vehicleImpactVelocity)) {
                ++pending.createdChunks;
            }
        } else {
            TreeAssemblyQueue.dropClusterDirectly(level, cluster, pending.snapshots);
            ++pending.droppedLeafClusters;
        }
    }

    private static boolean clusterHasOnlyLeaves(Set<BlockPos> cluster, Map<BlockPos, BlockState> snapshots) {
        if (cluster.isEmpty()) {
            return false;
        }
        for (BlockPos pos : cluster) {
            BlockState state = snapshots.get(pos);
            if (state != null && TreeBlockHelper.isLeaf(state)) continue;
            return false;
        }
        return true;
    }

    private static Set<BlockPos> clustersToPositions(List<Set<BlockPos>> clusters, int fromIndex, int toIndexExclusive) {
        HashSet<BlockPos> positions = new HashSet<BlockPos>();
        for (int i = fromIndex; i < toIndexExclusive; ++i) {
            positions.addAll((Collection<BlockPos>)clusters.get(i));
        }
        return positions;
    }

    private static void restoreWorldTreeBlocks(ServerLevel level, Iterable<BlockPos> positions, Map<BlockPos, BlockState> snapshots) {
        for (BlockPos pos : positions) {
            BlockState state = snapshots.get(pos);
            if (state == null || !TreeBlockHelper.isTreeBlock(state)) continue;
            level.setBlock(pos, state, 3);
        }
    }

    private static boolean assembleChunk(ServerLevel level, Set<BlockPos> cluster, Map<BlockPos, BlockState> snapshots, Vector3dc vehicleVelocity) {
        if (cluster.isEmpty()) {
            return false;
        }
        BlockPos anchor = cluster.iterator().next();
        ServerSubLevel chunkSubLevel = SubLevelAssemblyHelper.assembleBlocks((ServerLevel)level, (BlockPos)anchor, cluster, (BoundingBox3ic)BoundingBox3i.from(cluster));
        if (chunkSubLevel == null) {
            TreeAssemblyQueue.forceClearWorldTreeBlocks(level, cluster);
            return false;
        }
        BlockPos plotAnchor = chunkSubLevel.getPlot().getCenterBlock();
        int dx = plotAnchor.getX() - anchor.getX();
        int dy = plotAnchor.getY() - anchor.getY();
        int dz = plotAnchor.getZ() - anchor.getZ();
        ArrayList<PlotBlockSnapshot> plotBlocks = new ArrayList<PlotBlockSnapshot>(cluster.size());
        for (BlockPos pos : cluster) {
            BlockState state = snapshots.get(pos);
            if (state == null || state.isAir()) continue;
            BlockPos plotPos = pos.offset(dx, dy, dz);
            plotBlocks.add(new PlotBlockSnapshot(plotPos, state));
        }
        if (plotBlocks.isEmpty()) {
            TreeSubLevelRemoval.safeRemove(level, chunkSubLevel);
            return false;
        }
        if (!TreeAssemblyQueue.hasFiniteCenterOfMass(chunkSubLevel)) {
            TreeOverrunSublevels.LOGGER.warn("Chunk sub-level {} at {} has no usable mass after assembly; dropping its {} blocks directly", new Object[]{chunkSubLevel.getUniqueId(), anchor, cluster.size()});
            TreeAssemblyQueue.dropPlotBlocksAsItems(chunkSubLevel, level, plotBlocks);
            TreeAssemblyQueue.forceClearWorldTreeBlocks(level, cluster);
            TreeSubLevelRemoval.safeRemove(level, chunkSubLevel);
            return false;
        }
        PhysicsDeferredSync.queueStatsSync(chunkSubLevel, vehicleVelocity);
        return true;
    }

    private static boolean hasFiniteCenterOfMass(ServerSubLevel subLevel) {
        MassData massTracker = subLevel.getMassTracker();
        if (massTracker == null) {
            return false;
        }
        Vector3dc com = massTracker.getCenterOfMass();
        if (com == null) {
            return false;
        }
        return Double.isFinite(com.x()) && Double.isFinite(com.y()) && Double.isFinite(com.z());
    }

    private static void breakTreeAttachments(ServerLevel level, Set<BlockPos> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (BlockPos pos : attachments) {
            BlockState state = level.getBlockState(pos);
            if (!TreeBlockHelper.isTreeAttachment(state)) continue;
            Block.dropResources((BlockState)state, (Level)level, (BlockPos)pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void dropTreeBlocksAsItems(ServerLevel level, List<TreeAssembly.TreeBlock> blocks) {
        for (TreeAssembly.TreeBlock block : blocks) {
            BlockState state = block.state();
            if (state.isAir()) continue;
            Block.dropResources((BlockState)state, (Level)level, (BlockPos)block.pos());
        }
    }

    private static BlockState rollReplantSapling(ServerLevel level, TreeAssembly.TreePlan plan) {
        double chance = TreeOverrunSettings.saplingReplantChance();
        if (chance <= 0.0 || plan.basePos() == null) {
            return null;
        }
        if (level.getRandom().nextDouble() >= chance) {
            return null;
        }
        BlockState leafState = TreeAssemblyQueue.firstLeafState(plan);
        BlockState fromLeaves = SaplingHelper.saplingFromLeafDrops(level, leafState, plan.basePos());
        if (fromLeaves != null) {
            return fromLeaves;
        }
        return SaplingHelper.saplingFor(plan.baseLogState());
    }

    private static BlockState firstLeafState(TreeAssembly.TreePlan plan) {
        for (TreeAssembly.TreeBlock block : plan.blocks()) {
            if (!TreeBlockHelper.isLeaf(block.state())) continue;
            return block.state();
        }
        return null;
    }

    private static void plantSaplingIfPossible(ServerLevel level, BlockPos basePos, BlockState saplingState) {
        if (basePos == null || saplingState == null) {
            return;
        }
        if (!level.getBlockState(basePos).isAir()) {
            return;
        }
        if (!saplingState.canSurvive((LevelReader)level, basePos)) {
            return;
        }
        level.setBlock(basePos, saplingState, 3);
    }

    private static void dropClusterDirectly(ServerLevel level, Set<BlockPos> cluster, Map<BlockPos, BlockState> snapshots) {
        for (BlockPos pos : cluster) {
            BlockState state = snapshots.get(pos);
            if (state == null || state.isAir()) continue;
            Block.dropResources((BlockState)state, (Level)level, (BlockPos)pos);
            TreeAssemblyQueue.forceClearWorldTreeBlocks(level, List.of(pos));
        }
    }

    private static List<Set<BlockPos>> partitionIntoConnectedClusters(Map<BlockPos, BlockState> snapshots, int logChunkSize, int leafChunkSize) {
        HashSet<BlockPos> remaining = new HashSet<BlockPos>(snapshots.keySet());
        ArrayList<Set<BlockPos>> clusters = new ArrayList<Set<BlockPos>>();
        ArrayList<BlockPos> logSeeds = new ArrayList<BlockPos>();
        for (Map.Entry<BlockPos, BlockState> entry : snapshots.entrySet()) {
            if (!TreeBlockHelper.isTreeAnchor(entry.getValue())) continue;
            logSeeds.add(entry.getKey());
        }
        for (BlockPos seed : logSeeds) {
            Set<BlockPos> cluster;
            if (!remaining.contains(seed) || (cluster = TreeAssemblyQueue.growCluster(seed, remaining, logChunkSize)).isEmpty()) continue;
            clusters.add(cluster);
        }
        while (!remaining.isEmpty()) {
            BlockPos seed = (BlockPos)remaining.iterator().next();
            Set<BlockPos> cluster = TreeAssemblyQueue.growCluster(seed, remaining, leafChunkSize);
            if (cluster.isEmpty()) {
                remaining.remove(seed);
                continue;
            }
            clusters.add(cluster);
        }
        return clusters;
    }

    private static Set<BlockPos> growCluster(BlockPos seed, Set<BlockPos> remaining, int chunkSize) {
        LinkedHashSet<BlockPos> cluster = new LinkedHashSet<BlockPos>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<BlockPos>();
        frontier.add(seed);
        while (cluster.size() < chunkSize && !frontier.isEmpty()) {
            BlockPos pos = (BlockPos)frontier.poll();
            if (!remaining.remove(pos)) continue;
            cluster.add(pos);
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (!remaining.contains(neighbor)) continue;
                frontier.add(neighbor);
            }
        }
        return cluster;
    }

    private static int dropPlotBlocksAsItems(ServerSubLevel subLevel, ServerLevel level, List<PlotBlockSnapshot> plotBlocks) {
        int dropped = 0;
        for (PlotBlockSnapshot block : plotBlocks) {
            BlockState state = block.state();
            if (state.isAir()) continue;
            Vec3 worldCenter = subLevel.logicalPose().transformPosition(Vec3.atCenterOf((Vec3i)block.plotPos()));
            Block.dropResources((BlockState)state, (Level)level, (BlockPos)BlockPos.containing((Position)worldCenter));
            ++dropped;
        }
        return dropped;
    }

    private static void forceClearWorldTreeBlocks(ServerLevel level, Iterable<BlockPos> worldPositions) {
        for (BlockPos pos : worldPositions) {
            BlockState current = level.getBlockState(pos);
            if (current.isAir() || !TreeBlockHelper.isTreeBlock(current)) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private record PendingTreeImpact(ServerLevel level, BlockPos impactOrigin, Vector3d vehicleImpactVelocity, double impactSpeed, UUID impactingVehicleId) {
    }

    private static final class PendingTreeAssembly {
        private final ServerLevel level;
        private final List<TreeAssembly.TreeBlock> blocks;
        private final Set<BlockPos> blockPositions;
        private final Set<BlockPos> decayLeaves;
        private final Vector3d vehicleImpactVelocity;
        private final long readyAtTick;
        private final BlockPos plantBasePos;
        private final BlockState plantSaplingState;
        private Map<BlockPos, BlockState> snapshots;
        private List<Set<BlockPos>> clusters;
        private int nextClusterIndex;
        private int createdChunks;
        private int droppedLeafClusters;

        private PendingTreeAssembly(ServerLevel level, List<TreeAssembly.TreeBlock> blocks, Set<BlockPos> decayLeaves, Vector3d vehicleImpactVelocity, long readyAtTick, BlockPos plantBasePos, BlockState plantSaplingState) {
            this.level = level;
            this.blocks = blocks;
            this.plantBasePos = plantBasePos;
            this.plantSaplingState = plantSaplingState;
            this.blockPositions = new HashSet<BlockPos>();
            for (TreeAssembly.TreeBlock block : blocks) {
                this.blockPositions.add(block.pos());
            }
            this.decayLeaves = decayLeaves;
            this.vehicleImpactVelocity = vehicleImpactVelocity;
            this.readyAtTick = readyAtTick;
        }

        private boolean prepareSnapshots() {
            if (this.snapshots != null) {
                return true;
            }
            this.snapshots = new HashMap<BlockPos, BlockState>();
            for (TreeAssembly.TreeBlock block : this.blocks) {
                BlockState state = block.state();
                if (!TreeBlockHelper.isTreeBlock(state)) continue;
                this.snapshots.putIfAbsent(block.pos(), state);
            }
            if (this.snapshots.isEmpty()) {
                return false;
            }
            int chunkSize = Math.max(1, TreeOverrunSettings.chunkSize());
            int leafChunkSize = Math.max(chunkSize, TreeOverrunSettings.leafChunkSize());
            this.clusters = TreeAssemblyQueue.partitionIntoConnectedClusters(this.snapshots, chunkSize, leafChunkSize);
            return true;
        }
    }

    private record PlotBlockSnapshot(BlockPos plotPos, BlockState state) {
    }
}

