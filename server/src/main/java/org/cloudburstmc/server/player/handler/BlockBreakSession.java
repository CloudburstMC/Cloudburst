package org.cloudburstmc.server.player.handler;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;

public record BlockBreakSession(Vector3i position, BlockState state, ItemStack tool, Direction face, double progress, long inputTick) {

    private static final double MINIMUM_PREDICTION_PROGRESS = 0.65D;

    public boolean matches(Vector3i position) {
        return this.position.equals(position);
    }

    public boolean matches(Vector3i position, BlockState state, ItemStack tool) {
        return this.matches(position) && this.state.equals(state) && this.tool.equals(tool);
    }

    public BlockBreakSession advance(Direction face, double progress, long inputTick) {
        if (inputTick <= this.inputTick) {
            return this;
        }

        return new BlockBreakSession(this.position, this.state, this.tool, face, this.progress + progress, inputTick);
    }

    public boolean canConfirmPrediction() {
        return this.progress >= MINIMUM_PREDICTION_PROGRESS;
    }

    public @Nullable BlockBreakSession continueAfterBreak(BlockState remaining, ItemStack tool) {
        if (remaining == BlockStates.AIR || remaining.getType().isLiquid() || remaining.equals(this.state)) {
            return null;
        }

        return new BlockBreakSession(this.position, remaining, tool, this.face, 0, this.inputTick);
    }
}
