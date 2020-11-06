package org.cloudburstmc.server.block.trait;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static com.google.common.base.Preconditions.checkNotNull;

@ParametersAreNonnullByDefault
public class BooleanBlockTrait extends BlockTrait<Boolean> {
    private static final ImmutableList<Boolean> VALUES = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);

    private final boolean defaultValue;

    private BooleanBlockTrait(String name, @Nullable String vanillaName, boolean defaultValue, boolean onlySerialize) {
        super(name, vanillaName, Boolean.class, VALUES, onlySerialize);
        this.defaultValue = defaultValue;
    }

    public static BooleanBlockTrait of(String name) {
        return of(name, null, false);
    }

    public static BooleanBlockTrait of(String name, @Nullable String vanillaName) {
        return of(name, vanillaName, false);
    }

    public static BooleanBlockTrait of(String name, boolean defaultValue) {
        return of(name, null, defaultValue);
    }

    public static BooleanBlockTrait of(String name, @Nullable String vanillaName, boolean defaultValue) {
        return of(name, vanillaName, defaultValue, false);
    }

    public static BooleanBlockTrait of(String name, @Nullable String vanillaName, boolean defaultValue, boolean onlySerialize) {
        checkNotNull(name, "name");
        return new BooleanBlockTrait(name, vanillaName, defaultValue, onlySerialize);
    }

    @Override
    public Boolean getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public int getIndex(Object value) {
        return getIndex((boolean) value);
    }

    public int getIndex(boolean value) {
        return value ? 1 : 0;
    }

    @Override
    public Boolean parseValue(String text) {
        return Boolean.parseBoolean(text);
    }

    @Override
    public Boolean parseStorageValue(Object value) {
        return (byte) value == 1;
    }
}
