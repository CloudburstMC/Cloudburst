package org.cloudburstmc.server.nbt;

import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.data.CloudBlockEntityData;
import org.cloudburstmc.server.entity.data.CloudEntityData;
import org.junit.jupiter.api.Test;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaseDataTest {

    private final NbtMapper mapper = NbtMapper.create();

    @Test
    void blockEntityBaseDataRoundTrips() {
        NbtMap tag = NbtMap.builder()
                .putByte("isMovable", (byte) 0)
                .putString("CustomName", "Box")
                .build();

        CloudBlockEntityData read = mapper.read(tag, CloudBlockEntityData.class);

        assertFalse(read.isMovable());
        assertEquals("Box", read.getCustomName());

        NbtMap written = mapper.write(read);

        assertEquals(tag, written);
        assertEquals((byte) 0, assertInstanceOf(Byte.class, written.get("isMovable")));
        assertEquals("Box", assertInstanceOf(String.class, written.get("CustomName")));
    }

    @Test
    void entityBaseDataRoundTrips() {
        NbtMap tag = NbtMap.builder()
                .putBoolean("Invulnerable", true)
                .putList("Tags", NbtType.STRING, List.of("one", "two"))
                .build();

        CloudEntityData read = mapper.read(tag, CloudEntityData.class);

        assertTrue(read.isInvulnerable());
        assertEquals(new LinkedHashSet<>(List.of("one", "two")), read.getTags());

        NbtMap written = mapper.write(read);

        assertEquals((byte) 1, assertInstanceOf(Byte.class, written.get("Invulnerable")));
        assertSame(NbtType.STRING, assertInstanceOf(NbtList.class, written.get("Tags")).getType());
    }

    @Test
    void ownedKeysExcludeIdentity() {
        assertFalse(mapper.ownedKeys(CloudBlockEntityData.class).contains("id"));
        assertFalse(mapper.ownedKeys(CloudBlockEntityData.class).contains("x"));
        assertFalse(mapper.ownedKeys(CloudEntityData.class).contains("identifier"));
    }

    @Test
    void baseDataBindsFromYaml() throws Exception {
        CloudBlockEntityData read = new YAMLMapper().readValue("CustomName: Box", CloudBlockEntityData.class);

        assertEquals("Box", read.getCustomName());
    }

    @Test
    void baseDataUsesValueEquality() {
        CloudBlockEntityData firstBlock = new CloudBlockEntityData();
        firstBlock.setMovable(false);
        firstBlock.setCustomName("Box");
        CloudBlockEntityData secondBlock = new CloudBlockEntityData();
        secondBlock.setMovable(false);
        secondBlock.setCustomName("Box");

        assertEquals(firstBlock, secondBlock);
        assertEquals(firstBlock.hashCode(), secondBlock.hashCode());

        CloudEntityData firstEntity = new CloudEntityData();
        firstEntity.setInvulnerable(true);
        firstEntity.setTags(new LinkedHashSet<>(List.of("one", "two")));
        CloudEntityData secondEntity = new CloudEntityData();
        secondEntity.setInvulnerable(true);
        secondEntity.setTags(new LinkedHashSet<>(List.of("one", "two")));

        assertEquals(firstEntity, secondEntity);
        assertEquals(firstEntity.hashCode(), secondEntity.hashCode());

        NbtMap tag = NbtMap.builder()
                .putByte("isMovable", (byte) 0)
                .putString("CustomName", "Box")
                .build();

        assertEquals(mapper.read(tag, CloudBlockEntityData.class), mapper.read(tag, CloudBlockEntityData.class));
    }
}
