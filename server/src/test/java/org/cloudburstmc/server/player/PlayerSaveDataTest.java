package org.cloudburstmc.server.player;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSaveDataTest {

    private static NbtMap save(PlayerSaveData saveData, PlayerSaveData.Food food) {
        return save(saveData, food, PlayerSaveData.Progress.read(NbtMap.EMPTY, 123, 0.1f));
    }

    private static NbtMap save(PlayerSaveData saveData, PlayerSaveData.Food food, PlayerSaveData.Progress progress) {
        NbtMapBuilder tag = NbtMap.builder();
        progress.write(tag, 0.1f);
        saveData.save(tag, food, null);
        return tag.build();
    }

    @Test
    void newPlayerSaveWritesCreditsZero() {
        PersistenceFixtures.ensureInitialized();
        PlayerSaveData saveData = new PlayerSaveData();
        PlayerSaveData.Food food = saveData.load(NbtMap.EMPTY);
        NbtMap saved = save(saveData, food);

        assertInstanceOf(Byte.class, saved.get("seenCredits"), "seenCredits must be a byte");
        assertEquals((byte) 0, saved.get("seenCredits"));
        assertInstanceOf(Integer.class, saved.get("EXP"), "EXP must be an int");
        assertEquals(0, saved.getInt("EXP"));
        assertEquals(123, saved.getInt("enchantmentSeed"));
        assertFalse(saved.containsKey("Cloudburst.BaseMovementSpeed"));
        assertEquals(20, saved.getInt("foodLevel"));
        assertEquals(5.0f, saved.getFloat("foodSaturationLevel"));
        assertEquals(0.0f, saved.getFloat("foodExhaustionLevel"));
        assertEquals(0, saved.getInt("foodTickTimer"));
        assertFalse(saved.containsKey("firstPlayed"));
        assertFalse(saved.containsKey("lastPlayed"));
        assertTrue(saveData.firstPlayed().isEmpty());
        assertTrue(saveData.lastPlayed().isEmpty());
        assertFalse(saved.containsKey("Level"));
        assertFalse(saved.containsKey("SpawnLevel"));
        assertFalse(saved.containsKey("SpawnX"));
        assertFalse(saved.containsKey("SpawnYaw"));
        assertFalse(saved.containsKey("playerGameType"));
    }

    @Test
    void partialTimestampsWriteBackAsLoaded() {
        PersistenceFixtures.ensureInitialized();

        PlayerSaveData saveData = new PlayerSaveData();
        PlayerSaveData.Food food = saveData.load(NbtMap.builder().putLong("firstPlayed", 1000L).build());
        NbtMap saved = save(saveData, food);
        assertInstanceOf(Long.class, saved.get("firstPlayed"), "firstPlayed must be a long");
        assertEquals(1000L, saved.getLong("firstPlayed"));
        assertFalse(saved.containsKey("lastPlayed"));

        saveData = new PlayerSaveData();
        food = saveData.load(NbtMap.builder()
                .putLong("firstPlayed", 500L)
                .putLong("lastPlayed", 0L)
                .build());
        saved = save(saveData, food);
        assertEquals(500L, saved.getLong("firstPlayed"));
        assertTrue(saved.containsKey("lastPlayed"));
        assertEquals(0L, saved.getLong("lastPlayed"));
    }

    @Test
    void partialSpawnWritesBackAsLoaded() {
        PersistenceFixtures.ensureInitialized();

        PlayerSaveData saveData = new PlayerSaveData();
        PlayerSaveData.Food food = saveData.load(NbtMap.builder().putString("SpawnLevel", "world").build());
        NbtMap saved = save(saveData, food);
        assertEquals("world", saved.getString("SpawnLevel"));
        assertFalse(saved.containsKey("SpawnX"));
        assertFalse(saved.containsKey("SpawnY"));
        assertFalse(saved.containsKey("SpawnZ"));
        assertFalse(saved.containsKey("SpawnYaw"));

        saveData = new PlayerSaveData();
        food = saveData.load(NbtMap.builder()
                .putInt("SpawnX", 1)
                .putInt("SpawnY", 64)
                .putInt("SpawnZ", -3)
                .build());
        saved = save(saveData, food);
        assertFalse(saved.containsKey("SpawnLevel"));
        assertInstanceOf(Integer.class, saved.get("SpawnX"), "SpawnX must be an int");
        assertEquals(1, saved.getInt("SpawnX"));
        assertEquals(64, saved.getInt("SpawnY"));
        assertEquals(-3, saved.getInt("SpawnZ"));
        assertFalse(saved.containsKey("SpawnYaw"));

        saveData = new PlayerSaveData();
        food = saveData.load(NbtMap.builder().putFloat("SpawnYaw", 90.0f).build());
        saved = save(saveData, food);
        assertFalse(saved.containsKey("SpawnLevel"));
        assertInstanceOf(Float.class, saved.get("SpawnYaw"), "SpawnYaw must be a float");
        assertEquals(90.0f, saved.getFloat("SpawnYaw"));
    }

    @Test
    void respawnAnchorRoundTripsAndExplicitClearDropsSavedSpawn() {
        PersistenceFixtures.ensureInitialized();
        PlayerSaveData saveData = new PlayerSaveData();
        NbtMap stored = NbtMap.builder()
                .putString("SpawnLevel", "nether")
                .putInt("SpawnX", 4)
                .putInt("SpawnY", 64)
                .putInt("SpawnZ", -8)
                .putFloat("SpawnYaw", 0.0f)
                .putString("SpawnType", "RESPAWN_ANCHOR")
                .build();
        PlayerSaveData.Food food = saveData.load(stored);
        assertEquals(RespawnConfig.SpawnType.RESPAWN_ANCHOR, saveData.respawn().type());
        NbtMap saved = save(saveData, food);
        stored.forEach((key, value) -> assertEquals(value, saved.get(key), key));

        saveData.clearRespawn();
        NbtMap cleared = save(saveData, food);
        stored.keySet().forEach(key -> assertFalse(cleared.containsKey(key), key));
    }

    @Test
    void forcedRespawnRoundTripsWithAnExplicitType() {
        PlayerSaveData saveData = new PlayerSaveData();
        Vector3i position = Vector3i.from(4, 70, -8);
        saveData.setRespawn("world", position, 90f, RespawnConfig.SpawnType.FORCED);
        PlayerSaveData.Food food = new PlayerSaveData.Food(20, 5f, 0f, 0);

        NbtMap tag = save(saveData, food);
        PlayerSaveData reloaded = new PlayerSaveData();
        reloaded.load(tag);

        assertEquals("FORCED", tag.getString("SpawnType"));
        assertEquals(RespawnConfig.SpawnType.FORCED, reloaded.respawn().type());
        assertEquals(position, reloaded.respawn().position());
        assertEquals(90f, reloaded.respawn().yaw());
    }

    @Test
    void loneSpawnCoordinateWritesBack() {
        PersistenceFixtures.ensureInitialized();

        PlayerSaveData saveData = new PlayerSaveData();
        PlayerSaveData.Food food = saveData.load(NbtMap.builder().putInt("SpawnX", 5).build());
        NbtMap saved = save(saveData, food);
        assertInstanceOf(Integer.class, saved.get("SpawnX"), "SpawnX must be an int");
        assertEquals(5, saved.getInt("SpawnX"));
        assertFalse(saved.containsKey("SpawnY"));
        assertFalse(saved.containsKey("SpawnZ"));
    }

    @Test
    void twoSpawnCoordinatesWriteBack() {
        PersistenceFixtures.ensureInitialized();

        PlayerSaveData saveData = new PlayerSaveData();
        PlayerSaveData.Food food = saveData.load(NbtMap.builder()
                .putInt("SpawnX", 1)
                .putInt("SpawnY", 64)
                .build());
        NbtMap saved = save(saveData, food);
        assertInstanceOf(Integer.class, saved.get("SpawnX"), "SpawnX must be an int");
        assertEquals(1, saved.getInt("SpawnX"));
        assertInstanceOf(Integer.class, saved.get("SpawnY"), "SpawnY must be an int");
        assertEquals(64, saved.getInt("SpawnY"));
        assertFalse(saved.containsKey("SpawnZ"));
    }

    @Test
    void mistypedSpawnZKeepsGoodCoordinates() {
        PersistenceFixtures.ensureInitialized();

        PlayerSaveData saveData = new PlayerSaveData();
        PlayerSaveData.Food food = saveData.load(NbtMap.builder()
                .putInt("SpawnX", 1)
                .putInt("SpawnY", 64)
                .putString("SpawnZ", "high")
                .build());
        NbtMap saved = save(saveData, food);
        assertEquals(1, saved.getInt("SpawnX"));
        assertEquals(64, saved.getInt("SpawnY"));
        assertFalse(saved.containsKey("SpawnZ"));
    }

    @Test
    void newRespawnReplacesStoredCoordinates() {
        PersistenceFixtures.ensureInitialized();
        PlayerSaveData saveData = new PlayerSaveData();
        PlayerSaveData.Food food = saveData.load(NbtMap.builder()
                .putString("SpawnLevel", "world")
                .putInt("SpawnX", 1)
                .putInt("SpawnY", 64)
                .putInt("SpawnZ", -3)
                .putFloat("SpawnYaw", 90.0f)
                .build());
        NbtMap saved = save(saveData, food);
        assertEquals(1, saved.getInt("SpawnX"));

        saveData.setRespawn("world", Vector3i.from(10, 70, 30), 90f, RespawnConfig.SpawnType.BED);
        saved = save(saveData, food);
        assertEquals(10, saved.getInt("SpawnX"));
        assertEquals(70, saved.getInt("SpawnY"));
        assertEquals(30, saved.getInt("SpawnZ"));
        assertEquals("BED", saved.getString("SpawnType"));
    }

    @Test
    void hookRoundTripPreservesEveryKeyAndType() {
        PersistenceFixtures.ensureInitialized();
        NbtMap stored = NbtMap.builder()
                .putInt("EXP", 7)
                .putInt("expLevel", 3)
                .putInt("enchantmentSeed", 456)
                .putBoolean("seenCredits", true)
                .putFloat("Cloudburst.BaseMovementSpeed", 0.2f)
                .putInt("foodLevel", 14)
                .putFloat("foodSaturationLevel", 3.5f)
                .putFloat("foodExhaustionLevel", 1.25f)
                .putInt("foodTickTimer", 9)
                .putLong("firstPlayed", 1000L)
                .putLong("lastPlayed", 2000L)
                .putString("Level", "world")
                .putString("SpawnLevel", "world")
                .putInt("SpawnX", 1)
                .putInt("SpawnY", 64)
                .putInt("SpawnZ", -3)
                .putFloat("SpawnYaw", 90.0f)
                .putInt("playerGameType", 1)
                .build();

        PlayerSaveData saveData = new PlayerSaveData();
        PlayerSaveData.Food food = saveData.load(stored);

        PlayerSaveData.Progress progress = PlayerSaveData.Progress.read(stored, 123, 0.1f);
        assertEquals(new PlayerSaveData.Progress(7, 3, 456, true, 0.2f), progress);
        assertEquals(1000L, saveData.firstPlayed().getAsLong());
        assertEquals(2000L, saveData.lastPlayed().getAsLong());
        assertEquals("world", saveData.levelId());
        assertEquals("world", saveData.respawn().levelId());
        assertEquals(Vector3i.from(1, 64, -3), saveData.respawn().position());
        assertEquals(90.0f, saveData.respawn().yaw());
        assertEquals(GameMode.CREATIVE, saveData.gameMode());

        NbtMap saved = save(saveData, food, progress);
        assertInstanceOf(Integer.class, saved.get("EXP"), "EXP must be an int");
        assertEquals(7, saved.getInt("EXP"));
        assertInstanceOf(Integer.class, saved.get("expLevel"), "expLevel must be an int");
        assertEquals(3, saved.getInt("expLevel"));
        assertEquals(456, saved.getInt("enchantmentSeed"));
        assertEquals(0.2f, saved.getFloat("Cloudburst.BaseMovementSpeed"));
        assertInstanceOf(Byte.class, saved.get("seenCredits"), "seenCredits must be a byte");
        assertEquals((byte) 1, saved.get("seenCredits"));
        assertInstanceOf(Integer.class, saved.get("foodLevel"), "foodLevel must be an int");
        assertEquals(14, saved.getInt("foodLevel"));
        assertInstanceOf(Float.class, saved.get("foodSaturationLevel"), "foodSaturationLevel must be a float");
        assertEquals(3.5f, saved.getFloat("foodSaturationLevel"));
        assertInstanceOf(Float.class, saved.get("foodExhaustionLevel"), "foodExhaustionLevel must be a float");
        assertEquals(1.25f, saved.getFloat("foodExhaustionLevel"));
        assertInstanceOf(Integer.class, saved.get("foodTickTimer"), "foodTickTimer must be an int");
        assertEquals(9, saved.getInt("foodTickTimer"));
        assertInstanceOf(Long.class, saved.get("firstPlayed"), "firstPlayed must be a long");
        assertEquals(1000L, saved.getLong("firstPlayed"));
        assertInstanceOf(Long.class, saved.get("lastPlayed"), "lastPlayed must be a long");
        assertEquals(2000L, saved.getLong("lastPlayed"));
        assertEquals("world", saved.getString("Level"));
        assertEquals("world", saved.getString("SpawnLevel"));
        assertInstanceOf(Integer.class, saved.get("SpawnX"), "SpawnX must be an int");
        assertEquals(1, saved.getInt("SpawnX"));
        assertEquals(64, saved.getInt("SpawnY"));
        assertEquals(-3, saved.getInt("SpawnZ"));
        assertInstanceOf(Float.class, saved.get("SpawnYaw"), "SpawnYaw must be a float");
        assertEquals(90.0f, saved.getFloat("SpawnYaw"));
        assertInstanceOf(Integer.class, saved.get("playerGameType"), "playerGameType must be an int");
        assertEquals(1, saved.getInt("playerGameType"));
    }

    @Test
    void mistypedHookKeysStayDropped() {
        PersistenceFixtures.ensureInitialized();
        PlayerSaveData saveData = new PlayerSaveData();
        PlayerSaveData.Food food = saveData.load(NbtMap.builder()
                .putString("firstPlayed", "soon")
                .putInt("SpawnLevel", 123)
                .putString("SpawnX", "1")
                .putString("foodLevel", "full")
                .build());
        NbtMap saved = save(saveData, food);
        assertFalse(saved.containsKey("firstPlayed"));
        assertFalse(saved.containsKey("SpawnLevel"));
        assertFalse(saved.containsKey("SpawnX"));
        assertInstanceOf(Integer.class, saved.get("foodLevel"), "foodLevel must be an int");
        assertEquals(20, saved.getInt("foodLevel"));
    }

    @Test
    void nonByteSlotBindsSlotZero() {
        PersistenceFixtures.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        NbtMap base = ItemStackNbtCodec.serializeItem(diamond);
        NbtMapBuilder shortBuilder = NbtMap.builder();
        shortBuilder.putAll(base);
        shortBuilder.putShort("Slot", (short) 2);
        NbtMapBuilder intBuilder = NbtMap.builder();
        intBuilder.putAll(base);
        intBuilder.putInt("Slot", 2);
        NbtMapBuilder stringBuilder = NbtMap.builder();
        stringBuilder.putAll(base);
        stringBuilder.putString("Slot", "2");
        List<NbtMap> entries = List.of(shortBuilder.build(), intBuilder.build(), stringBuilder.build());
        for (NbtMap entry : entries) {
            CloudContainer inventory = new CloudContainer(36);
            NbtMap tag = NbtMap.builder()
                    .putList("Inventory", NbtType.COMPOUND, List.of(entry))
                    .build();
            PlayerSaveData.loadInventories(tag, inventory, new CloudContainer(27));

            assertEquals(diamond, inventory.getItem(0),
                    "a " + entry.get("Slot").getClass().getSimpleName() + " Slot must bind slot 0");
            assertTrue(inventory.getItem(2).isEmpty(),
                    "a " + entry.get("Slot").getClass().getSimpleName() + " Slot must not bind slot 2");
        }
    }
    @Test
    void sparseInventoryLoadPreservesUnlistedSlotsAndSavesBothContainers() {
        PersistenceFixtures.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        ItemStack ingot = ItemStack.from(ItemTypes.GOLD_INGOT, 3);
        CloudContainer inventory = new CloudContainer(36);
        CloudContainer enderChest = new CloudContainer(27);
        inventory.setItem(1, ingot);
        enderChest.setItem(4, ingot);

        NbtMap item = ItemStackNbtCodec.serializeItem(diamond, 0);
        NbtMap tag = NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of(item))
                .putList("EnderChestInventory", NbtType.COMPOUND, List.of())
                .build();
        PlayerSaveData.loadInventories(tag, inventory, enderChest);

        NbtMapBuilder saved = NbtMap.builder();
        PlayerSaveData.saveInventories(saved, inventory, enderChest);
        NbtMap result = saved.build();
        assertEquals(List.of(item, ItemStackNbtCodec.serializeItem(ingot, 1)),
                result.getList("Inventory", NbtType.COMPOUND));
        assertEquals(List.of(ItemStackNbtCodec.serializeItem(ingot, 4)),
                result.getList("EnderChestInventory", NbtType.COMPOUND));
    }

    @Test
    void invalidInventorySlotDoesNotPartiallyApplyTheList() {
        PersistenceFixtures.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        CloudContainer inventory = new CloudContainer(36);
        CloudContainer enderChest = new CloudContainer(27);
        NbtMap tag = NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of(
                        ItemStackNbtCodec.serializeItem(diamond, 0),
                        ItemStackNbtCodec.serializeItem(diamond, 36)))
                .build();

        assertThrows(IndexOutOfBoundsException.class, () -> PlayerSaveData.loadInventories(tag, inventory, enderChest));
        assertTrue(inventory.getItem(0).isEmpty());
    }

    @Test
    void invalidMovementSpeedUsesTheDefault() {
        for (float value : new float[]{-1f, Float.NaN, Float.POSITIVE_INFINITY}) {
            NbtMap tag = NbtMap.builder().putFloat("Cloudburst.BaseMovementSpeed", value).build();

            PlayerSaveData.Progress progress = PlayerSaveData.Progress.read(tag, 123, 0.15f);

            assertEquals(0.15f, progress.baseMovementSpeed());
        }
    }
}
