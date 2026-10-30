package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.skin.data.ImageData;
import org.cloudburstmc.api.player.skin.data.SkinAnimation;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.cloudburstmc.server.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the human data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types. The {@code PLAYER} type
 * builds an {@link EntityHuman} through the registry factory; production
 * {@code CloudPlayer} instances need a live server and cannot be built here.
 */
class HumanPersistenceTest {

    private static EntityHuman human() {
        return (EntityHuman) PersistenceFixtures.entity(EntityTypes.PLAYER);
    }

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    private static NbtMap skinTag() {
        byte[] skinImage = new byte[64 * 32 * 4];
        byte[] capeImage = new byte[32 * 32 * 4];
        NbtMap animation = NbtMap.builder()
                .putFloat("Frames", 1.0f)
                .putInt("Type", 0)
                .putInt("ImageWidth", 32)
                .putInt("ImageHeight", 32)
                .putByteArray("Image", capeImage)
                .build();
        return NbtMap.builder()
                .putByteArray("Data", skinImage)
                .putInt("SkinImageWidth", 64)
                .putInt("SkinImageHeight", 32)
                .putString("ModelId", "geometry.humanoid.custom")
                .putString("PlayFabId", "playfab")
                .putString("CapeId", "cape")
                .putByteArray("CapeData", capeImage)
                .putInt("CapeImageWidth", 32)
                .putInt("CapeImageHeight", 32)
                // Today's load reads the resource patch as a string but saves it back as bytes.
                .putString("SkinResourcePatch", "{}")
                .putByteArray("GeometryData", "{}".getBytes(UTF_8))
                .putString("GeometryDataEngineVersion", "1.0")
                .putByteArray("SkinAnimationData", "[]".getBytes(UTF_8))
                .putBoolean("PremiumSkin", true)
                .putBoolean("PersonaSkin", false)
                .putBoolean("CapeOnClassicSkin", false)
                .putBoolean("TrustedSkin", true)
                .putBoolean("OverrideSkin", false)
                .putString("ProfileHash", "abc")
                .putList("AnimatedImageData", NbtType.COMPOUND, List.of(animation))
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        EntityHuman human = human();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        ItemStack ingot = ItemStack.from(ItemTypes.GOLD_INGOT, 3);
        NbtMap skin = skinTag();
        // An explicit air slot: today's load stores empty, today's save keeps the raw entry.
        NbtMap airSlot = ItemUtils.serializeItem(ItemStack.EMPTY, 5);
        NbtMap enderSlot = ItemUtils.serializeItem(ingot, 4);

        NbtMap input = NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of(
                        ItemUtils.serializeItem(diamond, 0),
                        airSlot))
                .putString("NameTag", "Steve")
                .putCompound("Skin", skin)
                // Player-only keys a non-player tag can carry: not owned by the
                // human data, so today's save passes them through untouched.
                .putList("EnderChestInventory", NbtType.COMPOUND, List.of(enderSlot))
                .putInt("SpawnX", 11)
                .putInt("Mystery", 7)
                .build();
        human.loadAdditionalData(input);

        assertEquals(diamond, human.container.getItem(0));
        assertTrue(human.container.getItem(5).isEmpty());
        assertEquals("Steve", human.getNameTag());
        assertEquals("geometry.humanoid.custom", human.getSkin().getSkinId());
        assertEquals(64, human.getSkin().getSkinData().getWidth());
        assertTrue(human.getSkin().isPremium());
        // Today's load parses AnimatedImageData but never transfers it onto the skin,
        // so loaded skins always carry zero animations.
        assertEquals(0, human.getSkin().getAnimations().size());
        assertEquals(Utils.dataToUUID(
                String.valueOf(human.getUniqueId()).getBytes(UTF_8),
                skin.getByteArray("Data"),
                "Steve".getBytes(UTF_8)), human.getServerId());

        NbtMap saved = save(human);

        // Inventory is preserved from the raw tag; empty slots keep their air entry.
        assertEquals(List.of(ItemUtils.serializeItem(diamond, 0), airSlot),
                saved.getList("Inventory", NbtType.COMPOUND));
        // Player-only keys pass through byte-exact, with the same element types.
        assertEquals(List.of(enderSlot), saved.getList("EnderChestInventory", NbtType.COMPOUND));
        assertTrue(saved.get("SpawnX") instanceof Integer, "SpawnX must stay an int");
        assertEquals(11, saved.getInt("SpawnX"));
        // The legacy name tag alias survives through the raw-tag merge, untouched.
        assertEquals("Steve", saved.getString("NameTag"));
        // The base path writes the current name tag as CustomName.
        assertEquals("Steve", saved.getString("CustomName"));
        assertEquals(7, saved.getInt("Mystery"));

