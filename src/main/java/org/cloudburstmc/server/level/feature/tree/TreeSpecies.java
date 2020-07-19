package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.behavior.*;
import org.cloudburstmc.server.level.feature.FeatureChorusTree;
import org.cloudburstmc.server.level.feature.FeatureFallenTree;
import org.cloudburstmc.server.level.feature.WorldFeature;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.utils.Identifier;

/**
 * The different tree varieties in Minecraft.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public enum TreeSpecies {
    OAK(BlockTypes.LOG, BlockBehaviorLog.OAK, BlockTypes.LEAVES, BlockBehaviorLeaves.OAK, BlockTypes.SAPLING, BlockBehaviorSapling.OAK) {
        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureLargeOakTree(height, this, 0.1d, FeatureLargeOakTree.DEFAULT_HEIGHT);
        }

        @Override
        public WorldFeature getFallenGenerator() {
            return new FeatureFallenTree(FeatureNormalTree.DEFAULT_HEIGHT, this.logId, this.logDamage, 0.75d);
        }
    },
    SWAMP(BlockTypes.LOG, BlockBehaviorLog.OAK, BlockTypes.LEAVES, BlockBehaviorLeaves.OAK, null, -1) {
        @Override
        public WorldFeature getDefaultGenerator() {
            return this.getDefaultGenerator(FeatureSwampTree.DEFAULT_HEIGHT);
        }

        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureSwampTree(height, OAK);
        }
    },
    SPRUCE(BlockTypes.LOG, BlockBehaviorLog.SPRUCE, BlockTypes.LEAVES, BlockBehaviorLeaves.SPRUCE, BlockTypes.SAPLING, BlockBehaviorSapling.SPRUCE) {
        @Override
        public WorldFeature getDefaultGenerator() {
            return this.getDefaultGenerator(FeatureSpruceTree.DEFAULT_HEIGHT);
        }

        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureSpruceTree(height, this);
        }

        @Override
        public WorldFeature getHugeGenerator() {
            return new FeatureHugeSpruceTree(FeatureHugeSpruceTree.DEFAULT_HEIGHT, this);
        }

        @Override
        public WorldFeature getFallenGenerator() {
            return new FeatureFallenTree(FeatureSpruceTree.DEFAULT_HEIGHT, this.logId, this.logDamage, 0.0d);
        }
    },
    PINE(BlockTypes.LOG, BlockBehaviorLog.SPRUCE, BlockTypes.LEAVES, BlockBehaviorLeaves.SPRUCE, null, -1) {
        @Override
        public WorldFeature getDefaultGenerator() {
            return this.getDefaultGenerator(FeatureSpruceTree.DEFAULT_HEIGHT);
        }

        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureSpruceTree(FeatureSpruceTree.DEFAULT_HEIGHT, this);
        }

        @Override
        public WorldFeature getHugeGenerator() {
            return new FeatureHugePineTree(FeatureHugeSpruceTree.DEFAULT_HEIGHT, this);
        }
    },
    BIRCH(BlockTypes.LOG, BlockBehaviorLog.BIRCH, BlockTypes.LEAVES, BlockBehaviorLeaves.BIRCH, BlockTypes.SAPLING, BlockBehaviorSapling.BIRCH) {
        @Override
        public WorldFeature getFallenGenerator() {
            return new FeatureFallenTree(new IntRange(5, 8), this.logId, this.logDamage, 0.0d);
        }
    },
    JUNGLE(BlockTypes.LOG, BlockBehaviorLog.JUNGLE, BlockTypes.LEAVES, BlockBehaviorLeaves.JUNGLE, BlockTypes.SAPLING, BlockBehaviorSapling.JUNGLE) {
        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureJungleTree(height, this);
        }

        @Override
        public WorldFeature getHugeGenerator() {
            return new FeatureHugeJungleTree(FeatureHugeJungleTree.DEFAULT_HEIGHT, this);
        }

        @Override
        public WorldFeature getFallenGenerator() {
            return new FeatureFallenTree(new IntRange(4, 11), this.logId, this.logDamage, 0.75d);
        }
    },
    ACACIA(BlockTypes.LOG2, BlockBehaviorLog2.ACACIA, BlockTypes.LEAVES2, BlockBehaviorLeaves2.ACACIA, BlockTypes.SAPLING, BlockBehaviorSapling.ACACIA) {
        @Override
        public WorldFeature getDefaultGenerator() {
            return this.getDefaultGenerator(FeatureSavannaTree.DEFAULT_HEIGHT);
        }

        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureSavannaTree(height, this);
        }
    },
    DARK_OAK(BlockTypes.LOG2, BlockBehaviorLog2.DARK_OAK, BlockTypes.LEAVES2, BlockBehaviorLeaves2.DARK_OAK, BlockTypes.SAPLING, BlockBehaviorSapling.DARK_OAK) {
        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return null;
        }

        @Override
        public WorldFeature getHugeGenerator() {
            return new FeatureDarkOakTree(FeatureDarkOakTree.DEFAULT_HEIGHT, this);
        }
    },
    MUSHROOM_RED(BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.STEM, BlockTypes.RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed.ALL, BlockTypes.RED_MUSHROOM, 0) {
        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureMushroomRed(height);
        }
    },
    MUSHROOM_BROWN(BlockTypes.BROWN_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomBrown.STEM, BlockTypes.BROWN_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomBrown.ALL, BlockTypes.BROWN_MUSHROOM, 0) {
        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureMushroomBrown(height);
        }
    },
    CHORUS(BlockTypes.CHORUS_PLANT, 0, BlockTypes.CHORUS_FLOWER, 5, BlockTypes.CHORUS_FLOWER, 0) {
        @Override
        public WorldFeature getDefaultGenerator() {
            return this.getDefaultGenerator(FeatureChorusTree.DEFAULT_BRANCH_HEIGHT);
        }

        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureChorusTree(height, FeatureChorusTree.DEFAULT_MAX_RECURSION, FeatureChorusTree.DEFAULT_MAX_OVERHANG);
        }
    };

    private static final TreeSpecies[] VALUES = values();

    public static TreeSpecies fromItem(Identifier id, int damage) {
        for (TreeSpecies species : VALUES) {
            if (species.itemId == id && species.itemDamage == damage) {
                return species;
            }
        }
        throw new IllegalArgumentException(String.format("Unknown tree species item %s:%d", id, damage));
    }

    protected final Identifier logId;
    protected final int logDamage;
    protected final Identifier leavesId;
    protected final int leavesDamage;

    protected final Identifier itemId;
    protected final int itemDamage;

    public Identifier getLogId() {
        return this.logId;
    }

    public int getLogDamage() {
        return this.logDamage;
    }

    public Identifier getLeavesId() {
        return this.leavesId;
    }

    public int getLeavesDamage() {
        return this.leavesDamage;
    }

    public Identifier getItemId() {
        return this.itemId;
    }

    public int getItemDamage() {
        return this.itemDamage;
    }

    public WorldFeature getDefaultGenerator() {
        return this.getDefaultGenerator(FeatureNormalTree.DEFAULT_HEIGHT);
    }

    public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
        return new FeatureNormalTree(height, this);
    }

    public WorldFeature getHugeGenerator() {
        return null;
    }

    public WorldFeature getFallenGenerator() {
        return null;
    }
}
