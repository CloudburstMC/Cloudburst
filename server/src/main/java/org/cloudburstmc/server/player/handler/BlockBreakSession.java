package org.cloudburstmc.server.player.handler;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;

public record BlockBreakSession(Vector3i position, BlockState state, Direction face, double progress) {

    public boolean matches(Vector3i position) {
        return this.position.equals(position);
    }

    public BlockBreakSession advance(Direction face, double progress) {
        return new BlockBreakSession(this.position, this.state, face, this.progress + progress);
    }

    public @Nullable BlockBreakSession continueAfterBreak(BlockState remaining) {
        if (remaining == BlockStates.AIR || remaining.getType().isLiquid() || remaining.equals(this.state)) {
            return null;
        }

        return new BlockBreakSession(this.position, remaining, this.face, 0);
    }
}