        NbtMap savedSkin = saved.getCompound("Skin");
        assertEquals("geometry.humanoid.custom", savedSkin.getString("ModelId"));
        assertEquals("playfab", savedSkin.getString("PlayFabId"));
        assertEquals(64, savedSkin.getInt("SkinImageWidth"));
        assertEquals(32, savedSkin.getInt("SkinImageHeight"));
        assertTrue(savedSkin.get("PremiumSkin") instanceof Byte, "PremiumSkin must be a byte");
        assertTrue(savedSkin.getBoolean("PremiumSkin"));
        assertTrue(savedSkin.get("TrustedSkin") instanceof Byte, "TrustedSkin must be a byte");
        // Resource patch is written back as bytes even though it was read as a string.
        assertTrue(savedSkin.get("SkinResourcePatch") instanceof byte[], "SkinResourcePatch must be bytes");
        assertArrayEquals("{}".getBytes(UTF_8), savedSkin.getByteArray("SkinResourcePatch"));
        List<NbtMap> animations = savedSkin.getList("AnimatedImageData", NbtType.COMPOUND);
        // No animations survive the load, so none are written back.
        assertTrue(animations.isEmpty());
        assertFalse(savedSkin.containsKey("AnimatedImageData"));

        // The saved key set is exactly today's: every input key plus the keys
        // today's save always writes (a default save), plus the name-tag
        // aliases written because a name tag was loaded. No player-only key
        // (EXP, food, seenCredits, firstPlayed, Level, Spawn*, playerGameType)
        // may appear: the human data does not own them.
        Set<String> expectedKeys = new HashSet<>(save(human()).keySet());
        expectedKeys.addAll(input.keySet());
        expectedKeys.add("CustomName");
        expectedKeys.add("CustomNameVisible");
        expectedKeys.add("CustomNameAlwaysVisible");
        assertEquals(expectedKeys, saved.keySet());
    }

    @Test
    void skinAnimationsSaveWithExactTypes() {
        EntityHuman human = human();
        human.loadAdditionalData(NbtMap.builder()
                .putCompound("Skin", skinTag())
                .build());
        human.getSkin().getAnimations().add(new SkinAnimation(
                new ImageData(32, 32, new byte[32 * 32 * 4]), 0, 1.0f, 0));

        NbtMap savedSkin = save(human).getCompound("Skin");

        List<NbtMap> animations = savedSkin.getList("AnimatedImageData", NbtType.COMPOUND);
        assertEquals(1, animations.size());
        assertTrue(animations.get(0).get("Frames") instanceof Float, "Frames must be a float");
        assertEquals(1.0f, animations.get(0).getFloat("Frames"));
        assertTrue(animations.get(0).get("Type") instanceof Integer, "Type must be an int");
        assertEquals(0, animations.get(0).getInt("Type"));
        assertTrue(animations.get(0).get("Image") instanceof byte[], "Image must be bytes");
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityHuman human = human();

        NbtMap saved = save(human);

        // Today's human save never writes these keys fresh.
        assertFalse(saved.containsKey("Inventory"));
        assertFalse(saved.containsKey("Skin"));
        assertFalse(saved.containsKey("NameTag"));
        assertFalse(saved.containsKey("CustomName"));
        // Player-only keys belong to the player data, never to a non-player human.
        assertFalse(saved.containsKey("EXP"));
        assertFalse(saved.containsKey("expLevel"));
        assertFalse(saved.containsKey("foodLevel"));
        assertFalse(saved.containsKey("foodSaturationLevel"));
        assertFalse(saved.containsKey("foodExhaustionLevel"));
        assertFalse(saved.containsKey("foodTickTimer"));
        // Base and creature data still write their usual keys.
        assertTrue(saved.containsKey("Pos"));
        assertTrue(saved.containsKey("Offhand"));
        assertTrue(saved.containsKey("Armor"));
        assertEquals("minecraft:player", saved.getString("identifier"));
    }

    @Test
    void missingSkinThrowsOnLoad() {
        EntityHuman human = human();

        // Today's load recomputed the identity unconditionally and threw when a
        // non-player human tag carried no skin; the serializer dropped the NPC.
        assertThrows(NullPointerException.class, () -> human.loadAdditionalData(NbtMap.EMPTY));
        assertNull(human.getSkin());
    }

    @Test
    void skinLoadKeepsExistingNameTag() {
        EntityHuman human = human();
        human.setNameTag("Steve");

        human.loadAdditionalData(NbtMap.builder()
                .putCompound("Skin", skinTag())
                .build());

        assertEquals("Steve", human.getNameTag());
        assertEquals("Steve", save(human).getString("CustomName"));
    }

    @Test
    void humanClearedSkinIsNotWritten() {
        EntityHuman human = human();
        human.loadAdditionalData(NbtMap.builder()
                .putCompound("Skin", skinTag())
                .build());

        assertNotNull(human.getSkin());

        human.setSkin(null);

        NbtMap saved = save(human);
        assertFalse(saved.containsKey("Skin"), "saved tag still has Skin");
    }
}
