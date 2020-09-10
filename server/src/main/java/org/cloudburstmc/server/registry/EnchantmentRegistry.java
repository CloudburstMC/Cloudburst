package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.NonNull;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentType;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class EnchantmentRegistry implements Registry {

    private static final EnchantmentRegistry INSTANCE;

    static {
        INSTANCE = new EnchantmentRegistry();
    }

    public static EnchantmentRegistry get() {
        return INSTANCE;
    }

    private final Map<EnchantmentType, EnchantmentBehavior> behaviorMap = new Reference2ObjectOpenHashMap<>();

    public synchronized void register(@NonNull EnchantmentType type, @Nonnull EnchantmentBehavior behavior) {
        throw new UnsupportedOperationException("Custom enchantments are not currently supported!");
    }

    private synchronized void registerVanilla(@NonNull EnchantmentType type, @Nonnull EnchantmentBehavior behavior) {
        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(behavior, "behavior");
        Preconditions.checkState(!behaviorMap.containsKey(type), "Enchantment %s already registered", type);

        behaviorMap.put(type, behavior);
    }

    public EnchantmentInstance getEnchantment(@Nonnull EnchantmentType type) {
        return getEnchantment(type, 1);
    }

    public EnchantmentInstance getEnchantment(@Nonnull EnchantmentType type, int level) {
        Preconditions.checkNotNull(type, "type");

        return new CloudEnchantmentInstance(type, level);
    }

    public EnchantmentBehavior getBehavior(@Nonnull EnchantmentType type) {
        Preconditions.checkNotNull(type, "type");
        return behaviorMap.get(type);
    }

    @Override
    public void close() throws RegistryException {

    }

    private void registerVanillaEnchantments() {

    }
}
