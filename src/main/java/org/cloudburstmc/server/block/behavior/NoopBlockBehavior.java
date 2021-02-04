package org.cloudburstmc.server.block.behavior;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.block.behavior.BlockBehavior;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NoopBlockBehavior extends BlockBehavior {
    public static final NoopBlockBehavior INSTANCE = new NoopBlockBehavior();
}
