package org.cloudburstmc.server.block.trait;

import com.google.common.collect.ImmutableList;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@ParametersAreNonnullByDefault
public class BooleanBlockTrait implements BlockTrait<Boolean> {
    private static final ImmutableList<Boolean> VALUES = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);

    private final String name;
    private final boolean defaultValue;

    private BooleanBlockTrait(String name, boolean defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public static BooleanBlockTrait of(String name) {
        return of(name, false);
    }

    public static BooleanBlockTrait of(String name, boolean defaultValue) {
        checkNotNull(name, "name");
        return new BooleanBlockTrait(name, defaultValue);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<Boolean> getPossibleValues() {
        return VALUES;
    }

    @Override
    public Boolean getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Class<Boolean> getValueClass() {
        return Boolean.class;
    }

    @Override
    public int getIndex(Object value) {
        return getIndex((boolean) value);
    }

    public int getIndex(boolean value) {
        return value ? 1 : 0;
    }
}
