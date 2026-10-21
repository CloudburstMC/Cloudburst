package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VanillaBlockItemsTest {

    private static final CloudBlockRegistry BLOCKS = BlockTestRegistry.get();
    private static final CloudItemRegistry ITEMS = CloudItemRegistry.get();

    @Test
    void itemAndBlockNamesDoNotNeedToMatch() {
        assertSame(ItemTypes.SUGAR_CANE, BlockTypes.REEDS.asItem().orElseThrow());
        assertSame(BlockTypes.REEDS.getDefaultState(), placement(ItemStack.from(ItemTypes.SUGAR_CANE)).orElseThrow());
        assertSame(ItemTypes.WHEAT_SEEDS, BlockTypes.WHEAT.asItem().orElseThrow());
        assertSame(BlockTypes.WHEAT.getDefaultState(), placement(ItemStack.from(ItemTypes.WHEAT_SEEDS)).orElseThrow());
        assertTrue(placement(ItemStack.from(ItemTypes.WHEAT)).isEmpty());
    }

    @Test
    void sharedFormsCannotOverwriteTheDefaultPlacementBlock() {
        assertSame(ItemTypes.OAK_SIGN, BlockTypes.OAK_STANDING_SIGN.asItem().orElseThrow());
        assertSame(ItemTypes.OAK_SIGN, BlockTypes.OAK_WALL_SIGN.asItem().orElseThrow());
        assertSame(BlockTypes.OAK_STANDING_SIGN.getDefaultState(), placement(ItemStack.from(ItemTypes.OAK_SIGN)).orElseThrow());

        assertSame(ItemTypes.CUT_COPPER_SLAB, BlockTypes.DOUBLE_CUT_COPPER_SLAB.asItem().orElseThrow());
        assertSame(BlockTypes.CUT_COPPER_SLAB.getDefaultState(), placement(ItemStack.from(ItemTypes.CUT_COPPER_SLAB)).orElseThrow());

        assertSame(ItemTypes.REPEATER, BlockTypes.POWERED_REPEATER.asItem().orElseThrow());
        assertSame(BlockTypes.UNPOWERED_REPEATER.getDefaultState(), placement(ItemStack.from(ItemTypes.REPEATER)).orElseThrow());
    }

    @Test
    void pickedSharedFormsResolveToTheirItemsPlacementType() {
        for (BlockType form : List.of(BlockTypes.OAK_WALL_SIGN, BlockTypes.OAK_DOUBLE_SLAB,
                BlockTypes.LIT_FURNACE, BlockTypes.POWERED_REPEATER, BlockTypes.BRAIN_CORAL_WALL_FAN)) {
            ItemStack picked = ItemStack.from(form.getDefaultState());
            BlockState placed = assertInstanceOf(BlockState.class, placement(picked).orElseThrow());
            BlockState defaultPlacement = assertInstanceOf(BlockState.class,
                    placement(ItemStack.from(picked.getType())).orElseThrow());

            assertSame(defaultPlacement.getType(), placed.getType());
            assertSame(picked.getType(), placed.getType().asItem().orElseThrow());
        }
    }

    @Test
    void blocksWithoutItemsDoNotGainPlacementBehaviorsFromTheirNames() {
        for (BlockType block : List.of(BlockTypes.AIR, BlockTypes.FIRE, BlockTypes.PORTAL,
                BlockTypes.END_PORTAL, BlockTypes.END_GATEWAY, BlockTypes.WATER,
                BlockTypes.LAVA, BlockTypes.POWDER_SNOW, BlockTypes.CANDLE_CAKE)) {
            assertTrue(block.asItem().isEmpty(), block.getId().toString());
        }

        assertTrue(placement(ItemStack.from(ItemTypes.END_PORTAL)).isEmpty());
        assertTrue(placement(ItemStack.from(ItemTypes.FIRE)).isEmpty());
    }

    @Test
    void storedBlockStatesMustBelongToTheItemsDeclaredForms() {
        ItemStack valid = ItemStack.builder()
                .itemType(ItemTypes.SUGAR_CANE)
                .setData(ItemDataComponents.BLOCK_STATE, BlockTypes.REEDS.getDefaultState().withTrait(BlockTraits.AGE, 7))
                .build();
        ItemStack invalid = valid.toBuilder().setData(ItemDataComponents.BLOCK_STATE, BlockStates.STONE).build();

        assertSame(valid.requireBlockState(), placement(valid).orElseThrow());
        assertTrue(placement(invalid).isEmpty());
    }

    @Test
    void candleCakeHasNoItemButStillPicksCake() {
        BlockTestWorld world = new BlockTestWorld(BLOCKS);
        Vector3i position = Vector3i.from(0, 64, 0);
        world.setState(position, BlockTypes.CANDLE_CAKE.getDefaultState());

        ItemStack picked = BLOCKS.requireComponent(BlockTypes.CANDLE_CAKE, BlockComponents.GET_PICK_BLOCK)
                .execute(world.block(position));

        assertSame(ItemTypes.CAKE, picked.getType());
    }

    private static Optional<?> placement(ItemStack item) {
        return assertInstanceOf(Optional.class, ITEMS.requireComponent(item.getType(), ItemBehaviors.GET_BLOCK).execute(item));
    }
}
