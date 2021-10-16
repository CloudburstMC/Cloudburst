package org.cloudburstmc.api.item;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Identifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public sealed class ItemType permits BlockType {

    private final Identifier id;
    private final Class<?> metadataClass;

    protected ItemType(Identifier id, Class<?> metadataClass) {
        this.id = id;
        this.metadataClass = metadataClass;
    }

    public final Identifier getId() {
        return id;
    }

    @Nullable
    public final Class<?> getMetadataClass() {
        return metadataClass;
    }

    @Override
    public String toString() {
        return "ItemType{id=" + id + ')';
    }

    public static ItemType of(Identifier id) {
        return of(id, null);
    }

    public static ItemType of(Identifier id, Class<?> metadataClass) {
        checkNotNull(id, "id");

        return new ItemType(id, metadataClass);
    }
}
