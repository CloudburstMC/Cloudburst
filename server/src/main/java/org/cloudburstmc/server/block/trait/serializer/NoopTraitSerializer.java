package org.cloudburstmc.server.block.trait.serializer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoopTraitSerializer<T extends Comparable<T>> implements TraitSerializer<T> {

    private static final NoopTraitSerializer<?> INSTANCE = new NoopTraitSerializer<>();

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> NoopTraitSerializer<T> instance() {
        return (NoopTraitSerializer<T>) INSTANCE;
    }
}
