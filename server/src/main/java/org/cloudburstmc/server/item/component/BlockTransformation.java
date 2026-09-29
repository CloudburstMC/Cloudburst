package org.cloudburstmc.server.item.component;

import lombok.Builder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;

import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

@Builder
public record BlockTransformation(UnaryOperator<BlockState> transform, boolean requiresAirAbove,
                                  Set<Direction> disallowedFaces, SoundEvent sound,
                                  @Nullable LevelEvent particle, @Nullable ItemType drop, boolean consumesItem) {
    public BlockTransformation {
        Objects.requireNonNull(transform, "transform");
        disallowedFaces = disallowedFaces == null ? Set.of() : Set.copyOf(disallowedFaces);
        sound = sound == null ? SoundEvent.ITEM_USE_ON : sound;
    }

    public @Nullable BlockState apply(BlockState source, Direction face, boolean airAbove) {
        if (this.disallowedFaces.contains(face) || this.requiresAirAbove && !airAbove) {
            return null;
        }

        BlockState result = this.transform.apply(source);
        return result.equals(source) ? null : result;
    }
}
