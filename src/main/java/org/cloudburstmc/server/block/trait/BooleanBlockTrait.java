package org.cloudburstmc.server.block.trait;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static com.google.common.base.Preconditions.checkNotNull;

@ParametersAreNonnullByDefault
public class BooleanBlockTrait extends BlockTrait<Boolean> {
    private static final ImmutableList<Boolean> VALUES = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);

    private final boolean defaultValue;

    private BooleanBlockTrait(String name, @Nullable String vanillaName, boolean defaultValue) {
        super(name, vanillaName, Boolean.class, VALUES);
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
        checkNotNull(name, "name");
        return new BooleanBlockTrait(name, vanillaName, defaultValue);
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
}
