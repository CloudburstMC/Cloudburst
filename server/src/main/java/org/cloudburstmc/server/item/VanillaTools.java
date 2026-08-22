package org.cloudburstmc.server.item;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.item.Tool;
import org.cloudburstmc.api.item.ToolMaterial;

import static com.google.common.base.Preconditions.checkNotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VanillaTools {

    public static Tool shovel(float speed) {
        return Tool.builder()
                .addRule(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, speed))
                .build();
    }

    public static Tool pickaxe(ToolMaterial material) {
        checkNotNull(material, "material");
        return Tool.builder()
                .addRule(Tool.Rule.deniesDrops(material.getIncorrectBlocksForDrops()))
                .addRule(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, material.getSpeed()))
                .build();
    }

    public static Tool axe(ToolMaterial material) {
        checkNotNull(material, "material");
        return Tool.builder()
                .addRule(Tool.Rule.deniesDrops(material.getIncorrectBlocksForDrops()))
                .addRule(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, material.getSpeed()))
                .build();
    }

    public static Tool shears() {
        return Tool.builder().build();
    }
}
