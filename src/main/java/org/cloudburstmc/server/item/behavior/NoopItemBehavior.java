package org.cloudburstmc.server.item.behavior;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NoopItemBehavior extends CloudItemBehavior {

    public static final NoopItemBehavior INSTANCE = new NoopItemBehavior();
}
