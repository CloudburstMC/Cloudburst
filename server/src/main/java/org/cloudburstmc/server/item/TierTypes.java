package org.cloudburstmc.server.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.item.TierType.TierTarget;

public class TierTypes {

    public static final TierType WOOD = new IntTier(100, 2, 60, 15, -1, TierTarget.TOOL);
    public static final TierType LEATHER = new IntTier(250, -1, 0, -1, 15, TierTarget.ARMOR);
    public static final TierType GOLD = new IntTier(200, 12, 33, 22, 25, TierTarget.ANY);
    public static final TierType CHAINMAIL = new IntTier(250, -1, 0, -1, 12, TierTarget.ARMOR);
    public static final TierType STONE = new IntTier(300, 4, 132, 5, -1, TierTarget.TOOL);
    public static final TierType IRON = new IntTier(400, 6, 251, 14, 9, TierTarget.ANY);
    public static final TierType DIAMOND = new IntTier(500, 8, 1562, 10, 10, TierTarget.ANY);
    public static final TierType NETHERITE = new IntTier(600, 9, 2031, 15, 15, TierTarget.ANY);

    @RequiredArgsConstructor
    @Getter
    private static class IntTier implements TierType {

        private final int level;
        private final float miningEfficiency;
        private final int durability;
        private final int toolEnchantAbility;
        private final int armorEnchantAbility;
        private final TierTarget target;
    }
}
