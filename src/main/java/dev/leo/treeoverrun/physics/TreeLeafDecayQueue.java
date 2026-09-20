/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.leo.treeoverrun.physics;

import dev.leo.treeoverrun.util.TreeBlockHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class TreeLeafDecayQueue {
    private static final int OWNED_LEAF_DECAYS_PER_TICK = 8;
    private static final List<PendingLeafDecay> PENDING_LEAF_DECAYS = new ArrayList<PendingLeafDecay>();

    private TreeLeafDecayQueue() {
    }

    static void scheduleOwnedLeafDecay(ServerLevel level, Set<BlockPos> decayLeaves) {
        if (decayLeaves.isEmpty()) {
            return;
        }
        PENDING_LEAF_DECAYS.add(new PendingLeafDecay(level, new ArrayList<BlockPos>(decayLeaves)));
    }

    static void process(ServerLevel level) {
        TreeLeafDecayQueue.processLeafDecays(level);
    }

    private static void processLeafDecays(ServerLevel level) {
        long gameTime = level.getGameTime();
        int remainingThisTick = 8;
        Iterator<PendingLeafDecay> it = PENDING_LEAF_DECAYS.iterator();
        while (it.hasNext() && remainingThisTick > 0) {
            PendingLeafDecay decay = it.next();
            if (decay.level() != level) continue;
            remainingThisTick -= decay.processUpTo(remainingThisTick, gameTime);
            if (!decay.isDone()) continue;
            it.remove();
        }
    }

    private static final class PendingLeafDecay {
        private final ServerLevel level;
        private final ArrayList<BlockPos> positions;
        private long lastProcessedTick = -1L;

        private PendingLeafDecay(ServerLevel level, List<BlockPos> positions) {
            this.level = level;
            this.positions = new ArrayList<BlockPos>(positions);
        }

        private ServerLevel level() {
            return this.level;
        }

        private boolean isDone() {
            return this.positions.isEmpty();
        }

        private int processUpTo(int count, long gameTime) {
            if (gameTime <= this.lastProcessedTick) {
                return 0;
            }
            this.lastProcessedTick = gameTime;
            int broken = 0;
            while (broken < count && !this.positions.isEmpty()) {
                BlockPos pos = this.positions.remove(this.positions.size() - 1);
                BlockState state = this.level.getBlockState(pos);
                if (!TreeBlockHelper.isLeaf(state)) continue;
                this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                ++broken;
            }
            return broken;
        }
    }
}

