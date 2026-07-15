package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockTagKey;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.util.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockTagTest {

    @Test
    void keysAreCanonical() {
        BlockTagKey first = BlockTagKey.of("minecraft:test");
        BlockTagKey second = BlockTagKey.of(Identifier.parse("minecraft:test"));

        assertSame(first, second);
    }

    @Test
    void keysDoNotExposeRegistryMutation() {
        assertFalse(Arrays.stream(BlockTagKey.class.getMethods())
                .map(Method::getName)
                .anyMatch(name -> name.equals("add") || name.equals("include")));
    }

    @Test
    void blockStatesUseTheirTypeTags() {
        BlockTagKey key = BlockTagKey.of("test:holder_backed");
        BlockType type = BlockType.of(Identifier.parse("test:holder_backed"));
        type.bindTags(Set.of(key));

        assertTrue(type.is(key));
        assertTrue(type.getDefaultState().is(key));
    }
}
