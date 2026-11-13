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
        assertEquals(2, changes.get());

        data.remove(first);
        data.remove(second);
        data.remove(second);
        assertEquals(4, changes.get());

        NbtMapBuilder empty = NbtMap.builder();
        data.save(empty);
        assertFalse(empty.build().containsKey(CloudPersistentDataContainer.STORAGE_KEY));
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
