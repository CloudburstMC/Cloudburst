package org.cloudburstmc.server.item;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.item.Tool;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VanillaTools {

    public static Tool shovel(float speed) {
        return Tool.builder()
                .addRule(Tool.Rule.tag(BlockTags.MINEABLE_WITH_SHOVEL, speed, true))
                .build();
    }

    public static Tool shears() {
        return Tool.builder().build();
    }
}
