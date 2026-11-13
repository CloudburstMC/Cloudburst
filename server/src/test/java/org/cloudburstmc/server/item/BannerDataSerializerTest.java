package org.cloudburstmc.server.item;

import org.cloudburstmc.api.item.data.BannerData;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.data.serializer.BannerDataSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BannerDataSerializerTest {

    @Test
    void preservesTheBaseAndOrderedPatternLayers() {
        BannerData design = BannerData.of(0, DyeColor.BLUE, List.of(
                new BannerPattern(BannerPattern.Type.PATTERN_BORDER, DyeColor.WHITE),
                new BannerPattern(BannerPattern.Type.PATTERN_FLOW, DyeColor.ORANGE)
        ));

        NbtMapBuilder tag = NbtMap.builder();
        BannerDataSerializer.write(tag, design);

        assertEquals(design, BannerDataSerializer.read(tag.build()));
    }

    @Test
    void clearingTheDesignRemovesOldPatternData() {
        BannerData patterned = BannerData.of(0, DyeColor.RED,
                List.of(new BannerPattern(BannerPattern.Type.PATTERN_CREEPER, DyeColor.BLACK)));
        BannerData blank = BannerData.of(0, DyeColor.RED, List.of());
        NbtMapBuilder tag = NbtMap.builder();
        BannerDataSerializer.write(tag, patterned);
        BannerDataSerializer.write(tag, blank);

        assertEquals(blank, BannerDataSerializer.read(tag.build()));
    }
}
