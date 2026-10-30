package org.cloudburstmc.api.nbt;

import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.junit.jupiter.api.Test;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class BaseDataTest {

    private final NbtMapper mapper = NbtMapper.create();

    @Test
    void blockEntityBaseDataRoundTrips() {
        NbtMap tag = NbtMap.builder()
                .putByte("isMovable", (byte) 0)
                .putString("CustomName", "Box")
                .build();

        BlockEntityData read = mapper.read(tag, BlockEntityData.class);

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
                .putShort("Fire", (short) 5)
                .putList("Tags", NbtType.STRING, List.of("one", "two"))
                .build();

        EntityData read = mapper.read(tag, EntityData.class);

        assertEquals((short) 5, read.getFire());
        assertEquals(new LinkedHashSet<>(List.of("one", "two")), read.getTags());

        NbtMap written = mapper.write(read);

        assertEquals((short) 5, assertInstanceOf(Short.class, written.get("Fire")));
        assertSame(NbtType.STRING, assertInstanceOf(NbtList.class, written.get("Tags")).getType());
    }

    @Test
    void ownedKeysExcludeIdentity() {
        assertFalse(mapper.ownedKeys(BlockEntityData.class).contains("id"));
        assertFalse(mapper.ownedKeys(BlockEntityData.class).contains("x"));
        assertFalse(mapper.ownedKeys(EntityData.class).contains("identifier"));
    }

    @Test
    void baseDataBindsFromYaml() throws Exception {
        BlockEntityData read = new YAMLMapper().readValue("CustomName: Box", BlockEntityData.class);

        assertEquals("Box", read.getCustomName());
    }

    @Test
    void baseDataUsesValueEquality() {
        BlockEntityData firstBlock = new BlockEntityData();
        firstBlock.setMovable(false);
        firstBlock.setCustomName("Box");
        BlockEntityData secondBlock = new BlockEntityData();
        secondBlock.setMovable(false);
        secondBlock.setCustomName("Box");

        assertEquals(firstBlock, secondBlock);
        assertEquals(firstBlock.hashCode(), secondBlock.hashCode());

        EntityData firstEntity = new EntityData();
        firstEntity.setFire((short) 1);
        firstEntity.setInvulnerable(true);
        firstEntity.setTags(new LinkedHashSet<>(List.of("one", "two")));
        EntityData secondEntity = new EntityData();
        secondEntity.setFire((short) 1);
        secondEntity.setInvulnerable(true);
        secondEntity.setTags(new LinkedHashSet<>(List.of("one", "two")));

        assertEquals(firstEntity, secondEntity);
        assertEquals(firstEntity.hashCode(), secondEntity.hashCode());

        NbtMap tag = NbtMap.builder()
                .putByte("isMovable", (byte) 0)
                .putString("CustomName", "Box")
                .build();

        assertEquals(mapper.read(tag, BlockEntityData.class), mapper.read(tag, BlockEntityData.class));
    }
}
