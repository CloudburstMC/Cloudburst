package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.SignData;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Round-trip tests for the sign data path: every key today's load/saveClientData
 * handle, plus an unknown key, survive with the same NBT types. Legacy per-line
 * keys still load and pass through untouched.
 */
class SignPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap signTag() {
        return NbtMap.builder()
                .putString("id", "Sign")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Sign")
                .putString("Text", "one\ntwo\nthree\nfour")
                .putString("TextOwner", "Steve")
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(SignData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.SIGN));

        SignBlockEntity entity = (SignBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.SIGN, POSITION);
        NbtMap input = signTag();
        entity.loadAdditionalData(input);

        assertArrayEquals(new String[]{"one", "two", "three", "four"}, entity.getText());
        assertEquals("Steve", entity.getTextOwner());

        NbtMap saved = entity.getServerTag();
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(Set.of("id", "x", "y", "z"));
        assertEquals(expectedKeys, saved.keySet());
        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }

        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "Text", "TextOwner"), client.keySet());
        for (String key : Set.of("Text", "TextOwner")) {
            assertEquals(input.get(key), client.get(key), "client value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(client.get(key).getClass()), "client type of " + key);
        }
    }

    @Test
    void defaultSignWritesJoinedTextAndEmptyOwner() {
        SignBlockEntity entity = (SignBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.SIGN, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Sign")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        NbtMap saved = entity.getServerTag();
        assertEquals(Set.of("id", "x", "y", "z", "isMovable", "Text", "TextOwner"), saved.keySet());
        assertEquals("\n\n\n", saved.getString("Text"));
        assertEquals("", saved.getString("TextOwner"));

        assertEquals(Set.of("id", "Text", "TextOwner"), entity.getClientTag().keySet());
    }

    @Test
    void legacyLineKeysLoadAndPassThrough() {
        SignBlockEntity entity = (SignBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.SIGN, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Sign")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putString("Text1", "a")
                .putString("Text2", "b")
                .putString("Text3", "c")
                .putString("Text4", "d")
                .build());

        assertArrayEquals(new String[]{"a", "b", "c", "d"}, entity.getText());

        NbtMap saved = entity.getServerTag();
        assertEquals("a\nb\nc\nd", saved.getString("Text"));
        assertEquals("a", saved.getString("Text1"));
        assertEquals("d", saved.getString("Text4"));
    }

    @Test
    void shortSignTextIsSavedAsLoaded() {
        // A short loaded Text used to resave with literal "null" lines padding it;
        // the save now keeps the loaded value on both tags.
        SignBlockEntity entity = (SignBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.SIGN, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Sign")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putString("Text", "a\nb")
                .putString("TextOwner", "Steve")
                .build());

        assertArrayEquals(new String[]{"a", "b", null, null}, entity.getText());

        NbtMap saved = entity.getServerTag();
        assertEquals("a\nb", saved.getString("Text"));

        assertEquals("a\nb", entity.getClientTag().getString("Text"));
    }

    @Test
    void signBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(SignBlockEntity.class, SignData.class);
    }
}
