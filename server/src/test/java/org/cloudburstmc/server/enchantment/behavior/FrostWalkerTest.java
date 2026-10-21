package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.block.EntityBlockFormEvent;
import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.enchantment.EquipmentEnchantmentEffects;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FrostWalkerTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i WATER_POSITION = Vector3i.from(0, 63, 0);

    @Test
    void freezingACompleteDiskDoesNotMeltPreviouslyPlacedIce() {
        for (Vector3i center : List.of(WATER_POSITION, Vector3i.from(-10, 63, -20), Vector3i.from(10, 63, -20))) {
            Walker walker = new Walker();
            walker.position = center.toFloat().add(0.5f, 1, 0.5f);
            walker.world.enableNeighborUpdates(BlockTypes.FROSTED_ICE);
            for (int z = -3; z <= 3; z++) {
                for (int x = -3; x <= 3; x++) {
                    walker.world.setState(center.add(x, 0, z), BlockStates.WATER);
                }
            }

            walker.apply(1, EquipmentSlot.FEET);

            assertEquals(25, walker.world.getEvents(EntityBlockFormEvent.class).size());
            for (int z = -3; z <= 3; z++) {
                for (int x = -3; x <= 3; x++) {
                    assertSame(x * x + z * z < 9 ? BlockTypes.FROSTED_ICE : BlockTypes.WATER,
                            walker.world.state(center.add(x, 0, z)).getType());
                }
            }
        }
    }

    @Test
    void registryFreezesOnlySourceWaterBeneathAir() {
        Walker walker = new Walker();
        walker.world.setState(WATER_POSITION, BlockStates.WATER);
        Vector3i flowing = WATER_POSITION.add(1, 0, 0);
        Vector3i falling = WATER_POSITION.add(-1, 0, 0);
        Vector3i covered = WATER_POSITION.add(0, 0, 1);
        Vector3i waterlogged = WATER_POSITION.add(0, 0, -1);
        walker.world.setState(flowing, BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 1));
        walker.world.setState(falling, BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 8));
        walker.world.setState(covered, BlockStates.WATER);
        walker.world.setState(covered.add(0, 1, 0), BlockStates.STONE);
        walker.world.setState(waterlogged, BlockTypes.MANGROVE_ROOTS.getDefaultState());
        walker.world.setLiquid(waterlogged, LiquidState.of(BlockStates.WATER));

        walker.apply(1, EquipmentSlot.FEET);

        assertSame(BlockTypes.FROSTED_ICE.getDefaultState(), walker.world.state(WATER_POSITION));
        assertSame(BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 1), walker.world.state(flowing));
        assertSame(BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 8), walker.world.state(falling));
        assertSame(BlockStates.WATER, walker.world.state(covered));
        assertSame(BlockTypes.MANGROVE_ROOTS.getDefaultState(), walker.world.state(waterlogged));
        assertEquals(1, walker.world.getEvents(EntityBlockFormEvent.class).size());
        assertSame(walker.entity, walker.world.getEvents(EntityBlockFormEvent.class).getFirst().getEntity());
    }

    @Test
    void circleUsesTheExactEntityPositionAndExcludesItsBoundary() {
        Walker walker = new Walker();
        walker.position = Vector3f.from(-10.5f, 64, -20.5f);
        Vector3i center = walker.position.toInt().add(0, -1, 0);
        Vector3i inside = center.add(2, 0, 0);
        Vector3i boundary = center.add(3, 0, 0);
        Vector3i outsideCorner = center.add(2, 0, 3);
        for (Vector3i position : List.of(inside, boundary, outsideCorner)) {
            walker.world.setState(position, BlockStates.WATER);
        }

        walker.apply(1, EquipmentSlot.FEET);

        assertSame(BlockTypes.FROSTED_ICE.getDefaultState(), walker.world.state(inside));
        assertSame(BlockStates.WATER, walker.world.state(boundary));
        assertSame(BlockStates.WATER, walker.world.state(outsideCorner));
    }

    @Test
    void levelTwoExtendsTheRadiusAndLargeLevelsRemainCapped() {
        for (int enchantmentLevel : List.of(1, 2, Integer.MAX_VALUE)) {
            Walker walker = new Walker();
            Vector3i near = WATER_POSITION.add(3, 0, 0);
            Vector3i far = WATER_POSITION.add(15, 0, 0);
            Vector3i boundary = WATER_POSITION.add(16, 0, 0);
            for (Vector3i position : List.of(near, far, boundary)) {
                walker.world.setState(position, BlockStates.WATER);
            }

            walker.apply(enchantmentLevel, EquipmentSlot.FEET);

            assertEquals(enchantmentLevel >= 2 ? BlockTypes.FROSTED_ICE : BlockTypes.WATER, walker.world.state(near).getType());
            assertEquals(enchantmentLevel == Integer.MAX_VALUE ? BlockTypes.FROSTED_ICE : BlockTypes.WATER, walker.world.state(far).getType());
            assertSame(BlockStates.WATER, walker.world.state(boundary));
        }
    }

    @Test
    void airborneMountedAndIncorrectSlotUsesDoNothing() {
        Walker walker = new Walker();
        walker.world.setState(WATER_POSITION, BlockStates.WATER);
        walker.onGround = false;

        walker.apply(2, EquipmentSlot.FEET);
        assertSame(BlockStates.WATER, walker.world.state(WATER_POSITION));

        walker.onGround = true;
        walker.vehicle = InterfaceProxy.create(Entity.class);
        walker.apply(2, EquipmentSlot.FEET);
        assertSame(BlockStates.WATER, walker.world.state(WATER_POSITION));

        walker.vehicle = null;
        walker.apply(2, EquipmentSlot.HEAD);
        assertSame(BlockStates.WATER, walker.world.state(WATER_POSITION));
        assertTrue(walker.world.getEvents(EntityBlockFormEvent.class).isEmpty());
    }

    @Test
    void unloadedAndEntityObstructedPositionsAreNotFrozen() {
        Walker walker = new Walker();
        Vector3i unloadedWater = WATER_POSITION.add(1, 0, 0);
        Vector3i unloadedAbove = WATER_POSITION.add(-1, 0, 0);
        for (Vector3i position : List.of(WATER_POSITION, unloadedWater, unloadedAbove)) {
            walker.world.setState(position, BlockStates.WATER);
        }
        walker.world.obstruct(WATER_POSITION);
        walker.world.unload(unloadedWater);
        walker.world.unload(unloadedAbove.add(0, 1, 0));

        walker.apply(2, EquipmentSlot.FEET);

        assertTrue(walker.world.getEvents(EntityBlockFormEvent.class).isEmpty());
        for (Vector3i position : List.of(WATER_POSITION, unloadedWater, unloadedAbove)) {
            assertSame(BlockStates.WATER, walker.world.state(position));
        }
    }

    @Test
    void formationRespectsCancellationReplacementAndChangesMadeByListeners() {
        Walker walker = new Walker();
        walker.world.setState(WATER_POSITION, BlockStates.WATER);
        walker.world.onEvent(EntityBlockFormEvent.class, event -> event.setCancelled(true));

        walker.apply(1, EquipmentSlot.FEET);
        assertSame(BlockStates.WATER, walker.world.state(WATER_POSITION));

        walker.world.onEvent(EntityBlockFormEvent.class, event -> event.setNewState(BlockStates.STONE));
        walker.apply(1, EquipmentSlot.FEET);
        assertSame(BlockStates.STONE, walker.world.state(WATER_POSITION));

        walker.world.setState(WATER_POSITION, BlockStates.WATER);
        walker.world.onEvent(EntityBlockFormEvent.class, event -> walker.world.setState(WATER_POSITION, BlockStates.LAVA));
        walker.apply(1, EquipmentSlot.FEET);
        assertSame(BlockStates.LAVA, walker.world.state(WATER_POSITION));
    }

    @Test
    void formationRechecksAirAndStopsIfTheWearerMovesDuringTheEvent() {
        Walker walker = new Walker();
        walker.world.setState(WATER_POSITION, BlockStates.WATER);
        walker.world.onEvent(EntityBlockFormEvent.class, event -> walker.world.setState(WATER_POSITION.add(0, 1, 0), BlockStates.STONE));

        walker.apply(1, EquipmentSlot.FEET);
        assertSame(BlockStates.WATER, walker.world.state(WATER_POSITION));

        walker.world.setState(WATER_POSITION.add(0, 1, 0), BlockStates.AIR);
        walker.world.onEvent(EntityBlockFormEvent.class, event -> walker.position = Vector3f.from(100, 64, 100));
        walker.apply(1, EquipmentSlot.FEET);
        assertSame(BlockStates.WATER, walker.world.state(WATER_POSITION));
    }

    @Test
    void equipmentTrackerDoesNotRefreshIceWhileStationaryAndReactsToEquipAndLanding() {
        Walker walker = new Walker();
        walker.world.setState(WATER_POSITION, BlockStates.WATER);
        EquipmentEnchantmentEffects effects = new EquipmentEnchantmentEffects();
        ItemStack[] items = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        ArmorView armor = InterfaceProxy.create(ArmorView.class, (proxy, method, arguments) -> {
            if (method.getName().equals("getItem")) {
                return items[(Integer) arguments[0]];
            }

            throw new AssertionError("Unexpected armor access: " + method);
        });

        effects.update(walker.entity, armor);
        assertSame(BlockStates.WATER, walker.world.state(WATER_POSITION));

        items[EquipmentSlot.FEET.getArmorSlot()] = boots(1);
        effects.update(walker.entity, armor);
        assertSame(BlockTypes.FROSTED_ICE.getDefaultState(), walker.world.state(WATER_POSITION));

        walker.world.setState(WATER_POSITION, BlockStates.WATER);
        effects.update(walker.entity, armor);
        assertSame(BlockStates.WATER, walker.world.state(WATER_POSITION));

        walker.onGround = false;
        effects.update(walker.entity, armor);
        walker.onGround = true;
        effects.update(walker.entity, armor);
        assertSame(BlockTypes.FROSTED_ICE.getDefaultState(), walker.world.state(WATER_POSITION));
    }

    @Test
    void aGroundedMovementFrameFreezesWaterBeforeTheNextAirborneFrame() {
        Walker walker = new Walker();
        Vector3i edge = WATER_POSITION.add(3, 0, 0);
        walker.world.setState(edge, BlockStates.WATER);
        EquipmentEnchantmentEffects effects = new EquipmentEnchantmentEffects();
        ArmorView armor = InterfaceProxy.create(ArmorView.class, (proxy, method, arguments) -> {
            if (method.getName().equals("getItem")) {
                return (Integer) arguments[0] == EquipmentSlot.FEET.getArmorSlot() ? boots(1) : ItemStack.EMPTY;
            }

            throw new AssertionError("Unexpected armor access: " + method);
        });

        effects.update(walker.entity, armor);
        assertSame(BlockStates.WATER, walker.world.state(edge));

        walker.position = walker.position.add(1, 0, 0);
        effects.update(walker.entity, armor);
        walker.onGround = false;
        walker.position = walker.position.add(0.3f, 1, 0);
        effects.update(walker.entity, armor);

        assertSame(BlockTypes.FROSTED_ICE.getDefaultState(), walker.world.state(edge));
        assertEquals(1, walker.world.getEvents(EntityBlockFormEvent.class).size());
    }

    @Test
    void frostWalkerOnlyGrantsSteppingBurnImmunityFromTheFeetSlot() {
        ItemStack boots = boots(1);
        CloudEnchantmentRegistry enchantments = CloudEnchantmentRegistry.get();

        for (DamageSource source : List.of(DamageSource.of(DamageTypes.HOT_FLOOR), DamageSource.of(DamageTypes.CAMPFIRE))) {
            assertTrue(enchantments.grantsDamageImmunity(boots, EquipmentSlot.FEET, source));
            assertFalse(enchantments.grantsDamageImmunity(boots, EquipmentSlot.HEAD, source));
        }
        for (DamageSource source : List.of(DamageSource.of(DamageTypes.LAVA), DamageSource.of(DamageTypes.ON_FIRE),
                DamageSource.of(DamageTypes.FALL), DamageSource.of(DamageTypes.GENERIC_KILL))) {
            assertFalse(enchantments.grantsDamageImmunity(boots, EquipmentSlot.FEET, source));
        }
    }

    private static ItemStack boots(int level) {
        return ItemStack.builder().itemType(ItemTypes.DIAMOND_BOOTS)
                .setData(ItemDataComponents.ENCHANTMENTS, Map.of(EnchantmentTypes.FROST_WALKER, new Enchantment(EnchantmentTypes.FROST_WALKER, level)))
                .build();
    }

    private static class Walker {

        private final BlockTestWorld world = new BlockTestWorld(REGISTRY);
        private final Level level = this.world.block(WATER_POSITION).getLevel();
        private Vector3f position = Vector3f.from(0.5f, 64, 0.5f);
        private boolean onGround = true;
        private Entity vehicle;
        private final Entity entity = InterfaceProxy.create(Entity.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getLevel" -> this.level;
            case "getPosition" -> this.position;
            case "isOnGround" -> this.onGround;
            case "getVehicle" -> this.vehicle;
            default -> throw new AssertionError("Unexpected entity access: " + method);
        });

        private void apply(int level, EquipmentSlot slot) {
            CloudEnchantmentRegistry.get().applyLocationEffects(boots(level), this.entity, slot);
        }
    }
}
