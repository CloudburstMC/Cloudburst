package org.cloudburstmc.server.persistence;

import org.cloudburstmc.api.persistence.PersistentDataType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CloudPersistentDataContainerTest {

    @Test
    void roundTripsNamespacedValuesAndOnlyMarksChangesDirty() {
        Identifier first = Identifier.parse("first:score");
        Identifier second = Identifier.parse("second:score");
        AtomicInteger changes = new AtomicInteger();
        CloudPersistentDataContainer data = new CloudPersistentDataContainer(changes::incrementAndGet);

        data.set(first, PersistentDataType.INTEGER, 42);
        data.set(first, PersistentDataType.INTEGER, 42);
        data.set(second, PersistentDataType.STRING, "ready");
        assertEquals(2, changes.get());

        NbtMapBuilder saved = NbtMap.builder();
        data.save(saved);
        data.load(saved.build());

        assertEquals(Set.of(first, second), data.getKeys());
        assertEquals(42, data.get(first, PersistentDataType.INTEGER).orElseThrow().intValue());
        assertEquals("ready", data.get(second, PersistentDataType.STRING).orElseThrow());
        assertTrue(data.get(first, PersistentDataType.LONG).isEmpty());
        assertTrue(data.has(first));
        assertTrue(data.has(first, PersistentDataType.INTEGER));
        assertFalse(data.has(first, PersistentDataType.LONG));
        assertEquals(7L, data.getOrDefault(first, PersistentDataType.LONG, 7L).longValue());
        assertEquals(2, changes.get());

        data.remove(first);
        data.remove(second);
        data.remove(second);
        assertEquals(4, changes.get());
        assertTrue(data.isEmpty());

        data.save(saved);
        assertFalse(saved.build().containsKey(CloudPersistentDataContainer.STORAGE_KEY));
    }

    @Test
    void malformedStoredKeysStayOutOfTheView() {
        CloudPersistentDataContainer data = new CloudPersistentDataContainer(() -> {});
        Identifier valid = Identifier.parse("plugin:score");
        NbtMap stored = NbtMap.builder()
                .putInt(valid.toString(), 42)
                .putInt("bad!namespace:key", 1)
                .build();
        NbtMap tag = NbtMap.builder()
                .putCompound(CloudPersistentDataContainer.STORAGE_KEY, stored)
                .build();

        data.load(tag);

        assertEquals(Set.of(valid), data.getKeys());
        assertThrows(IllegalArgumentException.class, () -> data.has(Identifier.EMPTY));

        NbtMapBuilder saved = NbtMap.builder();
        data.save(saved);
        assertEquals(stored, saved.build().getCompound(CloudPersistentDataContainer.STORAGE_KEY));
    }

    @Test
    void adaptersMatchTheStoredPrimitiveTypeAfterReload() {
        Identifier key = Identifier.parse("plugin:enabled");
        CloudPersistentDataContainer data = new CloudPersistentDataContainer(() -> {});
        data.set(key, PersistentDataType.BOOLEAN, true);

        NbtMapBuilder saved = NbtMap.builder();
        data.save(saved);
        data.load(saved.build());

        assertTrue(data.has(key, PersistentDataType.BYTE));
        assertTrue(data.get(key, PersistentDataType.BOOLEAN).orElseThrow());
        assertEquals((byte) 1, data.get(key, PersistentDataType.BYTE).orElseThrow().byteValue());
        assertFalse(data.has(key, PersistentDataType.INTEGER));
    }

    @Test
    void arraysCannotBeChangedWithoutSettingTheValueAgain() {
        Identifier key = Identifier.parse("plugin:bytes");
        CloudPersistentDataContainer data = new CloudPersistentDataContainer(() -> {});
        byte[] original = {1, 2};

        data.set(key, PersistentDataType.BYTE_ARRAY, original);
        original[0] = 9;
        data.get(key, PersistentDataType.BYTE_ARRAY).orElseThrow()[1] = 9;

        assertArrayEquals(new byte[]{1, 2}, data.get(key, PersistentDataType.BYTE_ARRAY).orElseThrow());
    }
}
