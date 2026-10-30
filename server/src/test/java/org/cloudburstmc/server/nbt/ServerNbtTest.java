package org.cloudburstmc.server.nbt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.nbt.NbtMapper;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ServerNbtTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    private final NbtMapper mapper = ServerNbt.MAPPER;

    @Test
    void itemStackMatchesItemUtils() {
        List<ItemStack> stacks = List.of(
                ItemStack.from(ItemTypes.STONE, 5),
                ItemStack.from(ItemTypes.DIAMOND_SWORD).withDamage(10),
                ItemStack.EMPTY);

        NbtMap tag = mapper.write(new ItemHolder(stacks));

        NbtList<?> written = assertInstanceOf(NbtList.class, tag.get("items"));
        assertEquals(NbtType.COMPOUND, written.getType());
        assertEquals(stacks.size(), written.size());
        for (int i = 0; i < stacks.size(); i++) {
            assertEquals(ItemUtils.serializeItem(stacks.get(i)), written.get(i));
        }
    }

    @Test
    void slottedItemMatchesItemUtils() {
        ItemStack stack = ItemStack.from(ItemTypes.DIAMOND, 5);

        NbtMap tag = mapper.write(new SlottedHolder(List.of(new SlottedItem(3, stack))));

        NbtList<?> written = assertInstanceOf(NbtList.class, tag.get("items"));
        assertEquals(List.of(ItemUtils.serializeItem(stack, 3)), written);

        SlottedHolder read = mapper.read(tag, SlottedHolder.class);
        assertEquals(1, read.items.size());
        assertEquals(3, read.items.get(0).slot());
        assertEquals(stack, read.items.get(0).item());
    }

    @Test
    void blockStateRoundTrips() {
        BlockState poppy = REGISTRY.getBlock(BlockTypes.POPPY);

        NbtMap tag = mapper.write(new BlockHolder(List.of(poppy)));

        BlockHolder read = mapper.read(tag, BlockHolder.class);
        assertEquals(List.of(poppy), read.states);
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class ItemHolder {
        @JsonProperty("items")
        @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
        List<ItemStack> items = List.of();

        ItemHolder() {
        }

        ItemHolder(List<ItemStack> items) {
            this.items = items;
        }
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class SlottedHolder {
        @JsonProperty("items")
        @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
        List<SlottedItem> items = List.of();

        SlottedHolder() {
        }

        SlottedHolder(List<SlottedItem> items) {
            this.items = items;
        }
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class BlockHolder {
        @JsonProperty("blocks")
        @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
        List<BlockState> states = List.of();

        BlockHolder() {
        }

        BlockHolder(List<BlockState> states) {
            this.states = states;
        }
    }
}
