package org.cloudburstmc.server.level.feature.tree;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.TreeSpecies;
import org.cloudburstmc.server.level.feature.FeatureChorusTree;
import org.cloudburstmc.server.level.feature.FeatureFallenTree;
import org.cloudburstmc.server.level.feature.WorldFeature;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;

/**
 * The different tree varieties in Minecraft.
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
    MUSHROOM_RED(BlockStates.RED_MUSHROOM_BLOCK,//.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.STEM),
            BlockStates.RED_MUSHROOM_BLOCK,//.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomRed.ALL),
            BlockTypes.RED_MUSHROOM.getId(), 0) {
        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureMushroomRed(height);
        }
    },
    MUSHROOM_BROWN(BlockStates.BROWN_MUSHROOM_BLOCK,//.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomBrown.STEM),
            BlockStates.BROWN_MUSHROOM_BLOCK,//.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, BlockBehaviorHugeMushroomBrown.ALL),
            BlockTypes.BROWN_MUSHROOM.getId(), 0) {
        @Override
        public WorldFeature getDefaultGenerator(@NonNull IntRange height) {
            return new FeatureMushroomBrown(height);
        }
    },
    CHORUS(BlockStates.CHORUS_PLANT,
            BlockStates.CHORUS_FLOWER.withTrait(BlockTraits.AGE, 5),
            BlockTypes.CHORUS_FLOWER.getId(),
            0
    ) {
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
    protected final BlockState log;
    protected final BlockState leaves;
    protected final Identifier itemId;
    protected final int itemDamage;
    GenerationTreeSpecies(@NonNull TreeSpecies species) {
        this(species, true);
    }

    GenerationTreeSpecies(@NonNull TreeSpecies species, boolean hasSapling) {
        this(getLogState(species),
                getLeavesState(species),
                hasSapling ? getSaplingId(species) : null, hasSapling ? species.ordinal() : -1);
    }

    public static GenerationTreeSpecies fromItem(Identifier id, int damage) {
        for (GenerationTreeSpecies species : VALUES) {
            if (species.itemId == id && species.itemDamage == damage) {
                return species;
            }
        }
        throw new IllegalArgumentException(String.format("Unknown tree species item %s:%d", id, damage));
    }

    private static BlockState getLogState(TreeSpecies species) {
        return switch (species) {
            case OAK -> BlockStates.OAK_LOG;
            case SPRUCE -> BlockStates.SPRUCE_LOG;
            case BIRCH -> BlockStates.BIRCH_LOG;
            case JUNGLE -> BlockStates.JUNGLE_LOG;
            case ACACIA -> BlockStates.ACACIA_LOG;
            case DARK_OAK -> BlockStates.DARK_OAK_LOG;
            case CRIMSON -> BlockStates.CRIMSON_STEM;
            case WARPED -> BlockStates.WARPED_STEM;
            case MANGROVE -> BlockStates.MANGROVE_LOG;
        };
    }

    private static BlockState getLeavesState(TreeSpecies species) {
        return switch (species) {
            case OAK -> BlockStates.OAK_LEAVES;
            case SPRUCE -> BlockStates.SPRUCE_LEAVES;
            case BIRCH -> BlockStates.BIRCH_LEAVES;
            case JUNGLE -> BlockStates.JUNGLE_LEAVES;
            case ACACIA -> BlockStates.ACACIA_LEAVES;
            case DARK_OAK -> BlockStates.DARK_OAK_LEAVES;
            case CRIMSON -> BlockStates.NETHER_WART_BLOCK;
            case WARPED -> BlockStates.WARPED_WART_BLOCK;
            case MANGROVE -> BlockStates.MANGROVE_LEAVES;
        };
    }

    private static Identifier getSaplingId(TreeSpecies species) {
        return switch (species) {
            case OAK -> BlockTypes.OAK_SAPLING.getId();
            case SPRUCE -> BlockTypes.SPRUCE_SAPLING.getId();
            case BIRCH -> BlockTypes.BIRCH_SAPLING.getId();
            case JUNGLE -> BlockTypes.JUNGLE_SAPLING.getId();
            case ACACIA -> BlockTypes.ACACIA_SAPLING.getId();
            case DARK_OAK -> BlockTypes.DARK_OAK_SAPLING.getId();
            case CRIMSON -> BlockTypes.CRIMSON_FUNGUS.getId();
            case WARPED -> BlockTypes.WARPED_FUNGUS.getId();
            case MANGROVE -> BlockTypes.MANGROVE_PROPAGULE.getId();
        };
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
