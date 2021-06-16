package org.cloudburstmc.api.util.data;

import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.api.util.Identifier;

@RequiredArgsConstructor
public enum CaveVineType {
    NONE(BlockIds.CAVE_VINES),
    BODY(BlockIds.CAVE_VINES_BODY),
    HEAD(BlockIds.CAVE_VINES_HEAD);

    private final Identifier id;

    public Identifier getIdentifier() {
        return id;
    }
}
