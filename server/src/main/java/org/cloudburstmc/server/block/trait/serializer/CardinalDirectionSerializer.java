package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.serializer.util.BedrockStateTags;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Set;

import static org.cloudburstmc.api.block.BlockTypes.*;

@ParametersAreNonnullByDefault
public class CardinalDirectionSerializer implements TraitSerializer<CardinalDirection> {

    private static final Set<BlockType> CARDINAL_STRING_BLOCKS = Set.of(
            ACACIA_DOOR,
            ACACIA_FENCE_GATE,
            ACACIA_SHELF,
            ANVIL,
            BAMBOO_DOOR,
            BAMBOO_FENCE_GATE,
            BAMBOO_SHELF,
            BIG_DRIPLEAF,
            BIRCH_DOOR,
            BIRCH_FENCE_GATE,
            BIRCH_SHELF,
            BLAST_FURNACE,
            CALIBRATED_SCULK_SENSOR,
            CAMPFIRE,
            CARVED_PUMPKIN,
            CHERRY_DOOR,
            CHERRY_FENCE_GATE,
            CHERRY_SHELF,
            CHEST,
            CHIPPED_ANVIL,
            COPPER_CHEST,
            COPPER_DOOR,
            COPPER_GOLEM_STATUE,
            CRIMSON_DOOR,
            CRIMSON_FENCE_GATE,
            CRIMSON_SHELF,
            DAMAGED_ANVIL,
            DARK_OAK_DOOR,
            DARK_OAK_FENCE_GATE,
            DARK_OAK_SHELF,
            DEPRECATED_ANVIL,
            DRIED_GHAST,
            ENDER_CHEST,
            END_PORTAL_FRAME,
            EXPOSED_COPPER_CHEST,
            EXPOSED_COPPER_DOOR,
            EXPOSED_COPPER_GOLEM_STATUE,
            FURNACE,
            IRON_DOOR,
            JUNGLE_DOOR,
            JUNGLE_FENCE_GATE,
            JUNGLE_SHELF,
            LEAF_LITTER,
            LECTERN,
            LIT_BLAST_FURNACE,
            LIT_FURNACE,
            LIT_PUMPKIN,
            LIT_SMOKER,
            MANGROVE_DOOR,
            MANGROVE_FENCE_GATE,
            MANGROVE_SHELF,
            OAK_DOOR,
            OAK_FENCE_GATE,
            OAK_SHELF,
            OXIDIZED_COPPER_CHEST,
            OXIDIZED_COPPER_DOOR,
            OXIDIZED_COPPER_GOLEM_STATUE,
            PALE_OAK_DOOR,
            PALE_OAK_FENCE_GATE,
            PALE_OAK_SHELF,
            PINK_PETALS,
            POWERED_COMPARATOR,
            POWERED_REPEATER,
            PUMPKIN,
            SMALL_DRIPLEAF_BLOCK,
            SMOKER,
            SOUL_CAMPFIRE,
            SPRUCE_DOOR,
            SPRUCE_FENCE_GATE,
            SPRUCE_SHELF,
            STONECUTTER_BLOCK,
            TRAPPED_CHEST,
            UNPOWERED_COMPARATOR,
            UNPOWERED_REPEATER,
            VAULT,
            WARPED_DOOR,
            WARPED_FENCE_GATE,
            WARPED_SHELF,
            WAXED_COPPER_CHEST,
            WAXED_COPPER_DOOR,
            WAXED_COPPER_GOLEM_STATUE,
            WAXED_EXPOSED_COPPER_CHEST,
            WAXED_EXPOSED_COPPER_DOOR,
            WAXED_EXPOSED_COPPER_GOLEM_STATUE,
            WAXED_OXIDIZED_COPPER_CHEST,
            WAXED_OXIDIZED_COPPER_DOOR,
            WAXED_OXIDIZED_COPPER_GOLEM_STATUE,
            WAXED_WEATHERED_COPPER_CHEST,
            WAXED_WEATHERED_COPPER_DOOR,
            WAXED_WEATHERED_COPPER_GOLEM_STATUE,
            WEATHERED_COPPER_CHEST,
            WEATHERED_COPPER_DOOR,
            WEATHERED_COPPER_GOLEM_STATUE,
            WILDFLOWERS
    );

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, CardinalDirection direction) {
        if (CARDINAL_STRING_BLOCKS.contains(type)) {
            return direction.toDirection().name().toLowerCase();
        }
        return direction.ordinal();
    }

    @Override
    public String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> blockTrait) {
        if (CARDINAL_STRING_BLOCKS.contains(type)) {
            return BedrockStateTags.TAG_MINECRAFT_CARDINAL_DIRECTION;
        }

        return BedrockStateTags.TAG_GROUND_SIGN_DIRECTION;
    }
}
