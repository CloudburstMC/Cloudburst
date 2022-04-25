package org.cloudburstmc.server.level.provider;

public class LegacyBlockConverter {
    private static final LegacyBlockConverter INSTANCE = new LegacyBlockConverter();

    private LegacyBlockConverter() {
    }

    public static LegacyBlockConverter get() {
        return INSTANCE;
    }

    public void convertBlockState(final int[] blockState) {
        if (blockState[0] == 17) { // minecraft:log
            BlockBehaviorLog.upgradeLegacyBlock(blockState);
        } else if (blockState[0] == 162) { // minecraft:log2
            BlockBehaviorLog2.upgradeLegacyBlock(blockState);
        }
    }
}
