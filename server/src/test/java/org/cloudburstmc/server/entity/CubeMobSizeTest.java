package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.SCALE;
import static org.junit.jupiter.api.Assertions.*;

class CubeMobSizeTest {

    @Test
    void sizeAndEntityScaleRemainIndependentAcrossSaveAndLoad() {
        EntityAbstractCubeMob slime = assertInstanceOf(EntityAbstractCubeMob.class,
                PersistenceFixtures.entity(EntityTypes.SLIME));
        slime.setSize(2);
        slime.setScale(1.5f);
        slime.setHealth(2);

        NbtMapBuilder tag = NbtMap.builder();
        slime.saveAdditionalData(tag);
        EntityAbstractCubeMob reloaded = assertInstanceOf(EntityAbstractCubeMob.class,
                PersistenceFixtures.entity(EntityTypes.SLIME));
        reloaded.loadAdditionalData(tag.build());

        assertEquals(2, reloaded.getSize());
        assertEquals(4, reloaded.getMaxHealth());
        assertEquals(2f, reloaded.getHealth());
        assertEquals(1.02f, reloaded.getWidth());
        assertEquals(1.5f, reloaded.getScale());
        assertEquals(3f, reloaded.getData().require(SCALE).floatValue());
    }

    @Test
    void sulfurCubeUsesBabyStateInsteadOfMultiplyingNetworkScale() {
        EntityAbstractCubeMob sulfurCube = assertInstanceOf(EntityAbstractCubeMob.class,
                PersistenceFixtures.entity(EntityTypes.SULFUR_CUBE));
        assertFalse(sulfurCube.isBaby());
        sulfurCube.setSize(0);

        assertEquals(1, sulfurCube.getSize());
        assertTrue(sulfurCube.isBaby());
        assertEquals(0.49f, sulfurCube.getWidth());
        assertEquals(1f, sulfurCube.getData().require(SCALE).floatValue());

        sulfurCube.setSize(3);
        assertEquals(2, sulfurCube.getSize());
        assertFalse(sulfurCube.isBaby());
    }
}
