package org.cloudburstmc.api.util.data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.api.util.Identifier;

@UtilityClass
public class TerracottaColor {

    private final BiMap<Identifier, DyeColor> DYE_COLOR_MAP = HashBiMap.create(16);
    private final BiMap<Identifier, BlockColor> BLOCK_COLOR_MAP = HashBiMap.create(16);

    static {
        register(BlockIds.BLACK_GLAZED_TERRACOTTA, DyeColor.BLACK, BlockColor.BLACK_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.RED_GLAZED_TERRACOTTA, DyeColor.RED, BlockColor.RED_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.GREEN_GLAZED_TERRACOTTA, DyeColor.GREEN, BlockColor.GREEN_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.BROWN_GLAZED_TERRACOTTA, DyeColor.BROWN, BlockColor.BROWN_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.BLUE_GLAZED_TERRACOTTA, DyeColor.BLUE, BlockColor.BLUE_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.PURPLE_GLAZED_TERRACOTTA, DyeColor.PURPLE, BlockColor.PURPLE_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.CYAN_GLAZED_TERRACOTTA, DyeColor.CYAN, BlockColor.CYAN_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.SILVER_GLAZED_TERRACOTTA, DyeColor.SILVER, BlockColor.LIGHT_GRAY_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.GRAY_GLAZED_TERRACOTTA, DyeColor.GRAY, BlockColor.GRAY_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.PINK_GLAZED_TERRACOTTA, DyeColor.PINK, BlockColor.PINK_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.LIME_GLAZED_TERRACOTTA, DyeColor.LIME, BlockColor.LIME_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.YELLOW_GLAZED_TERRACOTTA, DyeColor.YELLOW, BlockColor.YELLOW_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.LIGHT_BLUE_GLAZED_TERRACOTTA, DyeColor.LIGHT_BLUE, BlockColor.LIGHT_BLUE_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.MAGENTA_GLAZED_TERRACOTTA, DyeColor.MAGENTA, BlockColor.MAGENTA_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.ORANGE_GLAZED_TERRACOTTA, DyeColor.ORANGE, BlockColor.ORANGE_TERRACOTA_BLOCK_COLOR);
        register(BlockIds.WHITE_GLAZED_TERRACOTTA, DyeColor.WHITE, BlockColor.WHITE_TERRACOTA_BLOCK_COLOR);
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
