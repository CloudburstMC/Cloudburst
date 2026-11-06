package org.cloudburstmc.server.golden;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whole-tag golden test (Goal 7 net): for every registered block-entity and entity type,
 * loads the representative input tag captured at the pre-plan commit and asserts the
 * saved server tag (plus the client tag for block entities) is {@link NbtMap}-equal to
 * the captured golden. NBT compounds are unordered, so key order may change freely; every
 * other difference fails unless it is allowlisted below with the ledger ruling that
 * covers it.
 */
class GoldenWholeTagTest {

    /**
     * Types with no golden files and why. Only types that cannot be built server-free
     * may be listed here.
     */
    private static final Set<String> SKIPPED_ENTITIES = Set.of(
            // Loading any powered key fires the power event through the server.
            // Covered by CreeperPersistenceTest through its event-free subclass.
            "minecraft:creeper");

    /**
     * Allowlisted (type, key) differences: the head value is asserted to pin the new
     * behaviour, every other key still compares against the golden. Each entry cites the
     * ledger ruling that covers it.
     */
    private static final Map<String, Map<String, Allowlisted>> ALLOWLIST = Map.of(
            // Ruling (Task 7 fix round 1): saving no longer pads short Text with literal
            // "null" lines; the save keeps the loaded value on both tags.
            "blockentity/Sign", Map.of("Text", new Allowlisted("a\nb",
                    "sign short-text padding fix (Task 7 fix round 1 ruling)")));

    private record Allowlisted(Object expectedHead, String ruling) {
    }

    private final List<String> failures = new ArrayList<>();
    private final List<String> allowlisted = new ArrayList<>();

    @Test
    void blockEntityTagsMatchGoldens() {
        PersistenceFixtures.ensureInitialized();
        Set<String> covered = new TreeSet<>();
        for (BlockEntityType<?> type : CloudBlockEntityRegistry.get().values()) {
            String id = CloudBlockEntityRegistry.get().getPersistentId(type);
            if (id == null) {
                failures.add("blockentity " + type.getId() + ": no persistent id");
                continue;
            }
            covered.add(id);
            String dir = "/golden/blockentity/" + safe(id) + "/";
            NbtMap input = read(dir + "input.nbt", "blockentity " + id);
            if (input == null) {
                continue;
            }
            try {
                BaseBlockEntity entity = (BaseBlockEntity) PersistenceFixtures.blockEntity(type,
                        Vector3i.from(input.getInt("x", 10), input.getInt("y", 64), input.getInt("z", -5)));
                entity.loadAdditionalData(input);
                compare("blockentity " + id + " server", read(dir + "server.nbt", "blockentity " + id),
                        entity.getServerTag(), "blockentity/" + id);
                compare("blockentity " + id + " client", read(dir + "client.nbt", "blockentity " + id),
                        entity.getClientTag(), "blockentity/" + id);
            } catch (AssertionError e) {
                throw e;
            } catch (Exception e) {
                failures.add("blockentity " + id + ": load/save failed (" + e + ")");
            }
        }
        report();
    }

    @Test
    void entityTagsMatchGoldens() {
        PersistenceFixtures.ensureInitialized();
        for (EntityType<?> type : CloudEntityRegistry.get().values()) {
            if (type != EntityTypes.PLAYER && Player.class.isAssignableFrom(type.getEntityClass())) {
                failures.add("entity " + type.getId() + ": player subclass has no golden");
                continue;
            }
            String id = type.getId().toString();
            if (SKIPPED_ENTITIES.contains(id)) {
                continue;
            }
            String dir = "/golden/entity/" + safe(id) + "/";
            NbtMap input = read(dir + "input.nbt", "entity " + id);
            if (input == null) {
                continue;
            }
            try {
                CloudEntity entity = buildEntity(type);
                entity.loadAdditionalData(input);
                NbtMapBuilder saved = NbtMap.builder();
                entity.saveAdditionalData(saved);
                compare("entity " + id + " server", read(dir + "server.nbt", "entity " + id),
                        saved.build(), "entity/" + id);
            } catch (AssertionError e) {
                throw e;
            } catch (Exception e) {
                failures.add("entity " + id + ": load/save failed (" + e + ")");
            }
        }
        report();
    }

