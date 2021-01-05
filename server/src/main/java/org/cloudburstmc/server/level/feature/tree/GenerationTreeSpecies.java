package org.cloudburstmc.server.level.feature.tree;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.behavior.BlockBehaviorHugeMushroomBrown;
import org.cloudburstmc.server.block.behavior.BlockBehaviorHugeMushroomRed;
import org.cloudburstmc.server.level.feature.FeatureChorusTree;
import org.cloudburstmc.server.level.feature.FeatureFallenTree;
import org.cloudburstmc.server.level.feature.WorldFeature;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.TreeSpecies;

/**
 * The different tree varieties in Minecraft.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum GenerationTreeSpecies {
    OAK(TreeSpecies.OAK) {
        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureLargeOakTree(height, this, 0.1d, FeatureLargeOakTree.DEFAULT_HEIGHT);
        }

        @Override
        public WorldFeature getFallenGenerator() {
            return new FeatureFallenTree(FeatureNormalTree.DEFAULT_HEIGHT, this.log, 0.75d);
        }
    },
    SWAMP(TreeSpecies.OAK, false) {
        @Override
        public WorldFeature getDefaultGenerator() {
            return this.getDefaultGenerator(FeatureSwampTree.DEFAULT_HEIGHT);
        }

        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureSwampTree(height, OAK);
        }
    },
    SPRUCE(TreeSpecies.SPRUCE) {
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
            return new FeatureFallenTree(FeatureSpruceTree.DEFAULT_HEIGHT, this.log, 0.0d);
        }
    },
    PINE(TreeSpecies.SPRUCE, false) {
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
    BIRCH(TreeSpecies.BIRCH) {
        @Override
        public WorldFeature getFallenGenerator() {
            return new FeatureFallenTree(new IntRange(5, 8), this.log, 0.0d);
        }
    },
    JUNGLE(TreeSpecies.JUNGLE) {
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
            return new FeatureFallenTree(new IntRange(4, 11), this.log, 0.75d);
        }
    },
    ACACIA(TreeSpecies.ACACIA) {
        @Override
        public WorldFeature getDefaultGenerator() {
            return this.getDefaultGenerator(FeatureSavannaTree.DEFAULT_HEIGHT);
        }

        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureSavannaTree(height, this);
        }
    },
    DARK_OAK(TreeSpecies.DARK_OAK) {
        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return null;
        }

        @Override
        public WorldFeature getHugeGenerator() {
            return new FeatureDarkOakTree(FeatureDarkOakTree.DEFAULT_HEIGHT, this);
        }
    },
    MUSHROOM_RED(BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.STEM),
            BlockStates.RED_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.ALL),
            BlockIds.RED_MUSHROOM, 0) {
        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureMushroomRed(height);
        }
    },
    MUSHROOM_BROWN(BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomBrown.STEM),
            BlockStates.BROWN_MUSHROOM_BLOCK.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomBrown.ALL),
            BlockIds.BROWN_MUSHROOM, 0) {
        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureMushroomBrown(height);
        }
    },
    CHORUS(BlockStates.CHORUS_PLANT, BlockStates.CHORUS_FLOWER.withTrait(BlockTraits.CHORUS_AGE, 5), BlockIds.CHORUS_FLOWER, 0) {
        @Override
        public WorldFeature getDefaultGenerator() {
            return this.getDefaultGenerator(FeatureChorusTree.DEFAULT_BRANCH_HEIGHT);
        }

        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureChorusTree(height, FeatureChorusTree.DEFAULT_MAX_RECURSION, FeatureChorusTree.DEFAULT_MAX_OVERHANG);
        }
    };

    private static final GenerationTreeSpecies[] VALUES = values();

    public static GenerationTreeSpecies fromItem(Identifier id, int damage) {
        for (GenerationTreeSpecies species : VALUES) {
            if (species.itemId == id && species.itemDamage == damage) {
                return species;
            }
        }
        throw new IllegalArgumentException(String.format("Unknown tree species item %s:%d", id, damage));
    }

    protected final BlockState log;
    protected final BlockState leaves;

    protected final Identifier itemId;
    protected final int itemDamage;

    GenerationTreeSpecies(@NonNull TreeSpecies species) {
        this(species, true);
    }

    GenerationTreeSpecies(@NonNull TreeSpecies species, boolean hasSapling) {
        this(BlockStates.LOG.withTrait(BlockTraits.TREE_SPECIES, species),
                BlockStates.LEAVES.withTrait(BlockTraits.TREE_SPECIES_OVERWORLD, species),
                hasSapling ? BlockIds.SAPLING : null, hasSapling ? species.ordinal() : -1);
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
