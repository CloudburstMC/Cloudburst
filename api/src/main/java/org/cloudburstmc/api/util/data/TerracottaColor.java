package org.cloudburstmc.api.util.data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Identifier;

@UtilityClass
public class TerracottaColor {

    private final BiMap<Identifier, DyeColor> DYE_COLOR_MAP = HashBiMap.create(16);
    private final BiMap<Identifier, BlockColor> BLOCK_COLOR_MAP = HashBiMap.create(16);

    static {
        register(BlockTypes.BLACK_GLAZED_TERRACOTTA.getId(), DyeColor.BLACK, BlockColor.BLACK_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.RED_GLAZED_TERRACOTTA.getId(), DyeColor.RED, BlockColor.RED_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.GREEN_GLAZED_TERRACOTTA.getId(), DyeColor.GREEN, BlockColor.GREEN_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.BROWN_GLAZED_TERRACOTTA.getId(), DyeColor.BROWN, BlockColor.BROWN_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.BLUE_GLAZED_TERRACOTTA.getId(), DyeColor.BLUE, BlockColor.BLUE_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.PURPLE_GLAZED_TERRACOTTA.getId(), DyeColor.PURPLE, BlockColor.PURPLE_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.CYAN_GLAZED_TERRACOTTA.getId(), DyeColor.CYAN, BlockColor.CYAN_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.LIGHT_GRAY_GLAZED_TERRACOTTA.getId(), DyeColor.SILVER, BlockColor.LIGHT_GRAY_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.GRAY_GLAZED_TERRACOTTA.getId(), DyeColor.GRAY, BlockColor.GRAY_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.PINK_GLAZED_TERRACOTTA.getId(), DyeColor.PINK, BlockColor.PINK_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.LIME_GLAZED_TERRACOTTA.getId(), DyeColor.LIME, BlockColor.LIME_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.YELLOW_GLAZED_TERRACOTTA.getId(), DyeColor.YELLOW, BlockColor.YELLOW_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.LIGHT_BLUE_GLAZED_TERRACOTTA.getId(), DyeColor.LIGHT_BLUE, BlockColor.LIGHT_BLUE_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.MAGENTA_GLAZED_TERRACOTTA.getId(), DyeColor.MAGENTA, BlockColor.MAGENTA_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.ORANGE_GLAZED_TERRACOTTA.getId(), DyeColor.ORANGE, BlockColor.ORANGE_TERRACOTA_BLOCK_COLOR);
        register(BlockTypes.WHITE_GLAZED_TERRACOTTA.getId(), DyeColor.WHITE, BlockColor.WHITE_TERRACOTA_BLOCK_COLOR);
    }

    private void register(Identifier type, DyeColor color, BlockColor blockColor) {
        DYE_COLOR_MAP.put(type, color);
        BLOCK_COLOR_MAP.put(type, blockColor);
    }

    public DyeColor getDyeColor(Identifier type) {
        return DYE_COLOR_MAP.get(type);
    }

    public BlockColor getBlockColor(Identifier type) {
        return BLOCK_COLOR_MAP.get(type);
    }

    public Identifier fromDyeColor(DyeColor color) {
        return DYE_COLOR_MAP.inverse().get(color);
    }

    public Identifier fromBlockColor(BlockColor color) {
        return BLOCK_COLOR_MAP.inverse().get(color);
    }
}
