package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DaylightDetectorBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void useSwitchesBothModesWithoutRecalculatingTheSignal() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        Player player = InterfaceProxy.create(Player.class, Map.of("isSurvival", true));
        BlockState normal = BlockStates.DAYLIGHT_DETECTOR.withTrait(BlockTraits.REDSTONE_SIGNAL, 7);
        BlockState inverted = BlockStates.DAYLIGHT_DETECTOR_INVERTED.withTrait(BlockTraits.REDSTONE_SIGNAL, 7);
        world.setState(POSITION, normal);

        assertTrue(REGISTRY.requireComponent(BlockTypes.DAYLIGHT_DETECTOR, BlockComponents.USE)
                .execute(world.block(POSITION), player, Direction.UP, ItemStack.EMPTY));
        assertSame(inverted, world.state(POSITION));
        assertEquals(0, REGISTRY.requireComponent(BlockTypes.DAYLIGHT_DETECTOR_INVERTED, BlockComponents.GET_REDSTONE_SIGNAL)
                .execute(world.block(POSITION), Direction.UP));

        assertTrue(REGISTRY.requireComponent(BlockTypes.DAYLIGHT_DETECTOR_INVERTED, BlockComponents.USE)
                .execute(world.block(POSITION), player, Direction.UP, ItemStack.EMPTY));
        assertSame(normal, world.state(POSITION));
    }

    @Test
    void useDoesNotChangeTheModeForAPlayerWithoutBuildPrivileges() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockStates.DAYLIGHT_DETECTOR);
        Player player = InterfaceProxy.create(Player.class, Map.of("isAdventure", true));

        assertFalse(REGISTRY.requireComponent(BlockTypes.DAYLIGHT_DETECTOR, BlockComponents.USE)
                .execute(world.block(POSITION), player, Direction.UP, ItemStack.EMPTY));
        assertSame(BlockStates.DAYLIGHT_DETECTOR, world.state(POSITION));
    }

    @Test
    void invertedDetectorPicksAndPlacesInvertedButDropsNormal() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockStates.DAYLIGHT_DETECTOR_INVERTED.withTrait(BlockTraits.REDSTONE_SIGNAL, 7));

        ItemStack picked = REGISTRY.requireComponent(BlockTypes.DAYLIGHT_DETECTOR_INVERTED, BlockComponents.GET_PICK_BLOCK)
                .execute(world.block(POSITION));
        assertSame(ItemTypes.DAYLIGHT_DETECTOR_INVERTED, picked.getType());
        assertSame(BlockStates.DAYLIGHT_DETECTOR_INVERTED, picked.requireBlockState());
        assertSame(BlockStates.DAYLIGHT_DETECTOR_INVERTED,
                CloudItemRegistry.get().requireComponent(picked.getType(), ItemBehaviors.GET_BLOCK)
                        .execute(picked).orElseThrow());

        assertEquals(List.of(ItemStack.from(BlockStates.DAYLIGHT_DETECTOR)),
                REGISTRY.requireComponent(BlockTypes.DAYLIGHT_DETECTOR_INVERTED, BlockComponents.GET_LOOT)
                        .execute(world.block(POSITION), BlockLootContext.empty(new Random(0))));
    }
}
