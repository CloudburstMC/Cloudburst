package org.cloudburstmc.server.item;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class BlockRestrictionSerializationTest {

    private static final BlockRegistry BLOCKS = BlockTestRegistry.get();

    @Test
    void restrictionsResolveCanonicalTypesAndSerializedNames() {
        BlockType sign = BLOCKS.getOrThrow(BlockTypes.OAK_STANDING_SIGN.getId());
        String serializedName = BlockPalette.INSTANCE.getTypeIdentifiers(sign).iterator().next().toString();
        NbtMap tag = itemTag().toBuilder()
                .putList("CanPlaceOn", NbtType.STRING, List.of(serializedName, sign.getId().toString()))
                .putList("CanDestroy", NbtType.STRING, List.of(serializedName))
                .build();

        ItemStack loaded = ItemStackNbtCodec.deserializeItem(tag);
        assertEquals(List.of(sign), loaded.getOrDefault(ItemDataComponents.CAN_PLACE_ON, List.of()));
        assertEquals(List.of(sign), loaded.getOrDefault(ItemDataComponents.CAN_DESTROY, List.of()));

        ItemStack restored = ItemStackNbtCodec.deserializeItem(ItemStackNbtCodec.serializeItem(loaded));
        assertEquals(List.of(sign), restored.getOrDefault(ItemDataComponents.CAN_PLACE_ON, List.of()));
        assertEquals(List.of(sign), restored.getOrDefault(ItemDataComponents.CAN_DESTROY, List.of()));
    }

    @Test
    void restrictionsRoundTripThroughItemPackets() {
        ItemStack original = ItemStackNbtCodec.deserializeItem(itemTag().toBuilder()
                .putList("CanPlaceOn", NbtType.STRING, List.of("minecraft:stone"))
                .putList("CanDestroy", NbtType.STRING, List.of("minecraft:dirt"))
                .build());

        ItemStack restored = ItemStackPacketCodec.fromNetwork(ItemStackPacketCodec.toNetwork(original));

        assertEquals(List.of(BlockTypes.STONE), restored.getOrDefault(ItemDataComponents.CAN_PLACE_ON, List.of()));
        assertEquals(List.of(BlockTypes.DIRT), restored.getOrDefault(ItemDataComponents.CAN_DESTROY, List.of()));
    }

    @Test
    void unknownRestrictionsDoNotCreateUnregisteredTypes() {
        NbtMap tag = itemTag().toBuilder()
                .putList("CanPlaceOn", NbtType.STRING, List.of("test:missing_restriction"))
                .build();

        assertThrows(IllegalArgumentException.class, () -> ItemStackNbtCodec.deserializeItem(tag));
    }

    private static NbtMap itemTag() {
        return NbtMap.builder().putString("Name", "minecraft:stick").putByte("Count", (byte) 1).build();
    }
}
