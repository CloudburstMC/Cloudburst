package org.cloudburstmc.server.block.serializer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.*;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.misc.Tuple;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.serializer.util.BedrockStateTags;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.Map.Entry;

import static org.cloudburstmc.server.block.BlockIds.*;

@SuppressWarnings("unchecked")
@UtilityClass
public class MultiBlockSerializers {

    public static final MultiBlockSerializer STONE_SLAB = builder()
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE, BlockIds.STONE_SLAB)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_2, STONE_SLAB2)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_3, STONE_SLAB3)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_4, STONE_SLAB4)
            .buildSerializer();

    public static final MultiBlockSerializer DOUBLE_STONE_SLAB = builder()
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE, BlockIds.DOUBLE_STONE_SLAB)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_2, DOUBLE_STONE_SLAB2)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_3, DOUBLE_STONE_SLAB3)
            .add(BedrockStateTags.TAG_STONE_SLAB_TYPE_4, DOUBLE_STONE_SLAB4)
            .buildSerializer();

    public static final MultiBlockSerializer LOG = builder()
            .add(BedrockStateTags.TAG_OLD_LOG_TYPE, BlockIds.LOG)
            .add(BedrockStateTags.TAG_NEW_LOG_TYPE, LOG2)
            .add(BedrockStateTags.TAG_NEW_LOG_TYPE, TreeSpecies.WARPED.name().toLowerCase(), WARPED_STEM)
            .add(BedrockStateTags.TAG_NEW_LOG_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), CRIMSON_STEM)
            .add(
                    STRIPPED_OAK_LOG,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.OAK.name().toLowerCase()),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true)
            )
            .add(
                    STRIPPED_SPRUCE_LOG,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.SPRUCE.name().toLowerCase()),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true)
            )
            .add(
                    STRIPPED_BIRCH_LOG,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.BIRCH.name().toLowerCase()),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true)
            )
            .add(
                    STRIPPED_JUNGLE_LOG,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.JUNGLE.name().toLowerCase()),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true)
            )
            .add(
                    STRIPPED_ACACIA_LOG,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.ACACIA.name().toLowerCase()),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true)
            )
            .add(
                    STRIPPED_DARK_OAK_LOG,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.DARK_OAK.name().toLowerCase()),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true)
            )
            .add(
                    STRIPPED_CRIMSON_STEM,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.CRIMSON.name().toLowerCase()),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true)
            )
            .add(
                    STRIPPED_WARPED_STEM,
                    Builder.combine(BedrockStateTags.TAG_OLD_LOG_TYPE, TreeSpecies.WARPED.name().toLowerCase()),
                    Builder.combine(BedrockStateTags.TAG_STRIPPED_BIT, true)
            )
            .buildSerializer();

    public static final MultiBlockSerializer LEAVES = builder()
            .add(BedrockStateTags.TAG_OLD_LEAF_TYPE, BlockIds.LEAVES)
            .add(BedrockStateTags.TAG_NEW_LEAF_TYPE, LEAVES2)
            .buildSerializer();

    public static final MultiBlockSerializer PLANKS = builder()
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.WARPED.name().toLowerCase(), WARPED_PLANKS)
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), CRIMSON_PLANKS)
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
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.WARPED.name().toLowerCase(), WARPED_FENCE)
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), CRIMSON_FENCE)
            .defaultId(BlockIds.FENCE)
            .buildSerializer();

    public static final MultiBlockSerializer WOOD = builder()
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.WARPED.name().toLowerCase(), WARPED_HYPHAE)
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), CRIMSON_HYPHAE)
            .defaultId(BlockIds.WOOD)
            .buildSerializer();

    public static final MultiBlockSerializer WOOD_SLAB = builder()
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.WARPED.name().toLowerCase(), WARPED_SLAB)
            .add(BedrockStateTags.TAG_WOOD_TYPE, TreeSpecies.CRIMSON.name().toLowerCase(), CRIMSON_SLAB)
            .defaultId(WOODEN_SLAB)
            .buildSerializer();

    public static final MultiBlockSerializer REDSTONE_TORCH = builder()
            .add(BedrockStateTags.TAG_EXTINGUISHED, true, UNLIT_REDSTONE_TORCH)
            .defaultId(BlockIds.REDSTONE_TORCH)
            .buildSerializer();

    public static final MultiBlockSerializer FURNACE = builder()
            .add(BedrockStateTags.TAG_EXTINGUISHED, false, LIT_FURNACE)
            .defaultId(BlockIds.FURNACE)
            .buildSerializer();

    public static final MultiBlockSerializer BLAST_FURNACE = builder()
            .add(BedrockStateTags.TAG_EXTINGUISHED, false, LIT_BLAST_FURNACE)
            .defaultId(BlockIds.BLAST_FURNACE)
            .buildSerializer();

    public static final MultiBlockSerializer REDSTONE_LAMP = builder()
            .add(BedrockStateTags.TAG_EXTINGUISHED, false, LIT_REDSTONE_LAMP)
            .defaultId(BlockIds.REDSTONE_LAMP)
            .buildSerializer();

    public static final MultiBlockSerializer SMOKER = builder()
            .add(BedrockStateTags.TAG_EXTINGUISHED, false, LIT_SMOKER)
            .defaultId(BlockIds.SMOKER)
            .buildSerializer();

    public static final MultiBlockSerializer TORCH = builder()
            .add("is_soul", true, SOUL_TORCH)
            .defaultId(BlockIds.TORCH)
            .buildSerializer();

    public static final MultiBlockSerializer CAMPFIRE = builder()
            .add("is_soul", true, SOUL_CAMPFIRE)
            .defaultId(BlockIds.CAMPFIRE)
            .buildSerializer();

    public static final MultiBlockSerializer LANTERN = builder()
            .add("is_soul", true, SOUL_LANTERN)
            .defaultId(BlockIds.LANTERN)
            .buildSerializer();


    public static MultiBlockSerializer buildTreeSpecies(String traitName, Identifier... ids) {
        Preconditions.checkNotNull(ids, "ids");
        Preconditions.checkArgument(ids.length == TreeSpecies.values().length);

        val builder = builder();
        for (int i = 0; i < ids.length; i++) {
            builder.add(traitName, TreeSpecies.values()[i].name().toLowerCase(), ids[i]);
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
        private Identifier defaultId;

        public Builder add(Identifier id, Tuple<String, Object>... combinations) {
            Preconditions.checkNotNull(id, "id");
            Preconditions.checkNotNull(combinations, "combinations");
            this.combinations.add(new Combination(id, Arrays.stream(combinations)
                    .collect(ImmutableMap.toImmutableMap(Tuple::getA, Tuple::getB))));
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
            Preconditions.checkNotNull(traitName, "traitName");
            Preconditions.checkNotNull(id, "id");

            val entry = traits.computeIfAbsent(traitName, (k) -> new TraitEntry(traitName));
            if (traitValue != null) {
                entry.values.put(traitValue, id);
            } else {
                entry.defaultValue = id;
            }
            return this;
        }

        public Builder defaultId(Identifier id) {
            Preconditions.checkNotNull(id, "id");
            this.defaultId = id;
            return this;
        }

        public MultiBlockSerializer buildSerializer() {
            return new MultiBlockSerializer(build());
        }

        public MultiBlock build() {
            return new MultiBlock(ImmutableMap.copyOf(traits), ImmutableList.copyOf(combinations), defaultId);
        }

        public static Tuple<String, Object> combine(String traitName, Object value) {
            return new Tuple<>(traitName, value);
        }
    }

    @Value
    public static class MultiBlock {

        ImmutableMap<String, TraitEntry> traits;
        ImmutableList<Combination> combinations;
        Identifier defaultId;

        public Identifier getId(Map<String, Object> traits) {
            if (!combinations.isEmpty()) {
                combinationLoop:
                for (Combination combination : combinations) {
                    for (Entry<String, Object> entry : combination.values.entrySet()) {
                        if (traits.get(entry.getKey()) != entry.getValue()) {
                            continue combinationLoop;
                        }
                    }

                    return combination.id;
                }
            }

            for (Entry<String, Object> traitEntry : traits.entrySet()) {
                val entry = this.traits.get(traitEntry.getKey());

                if (entry != null) {
                    return entry.getId(traitEntry.getValue());
                }
            }

            return this.defaultId;
        }
    }

    @RequiredArgsConstructor
    private static class TraitEntry {

        private final String traitName;
        private final Map<Object, Identifier> values = new HashMap<>();
        private Identifier defaultValue;

        public Identifier getId(Object traitValue) {
            return values.getOrDefault(traitValue, this.defaultValue);
        }
    }

    @Value
    private static class Combination {

        Identifier id;
        ImmutableMap<String, Object> values;
    }
}
