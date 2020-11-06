package org.cloudburstmc.server.item.provider;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EmptyItemProvider extends ItemDataProvider {

    public static final EmptyItemProvider INSTANCE = new EmptyItemProvider();

    private EmptyItemProvider() {
        super(null, null);
    }

    @Override
    public String getCustomName() {
        return null;
    }

    @Override
    public List<String> getLore() {
        return Collections.emptyList();
    }

    @Override
    public Set<EnchantmentInstance> getEnchantments() {
        return Collections.emptySet();
    }

    @Override
    public Set<Identifier> getCanDestroy() {
        return Collections.emptySet();
    }

    @Override
    public Set<Identifier> getCanPlaceOn() {
        return Collections.emptySet();
    }

    @Override
    public <T> T getMetadata(Class<T> clazz) {
        return null;
    }
}
