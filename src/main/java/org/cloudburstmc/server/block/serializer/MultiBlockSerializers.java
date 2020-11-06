package org.cloudburstmc.server.block.serializer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.*;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.misc.Tuple;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.serializer.util.BedrockStateTags;
import org.cloudburstmc.server.block.trait.EnumBlockTrait;
import org.cloudburstmc.server.item.data.Bucket;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.TerracottaColor;
import org.cloudburstmc.server.utils.data.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import static org.cloudburstmc.server.block.BlockIds.*;

@SuppressWarnings("unchecked")
@UtilityClass
public class MultiBlockSerializers {

    public static final MultiBlockSerializer STONE_SLAB = builder()
            .removeTrait("slab_slot")
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE, BlockIds.STONE_SLAB)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_2, STONE_SLAB2)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_3, STONE_SLAB3)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_4, STONE_SLAB4)
            .add(
                    BlockIds.DOUBLE_STONE_SLAB,
                    Builder.combine(BedrockStateTags.TAG_STONE_SLAB_TYPE),
                    Builder.combine("slab_slot")
            )
            .add(
                    DOUBLE_STONE_SLAB2,
                    Builder.combine(BedrockStateTags.TAG_STONE_SLAB_TYPE_2),
                    Builder.combine("slab_slot")
            )
            .add(
                    DOUBLE_STONE_SLAB3,
                    Builder.combine(BedrockStateTags.TAG_STONE_SLAB_TYPE_3),
                    Builder.combine("slab_slot")
            )
            .add(
                    DOUBLE_STONE_SLAB4,
                    Builder.combine(BedrockStateTags.TAG_STONE_SLAB_TYPE_4),
                    Builder.combine("slab_slot")
            )
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE, StoneSlabType.BLACKSTONE.name().toLowerCase(), BLACKSTONE_SLAB, true)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE, StoneSlabType.POLISHED_BLACKSTONE.name().toLowerCase(), POLISHED_BLACKSTONE_SLAB, true)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE, StoneSlabType.POLISHED_BLACKSTONE_BRICK.name().toLowerCase(), POLISHED_BLACKSTONE_BRICK_SLAB, true)
            .add(
                    BLACKSTONE_DOUBLE_SLAB,
                    Builder.combine(BedrockStateTags.TAG_STONE_SLAB_TYPE, StoneSlabType.BLACKSTONE.name().toLowerCase(), true),
                    Builder.combine("slab_slot")
            )
            .add(
                    POLISHED_BLACKSTONE_DOUBLE_SLAB,
                    Builder.combine(BedrockStateTags.TAG_STONE_SLAB_TYPE, StoneSlabType.POLISHED_BLACKSTONE.name().toLowerCase(), true),
                    Builder.combine("slab_slot")
            )
            .add(
                    POLISHED_BLACKSTONE_BRICK_DOUBLE_SLAB,
                    Builder.combine(BedrockStateTags.TAG_STONE_SLAB_TYPE, StoneSlabType.POLISHED_BLACKSTONE_BRICK.name().toLowerCase(), true),
                    Builder.combine("slab_slot")
            )
            .baseSerializer(NoopBlockSerializer.INSTANCE)
            .buildSerializer();

    public static final MultiBlockSerializer LOG = builder()
            .removeTrait(BedrockStateTags.TAG_STRIPPED_BIT)
            .add(BedrockStateTags.TAG_OLD_LOG_TYPE, BlockIds.LOG)
            .add(BedrockStateTags.TAG_NEW_LOG_TYPE, TreeSpecies.ACACIA.name().toLowerCase(), LOG2)
            .add(BedrockStateTags.TAG_NEW_LOG_TYPE, TreeSpecies.DARK_OAK.name().toLowerCase(), LOG2)
            .add(BedrockStateTags.TAG_NEW_LOG_TYPE, TreeSpecies.WARPED.name().toLowerCase(), WARPED_STEM, true)
            .add(BedrockStateTags.TAG_NEW_LOG_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), CRIMSON_STEM, true)
            .add(
                    STRIPPED_OAK_LOG,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.OAK.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true, true)
            )
            .add(
                    STRIPPED_SPRUCE_LOG,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.SPRUCE.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true, true)
            )
            .add(
                    STRIPPED_BIRCH_LOG,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.BIRCH.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true, true)
            )
            .add(
                    STRIPPED_JUNGLE_LOG,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.JUNGLE.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true, true)
            )
            .add(
                    STRIPPED_ACACIA_LOG,
                    Builder.combine(BedrockStateTags.TAG_NEW_LOG_TYPE, TreeSpecies.ACACIA.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true, true)
            )
            .add(
                    STRIPPED_DARK_OAK_LOG,
                    Builder.combine(BedrockStateTags.TAG_NEW_LOG_TYPE, TreeSpecies.DARK_OAK.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true, true)
            )
            .add(
                    STRIPPED_CRIMSON_STEM,
                    Builder.combine(BedrockStateTags.TAG_NEW_LOG_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true, true)
            )
            .add(
                    STRIPPED_WARPED_STEM,
                    Builder.combine(BedrockStateTags.TAG_NEW_LOG_TYPE, TreeSpecies.WARPED.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true, true)
            )
            .baseSerializer(DeprecatedSerializer.INSTANCE)
            .buildSerializer();

    public static final MultiBlockSerializer LEAVES = builder()
            .add(BedrockStateTags.TAG_OLD_LEAF_TYPE, BlockIds.LEAVES)
            .add(BedrockStateTags.TAG_NEW_LEAF_TYPE, LEAVES2)
            .buildSerializer();

