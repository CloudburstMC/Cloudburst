package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.entity.data.CloudHumanData;
import org.cloudburstmc.server.entity.data.CloudPlayerData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PlayerPersistenceTest {

    private static NbtMap write(CloudPlayerData data) {
        return ServerNbt.MAPPER.write(data);
    }

    private static CloudPlayerData read(NbtMap tag) {
        return ServerNbt.MAPPER.read(tag, CloudPlayerData.class);
    }

    private static CloudPlayerData read(NbtMap tag, CloudPlayerData data) {
        return ServerNbt.MAPPER.readInto(tag, data);
    }

    @Test
    void playerUsesPlayerData() {
        PersistenceFixtures.ensureInitialized();
        assertEquals(CloudHumanData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.PLAYER));
        assertTrue(new CloudPlayerData() instanceof CloudHumanData);
    }

    @Test
    void cloudPlayerResolvesPlayerData() throws Exception {
        assertEquals(CloudPlayer.class,
                CloudPlayer.class.getDeclaredMethod("minimumDataClass").getDeclaringClass());
        assertEquals(CloudHumanData.class,
                ((CloudEntity) PersistenceFixtures.entity(EntityTypes.PLAYER)).minimumDataClass());
    }

    @Test
    void cloudPlayerAssignsLevelBeforeBaseKeys() throws Exception {
        assertEquals(CloudPlayer.class,
                CloudPlayer.class.getDeclaredMethod("loadBaseCustom", NbtMap.class).getDeclaringClass());
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        PersistenceFixtures.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        ItemStack ingot = ItemStack.from(ItemTypes.GOLD_INGOT, 3);

        CloudPlayerData data = new CloudPlayerData();
        data.setExp(7);
        data.setExpLevel(3);
        data.setSeenCredits(true);
        ItemContainer inventory = new CloudContainer(36);
        inventory.setItem(0, diamond);
        data.setInventory(inventory);
        ItemContainer enderChest = new CloudContainer(27);
        enderChest.setItem(4, ingot);
        data.setEnderChest(enderChest);

        NbtMapBuilder writtenBuilder = NbtMap.builder();
        writtenBuilder.putAll(write(data));
        writtenBuilder.putInt("foodLevel", 14);
        writtenBuilder.putFloat("foodSaturationLevel", 3.5f);
        writtenBuilder.putFloat("foodExhaustionLevel", 1.25f);
        writtenBuilder.putInt("foodTickTimer", 9);
        writtenBuilder.putLong("firstPlayed", 1000L);
        writtenBuilder.putLong("lastPlayed", 2000L);
        writtenBuilder.putString("Level", "world");
        writtenBuilder.putString("SpawnLevel", "world");
        writtenBuilder.putInt("SpawnX", 1);
        writtenBuilder.putInt("SpawnY", 64);
        writtenBuilder.putInt("SpawnZ", -3);
        writtenBuilder.putFloat("SpawnYaw", 90.0f);
        writtenBuilder.putInt("playerGameType", 1);
        NbtMap written = writtenBuilder.build();

        assertTrue(written.get("EXP") instanceof Integer, "EXP must be an int");
        assertEquals(7, written.getInt("EXP"));
        assertTrue(written.get("expLevel") instanceof Integer, "expLevel must be an int");
        assertEquals(3, written.getInt("expLevel"));
        assertTrue(written.get("foodLevel") instanceof Integer, "foodLevel must be an int");
        assertEquals(14, written.getInt("foodLevel"));
        assertTrue(written.get("foodSaturationLevel") instanceof Float, "foodSaturationLevel must be a float");
        assertEquals(3.5f, written.getFloat("foodSaturationLevel"));
        assertTrue(written.get("foodExhaustionLevel") instanceof Float, "foodExhaustionLevel must be a float");
        assertTrue(written.get("foodTickTimer") instanceof Integer, "foodTickTimer must be an int");
        assertEquals(9, written.getInt("foodTickTimer"));
        assertTrue(written.get("seenCredits") instanceof Byte, "seenCredits must be a byte");
        assertTrue(written.getBoolean("seenCredits"));
        assertEquals(List.of(ItemStackNbtCodec.serializeItem(diamond, 0)),
                written.getList("Inventory", NbtType.COMPOUND));
        assertEquals(List.of(ItemStackNbtCodec.serializeItem(ingot, 4)),
                written.getList("EnderChestInventory", NbtType.COMPOUND));
        assertTrue(written.get("firstPlayed") instanceof Long, "firstPlayed must be a long");
        assertEquals(1000L, written.getLong("firstPlayed"));
        assertEquals(2000L, written.getLong("lastPlayed"));
        assertEquals("world", written.getString("Level"));
        assertEquals("world", written.getString("SpawnLevel"));
        assertEquals(1, written.getInt("SpawnX"));
        assertEquals(64, written.getInt("SpawnY"));
        assertEquals(-3, written.getInt("SpawnZ"));
        assertEquals(90.0f, written.getFloat("SpawnYaw"));
        assertEquals(1, written.getInt("playerGameType"));

        CloudPlayerData reloaded = read(written);
        assertEquals(write(data), write(reloaded));

        assertEquals(7, reloaded.getExp());
        assertEquals(3, reloaded.getExpLevel());
        assertTrue(reloaded.isSeenCredits());
        assertEquals(36, reloaded.getInventory().size());
        assertEquals(diamond, reloaded.getInventory().getItem(0));
        assertTrue(reloaded.getInventory().getItem(1).isEmpty());
        assertEquals(27, reloaded.getEnderChest().size());
        assertEquals(ingot, reloaded.getEnderChest().getItem(4));
        assertTrue(reloaded.getEnderChest().getItem(0).isEmpty());
    }

    @Test
    void defaultDataWritesTodayDefaults() {
        NbtMapBuilder writtenBuilder = NbtMap.builder();
        writtenBuilder.putAll(write(new CloudPlayerData()));
        writtenBuilder.putInt("foodLevel", 20);
        writtenBuilder.putFloat("foodSaturationLevel", 5.0f);
        writtenBuilder.putFloat("foodExhaustionLevel", 0.0f);
        writtenBuilder.putInt("foodTickTimer", 0);
        NbtMap written = writtenBuilder.build();

        assertEquals(0, written.getInt("EXP"));
        assertEquals(0, written.getInt("expLevel"));
        assertEquals(20, written.getInt("foodLevel"));
        assertEquals(5.0f, written.getFloat("foodSaturationLevel"));
        assertEquals(0.0f, written.getFloat("foodExhaustionLevel"));
        assertEquals(0, written.getInt("foodTickTimer"));
        assertFalse(written.containsKey("Inventory"));
        assertFalse(written.containsKey("EnderChestInventory"));
        assertTrue(written.get("seenCredits") instanceof Byte, "seenCredits must be a byte");
        assertEquals((byte) 0, written.get("seenCredits"));
        assertFalse(written.containsKey("firstPlayed"));
        assertFalse(written.containsKey("lastPlayed"));
        assertFalse(written.containsKey("Level"));
        assertFalse(written.containsKey("SpawnLevel"));
        assertFalse(written.containsKey("SpawnX"));
        assertFalse(written.containsKey("SpawnYaw"));
        assertFalse(written.containsKey("playerGameType"));
        assertFalse(written.containsKey("Skin"));
    }

    @Test
    void defaultPlayerTagKeepsConstructorState() {
        long now = 1234567890L;
        NbtMap tag = NbtMap.builder()
                .putLong("firstPlayed", now)
                .putLong("lastPlayed", now)
                .putList("Pos", NbtType.FLOAT, Arrays.asList(0.5f, 65.0f, 0.5f))
                .putString("Level", "world")
                .putInt("playerGameType", 0)
                .putList("Rotation", NbtType.FLOAT, Arrays.asList(0.0f, 0.0f))
                .build();

        CloudPlayerData data = read(tag);

        assertEquals(now, tag.getLong("firstPlayed"));
        assertEquals(now, tag.getLong("lastPlayed"));
        assertEquals("world", tag.getString("Level"));
        assertEquals(0, tag.getInt("playerGameType"));
        assertEquals(0, data.getExp());
        assertEquals(0, data.getExpLevel());
        assertFalse(data.isSeenCredits());
        assertNull(data.getInventory());
        assertNull(data.getEnderChest());
        assertNull(data.getSkin());

        NbtMap saved = write(data);
        for (String key : List.of("foodLevel", "foodSaturationLevel", "foodExhaustionLevel",
                "foodTickTimer", "firstPlayed", "lastPlayed", "Level", "SpawnLevel",
                "SpawnX", "SpawnY", "SpawnZ", "SpawnYaw", "playerGameType")) {
            assertFalse(saved.containsKey(key), "data object still writes " + key);
        }
    }

    @Test
    void absentKeysStayAbsent() {
        CloudPlayerData data = read(NbtMap.EMPTY);

        assertFalse(data.isSeenCredits());
        assertNull(data.getSkin());
        assertNull(data.getInventory());
        assertNull(data.getEnderChest());
        assertEquals(0, data.getExp());
        assertEquals(0, data.getExpLevel());
    }

    @Test
    void cloudPlayerKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                CloudPlayer.class, CloudPlayerData.class,
                Map.of(
                        "armor", "an armor view over the live container in data().getArmor()",
                        "offhand", "an offhand view over the live container in data().getOffhand()",
                        "container", "the live player-inventory container held by data().getInventory()",
                        "enderChestContainer", "the live ender-chest container held by data().getEnderChest()",
                        "inventory", "a view over the live container in data().getInventory()",
                        "hotbar", "a hotbar view over the live container in data().getInventory()",
                        "enderChest", "a view over the live container in data().getEnderChest()",
                        "activeUseStack", "the transient in-use item, never persisted",
                        "level", "the current level reference, not the saved level id in data().getLevel()"));
        Set<String> fields = new HashSet<>();
        for (Field field : CloudPlayer.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("exp", "expLevel", "seenCredits")) {
            assertFalse(fields.contains(removed),
                    "CloudPlayer keeps " + removed + "; state must live in CloudPlayerData only");
        }
        Set<String> dtoFields = new HashSet<>();
        for (Field field : CloudPlayerData.class.getDeclaredFields()) {
            dtoFields.add(field.getName());
        }
        for (String holderOwned : Set.of("foodLevel", "foodSaturation", "foodExhaustion",
                "foodTickTimer", "firstPlayed", "lastPlayed", "level", "spawnLevel",
                "spawnX", "spawnY", "spawnZ", "spawnYaw", "playerGameType")) {
            assertFalse(dtoFields.contains(holderOwned),
                    "CloudPlayerData keeps " + holderOwned + "; state must live in the runtime holders only");
        }
    }

    @Test
    void sparseInventoryReadFillsLiveContainersInPlace() {
        PersistenceFixtures.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        ItemStack ingot = ItemStack.from(ItemTypes.GOLD_INGOT, 3);
        ItemStack kept = ItemStack.from(ItemTypes.IRON_INGOT, 1);

        CloudPlayerData data = new CloudPlayerData();
        ItemContainer inventory = new CloudContainer(36);
        inventory.setItem(1, kept);
        data.setInventory(inventory);
        ItemContainer enderChest = new CloudContainer(27);
        enderChest.setItem(4, kept);
        data.setEnderChest(enderChest);

        read(NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of(ItemStackNbtCodec.serializeItem(diamond, 0)))
                .build(), data);
        assertSame(inventory, data.getInventory());
        assertEquals(diamond, data.getInventory().getItem(0));
        assertEquals(kept, data.getInventory().getItem(1));
        assertSame(enderChest, data.getEnderChest());
        assertEquals(kept, data.getEnderChest().getItem(4));
        read(NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of())
                .putList("EnderChestInventory", NbtType.COMPOUND, List.of())
                .build(), data);
        assertEquals(diamond, data.getInventory().getItem(0));
        assertEquals(kept, data.getInventory().getItem(1));
        assertEquals(kept, data.getEnderChest().getItem(4));

        data.getInventory().setItem(2, ingot);
        assertEquals(
                List.of(ItemStackNbtCodec.serializeItem(diamond, 0), ItemStackNbtCodec.serializeItem(kept, 1),
                        ItemStackNbtCodec.serializeItem(ingot, 2)),
                write(data).getList("Inventory", NbtType.COMPOUND));
    }

    @Test
    void missingSlotGoesToSlotZero() {
        PersistenceFixtures.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);

        CloudPlayerData data = new CloudPlayerData();
        data.setInventory(new CloudContainer(36));
        data.setEnderChest(new CloudContainer(27));

        NbtMapBuilder entryBuilder = NbtMap.builder();
        entryBuilder.putAll(ItemStackNbtCodec.serializeItem(diamond));
        NbtMap entry = entryBuilder.build();
        assertFalse(entry.containsKey("Slot"));
        read(NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of(entry))
                .build(), data);
        assertEquals(diamond, data.getInventory().getItem(0));
    }

    @Test
    void outOfRangeSlotFailsPlayerLoad() {
        PersistenceFixtures.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);

        CloudPlayerData data = new CloudPlayerData();
        ItemContainer inventory = new CloudContainer(36);
        data.setInventory(inventory);

        NbtMap tag = NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of(ItemStackNbtCodec.serializeItem(diamond, 36)))
                .build();
        JacksonException thrown = assertThrows(JacksonException.class, () -> read(tag, data));
        assertTrue(hasCause(thrown, IndexOutOfBoundsException.class));
    }

    private static boolean hasCause(Throwable thrown, Class<? extends Throwable> type) {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return true;
            }
        }
        return false;
    }
}
