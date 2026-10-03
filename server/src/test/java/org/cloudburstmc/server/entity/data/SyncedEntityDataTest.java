package org.cloudburstmc.server.entity.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.server.network.ProtocolInfo;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;
import static org.junit.jupiter.api.Assertions.*;

class SyncedEntityDataTest {

    @Test
    void requiredFieldsMustBeInitialized() {
        SyncedEntityData data = new SyncedEntityData();

        assertNull(data.get(AIR_SUPPLY));
        assertThrows(IllegalStateException.class, () -> data.require(AIR_SUPPLY));
        data.set(AIR_SUPPLY, (short) 400);
        assertEquals(400, data.require(AIR_SUPPLY).intValue());
        assertThrows(IllegalArgumentException.class, () -> data.require(FLAGS));
    }

    @Test
    void queuedFlagsRemainStableAfterLaterChanges() {
        SyncedEntityData data = new SyncedEntityData();
        data.setFlag(EntityFlag.BLOCKING, true);
        EntityDataMap raised = data.drainChanges();
        data.setFlag(EntityFlag.BLOCKING, false);
        EntityDataMap lowered = data.drainChanges();

        assertTrue(raised.getFlag(EntityFlag.BLOCKING));
        assertFalse(lowered.getFlag(EntityFlag.BLOCKING));
        assertTrue(lowered.isFlagPresent(EntityFlag.BLOCKING));
        assertTrue(lowered.containsKey(FLAGS));
        assertTrue(lowered.containsKey(FLAGS_2));
        assertTrue(data.drainChanges().isEmpty());
    }

    @Test
    void clearingTheLastEnabledFlagReachesTheClientThroughTheCodec() {
        SyncedEntityData data = new SyncedEntityData();
        BedrockCodecHelper helper = ProtocolInfo.getDefaultPacketCodec().createHelper();
        EntityDataMap client = new EntityDataMap();
        ByteBuf buffer = Unpooled.buffer();
        try {
            data.setFlag(EntityFlag.BLOCKING, true);
            helper.writeEntityData(buffer, data.drainChanges());
            helper.readEntityData(buffer, client);
            assertTrue(client.getFlag(EntityFlag.BLOCKING));

            buffer.clear();
            data.setFlag(EntityFlag.BLOCKING, false);
            helper.writeEntityData(buffer, data.drainChanges());
            helper.readEntityData(buffer, client);

            assertTrue(client.isFlagPresent(EntityFlag.BLOCKING));
            assertFalse(client.getFlag(EntityFlag.BLOCKING));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }

    @Test
    void spawnAndFlagSnapshotsRemainStable() {
        SyncedEntityData data = new SyncedEntityData();
        data.setFlag(EntityFlag.BLOCKING, true);
        EntityDataMap spawn = data.snapshot();
        EntityDataMap flags = data.snapshot(FLAGS_2);
        data.setFlag(EntityFlag.BLOCKING, false);

        assertTrue(spawn.getFlag(EntityFlag.BLOCKING));
        assertTrue(flags.getFlag(EntityFlag.BLOCKING));
        spawn.setFlag(EntityFlag.SNEAKING, true);
        assertFalse(data.getFlag(EntityFlag.SNEAKING));
        assertFalse(flags.getFlag(EntityFlag.SNEAKING));
    }

    @Test
    void changesMadeAfterDrainingRemainPending() {
        SyncedEntityData data = new SyncedEntityData();
        data.setFlag(EntityFlag.BLOCKING, true);
        EntityDataMap first = data.drainChanges();
        data.setFlag(EntityFlag.SNEAKING, true);
        EntityDataMap second = data.drainChanges();

        assertFalse(first.getFlag(EntityFlag.SNEAKING));
        assertTrue(second.getFlag(EntityFlag.SNEAKING));
        assertTrue(data.drainChanges().isEmpty());
    }

    @Test
    void unchangedValuesAndFlagsDoNotProduceUpdates() {
        SyncedEntityData data = new SyncedEntityData();
        data.set(HEIGHT, 1.8f);
        data.setFlag(EntityFlag.BLOCKING, true);
        data.setFlag(EntityFlag.SNEAKING, false);
        data.drainChanges();

        data.set(HEIGHT, 1.8f);
        data.setFlag(EntityFlag.BLOCKING, true);
        data.setFlag(EntityFlag.SNEAKING, false);

        assertTrue(data.drainChanges().isEmpty());
    }

    @Test
    void snapshotsDoNotConsumeChangesAndChangesContainLatestValues() {
        SyncedEntityData data = new SyncedEntityData();
        data.set(NAME, "old");
        data.drainChanges();
        data.set(HEIGHT, 1.8f);
        EntityDataMap snapshot = data.snapshot(HEIGHT);
        data.set(HEIGHT, 0.6f);
        EntityDataMap changes = data.drainChanges();

        assertEquals(Float.valueOf(1.8f), snapshot.get(HEIGHT));
        assertEquals(Float.valueOf(0.6f), changes.get(HEIGHT));
        assertEquals(1, changes.size());
        assertFalse(changes.containsKey(NAME));
        snapshot.put(HEIGHT, 10f);
        assertEquals(Float.valueOf(0.6f), data.get(HEIGHT));
    }

    @Test
    void mutableTextCannotModifyStoredMetadata() {
        SyncedEntityData data = new SyncedEntityData();
        StringBuilder name = new StringBuilder("old");
        data.set(NAME, name);
        name.append(" changed");

        assertEquals("old", data.get(NAME));
        assertEquals("old", data.drainChanges().get(NAME));
        data.set(NAME, new StringBuilder("old"));
        assertTrue(data.drainChanges().isEmpty());
    }

    @Test
    void flagFieldsCannotBypassFlagTracking() {
        SyncedEntityData data = new SyncedEntityData();
        EnumMap<EntityFlag, Boolean> flags = new EnumMap<>(EntityFlag.class);

        assertThrows(IllegalArgumentException.class, () -> data.set(FLAGS, flags));
        assertThrows(IllegalArgumentException.class, () -> data.set(FLAGS_2, flags));
        assertThrows(IllegalArgumentException.class, () -> data.get(FLAGS));
        assertThrows(IllegalArgumentException.class, () -> data.get(FLAGS_2));
        assertThrows(IllegalArgumentException.class, () -> data.snapshot(HEIGHT));
        assertTrue(data.drainChanges().isEmpty());
    }
}
