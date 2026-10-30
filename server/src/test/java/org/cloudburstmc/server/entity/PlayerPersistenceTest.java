package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.HumanData;
import org.cloudburstmc.api.entity.data.PlayerData;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
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

    @Test
    void playerUsesPlayerData() {
        PersistenceFixtures.ensureInitialized();
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
        data.setFoodLevel(14);
        data.setFoodSaturation(3.5f);
        data.setFoodExhaustion(1.25f);
        data.setFoodTickTimer(9);
        data.setSeenCredits(true);
        ItemContainer inventory = ItemContainers.detached(36);
        inventory.setItem(0, diamond);
        data.setInventory(inventory);
        ItemContainer enderChest = ItemContainers.detached(27);
        enderChest.setItem(4, ingot);
        data.setEnderChest(enderChest);
        data.setFirstPlayed(1000L);
        data.setLastPlayed(2000L);
        data.setLevel("world");
        data.setSpawnLevel("world");
        data.setSpawnX(1);
        data.setSpawnY(64);
        data.setSpawnZ(-3);
        data.setSpawnYaw(90.0f);
        data.setPlayerGameType(1);

        NbtMap written = write(data);

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
        // The mapper may coerce whole numbers across numeric types on read, so the
        // stable guarantee is NBT equality; fields are asserted below one by one.
        assertEquals(written, write(reloaded));

        assertEquals(7, reloaded.getExp());
        assertEquals(3, reloaded.getExpLevel());
        assertEquals(14, reloaded.getFoodLevel());
        assertEquals(3.5f, reloaded.getFoodSaturation());
        assertEquals(1.25f, reloaded.getFoodExhaustion());
        assertEquals(9, reloaded.getFoodTickTimer());
        assertEquals(Boolean.TRUE, reloaded.getSeenCredits());
        assertEquals(36, reloaded.getInventory().size());
        assertEquals(diamond, reloaded.getInventory().getItem(0));
        assertTrue(reloaded.getInventory().getItem(1).isEmpty());
        assertEquals(27, reloaded.getEnderChest().size());
        assertEquals(ingot, reloaded.getEnderChest().getItem(4));
        assertTrue(reloaded.getEnderChest().getItem(0).isEmpty());
        assertEquals(1000L, reloaded.getFirstPlayed());
        assertEquals(2000L, reloaded.getLastPlayed());
        assertEquals("world", reloaded.getLevel());
        assertEquals("world", reloaded.getSpawnLevel());
        assertEquals(1, reloaded.getSpawnX());
        assertEquals(64, reloaded.getSpawnY());
        assertEquals(-3, reloaded.getSpawnZ());
        assertEquals(90.0f, reloaded.getSpawnYaw());
        assertEquals(1, reloaded.getPlayerGameType());
    }

    @Test
    void defaultDataWritesTodayDefaults() {
        NbtMap written = write(new PlayerData());

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

        // Keys the default tag carries.
        assertEquals(now, data.getFirstPlayed());
        assertEquals(now, data.getLastPlayed());
        assertEquals("world", data.getLevel());
        assertEquals(0, data.getPlayerGameType());
        // Everything else keeps the constructor-set state today's code kept:
        // experience and food fall back to today's getX defaults,
        // presence-sensitive keys stay null so the load leaves live state alone.
        assertEquals(0, data.getExp());
        assertEquals(0, data.getExpLevel());
        assertEquals(20, data.getFoodLevel());
        assertEquals(5.0f, data.getFoodSaturation());
        assertEquals(0.0f, data.getFoodExhaustion());
        assertEquals(0, data.getFoodTickTimer());
        assertNull(data.getSeenCredits());
        assertNull(data.getInventory());
        assertNull(data.getEnderChest());
        assertNull(data.getSkin());
        assertNull(data.getSpawnLevel());
        assertNull(data.getSpawnX());
        assertNull(data.getSpawnYaw());
    }

    @Test
    void absentKeysStayAbsent() {
        PlayerData data = read(NbtMap.EMPTY);

        assertNull(data.getSeenCredits());
        assertNull(data.getFirstPlayed());
        assertNull(data.getLastPlayed());
        assertNull(data.getLevel());
        assertNull(data.getSpawnLevel());
        assertNull(data.getSpawnX());
        assertNull(data.getSpawnY());
        assertNull(data.getSpawnZ());
        assertNull(data.getSpawnYaw());
        assertNull(data.getPlayerGameType());
        assertNull(data.getSkin());
        assertNull(data.getInventory());
        assertNull(data.getEnderChest());
        // Unconditional keys fall back to today's getX defaults.
        assertEquals(0, data.getExp());
        assertEquals(20, data.getFoodLevel());
        assertEquals(5.0f, data.getFoodSaturation());
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
    }
}