//    public static final MultiBlockSerializer SAPLING = builder()
//            .add(BedrockStateTags.TAG_SAPLING_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), CRIMSON_FUNGUS, true)
//            .add(BedrockStateTags.TAG_SAPLING_TYPE, TreeSpecies.WARPED.name().toLowerCase(), WARPED_FUNGUS, true)
//            .defaultId(BlockIds.SAPLING)
//            .buildSerializer();

    public static final MultiBlockSerializer PLANKS = builder()
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.WARPED.name().toLowerCase(), WARPED_PLANKS, true)
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), CRIMSON_PLANKS, true)
            .defaultId(BlockIds.PLANKS)
            .buildSerializer();

    public static final MultiBlockSerializer WOOD_STAIRS = buildTreeSpecies(BedrockStateTags.TAG_WOOD_TYPE,
            OAK_STAIRS,
            SPRUCE_STAIRS,
            BIRCH_STAIRS,
            JUNGLE_STAIRS,
            ACACIA_STAIRS,
            DARK_OAK_STAIRS,
            CRIMSON_STAIRS,
            WARPED_STAIRS
    );

    public static final MultiBlockSerializer WOOD_BUTTON = buildTreeSpecies(BedrockStateTags.TAG_WOOD_TYPE,
            OAK_BUTTON,
            SPRUCE_BUTTON,
            BIRCH_BUTTON,
            JUNGLE_BUTTON,
            ACACIA_BUTTON,
            DARK_OAK_BUTTON,
            CRIMSON_BUTTON,
            WARPED_BUTTON
    );

    public static final MultiBlockSerializer WOOD_DOOR = buildTreeSpecies(BedrockStateTags.TAG_WOOD_TYPE,
            OAK_DOOR,
            SPRUCE_DOOR,
            BIRCH_DOOR,
            JUNGLE_DOOR,
            ACACIA_DOOR,
            DARK_OAK_DOOR,
            CRIMSON_DOOR,
            WARPED_DOOR
    );

    public static final MultiBlockSerializer WOOD_FENCE_GATE = buildTreeSpecies(BedrockStateTags.TAG_WOOD_TYPE,
            OAK_FENCE_GATE,
            SPRUCE_FENCE_GATE,
            BIRCH_FENCE_GATE,
            JUNGLE_FENCE_GATE,
            ACACIA_FENCE_GATE,
            DARK_OAK_FENCE_GATE,
            CRIMSON_FENCE_GATE,
            WARPED_FENCE_GATE
    );

    public static final MultiBlockSerializer WOOD_PRESSURE_PLATE = buildTreeSpecies(BedrockStateTags.TAG_WOOD_TYPE,
            OAK_PRESSURE_PLATE,
            SPRUCE_PRESSURE_PLATE,
            BIRCH_PRESSURE_PLATE,
            JUNGLE_PRESSURE_PLATE,
            ACACIA_PRESSURE_PLATE,
            DARK_OAK_PRESSURE_PLATE,
            CRIMSON_PRESSURE_PLATE,
            WARPED_PRESSURE_PLATE
    );

    public static final MultiBlockSerializer WOOD_STANDING_SIGN = buildTreeSpecies(BedrockStateTags.TAG_WOOD_TYPE,
            OAK_STANDING_SIGN,
            SPRUCE_STANDING_SIGN,
            BIRCH_STANDING_SIGN,
            JUNGLE_STANDING_SIGN,
            ACACIA_STANDING_SIGN,
            DARK_OAK_STANDING_SIGN,
            CRIMSON_STANDING_SIGN,
            WARPED_STANDING_SIGN
    );

    public static final MultiBlockSerializer WOOD_TRAPDOOR = buildTreeSpecies(BedrockStateTags.TAG_WOOD_TYPE,
            OAK_TRAPDOOR,
            SPRUCE_TRAPDOOR,
            BIRCH_TRAPDOOR,
            JUNGLE_TRAPDOOR,
            ACACIA_TRAPDOOR,
            DARK_OAK_TRAPDOOR,
            CRIMSON_TRAPDOOR,
            WARPED_TRAPDOOR
    );

    public static final MultiBlockSerializer WOOD_WALL_SIGN = buildTreeSpecies(BedrockStateTags.TAG_WOOD_TYPE,
            OAK_WALL_SIGN,
            SPRUCE_WALL_SIGN,
            BIRCH_WALL_SIGN,
            JUNGLE_WALL_SIGN,
            ACACIA_WALL_SIGN,
            DARK_OAK_WALL_SIGN,
            CRIMSON_WALL_SIGN,
            WARPED_WALL_SIGN
    );

    public static final MultiBlockSerializer WOOD_FENCE = builder()
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.WARPED.name().toLowerCase(), WARPED_FENCE, true)
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), CRIMSON_FENCE, true)
            .defaultId(BlockIds.FENCE)
            .buildSerializer();

    public static final MultiBlockSerializer WOOD = builder()
            .add(
                    WARPED_HYPHAE,
                    Builder.combine(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.WARPED.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, false, true)
            )
            .add(
                    CRIMSON_HYPHAE,
                    Builder.combine(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, false, true)
            )
            .add(
                    STRIPPED_CRIMSON_HYPHAE,
                    Builder.combine(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true, true)
            )
            .add(
                    STRIPPED_WARPED_HYPHAE,
                    Builder.combine(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.WARPED.name().toLowerCase(), true),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true, true)
            )
            .defaultId(BlockIds.WOOD)
            .baseSerializer(DeprecatedSerializer.INSTANCE)
            .buildSerializer();

    public static final MultiBlockSerializer WOODEN_SLAB = builder()
            .removeTrait("slab_slot")
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.WARPED.name().toLowerCase(), WARPED_SLAB, true)
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), CRIMSON_SLAB, true)
            .add(DOUBLE_WOODEN_SLAB, Builder.combine("slab_slot"))
            .add(
                    CRIMSON_DOUBLE_SLAB,
                    Builder.combine(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), true),
                    Builder.combine("slab_slot")
            )
            .add(
                    WARPED_DOUBLE_SLAB,
                    Builder.combine(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.WARPED.name().toLowerCase(), true),
                    Builder.combine("slab_slot")
            )
            .baseSerializer(NoopBlockSerializer.INSTANCE)
            .defaultId(BlockIds.WOODEN_SLAB)
            .buildSerializer();

    public static final MultiBlockSerializer REDSTONE_TORCH = builder()
            .removeTrait(BedrockStateTags.TAG_POWERED_BIT)
            .add(BedrockStateTags.TAG_POWERED_BIT, true, UNLIT_REDSTONE_TORCH)
            .defaultId(BlockIds.REDSTONE_TORCH)
            .buildSerializer();

    public static final MultiBlockSerializer FURNACE = builder()
            .removeTrait(BedrockStateTags.TAG_EXTINGUISHED)
            .add(BedrockStateTags.TAG_EXTINGUISHED, false, LIT_FURNACE)
            .defaultId(BlockIds.FURNACE)
            .buildSerializer();

    public static final MultiBlockSerializer BLAST_FURNACE = builder()
            .removeTrait(BedrockStateTags.TAG_EXTINGUISHED)
            .add(BedrockStateTags.TAG_EXTINGUISHED, false, LIT_BLAST_FURNACE)
            .defaultId(BlockIds.BLAST_FURNACE)
            .buildSerializer();

    public static final MultiBlockSerializer REDSTONE_LAMP = builder()
            .removeTrait(BedrockStateTags.TAG_POWERED_BIT)
            .add(BedrockStateTags.TAG_POWERED_BIT, true, LIT_REDSTONE_LAMP)
            .defaultId(BlockIds.REDSTONE_LAMP)
            .buildSerializer();

    public static final MultiBlockSerializer SMOKER = builder()
            .removeTrait(BedrockStateTags.TAG_EXTINGUISHED)
            .add(BedrockStateTags.TAG_EXTINGUISHED, false, LIT_SMOKER)
            .defaultId(BlockIds.SMOKER)
            .buildSerializer();

    public static final MultiBlockSerializer REDSTONE_ORE = builder()
            .removeTrait(BedrockStateTags.TAG_EXTINGUISHED)
            .add(BedrockStateTags.TAG_EXTINGUISHED, false, LIT_REDSTONE_ORE)
            .defaultId(BlockIds.REDSTONE_ORE)
            .buildSerializer();

    public static final MultiBlockSerializer TORCH = builder()
            .removeTrait("is_soul")
            .add("is_soul", true, SOUL_TORCH)
            .defaultId(BlockIds.TORCH)
            .buildSerializer();

    public static final MultiBlockSerializer CAMPFIRE = builder()
            .removeTrait("is_soul")
            .add("is_soul", true, SOUL_CAMPFIRE)
            .defaultId(BlockIds.CAMPFIRE)
            .buildSerializer();

    public static final MultiBlockSerializer LANTERN = builder()
            .removeTrait("is_soul")
            .add("is_soul", true, SOUL_LANTERN)
            .defaultId(BlockIds.LANTERN)
            .buildSerializer();

    public static final MultiBlockSerializer STONE_STAIRS = builder()
            .removeTrait(BedrockStateTags.TAG_STONE_TYPE)
            .removeTrait(BedrockStateTags.TAG_STONE_SLAB_TYPE)
            .removeTrait(BedrockStateTags.TAG_STONE_SLAB_TYPE_2)
            .removeTrait(BedrockStateTags.TAG_STONE_SLAB_TYPE_3)
            .removeTrait(BedrockStateTags.TAG_STONE_SLAB_TYPE_4)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE, StoneSlabType.class, StoneSlabType::getStairsId)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_2, StoneSlabType.class, StoneSlabType::getStairsId)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_3, StoneSlabType.class, StoneSlabType::getStairsId)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_4, StoneSlabType.class, StoneSlabType::getStairsId)
            .buildSerializer();

    public static final MultiBlockSerializer PISTON = builder()
            .removeTrait("is_sticky")
            .add("is_sticky", true, STICKY_PISTON)
            .defaultId(BlockIds.PISTON)
            .buildSerializer();

    public static final MultiBlockSerializer PISTON_ARM_COLLISION = builder()
            .removeTrait("is_sticky")
            .add("is_sticky", true, STICKY_PISTON_ARM_COLLISION)
            .defaultId(BlockIds.PISTON_ARM_COLLISION)
            .buildSerializer();

    public static final MultiBlockSerializer NETHER_BRICKS = builder()
            .add("nether_brick_type", NetherBrickType.class, NetherBrickType::getId, true)
            .buildSerializer();

    public static final MultiBlockSerializer STONE_BUTTON = builder()
            .removeTrait(BedrockStateTags.TAG_STONE_TYPE)
            .add(BedrockStateTags.TAG_STONE_TYPE, StoneSlabType.POLISHED_BLACKSTONE.name().toLowerCase(), POLISHED_BLACKSTONE_BUTTON)
            .defaultId(BlockIds.STONE_BUTTON)
            .buildSerializer();

    public static final MultiBlockSerializer STONE_PRESSURE_PLATE = builder()
            .removeTrait(BedrockStateTags.TAG_STONE_TYPE)
            .add(BedrockStateTags.TAG_STONE_TYPE, StoneSlabType.POLISHED_BLACKSTONE.name().toLowerCase(), POLISHED_BLACKSTONE_PRESSURE_PLATE)
            .defaultId(BlockIds.STONE_PRESSURE_PLATE)
            .buildSerializer();

    public static final MultiBlockSerializer FLOWER = builder()
            .add(BedrockStateTags.TAG_FLOWER_TYPE, FlowerType.DANDELION.name().toLowerCase(), YELLOW_FLOWER, true)
            .defaultId(RED_FLOWER)
            .buildSerializer();

    public static final MultiBlockSerializer TERRACOTTA = builder()
            .removeTrait(BedrockStateTags.TAG_COLOR)
            .add(BedrockStateTags.TAG_COLOR, DyeColor.class, TerracottaColor::fromDyeColor)
            .add(BedrockStateTags.TAG_COLOR, "silver", SILVER_GLAZED_TERRACOTTA)
            .buildSerializer();

    public static final MultiBlockSerializer WALL = builder()
            .add(BedrockStateTags.TAG_WALL_BLOCK_TYPE, WallBlockType.BLACKSTONE.name().toLowerCase(), BLACKSTONE_WALL, true)
            .add(BedrockStateTags.TAG_WALL_BLOCK_TYPE, WallBlockType.POLISHED_BLACKSTONE.name().toLowerCase(), POLISHED_BLACKSTONE_WALL, true)
            .add(BedrockStateTags.TAG_WALL_BLOCK_TYPE, WallBlockType.POLISHED_BLACKSTONE_BRICK.name().toLowerCase(), POLISHED_BLACKSTONE_BRICK_WALL, true)
            .defaultId(COBBLESTONE_WALL)
            .buildSerializer();

    public static final MultiBlockSerializer CAULDRON = builder()
            .removeTrait("cauldron_type")
            .add("cauldron_type", Bucket.LAVA.name().toLowerCase(), LAVA_CAULDRON)
            .defaultId(BlockIds.CAULDRON)
            .buildSerializer();

    public static final MultiBlockSerializer REPEATER = builder()
            .removeTrait(BedrockStateTags.TAG_POWERED_BIT)
            .add(BedrockStateTags.TAG_POWERED_BIT, true, POWERED_REPEATER)
            .defaultId(UNPOWERED_REPEATER)
            .buildSerializer();

    public static final MultiBlockSerializer COMPARATOR = builder()
            .removeTrait(BedrockStateTags.TAG_POWERED_BIT)
            .add(BedrockStateTags.TAG_POWERED_BIT, true, POWERED_COMPARATOR)
            .defaultId(UNPOWERED_COMPARATOR)
            .buildSerializer();

    public static final MultiBlockSerializer ELEMENT = builder()
            .add("element_type", ElementType.class, (e) -> Identifier.fromString(e.name().toLowerCase()), true)
            .buildSerializer();

    public static final MultiBlockSerializer NYLIUM = buildTrait("wood_type", BlockTraits.TREE_SPECIES_NETHER,
            CRIMSON_NYLIUM,
            WARPED_NYLIUM
    );

    public static final MultiBlockSerializer WART_BLOCK = buildTrait("wood_type", BlockTraits.TREE_SPECIES_NETHER,
            NETHER_WART_BLOCK,
            WARPED_WART_BLOCK
    );

    public static final MultiBlockSerializer ROOTS = buildTrait("wood_type", BlockTraits.TREE_SPECIES_NETHER,
            CRIMSON_ROOTS,
            WARPED_ROOTS
    );

    public static final MultiBlockSerializer FUNGUS = buildTrait("wood_type", BlockTraits.TREE_SPECIES_NETHER,
            CRIMSON_FUNGUS,
            WARPED_FUNGUS
    );


    public static MultiBlockSerializer buildTreeSpecies(String traitName, Identifier... ids) {
        return buildTrait(traitName, BlockTraits.TREE_SPECIES, ids);
    }

    public static <T extends Enum<T>> MultiBlockSerializer buildTrait(String traitName, EnumBlockTrait<T> trait, Identifier... ids) {
        Preconditions.checkNotNull(traitName, "traitName");
        Preconditions.checkNotNull(trait, "trait");
        Preconditions.checkNotNull(ids, "ids");

        val values = trait.getPossibleValues();
        Preconditions.checkArgument(ids.length == values.size());

        val builder = builder();
        builder.removeTrait(traitName);
        for (int i = 0; i < ids.length; i++) {
            builder.add(traitName, values.get(i).name().toLowerCase(), ids[i]);
        }

        return builder.buildSerializer();
    }

    public static Builder builder() {
        return new Builder();
    }

    @ParametersAreNonnullByDefault
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {

        private final Map<String, TraitEntry> traits = new HashMap<>();
        private final List<Combination> combinations = new ArrayList<>();
        private final Set<String> toRemove = new HashSet<>();
        private Identifier defaultId;
        private BlockSerializer baseSerializer = DefaultBlockSerializer.INSTANCE;

        public Builder removeTrait(String trait) {
            this.toRemove.add(trait);
            return this;
        }

        public <E extends Enum<E>> Builder add(String traitName, Class<E> em, Function<E, Identifier> mappingFunction) {
            return add(traitName, em, mappingFunction, false);
        }

        public <E extends Enum<E>> Builder add(String traitName, Class<E> em, Function<E, Identifier> mappingFunction, boolean remove) {
            for (E e : em.getEnumConstants()) {
                add(traitName, e.name().toLowerCase(), mappingFunction.apply(e), remove);
            }
            return this;
        }

        public Builder add(Identifier id, Tuple<String, TraitEntry>... combinations) {
            Preconditions.checkNotNull(id, "id");
            Preconditions.checkNotNull(combinations, "combinations");
            this.combinations.add(new Combination(id, Arrays.stream(combinations)
                    .collect(ImmutableList.toImmutableList())));
            return this;
        }

        public Builder add(String traitName, Identifier id) {
            add(traitName, null, id);
            return this;
        }

        public Builder add(String traitName, Map<Object, Identifier> idMap) {
            Preconditions.checkNotNull(idMap, "idMap");
            idMap.forEach((value, id) -> add(traitName, value, id));
            return this;
        }

        public Builder add(String traitName, @Nullable Object traitValue, Identifier id) {
            return add(traitName, traitValue, id, false);
        }

        public Builder add(String traitName, @Nullable Object traitValue, Identifier id, boolean removeTrait) {
            Preconditions.checkNotNull(traitName, "traitName");
            Preconditions.checkNotNull(id, "id");

            val entry = traits.computeIfAbsent(traitName, (k) -> new TraitEntry(traitName));
            if (traitValue != null) {
                if (removeTrait) {
                    entry.removeValues.add(traitValue);
                }
                entry.values.put(traitValue, id);
            } else {
                entry.remove = removeTrait;
                entry.defaultValue = id;
            }

            return this;
        }

        public Builder defaultId(Identifier id) {
            Preconditions.checkNotNull(id, "id");
            this.defaultId = id;
            return this;
        }

        public Builder baseSerializer(BlockSerializer serializer) {
            Preconditions.checkNotNull(serializer, "serializer");
            this.baseSerializer = serializer;
            return this;
        }

        public MultiBlockSerializer buildSerializer() {
            return new MultiBlockSerializer(this.baseSerializer, build());
        }

        public MultiBlock build() {
            combinations.sort(Comparator.reverseOrder());

            new ArrayList<>(traits.values()).forEach(t -> new HashMap<>(t.values).forEach((k, v) -> {
                if (k instanceof Boolean) {
                    t.values.put((byte) ((boolean) k ? 1 : 0), v);
                }
            }));

            Map<String, TraitEntry> sorted = new LinkedHashMap<>();
            traits.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(entry -> {
                sorted.put(entry.getKey(), entry.getValue());
            });

            return new MultiBlock(sorted, ImmutableList.copyOf(combinations), ImmutableSet.copyOf(toRemove), defaultId);
        }

        public static Tuple<String, TraitEntry> combine(String traitName) {
            return combine(traitName, false);
        }

        public static Tuple<String, TraitEntry> combine(String traitName, boolean remove) {
            return combine(traitName, null);
        }

        public static Tuple<String, TraitEntry> combine(String traitName, @Nullable Object value) {
            return combine(traitName, value, false);
        }

        public static Tuple<String, TraitEntry> combine(String traitName, @Nullable Object value, boolean remove) {
            val entry = new TraitEntry(traitName);
            if (remove) {
                entry.remove = true;
            }
            entry.values.put(value, AIR);
            return new Tuple<>(traitName, entry);
        }
    }

    @Value
    public static class MultiBlock {

        Map<String, TraitEntry> traits;
        ImmutableList<Combination> combinations;
        ImmutableSet<String> toRemove;
        Identifier defaultId;

        public Identifier getId(Map<String, Object> traits, List<String> toRemove) {
            toRemove.addAll(this.toRemove);

            if (!combinations.isEmpty()) {
                combinationLoop:
                for (Combination combination : combinations) {
                    for (Tuple<String, TraitEntry> entry : combination.values) {
                        TraitEntry te = entry.getB();

                        boolean found = false;
                        for (Object val : te.values.keySet()) {
                            val trait = traits.get(entry.getA());

                            if (trait == null) {
                                continue;
                            }

                            if (val == null || Objects.equals(trait, val)) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            continue combinationLoop;
                        }
                    }

                    combination.values.forEach(t -> {
                        if (t.getB().remove || !t.getB().removeValues.isEmpty()) {
                            toRemove.add(t.getA());
                        }
                    });
                    return combination.id;
                }
            }

            for (Entry<String, Object> traitEntry : traits.entrySet()) {
                val entry = this.traits.get(traitEntry.getKey());

                if (entry != null) {
                    return entry.getId(traitEntry.getValue(), toRemove);
                }
            }

            return this.defaultId;
        }
    }

    @ToString
    @RequiredArgsConstructor
    private static class TraitEntry implements Comparable<TraitEntry> {

        private final String traitName;
        private final Map<Object, Identifier> values = new HashMap<>();
        private final Set<Object> removeValues = new HashSet<>();
        private boolean remove;
        private Identifier defaultValue;

        public Identifier getId(Object traitValue, List<String> toRemove) {
            Identifier r = values.get(traitValue);

            if (r == null) {
                return this.defaultValue;
            }

            if (remove || removeValues.contains(traitValue)) {
                toRemove.add(traitName);
            }

            return r;
        }

        @Override
        public int compareTo(TraitEntry o) {
            return Integer.compare(this.values.size(), o.values.size());
        }
    }

    @Value
    private static class Combination implements Comparable<Combination> {

        Identifier id;
        List<Tuple<String, TraitEntry>> values;
        int weight;

        public Combination(Identifier id, ImmutableList<Tuple<String, TraitEntry>> values) {
            this.id = id;
            this.values = values;
            this.weight = (int) values.stream().filter(t -> {
                val ids = t.getB().values;

                return !ids.isEmpty() && ids.get(null) == null;
            }).count();

            values.forEach(t -> new HashMap<>(t.getB().values).forEach((k, v) -> {
                if (k instanceof Boolean) {
                    t.getB().values.put((byte) ((boolean) k ? 1 : 0), v);
                }
            }));
        }

        @Override
        public int compareTo(@Nullable Combination o) {
            if (o == null) {
                return 1;
            }

            return Integer.compare(this.weight, o.weight);
        }
    }

    @Value
    public static class Result {
        Identifier id;
        List<String> toRemove;
    }
}
