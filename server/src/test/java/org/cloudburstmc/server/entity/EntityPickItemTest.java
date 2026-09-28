package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.SpawnEggComponent;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.data.TreeSpecies;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.component.PickItemEntityHandlers;
import org.cloudburstmc.server.entity.vehicle.EntityBoat;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityPickItemTest {

    @Test
    void resolvesSpawnEggsFromRegisteredItemComponents() {
        CloudItemRegistry items = CloudItemRegistry.get();
        CloudEntityRegistry entities = CloudEntityRegistry.get();

        int checked = 0;
        for (ItemType item : items.values()) {
            SpawnEggComponent egg = items.getComponent(item, ItemBehaviors.SPAWN_EGG);
            if (egg != null && entities.get(egg.entityType().getId()).isPresent()) {
                assertEquals(item, entity(egg.entityType()).getPickItem(false).getType());
                checked++;
            }
        }

        assertTrue(checked > 0);
    }

    @Test
    void resolvesSpecialItemsAndLeavesNonPickableEntitiesEmpty() {
        assertEquals(ItemTypes.END_CRYSTAL, entity(EntityTypes.ENDER_CRYSTAL).getPickItem(false).getType());
        assertEquals(ItemTypes.ARMOR_STAND, entity(EntityTypes.ARMOR_STAND).getPickItem(false).getType());
        assertEquals(ItemTypes.PAINTING, entity(EntityTypes.PAINTING).getPickItem(false).getType());
        assertEquals(ItemTypes.TNT_MINECART, entity(EntityTypes.TNT_MINECART).getPickItem(false).getType());
        assertSame(ItemStack.EMPTY, entity(EntityTypes.ARROW).getPickItem(false));
        assertSame(ItemStack.EMPTY, entity(EntityTypes.ITEM).getPickItem(false));
        assertSame(ItemStack.EMPTY, entity(EntityTypes.PLAYER).getPickItem(false));
    }

    @Test
    void picksTheBoatVariantRatherThanAlwaysPickingOak() {
        PickBoat boat = new PickBoat(TreeSpecies.SPRUCE.ordinal());

        assertEquals(ItemTypes.SPRUCE_BOAT, PickItemEntityHandlers.BOAT.execute(boat, false).getType());
        assertEquals(ItemTypes.SPRUCE_CHEST_BOAT, PickItemEntityHandlers.CHEST_BOAT.execute(boat, false).getType());
        assertSame(ItemStack.EMPTY, PickItemEntityHandlers.BOAT.execute(new PickBoat(-1), false));
    }

    private static CloudEntity entity(EntityType<?> type) {
        return new CloudEntity(type, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class))) {};
    }

    private static class PickBoat extends EntityBoat {
        private final int woodType;

        private PickBoat(int woodType) {
            super(EntityTypes.BOAT, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
            this.woodType = woodType;
        }

        @Override
        public int getWoodType() {
            return this.woodType;
        }
    }
}
