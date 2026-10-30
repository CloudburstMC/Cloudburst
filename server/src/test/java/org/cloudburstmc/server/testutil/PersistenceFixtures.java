package org.cloudburstmc.server.testutil;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

/**
 * Builds real persistent objects backed by stub worlds, so round-trip tests
 * exercise the production save and load paths.
 */
@UtilityClass
public class PersistenceFixtures {

    private static boolean initialized;

    /**
     * Creates a block entity of the given type through the registry factory,
     * backed by a real chunk on a stub level.
     *
     * @param type     the block-entity type
     * @param position the position of the entity
     * @param <T>      the entity class
     * @return a real instance, constructed through the registered factory
     */
    public static <T extends BlockEntity> T blockEntity(BlockEntityType<T> type, Vector3i position) {
        ensureInitialized();
        Level level = InterfaceProxy.create(Level.class);
        CloudChunk chunk = new CloudChunk(position.getX() >> 4, position.getZ() >> 4, level);
        return CloudBlockEntityRegistry.get().newEntity(type, chunk, position);
    }

    /**
     * Creates an entity of the given type through its registered factory,
     * backed by a stub level. The entity is constructed but not initialized,
     * so persistence tests exercise the production save and load paths
     * without needing a live world.
     *
     * @param type the entity type
     * @param <T>  the entity class
     * @return a real instance, constructed through the registered factory's constructor
     */
    @SuppressWarnings("unchecked")
    public static <T extends Entity> T entity(EntityType<T> type) {
        ensureInitialized();
        Location location = Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class));
        try {
            // Build through the registry's factory (EntityX::new), but without initialize(),
            // which needs a live world. The factory constructs the runtime class directly;
            // type.getEntityClass() is the api interface, so it cannot be instantiated.
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
            org.cloudburstmc.api.entity.EntityFactory<T> factory =
                    (org.cloudburstmc.api.entity.EntityFactory<T>) factoryMethod.invoke(factoryData);
            T entity = factory.create(type, location);
            // Initialize synced metadata (e.g. Sheep COLOR) without needing a live world.
            // initialize() itself requires chunk/level/server, but initEntity() only
            // touches data and registries.
            Class<?> current = entity.getClass();
            java.lang.reflect.Method initMethod = null;
            while (current != null) {
                try {
                    initMethod = current.getDeclaredMethod("initEntity");
                    break;
                } catch (NoSuchMethodException ignored) {
                    current = current.getSuperclass();
                }
            }
            if (initMethod != null) {
                initMethod.setAccessible(true);
                initMethod.invoke(entity);
            }
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create entity of type " + type.getId(), e);
        }
    }

    /**
     * Registers the test-only types and closes the shared registries exactly once,
     * so every test class can use this fixture alone, in any execution order.
     */
    public static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        // The block palette is identity-keyed: air and every other state only
        // resolve once a block registry has registered them, so persistence tests
        // run on the production codec path.
        BlockTestRegistry.get();
        CloudBlockEntityRegistry registry = CloudBlockEntityRegistry.get();
        // Test ids stay in the singleton for the JVM's lifetime; registering them in
        // exactly this one place keeps every test order safe.
        TestBlockEntities.registerAll(registry);
        if (!registry.isClosed()) {
            registry.close();
        }
        // The entity registry is a singleton too: vanilla types register on class load,
        // and creation plus data-class lookups need it closed. Closing twice throws,
        // so ignore the already-closed state to stay order-independent.
        try {
            CloudEntityRegistry.get().close();
        } catch (IllegalStateException | org.cloudburstmc.api.registry.RegistryException ignored) {
        }
        initialized = true;
    }
}
