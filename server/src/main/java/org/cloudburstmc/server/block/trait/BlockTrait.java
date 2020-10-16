package org.cloudburstmc.server.block.trait;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.cloudburstmc.server.block.BlockTraits;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

@Getter
@ParametersAreNonnullByDefault
public abstract class BlockTrait<E extends Comparable<E>> {

    protected final String name;
    protected final String vanillaName;
    protected final Class<E> valueClass;
    protected final boolean onlySerialize;
    protected final ImmutableList<E> possibleValues;

    public BlockTrait(String name, @Nullable String vanillaName, Class<E> valueClass, List<E> possibleValues, boolean onlySerialize) {
        checkNotNull(name);
        checkNotNull(valueClass);
        checkNotNull(possibleValues);
        this.name = name;
        this.vanillaName = vanillaName;
        this.valueClass = valueClass;
        this.onlySerialize = onlySerialize;
        this.possibleValues = ImmutableList.copyOf(possibleValues);

        BlockTraits.register(this);
    }

    public String getVanillaName() {
        if (vanillaName != null) {
            return vanillaName;
        }

        return name;
    }

    public abstract E getDefaultValue();

    Predicate<E> getValidator() {
        return e -> this.getPossibleValues().contains(e);
    }

    @SuppressWarnings("unchecked")
    public boolean testValue(Object value) {
        return getValidator().test((E) value);
    }

    public boolean isOnlySerialize() {
        return onlySerialize;
    }

    public ImmutableList<E> getPossibleValues() {
        return possibleValues;
    }

    public abstract int getIndex(Object value);

    public abstract E parseValue(String text);

    public abstract E parseStorageValue(Object value);

    @Override
    public String toString() {
        return this.name;
    }
}
