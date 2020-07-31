package org.cloudburstmc.server.block.behavior;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NoopBlockBehavior extends BlockBehavior {
    public static final NoopBlockBehavior INSTANCE = new NoopBlockBehavior();
}