    private static CloudEntity buildEntity(EntityType<?> type) {
        if (Set.of(EntityTypes.BOAT, EntityTypes.CHEST_BOAT, EntityTypes.ENDER_DRAGON,
                EntityTypes.LIGHTNING_BOLT).contains(type)) {
            // initEntity needs a live world for these, exactly as in the capture.
            return (CloudEntity) buildWithoutInit(type);
        }
        return (CloudEntity) PersistenceFixtures.entity(type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends org.cloudburstmc.api.entity.Entity> T buildWithoutInit(EntityType<T> type) {
        PersistenceFixtures.ensureInitialized();
        Location location = Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class));
        try {
            CloudEntityRegistry registry = CloudEntityRegistry.get();
            java.lang.reflect.Field dataMapField = CloudEntityRegistry.class.getDeclaredField("dataMap");
            dataMapField.setAccessible(true);
            java.util.Map<EntityType<?>, ?> dataMap =
                    (java.util.Map<EntityType<?>, ?>) dataMapField.get(registry);
            Object entityData = dataMap.get(type);
            if (entityData == null) {
                throw new IllegalStateException("No factory registered for " + type.getId());
            }
            java.lang.reflect.Method serviceProviderMethod =
                    entityData.getClass().getDeclaredMethod("serviceProvider");
            serviceProviderMethod.setAccessible(true);
            Object serviceProvider = serviceProviderMethod.invoke(entityData);
            java.lang.reflect.Method getProviderMethod =
                    serviceProvider.getClass().getDeclaredMethod("getProvider");
            getProviderMethod.setAccessible(true);
            Object provider = getProviderMethod.invoke(serviceProvider);
            java.lang.reflect.Method getValueMethod = provider.getClass().getDeclaredMethod("getValue");
            getValueMethod.setAccessible(true);
            Object factoryData = getValueMethod.invoke(provider);
            java.lang.reflect.Method factoryMethod = factoryData.getClass().getDeclaredMethod("factory");
            factoryMethod.setAccessible(true);
            org.cloudburstmc.api.entity.EntityFactory factory =
                    (org.cloudburstmc.api.entity.EntityFactory) factoryMethod.invoke(factoryData);
            return (T) factory.create(type, location);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create entity of type " + type.getId(), e);
        }
    }

    private void compare(String what, NbtMap golden, NbtMap saved, String allowlistKey) {
        if (golden == null) {
            return;
        }
        Map<String, Allowlisted> allowed = ALLOWLIST.getOrDefault(allowlistKey, Map.of());
        Map<String, Object> expected = new LinkedHashMap<>(golden);
        Map<String, Object> actual = new LinkedHashMap<>(saved);
        for (Map.Entry<String, Allowlisted> entry : allowed.entrySet()) {
            String key = entry.getKey();
            Allowlisted rule = entry.getValue();
            Object head = actual.get(key);
            if (!rule.expectedHead().equals(head)) {
                failures.add(what + ": allowlisted key '" + key + "' is " + render(head)
                        + ", expected " + render(rule.expectedHead())
                        + " (" + rule.ruling() + ")");
            } else {
                allowlisted.add(what + " '" + key + "' (" + rule.ruling() + ")");
            }
            expected.remove(key);
            actual.remove(key);
        }
        List<String> diffs = diff(expected, actual);
        if (!diffs.isEmpty()) {
            StringBuilder message = new StringBuilder(what + " differs from the golden:");
            for (String diff : diffs) {
                message.append('\n').append("  ").append(diff);
            }
            failures.add(message.toString());
        }
    }

    private static List<String> diff(Map<String, Object> golden, Map<String, Object> saved) {
        List<String> diffs = new ArrayList<>();
        for (Map.Entry<String, Object> entry : new TreeMap<>(golden).entrySet()) {
            String key = entry.getKey();
            if (!saved.containsKey(key)) {
                diffs.add("missing key '" + key + "' (golden " + render(entry.getValue()) + ")");
            } else if (!valueEquals(entry.getValue(), saved.get(key))) {
                diffs.add("key '" + key + "': golden " + render(entry.getValue())
                        + " vs head " + render(saved.get(key)));
            }
        }
        for (String key : new TreeSet<>(saved.keySet())) {
            if (!golden.containsKey(key)) {
                diffs.add("extra key '" + key + "' (head " + render(saved.get(key)) + ")");
            }
        }
        return diffs;
    }

    private static boolean valueEquals(Object golden, Object saved) {
        if (golden instanceof NbtMap goldenMap && saved instanceof NbtMap savedMap) {
            return goldenMap.equals(savedMap);
        }
        if (golden instanceof List<?> goldenList && saved instanceof List<?> savedList) {
            return goldenList.equals(savedList);
        }
        return java.util.Objects.equals(golden, saved);
    }

    private static String render(Object value) {
        if (value == null) {
            return "absent";
        }
        String text = value.toString();
        if (text.length() > 300) {
            text = text.substring(0, 300) + "...(" + text.length() + " chars)";
        }
        String type = value instanceof NbtMap ? "compound"
                : value instanceof List<?> ? "list[" + ((List<?>) value).size() + "]"
                : value.getClass().getSimpleName();
        return type + " " + text;
    }

    private NbtMap read(String resource, String what) {
        try (InputStream stream = GoldenWholeTagTest.class.getResourceAsStream(resource)) {
            if (stream == null) {
                failures.add(what + ": missing resource " + resource);
                return null;
            }
            try (NBTInputStream in = NbtUtils.createReaderLE(stream)) {
                return (NbtMap) in.readTag();
            }
        } catch (Exception e) {
            failures.add(what + ": cannot read " + resource + " (" + e + ")");
            return null;
        }
    }

    private static String safe(String id) {
        StringBuilder name = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            name.append(Character.isLetterOrDigit(c) ? c : '_');
        }
        return name.toString();
    }

    private void report() {
        for (String note : allowlisted) {
            System.out.println("Golden allowlisted: " + note);
        }
        if (!failures.isEmpty()) {
            StringBuilder message = new StringBuilder(
                    "Whole-tag golden differences (" + failures.size() + "):");
            for (String failure : failures) {
                message.append('\n').append(failure);
            }
            System.out.println(message);
        }
        assertTrue(failures.isEmpty(), "Whole-tag golden differences: " + failures.size()
                + " (see test stdout for the full diff)");
    }
}
