package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockEntityRegistryTest {

    private final CloudBlockEntityRegistry registry = CloudBlockEntityRegistry.get();

    @Test
    void acceptsBlocksFromRegisteredFamilies() {
        for (ValidBlockFamily family : validBlockFamilies()) {
            assertTrue(this.registry.isValid(family.type(), family.block().getDefaultState()),
                    () -> family.block() + " should be valid for " + family.type());
        }
    }

    @Test
    void rejectsBlockFromDifferentFamily() {
        assertFalse(this.registry.isValid(BlockEntityTypes.SHULKER_BOX, BlockTypes.CHEST.getDefaultState()));
    }

    private record ValidBlockFamily(BlockEntityType<?> type, BlockType block) {
    }

    private static ValidBlockFamily[] validBlockFamilies() {
        return new ValidBlockFamily[]{
                new ValidBlockFamily(BlockEntityTypes.SHULKER_BOX, BlockTypes.UNDYED_SHULKER_BOX),
                new ValidBlockFamily(BlockEntityTypes.SHULKER_BOX, BlockTypes.RED_SHULKER_BOX),
                new ValidBlockFamily(BlockEntityTypes.SIGN, BlockTypes.PALE_OAK_HANGING_SIGN),
                new ValidBlockFamily(BlockEntityTypes.CAMPFIRE, BlockTypes.SOUL_CAMPFIRE),
                new ValidBlockFamily(BlockEntityTypes.ITEM_FRAME, BlockTypes.GLOW_FRAME)
        };
    }
}
