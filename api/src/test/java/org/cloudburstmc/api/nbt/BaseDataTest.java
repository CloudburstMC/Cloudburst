package org.cloudburstmc.api.nbt;

import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.junit.jupiter.api.Test;
import tools.jackson.dataformat.yaml.YAMLMapper;

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
                .putList("Pos", NbtType.FLOAT, List.of(1.5f, 64.0f, -2.5f))
                .putList("Rotation", NbtType.FLOAT, List.of(90.0f, 0.0f))
                .putList("Motion", NbtType.FLOAT, List.of(0.0f, 0.0f, 0.0f))
                .putShort("Fire", (short) 5)
                .putList("Tags", NbtType.STRING, List.of("one", "two"))
                .build();

        EntityData read = mapper.read(tag, EntityData.class);

        assertEquals(List.of(1.5f, 64.0f, -2.5f), read.getPos());
        assertEquals(List.of(90.0f, 0.0f), read.getRotation());
        assertEquals((short) 5, read.getFire());
        assertEquals(List.of("one", "two"), read.getTags());

        NbtMap written = mapper.write(read);

        assertSame(NbtType.FLOAT, assertInstanceOf(NbtList.class, written.get("Pos")).getType());
        assertSame(NbtType.FLOAT, assertInstanceOf(NbtList.class, written.get("Rotation")).getType());
        assertSame(NbtType.FLOAT, assertInstanceOf(NbtList.class, written.get("Motion")).getType());
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
        EntityData.Effect firstEffect = new EntityData.Effect();
        firstEffect.setId((byte) 1);
        firstEffect.setAmplifier((byte) 2);
        firstEffect.setDuration(100);
        firstEffect.setAmbient(true);
        firstEntity.setActiveEffects(List.of(firstEffect));
        EntityData secondEntity = new EntityData();
        EntityData.Effect secondEffect = new EntityData.Effect();
        secondEffect.setId((byte) 1);
        secondEffect.setAmplifier((byte) 2);
        secondEffect.setDuration(100);
        secondEffect.setAmbient(true);
        secondEntity.setActiveEffects(List.of(secondEffect));

        assertEquals(firstEntity, secondEntity);
        assertEquals(firstEntity.hashCode(), secondEntity.hashCode());

        NbtMap tag = NbtMap.builder()
                .putByte("isMovable", (byte) 0)
                .putString("CustomName", "Box")
                .build();

        assertEquals(mapper.read(tag, BlockEntityData.class), mapper.read(tag, BlockEntityData.class));
    }
}
