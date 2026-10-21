package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.event.block.CauldronChangeReason;
import org.cloudburstmc.api.event.block.CauldronLevelChangeEvent;
import org.cloudburstmc.api.event.entity.EntityInsideBlockEvent;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.data.Bucket;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.EntityInsideBlockScanner;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CauldronEntityInteractionTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void cancellingContactPreventsAllCauldronEffects() {
        for (Bucket liquid : List.of(Bucket.WATER, Bucket.POWDER_SNOW, Bucket.LAVA)) {
            BlockTestWorld world = world(liquid);
            world.onEvent(EntityInsideBlockEvent.class, event -> event.setCancelled(true));
            ContactEntity entity = new ContactEntity();
            entity.setOnFire(8);
            entity.setFreezeTicks(100);

            EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

            assertEquals(160, entity.getFireTicks());
            assertEquals(100, entity.getFreezeTicks());
            assertEquals(0, entity.damageTaken);
            assertSame(state(liquid, 6), world.state(POSITION));
            assertEquals(1, world.getEvents(EntityInsideBlockEvent.class).size());
            assertTrue(world.getEvents(CauldronLevelChangeEvent.class).isEmpty());
        }
    }

    @Test
    void contactPrecedesTheCauldronChangeEvent() {
        BlockTestWorld world = world(Bucket.WATER);
        ContactEntity entity = new ContactEntity();
        entity.setOnFire(8);

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

        List<Event> events = world.getEvents(Event.class);
        assertEquals(2, events.size());
        assertInstanceOf(EntityInsideBlockEvent.class, events.get(0));
        assertInstanceOf(CauldronLevelChangeEvent.class, events.get(1));
    }

    @Test
    void changingTheBlockDuringContactPreventsItsStaleResponse() {
        BlockTestWorld world = world(Bucket.LAVA);
        world.onEvent(EntityInsideBlockEvent.class,
                event -> world.setState(POSITION, BlockTypes.STONE.getDefaultState()));
        ContactEntity entity = new ContactEntity();

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

        assertFalse(entity.isOnFire());
        assertEquals(0, entity.damageTaken);
        assertSame(BlockTypes.STONE.getDefaultState(), world.state(POSITION));
    }

    @Test
    void blocksWithoutAnInsideResponseDoNotFireContactEvents() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockTypes.STONE.getDefaultState());

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), new ContactEntity(), true);

        assertTrue(world.getEvents(EntityInsideBlockEvent.class).isEmpty());
    }

    @Test
    void powderSnowBecomesWaterOnlyAfterAnAcceptedExtinguishingChange() {
        for (boolean cancelled : new boolean[]{false, true}) {
            BlockTestWorld world = world(Bucket.POWDER_SNOW);
            world.onEvent(CauldronLevelChangeEvent.class, event -> event.setCancelled(cancelled));
            ContactEntity entity = new ContactEntity();
            entity.setOnFire(8);

            EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

            assertEquals(cancelled, entity.isOnFire());
            assertSame(cancelled ? state(Bucket.POWDER_SNOW, 6) : state(Bucket.WATER, 4), world.state(POSITION));
            CauldronLevelChangeEvent event = world.getEvents(CauldronLevelChangeEvent.class).getFirst();
            assertSame(entity, event.getEntity());
            assertSame(CauldronChangeReason.EXTINGUISH, event.getReason());
        }
    }

    @Test
    void disabledMobGriefingPreventsExtinguishingAndLevelChangesAsInPaper() {
        BlockTestWorld world = world(Bucket.WATER);
        world.block(POSITION).getLevel().getGameRules().set(GameRules.MOB_GRIEFING, false);
        ContactEntity entity = new ContactEntity();
        entity.setOnFire(8);

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

        assertTrue(entity.isOnFire());
        assertSame(state(Bucket.WATER, 6), world.state(POSITION));
        assertTrue(world.getEvents(CauldronLevelChangeEvent.class).isEmpty());
    }

    @Test
    void lavaClearsFreezingWithoutIgnitingOrDamagingFireImmuneEntities() {
        BlockTestWorld world = world(Bucket.LAVA);
        ContactEntity entity = new ContactEntity();
        entity.fireProof = true;
        entity.setFreezeTicks(100);

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);
        DefaultBlockHandlers.FIRE_ENTITY_INSIDE.execute(world.block(POSITION), entity, true);

        assertEquals(0, entity.getFreezeTicks());
        assertFalse(entity.isOnFire());
        assertEquals(0, entity.damageTaken);
    }

    @Test
    void ordinaryLavaContactUsesTheSameDamagePathInsideAndOutsideCauldrons() {
        BlockTestWorld world = world(Bucket.LAVA);
        ContactEntity entity = new ContactEntity();
        entity.setFreezeTicks(100);

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

        assertEquals(0, entity.getFreezeTicks());
        assertEquals(300, entity.getFireTicks());
        assertEquals(4, entity.damageTaken);
        assertNotNull(entity.source);
        assertSame(DamageTypes.LAVA, entity.source.getDamageType());
        assertEquals(1, world.getEvents(EntityInsideBlockEvent.class).size());

        DefaultBlockHandlers.LAVA_ENTITY_INSIDE.execute(world.block(POSITION), entity, true);
        assertEquals(8, entity.damageTaken);
    }

    @Test
    void touchingTheOutsideWallDoesNotReachTheContents() {
        BlockTestWorld world = world(Bucket.LAVA);
        ContactEntity entity = new ContactEntity();
        entity.bounds = new BoundingBox(0.9f, 64.3f, 0.25f, 1.2f, 65.6f, 0.75f);

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

        assertFalse(entity.isOnFire());
        assertEquals(0, entity.damageTaken);
    }

    private static BlockTestWorld world(Bucket liquid) {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, state(liquid, 6));
        world.block(POSITION).getLevel().getGameRules().set(GameRules.MOB_GRIEFING, true);
        return world;
    }

    private static BlockState state(Bucket liquid, int level) {
        return BlockTypes.CAULDRON.getDefaultState()
                .withTrait(BlockTraits.CAULDRON_TYPE, liquid)
                .withTrait(BlockTraits.FILL_LEVEL, level);
    }

    private static class ContactEntity extends EntityLiving {

        private BoundingBox bounds = new BoundingBox(0.25f, 64.3f, 0.25f, 0.75f, 65.6f, 0.75f);
        private float damageTaken;
        private DamageSource source;

        private ContactEntity() {
            super(EntityTypes.ZOMBIE, Location.from(POSITION.toFloat(), InterfaceProxy.create(Level.class)));
        }

        @Override
        public BoundingBox getBoundingBox() {
            return this.bounds;
        }

        @Override
        public boolean damage(float amount, DamageSource source) {
            this.damageTaken += amount;
            this.source = source;
            return true;
        }
    }
}
