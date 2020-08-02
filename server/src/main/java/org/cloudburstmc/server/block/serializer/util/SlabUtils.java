package org.cloudburstmc.server.block.serializer.util;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.StoneSlabType;

import java.util.EnumMap;

@UtilityClass
public class SlabUtils {

    private static final EnumMap<StoneSlabType, SlabInfo> SLAB_INFO = new EnumMap<>(StoneSlabType.class);

    private static final String[][] SLAB_NAMES = {
            {"stone_slab", "double_stone_slab", "stone_slab_type"},
            {"stone_slab2", "double_stone_slab2", "stone_slab_type_2"},
            {"stone_slab3", "double_stone_slab3", "stone_slab_type_3"},
            {"stone_slab4", "double_stone_slab4", "stone_slab_type_4"},
    };

    static {
        StoneSlabType[] values = StoneSlabType.values();

        for (int i = 0; i < values.length; i++) {
            StoneSlabType type = values[i];
            String[] name = SLAB_NAMES[i & 7];
            SLAB_INFO.put(type, new SlabInfo(name[0], name[1], name[2], type.name().toLowerCase()));
        }
    }

    public static SlabInfo getSlabInfo(StoneSlabType type) {
        return SLAB_INFO.get(type);
    }

    @Getter
    public static class SlabInfo {
        private final Identifier type;
        private final Identifier doubleType;
        private final String stateName;
        private final String state;

        private SlabInfo(String type, String doubleType, String stateName, String state) {
            this.type = Identifier.from("minecraft", type);
            this.doubleType = Identifier.from("minecraft", doubleType);
            this.stateName = stateName;
            this.state = state;
        }
    }
}
