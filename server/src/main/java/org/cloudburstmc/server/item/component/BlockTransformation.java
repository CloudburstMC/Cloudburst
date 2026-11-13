package org.cloudburstmc.server.item.component;

import lombok.Builder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;

import java.util.Objects;

@Builder
public record BlockTransformation(
        BlockState state,
        SoundEvent sound,
        @Nullable SoundType soundOverride,
        @Nullable LevelEvent particle,
        @Nullable ItemType drop,
        boolean consumesItem
) {
    public BlockTransformation {
        Objects.requireNonNull(state, "state");
        sound = sound == null ? SoundEvent.ITEM_USE_ON : sound;
    }

    @FunctionalInterface
    public interface Resolver {
        @Nullable
        BlockTransformation resolve(BlockState state, Direction face, boolean airAbove);
    }
}
