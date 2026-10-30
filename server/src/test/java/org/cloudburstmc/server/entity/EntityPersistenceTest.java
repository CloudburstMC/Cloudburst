package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.CreatureData;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.data.LivingData;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.nbt.NbtMapper;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.cloudburstmc.server.testutil.TestEntities;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the entity data path: unknown keys survive in the
 * raw tag, DTO properties write exactly, conditional keys clear cleanly,
 * plugin data stays server-side, and every registration round-trips.
 */
class EntityPersistenceTest {

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
    void clearedNameTagStaysCleared() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("CustomName", "A")
                .putBoolean("CustomNameVisible", true)
                .putBoolean("CustomNameAlwaysVisible", true)
                .build());

        assertEquals("A", entity.getNameTag());

        entity.setNameTag("");

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        assertFalse(saved.containsKey("CustomName"));
        assertFalse(saved.containsKey("CustomNameVisible"));
        assertFalse(saved.containsKey("CustomNameAlwaysVisible"));
    }

    @Test
    void absentCustomNameKeepsExistingNameTag() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.setNameTag("Steve");
        entity.setNameTagVisible(true);
        entity.setNameTagAlwaysVisible(true);

        entity.loadAdditionalData(NbtMap.EMPTY);

        assertEquals("Steve", entity.getNameTag());
        assertTrue(entity.isNameTagVisible());
        assertTrue(entity.isNameTagAlwaysVisible());
    }

    @Test
    void entityPluginDataSurvives() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);

        entity.setPluginData("myplugin", NbtMap.builder().putInt("a", 1).build());

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        assertEquals(1, saved.getCompound(NbtMapper.PLUGIN_DATA_KEY).getCompound("myplugin").getInt("a"));

        CloudEntity reloaded = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        reloaded.loadAdditionalData(saved);

        assertEquals(1, reloaded.getPluginData("myplugin").orElseThrow().getInt("a"));

        reloaded.removePluginData("myplugin");

        assertTrue(reloaded.getPluginData("myplugin").isEmpty());

        NbtMapBuilder afterRemove = NbtMap.builder();
        reloaded.saveAdditionalData(afterRemove);
        assertFalse(afterRemove.build().containsKey(NbtMapper.PLUGIN_DATA_KEY));
    }

    @Test
    void pluginDataMarksChunkDirty() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);

        // Fixture entities never run initialize, so they have no chunk; attach a
        // stub-backed chunk the way the block-entity fixture builds one.
        entity.chunk = new CloudChunk(0, 0, InterfaceProxy.create(Level.class));

        long revision = entity.chunk.captureSaveRevision();
        entity.setPluginData("myplugin", NbtMap.builder().putInt("a", 1).build());
        assertNotEquals(revision, entity.chunk.captureSaveRevision());

        revision = entity.chunk.captureSaveRevision();
        entity.removePluginData("myplugin");
        assertNotEquals(revision, entity.chunk.captureSaveRevision());
    }

    @Test
    void pluginDataWithoutChunkDoesNotThrow() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);

        entity.setPluginData("myplugin", NbtMap.builder().putInt("a", 1).build());
        entity.removePluginData("myplugin");
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
        ((EntityLiving) entity).setHealth(5f);

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        Object raw = saved.get("Health");
        assertTrue(raw instanceof Float, "Health must be a float, was: " + raw);
        assertEquals(5f, saved.getFloat("Health"));

        CloudEntity reloaded = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        reloaded.loadAdditionalData(saved);
        assertEquals(5f, ((EntityLiving) reloaded).getHealth());
    }

    @Test
    void legacyCreatureRegistrationRoundTripsWithCreatureData() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(TestEntities.LEGACY_CREATURE_TYPE);

        // The old overload registered the base data class; the runtime class's
        // minimum resolves the effective class so the creature cast cannot fail.
        assertEquals(EntityData.class,
                CloudEntityRegistry.get().getDataClass(TestEntities.LEGACY_CREATURE_TYPE));
        assertEquals(org.cloudburstmc.api.entity.data.CreatureData.class, entity.dataClass());

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
            if ("minecraft".equals(type.getId().getNamespace())
                    && !entity.minimumDataClass().isAssignableFrom(CloudEntityRegistry.get().getDataClass(type))) {
                failures.add(type.getId() + ": registered " + CloudEntityRegistry.get().getDataClass(type).getName()
                        + " is not usable by runtime minimum " + entity.minimumDataClass().getName());
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
                // Every type is migrated now, so a reload must save identically.
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
        assertEquals(org.cloudburstmc.api.entity.data.CreatureData.class,
                CloudEntityRegistry.get().getDataClass(EntityTypes.PIG));
        assertEquals(org.cloudburstmc.api.entity.data.PaintingData.class,
                CloudEntityRegistry.get().getDataClass(EntityTypes.PAINTING));
    }




    @Test
    void entityDataInstanceIsStable() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        EntityData before = entity.data();

        entity.loadAdditionalData(NbtMap.builder()
                .putShort("Fire", (short) 5)
                .putList("Tags", NbtType.STRING, List.of("one"))
                .build());

        assertSame(before, entity.data(), "load must fill the held instance in place");
        // Fire loads as seconds into the live countdown, exactly as today's applyData did.
        assertEquals(100, entity.getFireTicks());
        // Fire is hook-owned now; no DTO may declare it.
        assertFalse(ServerNbt.MAPPER.ownedKeys(EntityData.class).contains("Fire"));
        assertTrue(entity.customKeys().contains("Fire"));
        assertEquals(Set.of("one"), before.getTags());

        entity.loadAdditionalData(NbtMap.EMPTY);
        assertSame(before, entity.data(), "save must not replace the held instance");

        // An empty tag set still saves as an empty STRING list, as today.
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
        assertEquals(100, entity.getFireTicks());

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        assertTrue(saved.get("Fire") instanceof Short, "Fire must stay a short");
        assertEquals((short) 100, saved.getShort("Fire"));
    }

    @Test
    void creatureArmorLivesInData() {
        EntityCreature creature = (EntityCreature) PersistenceFixtures.entity(EntityTypes.PIG);
        CreatureData data = creature.data();

        assertSame(data.getArmor(), creature.armor.getContainer(),
                "the armor DTO must be the live armor container");
        assertSame(data.getOffhand(), creature.offhand.getContainer(),
                "the offhand DTO must be the live offhand container");

        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        creature.loadAdditionalData(NbtMap.builder()
                .putList("Armor", NbtType.COMPOUND, List.of(
                        ItemUtils.serializeItem(diamond, 0),
                        ItemUtils.serializeItem(ItemStack.EMPTY, 1),
                        ItemUtils.serializeItem(ItemStack.EMPTY, 2),
                        ItemUtils.serializeItem(ItemStack.EMPTY, 3)))
                .putList("Offhand", NbtType.COMPOUND, List.of(
                        ItemUtils.serializeItem(ItemStack.EMPTY, 0)))
                .build());

        assertEquals(diamond, creature.getArmor().getHelmet());
        assertSame(data.getArmor(), creature.armor.getContainer(),
                "load must fill the live container in place");

        creature.getArmor().setBoots(diamond);

        NbtMapBuilder builder = NbtMap.builder();
        creature.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        List<NbtMap> armor = saved.getList("Armor", NbtType.COMPOUND);
        assertEquals(4, armor.size());
        assertEquals(ItemUtils.serializeItem(diamond, 3), armor.get(3),
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
                .putInt("Fire", 5)
                .putList("Pos", NbtType.DOUBLE, List.of(1.0, 2.0, 3.0))
                .putList("Motion", NbtType.DOUBLE, List.of(0.0, 1.0, 0.0))
                .putList("Rotation", NbtType.DOUBLE, List.of(90.0, 0.0))
                .build());

        assertEquals(7f, ((EntityLiving) entity).getHealth());
        assertEquals(300, entity.getAirTicks());
        assertTrue(entity.isNameTagVisible());
        assertEquals(2f, entity.getScale());
        assertEquals(3f, entity.fallDistance);
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

        // The old load set the base scale first, then the transition hook
        // derived the baby scale; an explicit tag scale never won for a baby.
        assertEquals(0.2375f, ghast.getScale());
    }

    @Test
    void activeEffectsAppliedOncePerLoad() {
        byte absorptionId = NetworkUtils.effectToNetwork(EffectTypes.ABSORPTION);
        TestEntities.CountingCreatureEntity creature =
                PersistenceFixtures.entity(TestEntities.COUNTING_CREATURE_TYPE);
        creature.absorptionApplications = 0;
        creature.loadAdditionalData(NbtMap.builder()
                .putList("ActiveEffects", NbtType.COMPOUND, List.of(NbtMap.builder()
                        .putByte("Id", absorptionId)
                        .putByte("Amplifier", (byte) 0)
                        .putInt("Duration", 200)
                        .putBoolean("Ambient", false)
                        .putBoolean("ShowParticles", false)
                        .build()))
                .build());

        assertEquals(1, creature.absorptionApplications,
                "each active effect must apply exactly once per load");
        assertNotNull(creature.getPotionEffect(EffectTypes.ABSORPTION));
    }

    @Test
    void fireLoadIgnoresFireProtectionArmor() {
        EntityCreature creature = (EntityCreature) PersistenceFixtures.entity(EntityTypes.PIG);
        ItemStack boots = ItemStack.builder().itemType(ItemTypes.DIAMOND_BOOTS)
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.FIRE_PROTECTION,
                                new Enchantment(EnchantmentTypes.FIRE_PROTECTION, 4)))
                .build();
        creature.loadAdditionalData(NbtMap.builder()
                .putShort("Fire", (short) 5)
                .putList("Armor", NbtType.COMPOUND, List.of(
                        ItemUtils.serializeItem(ItemStack.EMPTY, 0),
                        ItemUtils.serializeItem(ItemStack.EMPTY, 1),
                        ItemUtils.serializeItem(ItemStack.EMPTY, 2),
                        ItemUtils.serializeItem(boots, 3)))
                .build());

        // The old load ran setOnFire before the armor was loaded, so no
        // reduction applied: 5 seconds become 100 ticks and save back as 100.
        assertEquals(100, creature.getFireTicks());
        assertEquals((short) 100, save(creature).getShort("Fire"));
    }

    @Test
    void slotlessArmorListLoadsPositionally() {
        EntityCreature creature = (EntityCreature) PersistenceFixtures.entity(EntityTypes.PIG);
        ItemStack iron = ItemStack.from(ItemTypes.IRON_INGOT, 1);
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        creature.getArmor().setBoots(diamond);

        NbtMap ironNoSlot = withoutSlot(ItemUtils.serializeItem(iron, 3));
        NbtMap goldNoSlot = withoutSlot(ItemUtils.serializeItem(diamond, 0));
        creature.loadAdditionalData(NbtMap.builder()
                .putList("Armor", NbtType.COMPOUND, List.of(ironNoSlot, goldNoSlot))
                .putList("Offhand", NbtType.COMPOUND,
                        List.of(withoutSlot(ItemUtils.serializeItem(diamond, 0))))
                .build());

        // Entries bind by position, ignoring any Slot tag...
        assertEquals(iron, creature.getArmor().getItem(0));
        assertEquals(diamond, creature.getArmor().getItem(1));
        // ...and missing slots keep their current item instead of clearing.
        assertEquals(diamond, creature.getArmor().getItem(3));
        assertEquals(diamond, creature.getOffhand().getOffhandItem());
    }

    @Test
    void cloudEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(CloudEntity.class, EntityData.class);
        // The old shape kept base state in runtime fields whose names match no DTO
        // property, so the shared check stays blind to them. Forbid them explicitly;
        // this fails on the pre-migration class.
        Set<String> fields = new HashSet<>();
        for (Field field : CloudEntity.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("freezeTicks", "scale")) {
            assertFalse(fields.contains(removed),
                    "CloudEntity keeps " + removed + "; state must live in EntityData only");
        }
    }

    @Test
    void livingEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(EntityLiving.class, LivingData.class);
    }

    @Test
    void creatureKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(EntityCreature.class, CreatureData.class,
                java.util.Map.of(
                        "armor", "an armor view over the live container in data().getArmor()",
                        "offhand", "an offhand view over the live container in data().getOffhand()"));
    }

    /**
     * Types whose load or init paths need a live world (server, level or game rules)
     * and cannot run in the stub fixture. Loading a creeper fires its power event
     * through the server; the others touch the level during initialization.
     */
    private static final java.util.Set<String> KNOWN_UNSTABLE_TYPES = java.util.Set.of(
            // load fires CreeperPowerEvent via server
            "minecraft:creeper",
            // init reads level dimension/end fight
            "minecraft:ender_dragon",
            // init captures location/last event state needing a level
            "minecraft:boat",
            "minecraft:chest_boat",
            // init reads level game rules and server difficulty
            "minecraft:lightning_bolt");
}
