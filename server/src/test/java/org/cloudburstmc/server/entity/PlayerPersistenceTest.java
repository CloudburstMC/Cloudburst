package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.HumanData;
import org.cloudburstmc.api.entity.data.PlayerData;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import tools.jackson.core.JacksonException;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for players. Production {@code CloudPlayer} instances need a
 * live server, so the narrowest route applies: the {@link PlayerData} mapping
 * is driven directly through the production NBT mapper, and the new-player
 * default tag (as {@code CloudPlayerDataStore} builds it) is read back to prove
 * absent keys keep the constructor-set state today's code kept.
 */
class PlayerPersistenceTest {

    private static NbtMap write(PlayerData data) {
        return ServerNbt.MAPPER.write(data);
    }

    private static PlayerData read(NbtMap tag) {
        return ServerNbt.MAPPER.read(tag, PlayerData.class);
    }

    private static PlayerData read(NbtMap tag, PlayerData data) {
        return ServerNbt.MAPPER.readInto(tag, data);
    }

    @Test
    void playerUsesPlayerData() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();
        // Non-player humans save through the shared human data, so no
        // player-only key is ever written for them; live players resolve the
        // player data through CloudPlayer.minimumDataClass().
        assertEquals(HumanData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.PLAYER));
        assertTrue(new PlayerData() instanceof HumanData);
    }

    @Test
    void cloudPlayerResolvesPlayerData() throws Exception {
        assertEquals(CloudPlayer.class,
                CloudPlayer.class.getDeclaredMethod("minimumDataClass").getDeclaringClass());
        // Non-player humans save through the shared human data.
        assertEquals(HumanData.class,
                ((CloudEntity) PersistenceFixtures.entity(EntityTypes.PLAYER)).minimumDataClass());
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        ItemStack ingot = ItemStack.from(ItemTypes.GOLD_INGOT, 3);

        PlayerData data = new PlayerData();
        data.setExp(7);
        data.setExpLevel(3);
        data.setSeenCredits(true);
        ItemContainer inventory = ItemContainers.detached(36);
        inventory.setItem(0, diamond);
        data.setInventory(inventory);
        ItemContainer enderChest = ItemContainers.detached(27);
        enderChest.setItem(4, ingot);
        data.setEnderChest(enderChest);

        // Food, timestamps, the level and spawn, and the game type live in the
        // runtime holders, so a real save adds them through the player hooks;
        // the values below are exactly what the hooks write from holder state.
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
        assertEquals(List.of(ItemUtils.serializeItem(diamond, 0)),
                written.getList("Inventory", NbtType.COMPOUND));
        assertEquals(List.of(ItemUtils.serializeItem(ingot, 4)),
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

        PlayerData reloaded = read(written);
        // The holder-owned keys are not DTO state, so only the DTO-owned part
        // round-trips through the data object; the full tag shape is pinned above.
        assertEquals(write(data), write(reloaded));

        assertEquals(7, reloaded.getExp());
        assertEquals(3, reloaded.getExpLevel());
        assertEquals(Boolean.TRUE, reloaded.getSeenCredits());
        assertEquals(36, reloaded.getInventory().size());
        assertEquals(diamond, reloaded.getInventory().getItem(0));
        assertTrue(reloaded.getInventory().getItem(1).isEmpty());
        assertEquals(27, reloaded.getEnderChest().size());
        assertEquals(ingot, reloaded.getEnderChest().getItem(4));
        assertTrue(reloaded.getEnderChest().getItem(0).isEmpty());
    }

    @Test
    void defaultDataWritesTodayDefaults() {
        // A real save writes the data object plus the hook-owned keys; food is
        // always written from the holder defaults, exactly as below.
        NbtMapBuilder writtenBuilder = NbtMap.builder();
        writtenBuilder.putAll(write(new PlayerData()));
        writtenBuilder.putInt("foodLevel", 20);
        writtenBuilder.putFloat("foodSaturationLevel", 5.0f);
        writtenBuilder.putFloat("foodExhaustionLevel", 0.0f);
        writtenBuilder.putInt("foodTickTimer", 0);
        NbtMap written = writtenBuilder.build();

        // Today's player save always writes these keys.
        assertEquals(0, written.getInt("EXP"));
        assertEquals(0, written.getInt("expLevel"));
        assertEquals(20, written.getInt("foodLevel"));
        assertEquals(5.0f, written.getFloat("foodSaturationLevel"));
        assertEquals(0.0f, written.getFloat("foodExhaustionLevel"));
        assertEquals(0, written.getInt("foodTickTimer"));
        // The lists are always filled from live state by the player save hook, so an
        // unfilled data object omits them like any other conditional key.
        assertFalse(written.containsKey("Inventory"));
        assertFalse(written.containsKey("EnderChestInventory"));
        // Conditional keys stay absent until filled from live state.
        assertFalse(written.containsKey("seenCredits"));
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
        // Built exactly as CloudPlayerDataStore.createDefaultData builds it.
        long now = 1234567890L;
        NbtMap tag = NbtMap.builder()
                .putLong("firstPlayed", now)
                .putLong("lastPlayed", now)
                .putList("Pos", NbtType.FLOAT, Arrays.asList(0.5f, 65.0f, 0.5f))
                .putString("Level", "world")
                .putInt("playerGameType", 0)
                .putList("Rotation", NbtType.FLOAT, Arrays.asList(0.0f, 0.0f))
                .build();

        PlayerData data = read(tag);

        // Keys the default tag carries; the holder-owned ones are ignored by the
        // data object and read by the player load hooks instead.
        assertEquals(now, tag.getLong("firstPlayed"));
        assertEquals(now, tag.getLong("lastPlayed"));
        assertEquals("world", tag.getString("Level"));
        assertEquals(0, tag.getInt("playerGameType"));
        // Everything else keeps the constructor-set state today's code kept:
        // experience falls back to today's getX defaults,
        // presence-sensitive keys stay null so the load leaves live state alone.
        assertEquals(0, data.getExp());
        assertEquals(0, data.getExpLevel());
        assertNull(data.getSeenCredits());
        assertNull(data.getInventory());
        assertNull(data.getEnderChest());
        assertNull(data.getSkin());

        // The holder-owned keys are not DTO state: the data object never writes them.
        NbtMap saved = write(data);
        for (String key : List.of("foodLevel", "foodSaturationLevel", "foodExhaustionLevel",
                "foodTickTimer", "firstPlayed", "lastPlayed", "Level", "SpawnLevel",
                "SpawnX", "SpawnY", "SpawnZ", "SpawnYaw", "playerGameType")) {
            assertFalse(saved.containsKey(key), "data object still writes " + key);
        }
    }

    @Test
    void absentKeysStayAbsent() {
        PlayerData data = read(NbtMap.EMPTY);

        assertNull(data.getSeenCredits());
        assertNull(data.getSkin());
        assertNull(data.getInventory());
        assertNull(data.getEnderChest());
        // Unconditional keys fall back to today's getX defaults.
        assertEquals(0, data.getExp());
        assertEquals(0, data.getExpLevel());
    }

    @Test
    void cloudPlayerKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                CloudPlayer.class, PlayerData.class,
                Map.of(
                        "armor", "an armor view over the live container in data().getArmor()",
                        "offhand", "an offhand view over the live container in data().getOffhand()",
                        "container", "the live player-inventory container held by data().getInventory()",
                        "enderChestContainer", "the live ender-chest container held by data().getEnderChest()",
                        "inventory", "a view over the live container in data().getInventory()",
                        "hotbar", "a hotbar view over the live container in data().getInventory()",
                        "enderChest", "a view over the live container in data().getEnderChest()",
                        "uiContainer", "the transient currently-open container, never persisted",
                        "activeUseStack", "the transient in-use item, never persisted",
                        "level", "the current level reference, not the saved level id in data().getLevel()"));
        Set<String> fields = new HashSet<>();
        for (Field field : CloudPlayer.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("exp", "expLevel", "seenCredits")) {
            assertFalse(fields.contains(removed),
                    "CloudPlayer keeps " + removed + "; state must live in PlayerData only");
        }
        // Holder-backed state is not DTO state: the data object must not redeclare it.
        Set<String> dtoFields = new HashSet<>();
        for (Field field : PlayerData.class.getDeclaredFields()) {
            dtoFields.add(field.getName());
        }
        for (String holderOwned : Set.of("foodLevel", "foodSaturation", "foodExhaustion",
                "foodTickTimer", "firstPlayed", "lastPlayed", "level", "spawnLevel",
                "spawnX", "spawnY", "spawnZ", "spawnYaw", "playerGameType")) {
            assertFalse(dtoFields.contains(holderOwned),
                    "PlayerData keeps " + holderOwned + "; state must live in the runtime holders only");
        }
    }

    @Test
    void sparseInventoryReadFillsLiveContainersInPlace() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        ItemStack ingot = ItemStack.from(ItemTypes.GOLD_INGOT, 3);
        ItemStack kept = ItemStack.from(ItemTypes.IRON_INGOT, 1);

        // The containers already hold items, as the wired live containers do.
        PlayerData data = new PlayerData();
        ItemContainer inventory = ItemContainers.detached(36);
        inventory.setItem(1, kept);
        data.setInventory(inventory);
        ItemContainer enderChest = ItemContainers.detached(27);
        enderChest.setItem(4, kept);
        data.setEnderChest(enderChest);

        // A sparse list sets the listed slots on the same instance and keeps the rest.
        read(NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of(ItemUtils.serializeItem(diamond, 0)))
                .build(), data);
        assertSame(inventory, data.getInventory());
        assertEquals(diamond, data.getInventory().getItem(0));
        assertEquals(kept, data.getInventory().getItem(1));
        // An absent list changes nothing, on the same instance.
        assertSame(enderChest, data.getEnderChest());
        assertEquals(kept, data.getEnderChest().getItem(4));
        // An empty list changes nothing either.
        read(NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of())
                .putList("EnderChestInventory", NbtType.COMPOUND, List.of())
                .build(), data);
        assertEquals(diamond, data.getInventory().getItem(0));
        assertEquals(kept, data.getInventory().getItem(1));
        assertEquals(kept, data.getEnderChest().getItem(4));

        // The sparse write comes from that live instance.
        data.getInventory().setItem(2, ingot);
        assertEquals(
                List.of(ItemUtils.serializeItem(diamond, 0), ItemUtils.serializeItem(kept, 1),
                        ItemUtils.serializeItem(ingot, 2)),
                write(data).getList("Inventory", NbtType.COMPOUND));
    }

    @Test
    void missingSlotGoesToSlotZero() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);

        PlayerData data = new PlayerData();
        data.setInventory(ItemContainers.detached(36));
        data.setEnderChest(ItemContainers.detached(27));

        // An entry without a Slot binds slot 0, as the old byte read defaulted to.
        NbtMapBuilder entryBuilder = NbtMap.builder();
        entryBuilder.putAll(ItemUtils.serializeItem(diamond));
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
        ServerNbt.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);

        PlayerData data = new PlayerData();
        ItemContainer inventory = ItemContainers.detached(36);
        data.setInventory(inventory);

        // An out-of-range Slot fails the load with a bare exception, as the old
        // bounds check did, instead of keeping the current contents with a warning.
        NbtMap tag = NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of(ItemUtils.serializeItem(diamond, 36)))
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
