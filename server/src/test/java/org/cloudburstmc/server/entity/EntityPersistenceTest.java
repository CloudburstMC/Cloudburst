package org.cloudburstmc.server.entity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.persistence.PersistentDataType;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.data.CloudEntityData;
import org.cloudburstmc.server.entity.data.CloudLivingData;
import org.cloudburstmc.server.entity.data.CloudMobData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.persistence.CloudPersistentDataContainer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.cloudburstmc.server.testutil.TestEntities;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityPersistenceTest {

    private static final Identifier PLUGIN_KEY = Identifier.parse("myplugin:a");

    @Test
    void customMaximumHealthRoundTripsBeforeHealthAndDoesNotSaveHealthBoostTwice() {
        CloudEntity pig = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        NbtMapBuilder defaults = NbtMap.builder();
        pig.saveAdditionalData(defaults);
        assertFalse(defaults.build().containsKey("Cloudburst.BaseMaxHealth"));

        pig.loadAdditionalData(NbtMap.builder()
                .putList("ActiveEffects", NbtType.COMPOUND, List.of(NbtMap.builder()
                        .putByte("Id", NetworkUtils.effectToNetwork(EffectTypes.HEALTH_BOOST))
                        .putInt("Duration", 100)
                        .build()))
                .build());
        pig.setMaxHealth(40);
        pig.setHealth(43f);
        NbtMapBuilder saved = NbtMap.builder();
        pig.saveAdditionalData(saved);
        NbtMap tag = saved.build();
        assertEquals(40, tag.getInt("Cloudburst.BaseMaxHealth"));

        CloudEntity restored = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        restored.loadAdditionalData(tag);
        assertEquals(44, restored.getMaxHealth());
        assertEquals(43f, restored.getHealth());

        restored.loadAdditionalData(NbtMap.builder().putInt("Cloudburst.BaseMaxHealth", -2).build());
        assertEquals(10, restored.getMaxHealth());
    }

    @Test
    void portalCooldownPersistsRemainingTicksAndOmitsZero() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.setPortalCooldown(40);
        entity.tickPortalCooldown();
        NbtMapBuilder saved = NbtMap.builder();
        entity.saveAdditionalData(saved);

        CloudEntity restored = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        restored.loadAdditionalData(saved.build());
        assertEquals(39, restored.getPortalCooldown());
        assertTrue(restored.isOnPortalCooldown());
        assertThrows(IllegalArgumentException.class, () -> restored.setPortalCooldown(-1));

        restored.loadAdditionalData(NbtMap.builder().putInt("PortalCooldown", -1).build());
        assertEquals(0, restored.getPortalCooldown());
        NbtMapBuilder cleared = NbtMap.builder();
        restored.saveAdditionalData(cleared);
        assertFalse(cleared.build().containsKey("PortalCooldown"));
    }

    @Test
    void malformedSavedHealthDoesNotReplaceCurrentHealth() {
        CloudLivingEntity entity = (CloudLivingEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.setHealth(5f);

        for (float invalidHealth : new float[]{-1f, Float.NaN, Float.POSITIVE_INFINITY}) {
            entity.loadAdditionalData(NbtMap.builder().putFloat("Health", invalidHealth).build());
            assertEquals(5f, entity.getHealth());
        }
    }

    @Test
    void persistedCustomNameTakesPrecedenceOverEntityTypeName() {
        CloudEntity creeper = (CloudEntity) PersistenceFixtures.entity(EntityTypes.CREEPER);
        assertEquals("Creeper", creeper.getName());

        creeper.customName(Component.text("Sparky"));
        NbtMapBuilder saved = NbtMap.builder();
        creeper.saveAdditionalData(saved);

        CloudEntity restored = (CloudEntity) PersistenceFixtures.entity(EntityTypes.CREEPER);
        restored.loadAdditionalData(saved.build());
        assertEquals("Sparky", restored.getName());
    }

    @Test
    void styledCustomNameSurvivesSaveAndLoad() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.CREEPER);
        Component name = Component.text("Sparky", NamedTextColor.GOLD)
                .append(Component.text(" II", NamedTextColor.AQUA));
        entity.customName(name);

        NbtMapBuilder saved = NbtMap.builder();
        entity.saveAdditionalData(saved);
        NbtMap tag = saved.build();
        assertTrue(tag.containsKey("CustomName", NbtType.COMPOUND));

        CloudEntity restored = (CloudEntity) PersistenceFixtures.entity(EntityTypes.CREEPER);
        restored.loadAdditionalData(tag);

        assertEquals(name, restored.customName());
        assertEquals(name, restored.displayName());
        assertEquals("Sparky II", restored.getName());
    }

    @Test
    void entityUnknownKeysSurvive() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder()
                .putList("Pos", NbtType.FLOAT, List.of(1f, 2f, 3f))
                .putList("Motion", NbtType.FLOAT, List.of(0f, 0f, 0f))
                .putList("Rotation", NbtType.FLOAT, List.of(0f, 0f))
                .putInt("Mystery", 7)
                .build());

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void clearingCustomNameDoesNotChangeVisibility() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("CustomName", "A")
                .putBoolean("CustomNameVisible", true)
                .putBoolean("CustomNameAlwaysVisible", true)
                .build());

        assertEquals(Component.text("A"), entity.customName());

        entity.customName(null);

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        assertFalse(saved.containsKey("CustomName"));
        assertTrue(saved.getBoolean("CustomNameVisible"));
        assertTrue(saved.getBoolean("CustomNameAlwaysVisible"));
    }

    @Test
    void absentCustomNameKeepsExistingName() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.customName(Component.text("Steve"));
        entity.setCustomNameVisible(true);
        entity.setCustomNameAlwaysVisible(true);

        entity.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(Component.text("Steve"), entity.customName());
        assertTrue(entity.isCustomNameVisible());
        assertTrue(entity.isCustomNameAlwaysVisible());
    }

    @Test
    void entityPluginDataSurvives() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);

        entity.getPersistentDataContainer().set(PLUGIN_KEY, PersistentDataType.INTEGER, 1);

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        assertEquals(1, saved.getCompound(CloudPersistentDataContainer.STORAGE_KEY).getInt("myplugin:a"));

        CloudEntity reloaded = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        reloaded.loadAdditionalData(saved);

        int storedValue = reloaded.getPersistentDataContainer()
                .get(PLUGIN_KEY, PersistentDataType.INTEGER)
                .orElseThrow();
        assertEquals(1, storedValue);

        reloaded.getPersistentDataContainer().remove(PLUGIN_KEY);

        assertTrue(reloaded.getPersistentDataContainer().get(PLUGIN_KEY, PersistentDataType.INTEGER).isEmpty());

        NbtMapBuilder afterRemove = NbtMap.builder();
        reloaded.saveAdditionalData(afterRemove);
        assertFalse(afterRemove.build().containsKey(CloudPersistentDataContainer.STORAGE_KEY));
    }

    @Test
    void pluginDataMarksChunkDirty() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);

        entity.chunk = new CloudChunk(0, 0, InterfaceProxy.create(Level.class));

        long revision = entity.chunk.captureSaveRevision();
        entity.getPersistentDataContainer().set(PLUGIN_KEY, PersistentDataType.INTEGER, 1);
        assertNotEquals(revision, entity.chunk.captureSaveRevision());

        revision = entity.chunk.captureSaveRevision();
        entity.getPersistentDataContainer().remove(PLUGIN_KEY);
        assertNotEquals(revision, entity.chunk.captureSaveRevision());
    }

    @Test
    void pluginDataWithoutChunkDoesNotThrow() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);

        entity.getPersistentDataContainer().set(PLUGIN_KEY, PersistentDataType.INTEGER, 1);
        entity.getPersistentDataContainer().remove(PLUGIN_KEY);
    }

    @Test
    void posIsWrittenAsFloatList() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);
        entity.setPosition(Vector3f.from(1f, 2f, 3f));

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        Object raw = saved.get("Pos");
        assertTrue(raw instanceof NbtList<?>, "Pos must be a list");
        assertEquals(NbtType.FLOAT, ((NbtList<?>) raw).getType());
        assertEquals(List.of(1f, 2f, 3f), saved.getList("Pos", NbtType.FLOAT));
    }

    @Test
    void livingHealthRoundTripsAsFloat() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);
        ((CloudLivingEntity) entity).setHealth(5f);

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        Object raw = saved.get("Health");
        assertTrue(raw instanceof Float, "Health must be a float, was: " + raw);
        assertEquals(5f, saved.getFloat("Health"));

        CloudEntity reloaded = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        reloaded.loadAdditionalData(saved);
        assertEquals(5f, ((CloudLivingEntity) reloaded).getHealth());
    }

    @Test
    void legacyCreatureRegistrationRoundTripsWithCreatureData() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(TestEntities.LEGACY_CREATURE_TYPE);

        assertEquals(org.cloudburstmc.server.entity.data.CloudMobData.class, entity.data().getClass());

        entity.loadAdditionalData(NbtMap.builder()
                .putList("Pos", NbtType.FLOAT, List.of(1f, 2f, 3f))
                .putList("Motion", NbtType.FLOAT, List.of(0f, 0f, 0f))
                .putList("Rotation", NbtType.FLOAT, List.of(0f, 0f))
                .build());

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        CloudEntity second = (CloudEntity) PersistenceFixtures.entity(TestEntities.LEGACY_CREATURE_TYPE);
        second.loadAdditionalData(saved);

        NbtMapBuilder again = NbtMap.builder();
        second.saveAdditionalData(again);
        assertEquals(saved, again.build());
    }

    @Test
    void allRegisteredEntityTypesBuildAndRoundTrip() {
        List<String> failures = new ArrayList<>();
        for (EntityType<?> type : CloudEntityRegistry.get().values()) {
            if (type == EntityTypes.PLAYER || Player.class.isAssignableFrom(type.getEntityClass())) {
                continue;
            }
            if (KNOWN_UNSTABLE_TYPES.contains(type.getId().toString())) {
                continue;
            }
            CloudEntity entity;
            try {
                entity = (CloudEntity) PersistenceFixtures.entity(type);
            } catch (Exception e) {
                failures.add(type.getId() + ": construction failed (" + e + ")");
                continue;
            }
            NbtMap saved;
            try {
                NbtMapBuilder builder = NbtMap.builder();
                entity.saveAdditionalData(builder);
                saved = builder.build();
            } catch (Exception e) {
                failures.add(type.getId() + ": save failed (" + e + ")");
                continue;
            }

            CloudEntity reloaded;
            try {
                reloaded = (CloudEntity) PersistenceFixtures.entity(type);
            } catch (Exception e) {
                failures.add(type.getId() + ": reload construction failed (" + e + ")");
                continue;
            }
            try {
                reloaded.loadAdditionalData(saved);
            } catch (Exception e) {
                failures.add(type.getId() + ": load failed (" + e + ")");
                continue;
            }
            try {
                NbtMapBuilder second = NbtMap.builder();
                reloaded.saveAdditionalData(second);
                assertEquals(saved, second.build(), type.getId() + " must save identically after a reload");
            } catch (AssertionError e) {
                failures.add(type.getId() + ": resave differs (" + e.getMessage() + ")");
            } catch (Exception e) {
                failures.add(type.getId() + ": resave failed (" + e + ")");
            }
        }
        if (!failures.isEmpty()) {
            System.out.println("Unstable entity types (" + failures.size() + "):");
            failures.forEach(System.out::println);
        }
        assertTrue(failures.isEmpty(), "Entity round-trip failures: " + failures);
    }




    @Test
    void loadingInvalidScalePreservesCurrentValue() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.setScale(2f);

        entity.loadAdditionalData(NbtMap.builder().putFloat("Scale", Float.NaN).build());
        assertEquals(2f, entity.getScale());

        entity.loadAdditionalData(NbtMap.builder().putFloat("Scale", 0f).build());
        assertEquals(2f, entity.getScale());

        entity.loadAdditionalData(NbtMap.builder().putFloat("Scale", 0.5f).build());
        assertEquals(0.5f, entity.getScale());
    }

    @Test
    void entityDataInstanceIsStable() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        CloudEntityData before = entity.data();

        entity.loadAdditionalData(NbtMap.builder()
                .putShort("Fire", (short) 100)
                .putList("Tags", NbtType.STRING, List.of("one"))
                .build());

        assertSame(before, entity.data(), "load must fill the held instance in place");
        assertEquals(100, entity.getFireTicks());
        assertFalse(ServerNbt.MAPPER.ownedKeys(CloudEntityData.class).contains("Fire"));
        assertTrue(entity.customKeys().contains("Fire"));
        assertEquals(Set.of("one"), before.getTags());

        entity.loadAdditionalData(NbtMap.EMPTY);
        assertSame(before, entity.data(), "save must not replace the held instance");

        CloudEntity fresh = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        fresh.loadAdditionalData(NbtMap.EMPTY);
        NbtMapBuilder freshBuilder = NbtMap.builder();
        fresh.saveAdditionalData(freshBuilder);
        NbtMap resaved = freshBuilder.build();
        assertTrue(resaved.get("Tags") instanceof NbtList<?>, "Tags must be a list");
        assertSame(NbtType.STRING, ((NbtList<?>) resaved.get("Tags")).getType(),
                "Tags must keep the STRING element type");
    }

    @Test
    void exceptionKeysStillRoundTrip() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        byte speedId = NetworkUtils.effectToNetwork(EffectTypes.SPEED);
        NbtMap input = NbtMap.builder()
                .putList("Pos", NbtType.FLOAT, List.of(1f, 2f, 3f))
                .putList("Motion", NbtType.FLOAT, List.of(0f, 1f, 0f))
                .putList("Rotation", NbtType.FLOAT, List.of(90f, 0f))
                .putFloat("FallDistance", 2.5f)
                .putShort("Air", (short) 300)
                .putBoolean("OnGround", false)
                .putFloat("Scale", 2.5f)
                .putFloat("Health", 5f)
                .putString("CustomName", "Bob")
                .putBoolean("CustomNameVisible", true)
                .putBoolean("CustomNameAlwaysVisible", false)
                .putList("ActiveEffects", NbtType.COMPOUND, List.of(NbtMap.builder()
                        .putByte("Id", speedId)
                        .putByte("Amplifier", (byte) 1)
                        .putInt("Duration", 200)
                        .putBoolean("Ambient", true)
                        .putBoolean("ShowParticles", false)
                        .build()))
                .build();
        entity.loadAdditionalData(input);

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }

        PotionEffect restored = entity.getPotionEffect(EffectTypes.SPEED);
        assertNotNull(restored, "the ActiveEffects entry must restore");
        assertEquals(1, restored.getAmplifier());
        assertEquals(200, restored.getDuration());
        assertTrue(restored.isAmbient());
        assertFalse(restored.hasParticles());
    }

    @Test
    void fireRoundTripsAsLiveCountdown() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder().putShort("Fire", (short) 5).build());
        assertEquals(5, entity.getFireTicks());

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        assertTrue(saved.get("Fire") instanceof Short, "Fire must stay a short");
        assertEquals((short) 5, saved.getShort("Fire"));

        CloudEntity restored = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        restored.loadAdditionalData(saved);
        assertEquals(5, restored.getFireTicks());
    }

    @Test
    void remainingAbsorptionSurvivesReload() {
        CloudLivingEntity entity = (CloudLivingEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.setAbsorption(2.5f);

        NbtMap saved = save(entity);
        assertEquals(2.5f, saved.getFloat("AbsorptionAmount"));

        CloudLivingEntity restored = (CloudLivingEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        restored.loadAdditionalData(saved);
        assertEquals(2.5f, restored.getAbsorption());
    }

    @Test
    void freezeLockSurvivesReloadAndUnlockIsOmitted() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.setFreezeTicks(40);
        entity.lockFreezeTicks(true);

        CloudEntity restored = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        restored.loadAdditionalData(save(entity));
        assertTrue(restored.isFreezeTickingLocked());
        assertEquals(40, restored.getFreezeTicks());

        restored.lockFreezeTicks(false);
        NbtMap unlocked = save(restored);
        assertFalse(unlocked.containsKey("Cloudburst.FreezeLock"));

        CloudEntity reloaded = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        reloaded.loadAdditionalData(unlocked);
        assertFalse(reloaded.isFreezeTickingLocked());
    }

    @Test
    void mobArmorLivesInData() {
        CloudMob mob = (CloudMob) PersistenceFixtures.entity(EntityTypes.PIG);
        CloudMobData data = mob.data();

        assertSame(data.getArmor(), mob.armor.getContainer(),
                "the armor DTO must be the live armor container");
        assertSame(data.getOffhand(), mob.offhand.getContainer(),
                "the offhand DTO must be the live offhand container");

        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        mob.loadAdditionalData(NbtMap.builder()
                .putList("Armor", NbtType.COMPOUND, List.of(
                        ItemStackNbtCodec.serializeItem(diamond, 0),
                        ItemStackNbtCodec.serializeItem(ItemStack.EMPTY, 1),
                        ItemStackNbtCodec.serializeItem(ItemStack.EMPTY, 2),
                        ItemStackNbtCodec.serializeItem(ItemStack.EMPTY, 3)))
                .putList("Offhand", NbtType.COMPOUND, List.of(
                        ItemStackNbtCodec.serializeItem(ItemStack.EMPTY, 0)))
                .build());

        assertEquals(diamond, mob.getArmor().getHelmet());
        assertSame(data.getArmor(), mob.armor.getContainer(),
                "load must fill the live container in place");

        mob.getArmor().setBoots(diamond);

        NbtMapBuilder builder = NbtMap.builder();
        mob.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        List<NbtMap> armor = saved.getList("Armor", NbtType.COMPOUND);
        assertEquals(4, armor.size());
        assertEquals(ItemStackNbtCodec.serializeItem(diamond, 3), armor.get(3),
                "a viewer-side armor change must reach the saved tag");
    }

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    private static NbtMap withoutSlot(NbtMap tag) {
        NbtMapBuilder stripped = tag.toBuilder();
        stripped.remove("Slot");
        return stripped.build();
    }

    @Test
    void tagsKeepOrderAcrossLoadAndSave() {
        List<String> ordered = List.of(
                "tag10", "tag02", "tag33", "alpha", "zeta",
                "m1", "m2", "m3", "m4", "k", "tag00", "last");
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder()
                .putList("Tags", NbtType.STRING, ordered)
                .build());

        assertEquals(ordered, save(entity).getList("Tags", NbtType.STRING));

        CloudEntity reloaded = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        reloaded.loadAdditionalData(save(entity));
        assertEquals(ordered, save(reloaded).getList("Tags", NbtType.STRING),
                "tag order must survive a load-save cycle");
    }

    @Test
    void blankAndExcessTagsAreDroppedOnLoad() {
        List<String> tags = new ArrayList<>(List.of("", "   "));
        for (int i = 0; i < 1030; i++) {
            tags.add("tag" + i);
        }
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder()
                .putList("Tags", NbtType.STRING, tags)
                .build());

        assertEquals(1024, entity.getScoreboardTags().size());
        assertFalse(entity.getScoreboardTags().contains(""));

        List<String> written = save(entity).getList("Tags", NbtType.STRING);
        assertEquals(1024, written.size());
        assertEquals("tag0", written.get(0));
        assertEquals("tag1023", written.get(1023));
    }

    @Test
    void overMaxFreezeTicksAreClampedOnLoad() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder().putInt("TicksFrozen", 500).build());

        assertEquals(140, entity.getFreezeTicks());
        assertEquals(140, save(entity).getInt("TicksFrozen"));
    }

    @Test
    void explicitZeroFreezeTicksAreOmitted() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder().putInt("TicksFrozen", 0).build());

        assertEquals(0, entity.getFreezeTicks());
        assertFalse(save(entity).containsKey("TicksFrozen"),
                "an explicit zero must save like an absent key");
    }

    @Test
    void negativeFreezeTicksAreDroppedOnLoad() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder().putInt("TicksFrozen", -5).build());

        assertEquals(0, entity.getFreezeTicks());
        assertFalse(save(entity).containsKey("TicksFrozen"));
    }

    @Test
    void fromBucketIsDroppedForNonBucketable() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder().putBoolean("FromBucket", true).build());

        assertFalse(entity.isFromBucket());
        assertFalse(save(entity).containsKey("FromBucket"));
    }

    @Test
    void fromBucketSurvivesForBucketable() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.SALMON);
        entity.loadAdditionalData(NbtMap.builder().putBoolean("FromBucket", true).build());

        assertTrue(entity.isFromBucket());

        NbtMap saved = save(entity);
        assertTrue(saved.get("FromBucket") instanceof Byte, "FromBucket must stay a byte");
        assertTrue(saved.getBoolean("FromBucket"));
    }

    @Test
    void hookReadsAreLenientAcrossKinds() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder()
                .putShort("Health", (short) 7)
                .putInt("Air", 300)
                .putInt("CustomNameVisible", 1)
                .putDouble("Scale", 2.0)
                .putDouble("FallDistance", 3.0)
                .putInt("Fire", 100)
                .putList("Pos", NbtType.DOUBLE, List.of(1.0, 2.0, 3.0))
                .putList("Motion", NbtType.DOUBLE, List.of(0.0, 1.0, 0.0))
                .putList("Rotation", NbtType.DOUBLE, List.of(90.0, 0.0))
                .build());

        assertEquals(7f, ((CloudLivingEntity) entity).getHealth());
        assertEquals(300, entity.getAirTicks());
        assertTrue(entity.isCustomNameVisible());
        assertEquals(2f, entity.getScale());
        assertEquals(3f, entity.getFallDistance());
        assertEquals(100, entity.getFireTicks());
        assertEquals(Vector3f.from(1f, 2f, 3f), entity.getPosition());
        assertEquals(Vector3f.from(0f, 1f, 0f), entity.getMotion());
        assertEquals(90f, entity.getYaw());
        assertEquals(0f, entity.getPitch());
    }

    @Test
    void activeEffectsBindAcrossKinds() {
        byte speedId = NetworkUtils.effectToNetwork(EffectTypes.SPEED);
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder()
                .putList("ActiveEffects", NbtType.COMPOUND, List.of(NbtMap.builder()
                        .putShort("Id", speedId)
                        .putInt("Amplifier", 2)
                        .putShort("Duration", (short) 200)
                        .putInt("Ambient", 1)
                        .putShort("ShowParticles", (short) 0)
                        .build()))
                .build());

        PotionEffect restored = entity.getPotionEffect(EffectTypes.SPEED);
        assertNotNull(restored, "a wrong-kind ActiveEffects entry must still restore");
        assertEquals(2, restored.getAmplifier());
        assertEquals(200, restored.getDuration());
        assertTrue(restored.isAmbient());
        assertFalse(restored.hasParticles());

        NbtMap written = save(entity).getList("ActiveEffects", NbtType.COMPOUND).get(0);
        assertTrue(written.get("Id") instanceof Byte, "Id must stay a byte");
        assertTrue(written.get("Amplifier") instanceof Byte, "Amplifier must stay a byte");
        assertTrue(written.get("Duration") instanceof Integer, "Duration must stay an int");
        assertTrue(written.get("Ambient") instanceof Byte, "Ambient must stay a byte");
        assertTrue(written.get("ShowParticles") instanceof Byte, "ShowParticles must stay a byte");
    }

    @Test
    void babyHappyGhastLoadsWithBabyScale() {
        CloudEntity ghast = (CloudEntity) PersistenceFixtures.entity(EntityTypes.HAPPY_GHAST);
        ghast.loadAdditionalData(NbtMap.builder().putInt("Age", -24000).build());

        assertEquals(0.2375f, ghast.getScale());
    }

    @Test
    void explicitScaleBabyHappyGhastKeepsBabyScale() {
        CloudEntity ghast = (CloudEntity) PersistenceFixtures.entity(EntityTypes.HAPPY_GHAST);
        ghast.loadAdditionalData(NbtMap.builder()
                .putInt("Age", -24000)
                .putFloat("Scale", 1.0f)
                .build());

        assertEquals(0.2375f, ghast.getScale());
    }

    @Test
    void activeEffectsAppliedOncePerLoad() {
        byte absorptionId = NetworkUtils.effectToNetwork(EffectTypes.ABSORPTION);
        TestEntities.CountingMob mob =
                PersistenceFixtures.entity(TestEntities.COUNTING_MOB_TYPE);
        mob.absorptionApplications = 0;
        mob.loadAdditionalData(NbtMap.builder()
                .putList("ActiveEffects", NbtType.COMPOUND, List.of(NbtMap.builder()
                        .putByte("Id", absorptionId)
                        .putByte("Amplifier", (byte) 0)
                        .putInt("Duration", 200)
                        .putBoolean("Ambient", false)
                        .putBoolean("ShowParticles", false)
                        .build()))
                .build());

        assertEquals(1, mob.absorptionApplications,
                "each active effect must apply exactly once per load");
        assertNotNull(mob.getPotionEffect(EffectTypes.ABSORPTION));
    }

    @Test
    void fireLoadIgnoresFireProtectionArmor() {
        CloudMob mob = (CloudMob) PersistenceFixtures.entity(EntityTypes.PIG);
        ItemStack boots = ItemStack.builder().itemType(ItemTypes.DIAMOND_BOOTS)
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.FIRE_PROTECTION,
                                new Enchantment(EnchantmentTypes.FIRE_PROTECTION, 4)))
                .build();
        mob.loadAdditionalData(NbtMap.builder()
                .putShort("Fire", (short) 100)
                .putList("Armor", NbtType.COMPOUND, List.of(
                        ItemStackNbtCodec.serializeItem(ItemStack.EMPTY, 0),
                        ItemStackNbtCodec.serializeItem(ItemStack.EMPTY, 1),
                        ItemStackNbtCodec.serializeItem(ItemStack.EMPTY, 2),
                        ItemStackNbtCodec.serializeItem(boots, 3)))
                .build());

        assertEquals(100, mob.getFireTicks());
        assertEquals((short) 100, save(mob).getShort("Fire"));
    }

    @Test
    void slotlessArmorListLoadsPositionally() {
        CloudMob mob = (CloudMob) PersistenceFixtures.entity(EntityTypes.PIG);
        ItemStack iron = ItemStack.from(ItemTypes.IRON_INGOT, 1);
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        mob.getArmor().setBoots(diamond);

        NbtMap ironNoSlot = withoutSlot(ItemStackNbtCodec.serializeItem(iron, 3));
        NbtMap goldNoSlot = withoutSlot(ItemStackNbtCodec.serializeItem(diamond, 0));
        mob.loadAdditionalData(NbtMap.builder()
                .putList("Armor", NbtType.COMPOUND, List.of(ironNoSlot, goldNoSlot))
                .putList("Offhand", NbtType.COMPOUND,
                        List.of(withoutSlot(ItemStackNbtCodec.serializeItem(diamond, 0))))
                .build());

        assertEquals(iron, mob.getArmor().getItem(0));
        assertEquals(diamond, mob.getArmor().getItem(1));
        assertEquals(diamond, mob.getArmor().getItem(3));
        assertEquals(diamond, mob.getOffhand().getOffhandItem());
    }

    @Test
    void cloudEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(CloudEntity.class, CloudEntityData.class);
        assertThrows(NoSuchFieldException.class, () -> CloudEntity.class.getDeclaredField("freezeTicks"));
    }

    @Test
    void livingEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(CloudLivingEntity.class, CloudLivingData.class);
    }

    @Test
    void mobKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(CloudMob.class, CloudMobData.class,
                java.util.Map.of(
                        "armor", "an armor view over the live container in data().getArmor()",
                        "offhand", "an offhand view over the live container in data().getOffhand()"));
    }

    private static final java.util.Set<String> KNOWN_UNSTABLE_TYPES = java.util.Set.of(
            "minecraft:creeper",
            "minecraft:ender_dragon",
            "minecraft:boat",
            "minecraft:chest_boat",
            "minecraft:lightning_bolt");
}
