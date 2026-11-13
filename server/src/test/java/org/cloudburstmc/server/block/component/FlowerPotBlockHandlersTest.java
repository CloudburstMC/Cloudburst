package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.blockentity.FlowerPot;
import org.cloudburstmc.api.event.player.PlayerFlowerPotManipulateEvent;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FlowerPotBlockHandlersTest {

    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void cancellationPreservesThePlantAndTheHeldStack() {
        BlockTestWorld world = world();
        BlockState[] plant = attachEmptyPot(world);
        ItemStack bush = ItemStack.from(BlockTypes.DEADBUSH.getDefaultState());
        ItemStack[] held = {bush};
        Player player = player(world, held, new ArrayList<>());
        world.onEvent(PlayerFlowerPotManipulateEvent.class, event -> event.setCancelled(true));

        assertTrue(use(world, player, held[0]));
        assertSame(BlockStates.AIR, plant[0]);
        assertSame(bush, held[0]);

        PlayerFlowerPotManipulateEvent event = world.getEvents(PlayerFlowerPotManipulateEvent.class).getFirst();
        assertTrue(event.isPlacing());
        assertEquals(POSITION, event.getFlowerpot().getPosition());
        assertEquals(bush, event.getItem());
    }

    @Test
    void plantedDeadBushCanBePickedRemovedOrDropped() {
        BlockTestWorld world = world();
        BlockState bushState = BlockTypes.DEADBUSH.getDefaultState();
        BlockState[] plant = attachEmptyPot(world);
        ItemStack bush = ItemStack.from(bushState);
        ItemStack[] held = {bush.withCount(2)};
        List<ItemStack> received = new ArrayList<>();
        Player player = player(world, held, received);

        assertTrue(use(world, player, held[0]));
        assertSame(bushState, plant[0]);
        assertEquals(bush, held[0]);
        assertFalse(use(world, player, held[0]));

        Block pot = world.block(POSITION);
        assertEquals(bush, pot.getComponents().require(BlockComponents.GET_PICK_BLOCK).execute(pot));
        assertEquals(List.of(ItemStack.from(ItemTypes.FLOWER_POT), bush),
                pot.getComponents().require(BlockComponents.GET_LOOT)
                        .execute(pot, BlockLootContext.empty(new Random(0))));

        held[0] = ItemStack.EMPTY;
        assertTrue(use(world, player, held[0]));
        assertSame(BlockStates.AIR, plant[0]);
        assertEquals(List.of(bush), received);
        assertFalse(world.getEvents(PlayerFlowerPotManipulateEvent.class).getLast().isPlacing());
    }

    private static BlockTestWorld world() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockStates.FLOWER_POT);
        return world;
    }

    private static BlockState[] attachEmptyPot(BlockTestWorld world) {
        BlockState[] plant = {BlockStates.AIR};
        FlowerPot pot = InterfaceProxy.create(FlowerPot.class, (_, method, arguments) -> switch (method.getName()) {
            case "getPlant" -> plant[0];
            case "setPlant" -> {
                plant[0] = (BlockState) arguments[0];
                yield null;
            }
            case "isClosed" -> false;
            default -> throw new AssertionError("Unexpected flower pot access: " + method);
        });
        world.setBlockEntity(POSITION, pot);
        return plant;
    }

    private static boolean use(BlockTestWorld world, Player player, ItemStack held) {
        Block pot = world.block(POSITION);
        assertTrue(pot.getComponents().require(BlockComponents.CAN_BE_USED).execute(pot, player));
        return pot.getComponents().require(BlockComponents.USE).execute(pot, player, Direction.UP, held);
    }

    private static Player player(BlockTestWorld world, ItemStack[] held, List<ItemStack> received) {
        PlayerInventoryView inventory = InterfaceProxy.create(PlayerInventoryView.class,
                (_, method, arguments) -> switch (method.getName()) {
                    case "getSelectedItem" -> held[0];
                    case "setSelectedItem" -> {
                        held[0] = (ItemStack) arguments[0];
                        yield null;
                    }
                    case "addItem" -> {
                        received.addAll(List.of((ItemStack[]) arguments[0]));
                        yield new ItemStack[0];
                    }
                    default -> throw new AssertionError("Unexpected inventory access: " + method);
                });
        return InterfaceProxy.create(Player.class, Map.of("getInventory", inventory, "isCreative", false,
                "getServer", world.block(POSITION).getLevel().getServer()));
    }
}
