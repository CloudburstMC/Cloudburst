package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.DefaultBlockHandlers;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StriderFireImmunityTest {

    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void initializationSetsServerAndClientFireImmunity() {
        ContactStrider strider = strider();

        assertTrue(strider.fireProof);
        assertTrue(strider.hasClientFireImmunity());
    }

    @Test
    void fireContactNeitherIgnitesNorRequestsDamage() {
        BlockTestWorld world = world();
        ContactStrider strider = strider();

        DefaultBlockHandlers.FIRE_ENTITY_INSIDE.execute(world.block(POSITION), strider, true);

        assertEquals(0, strider.getFireTicks());
        assertEquals(0f, strider.damageRequested);
    }

    @Test
    void lavaContactThawsWithoutIgnitingOrRequestingDamage() {
        BlockTestWorld world = world();
        ContactStrider strider = strider();
        strider.setFreezeTicks(100);

        DefaultBlockHandlers.LAVA_ENTITY_INSIDE.execute(world.block(POSITION), strider, true);

        assertEquals(0, strider.getFreezeTicks());
        assertEquals(0, strider.getFireTicks());
        assertEquals(0f, strider.damageRequested);
    }

    @Test
    void contactHandlersStillRequestDamageWhenServerImmunityIsDisabled() {
        BlockTestWorld world = world();
        ContactStrider strider = strider();
        strider.fireProof = false;

        DefaultBlockHandlers.FIRE_ENTITY_INSIDE.execute(world.block(POSITION), strider, true);
        assertEquals(160, strider.getFireTicks());
        assertEquals(1f, strider.damageRequested);

        DefaultBlockHandlers.LAVA_ENTITY_INSIDE.execute(world.block(POSITION), strider, true);
        assertEquals(300, strider.getFireTicks());
        assertEquals(5f, strider.damageRequested);
    }

    private static BlockTestWorld world() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockTypes.LAVA.getDefaultState());
        return world;
    }

    private static ContactStrider strider() {
        PersistenceFixtures.ensureInitialized();
        ContactStrider strider = new ContactStrider();
        strider.initEntity();
        return strider;
    }

    private static final class ContactStrider extends EntityStrider {
        private float damageRequested;

        private ContactStrider() {
            super(EntityTypes.STRIDER,
                    Location.from(POSITION.toFloat(), InterfaceProxy.create(Level.class)));
        }

        private boolean hasClientFireImmunity() {
            return this.data.getFlag(FIRE_IMMUNE);
        }

        @Override
        public boolean damage(float amount, DamageSource source) {
            this.damageRequested += amount;
            return true;
        }
    }
}
