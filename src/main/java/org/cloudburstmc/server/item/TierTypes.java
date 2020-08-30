package org.cloudburstmc.server.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class TierTypes {

    public static final TierType WOOD = new IntTier(100, 2, 60);
    public static final TierType LEATHER = new IntTier(250, -1, 0);
    public static final TierType GOLD = new IntTier(200, 12, 33);
    public static final TierType CHAINMAIL = new IntTier(250, -1, 0);
    public static final TierType STONE = new IntTier(300, 4, 132);
    public static final TierType IRON = new IntTier(400, 6, 251);
    public static final TierType DIAMOND = new IntTier(500, 8, 1562);
    public static final TierType NETHERITE = new IntTier(600, 9, 2031);

    @RequiredArgsConstructor
    @Getter
    private static class IntTier implements TierType {

        private final int level;
        private final float miningEfficiency;
        private final int durability;
    }
}
