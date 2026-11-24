package org.cloudburstmc.server.entity;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.HumanEntity;
import org.cloudburstmc.api.entity.Pose;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.skin.data.ImageData;
import org.cloudburstmc.api.player.skin.data.SkinAnimation;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.data.CloudEquipmentData;
import org.cloudburstmc.server.entity.data.CloudHumanData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.item.ItemStackPacketCodec;
import org.cloudburstmc.server.nbt.serializer.SkinNbt;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class HumanPersistenceTest {

    private static CloudHumanEntity human() {
        return (CloudHumanEntity) PersistenceFixtures.entity(EntityTypes.PLAYER);
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
        CloudHumanEntity human = human();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        ItemStack ingot = ItemStack.from(ItemTypes.GOLD_INGOT, 3);
        NbtMap skin = skinTag();
        NbtMap airSlot = ItemStackNbtCodec.serializeItem(ItemStack.EMPTY, 5);
        NbtMap enderSlot = ItemStackNbtCodec.serializeItem(ingot, 4);

        NbtMap input = NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of(
                        ItemStackNbtCodec.serializeItem(diamond, 0),
                        airSlot))
                .putString("CustomName", "Steve")
                .putCompound("Skin", skin)
                .putList("EnderChestInventory", NbtType.COMPOUND, List.of(enderSlot))
                .putInt("SpawnX", 11)
                .putInt("Mystery", 7)
                .build();
        human.loadAdditionalData(input);

        assertEquals(diamond, human.getInventory().getItem(0));
        assertTrue(human.getInventory().getItem(5).isEmpty());
        assertEquals(Component.text("Steve"), human.customName());
        assertEquals("geometry.humanoid.custom", human.getSkin().getSkinId());
        assertEquals(64, human.getSkin().getSkinData().getWidth());
        assertTrue(human.getSkin().isPremium());
        assertEquals(0, human.getSkin().getAnimations().size());

        NbtMap saved = save(human);

        assertEquals(List.of(ItemStackNbtCodec.serializeItem(diamond, 0)),
                saved.getList("Inventory", NbtType.COMPOUND));
        assertEquals(List.of(enderSlot), saved.getList("EnderChestInventory", NbtType.COMPOUND));
        assertTrue(saved.get("SpawnX") instanceof Integer, "SpawnX must stay an int");
        assertEquals(11, saved.getInt("SpawnX"));
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
        assertTrue(savedSkin.get("SkinResourcePatch") instanceof byte[], "SkinResourcePatch must be bytes");
        assertArrayEquals("{}".getBytes(UTF_8), savedSkin.getByteArray("SkinResourcePatch"));
        List<NbtMap> animations = savedSkin.getList("AnimatedImageData", NbtType.COMPOUND);
        assertTrue(animations.isEmpty());
        assertFalse(savedSkin.containsKey("AnimatedImageData"));

        Set<String> expectedKeys = new HashSet<>(save(human()).keySet());
        expectedKeys.addAll(input.keySet());
        expectedKeys.add("CustomNameVisible");
        expectedKeys.add("CustomNameAlwaysVisible");
        assertEquals(expectedKeys, saved.keySet());
    }

    @Test
    void skinAnimationsSaveWithExactTypes() {
        CloudHumanEntity human = human();
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
        CloudHumanEntity human = human();

        NbtMap saved = save(human);

        assertFalse(saved.containsKey("Inventory"));
        assertFalse(saved.containsKey("Skin"));
        assertFalse(saved.containsKey("CustomName"));
        assertFalse(saved.containsKey("EXP"));
        assertFalse(saved.containsKey("expLevel"));
        assertFalse(saved.containsKey("foodLevel"));
        assertFalse(saved.containsKey("foodSaturationLevel"));
        assertFalse(saved.containsKey("foodExhaustionLevel"));
        assertFalse(saved.containsKey("foodTickTimer"));
        assertTrue(saved.containsKey("Pos"));
        assertTrue(saved.containsKey("Offhand"));
        assertTrue(saved.containsKey("Armor"));
        assertEquals("minecraft:player", saved.getString("identifier"));
    }

    @Test
    void missingSkinLoadsWithoutChangingClientIdentity() {
        CloudHumanEntity human = human();
        UUID clientId = human.getServerId();

        human.loadAdditionalData(NbtMap.EMPTY);
        human.customName(Component.text("another name"));
        assertNull(human.getSkin());
        assertNotNull(clientId);
        assertEquals(clientId, human.getServerId());
        assertNotEquals(clientId, human().getServerId());
    }

    @Test
    void inventorySavesLiveChangesAndSelection() {
        CloudHumanEntity source = human();
        HumanEntity human = source;
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        ItemStack ingot = ItemStack.from(ItemTypes.GOLD_INGOT, 3);
        source.loadAdditionalData(NbtMap.builder()
                .putList("Inventory", NbtType.COMPOUND, List.of(
                        ItemStackNbtCodec.serializeItem(diamond, 0),
                        ItemStackNbtCodec.serializeItem(ingot, 36),
                        NbtMap.EMPTY))
                .putInt("SelectedInventorySlot", 9)
                .build());
        assertEquals(0, human.getSelectedHotbarSlot());
        assertEquals(diamond, human.getMainHandItem());
        assertEquals(36, human.getInventory().size());
        assertThrows(IllegalArgumentException.class, () -> human.setSelectedHotbarSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> human.setSelectedHotbarSlot(9));

        human.setMainHandItem(ItemStack.EMPTY);
        human.setSelectedHotbarSlot(8);
        human.setMainHandItem(ingot);

        NbtMap saved = save(source);
        assertEquals(List.of(ItemStackNbtCodec.serializeItem(ingot, 8)),
                saved.getList("Inventory", NbtType.COMPOUND));
        assertEquals(8, saved.getInt("SelectedInventorySlot"));

        CloudHumanEntity restored = human();
        restored.loadAdditionalData(saved);
        assertEquals(8, restored.getSelectedHotbarSlot());
        assertEquals(ingot, restored.getMainHandItem());
        assertEquals(ItemStack.EMPTY, restored.getInventory().getItem(0));
        assertArrayEquals(new ItemStack[]{ingot}, restored.getDrops());

        restored.loadAdditionalData(NbtMap.EMPTY);
        assertEquals(0, restored.getSelectedHotbarSlot());
        var inventory = restored.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            assertTrue(inventory.getItem(slot).isEmpty(), "Expected empty inventory slot " + slot);
        }
        assertFalse(save(restored).containsKey("Inventory"));
        assertFalse(save(restored).containsKey("SelectedInventorySlot"));
    }

    @Test
    void sessionBackedHumanRoundTripsSelectionWithoutStandaloneStorage() {
        PersistenceFixtures.ensureInitialized();
        Location location = Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class));
        CloudHumanEntity source = new CloudHumanEntity(EntityTypes.PLAYER, location, new CloudEquipmentData());
        source.setSelectedHotbarSlot(6);

        NbtMap saved = save(source);
        assertEquals(6, saved.getInt("SelectedInventorySlot"));

        CloudHumanEntity restored = new CloudHumanEntity(EntityTypes.PLAYER, location, new CloudEquipmentData());
        NbtMapBuilder input = saved.toBuilder();
        input.putCompound("Skin", skinTag());
        restored.loadAdditionalData(input.build());
        assertEquals(6, restored.getSelectedHotbarSlot());
        assertNull(restored.getSkin());

        restored.loadAdditionalData(NbtMap.builder().putString("SelectedInventorySlot", "6").build());
        assertEquals(0, restored.getSelectedHotbarSlot());
        assertFalse(save(restored).containsKey("SelectedInventorySlot"));
    }

    @Test
    void addPacketUsesTheSelectedStackAndClientIdentity() {
        CloudHumanEntity human = human();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        human.setSelectedHotbarSlot(4);
        human.setMainHandItem(diamond);

        AddPlayerPacket packet = assertInstanceOf(AddPlayerPacket.class, human.createAddEntityPacket());
        assertEquals(human.getServerId(), packet.getUuid());
        assertEquals(ItemStackPacketCodec.toNetwork(diamond), packet.getHand());
    }

    @Test
    void uninitializedHumanCanSetLocomotionRequests() {
        HumanEntity human = human();
        human.setSneaking(true);
        assertEquals(Pose.CROUCHING, human.getPose());
        human.setSwimming(true);
        assertTrue(human.isSneaking());
        assertEquals(Pose.SWIMMING, human.getPose());
        human.setSwimming(false);
        assertEquals(Pose.CROUCHING, human.getPose());
    }

    @Test
    void glideRequestDoesNotCountAsFlightWhileSwimming() {
        HumanEntity human = human();
        human.setSwimming(true);
        human.setGliding(true);
        assertEquals(Pose.SWIMMING, human.getPose());
        assertFalse(human.isGliding());

        human.setSwimming(false);
        assertEquals(Pose.FALL_FLYING, human.getPose());
        assertTrue(human.isGliding());

        human.setGliding(false);
        assertFalse(human.isGliding());
    }

    @Test
    void closeDetachesStandaloneInventoryListeners() {
        CloudHumanEntity human = human();
        assertFalse(human.container.getListeners().isEmpty());
        assertFalse(human.armor.getContainer().getListeners().isEmpty());
        assertFalse(human.offhand.getContainer().getListeners().isEmpty());

        human.close();
        human.close();

        assertTrue(human.isClosed());
        assertTrue(human.container.getListeners().isEmpty());
        assertTrue(human.armor.getContainer().getListeners().isEmpty());
        assertTrue(human.offhand.getContainer().getListeners().isEmpty());
    }

    @Test
    void skinLoadKeepsExistingCustomName() {
        CloudHumanEntity human = human();
        human.customName(Component.text("Steve"));

        human.loadAdditionalData(NbtMap.builder()
                .putCompound("Skin", skinTag())
                .build());

        assertEquals(Component.text("Steve"), human.customName());
        assertEquals("Steve", save(human).getString("CustomName"));
    }

    @Test
    void settingSkinPersistsItWithoutChangingClientIdentity() {
        CloudHumanEntity human = human();
        UUID clientId = human.getServerId();

        human.setSkin(SkinNbt.fromNbt(skinTag()));

        assertEquals(clientId, human.getServerId());
        assertEquals("geometry.humanoid.custom", save(human).getCompound("Skin").getString("ModelId"));
        assertThrows(NullPointerException.class, () -> human.setSkin(null));
    }

    @Test
    void humanKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                CloudHumanEntity.class, CloudHumanData.class,
                Map.of(
                        "armor", "an armor view over the live container in data().getArmor()",
                        "offhand", "an offhand view over the live container in data().getOffhand()",
                        "container", "the live main inventory: the standalone human writes it through its own save hook"));
        Set<String> fields = new HashSet<>();
        for (Field field : CloudHumanEntity.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("skin")) {
            assertFalse(fields.contains(removed),
                    "CloudHumanEntity keeps " + removed + "; state must live in CloudHumanData only");
        }
    }
}
