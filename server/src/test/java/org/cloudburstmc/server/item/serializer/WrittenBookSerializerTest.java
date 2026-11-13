package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.WrittenBook;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.data.serializer.WrittenBookSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WrittenBookSerializerTest {

    @Test
    void readsBedrockDraftPagesWithoutSignatureFields() {
        WrittenBookSerializer serializer = new WrittenBookSerializer();
        NbtMap tag = NbtMap.builder().putList("pages", NbtType.COMPOUND, List.of(
                NbtMap.builder().putString("text", "First page").build(),
                NbtMap.builder().putString("page", "Previously saved page").build())).build();

        WrittenBook book = serializer.deserialize(ItemTypes.WRITABLE_BOOK.getId(), tag);

        assertNotNull(book);
        assertEquals("", book.getTitle());
        assertEquals("First page", book.getPages().getFirst().text());
        assertEquals("Previously saved page", book.getPages().getLast().text());

        NbtMapBuilder serialized = NbtMap.builder();
        serializer.serialize(ItemStack.from(ItemTypes.WRITABLE_BOOK), serialized, book);
        NbtMap draft = serialized.build();
        NbtMap page = draft.getList("pages", NbtType.COMPOUND).getLast();

        assertEquals("Previously saved page", page.getString("text"));
        assertFalse(page.containsKey("page"));
        assertFalse(draft.containsKey("title"));
        assertFalse(draft.containsKey("generation"));
    }
}
