package org.cloudburstmc.server.item;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.Tool;
import org.cloudburstmc.api.item.ToolMaterial;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VanillaTools {

    public static Tool shovel(float speed) {
        return Tool.builder()
                .addRule(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, speed))
                .build();
    }

    public static Tool hoe(ToolMaterial material) {
        checkNotNull(material, "material");
        return Tool.builder()
                .addRule(Tool.Rule.deniesDrops(material.getIncorrectBlocksForDrops()))
                .addRule(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_HOE, material.getSpeed()))
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

    public static Tool sword() {
        return Tool.builder()
                .damagePerBlock(2)
                .canDestroyBlocksInCreative(false)
                .addRule(Tool.Rule.minesAndDrops(List.of(BlockTypes.WEB), 15.0F))
                .addRule(Tool.Rule.overrideSpeed(BlockTags.SWORD_INSTANTLY_MINES, Float.MAX_VALUE))
                .addRule(Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F))
                .build();
    }

    public static Tool weapon() {
        return Tool.builder()
                .damagePerBlock(2)
                .canDestroyBlocksInCreative(false)
                .build();
    }

    public static Tool shears() {
        return Tool.builder().build();
    }
}
