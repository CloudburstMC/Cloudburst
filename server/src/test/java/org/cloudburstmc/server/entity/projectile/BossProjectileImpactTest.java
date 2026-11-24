package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BossProjectileImpactTest {

    @Test
    void dragonFireballDoesNotDealDirectContactDamage() {
        EntityDragonFireball fireball = assertInstanceOf(EntityDragonFireball.class,
                PersistenceFixtures.entity(EntityTypes.DRAGON_FIREBALL));
        Entity target = InterfaceProxy.create(Entity.class,
                (_, _, _) -> fail("Fireball contact must not interact with the target"));

        fireball.onCollideWithEntity(target);

        assertFalse(fireball.isClosed());
    }

    @Test
    void bothSkullsHealTheirLivingOwnerOnlyAfterAnAcceptedKill() {
        AtomicInteger heals = new AtomicInteger();
        LivingEntity owner = InterfaceProxy.create(LivingEntity.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> 1L;
            case "isClosed" -> false;
            case "heal" -> {
                assertEquals(5.0F, arguments[0]);
                assertSame(RegainReason.WITHER, arguments[1]);
                heals.incrementAndGet();
                yield null;
            }
            default -> fail("Unexpected owner call: " + method.getName());
        });

        for (EntityWitherSkull skull : skulls()) {
            skull.setOwner(owner);
            heals.set(0);

            skull.onCollideWithEntity(InterfaceProxy.create(Damageable.class, (proxy, method, arguments) -> {
                assertEquals("damage", method.getName());
                return false;
            }));
            assertEquals(0, heals.get());

            skull.onCollideWithEntity(InterfaceProxy.create(Damageable.class,
                    (proxy, method, arguments) -> switch (method.getName()) {
                        case "damage" -> {
                            assertEquals(8.0F, arguments[0]);
                            DamageSource source = (DamageSource) arguments[1];
                            assertSame(DamageTypes.WITHER_SKULL, source.getDamageType());
                            assertSame(skull, source.getDirectEntity());
                            assertSame(owner, source.getCausingEntity());
                            yield true;
                        }
                        case "isAlive" -> false;
                        default -> fail("Unexpected target call: " + method.getName());
                    }));
            assertEquals(1, heals.get());
            assertFalse(skull.isClosed());
        }
    }

    @Test
    void bothUnownedSkullsDealFixedMagicDamage() {
        for (EntityWitherSkull skull : skulls()) {
            AtomicInteger hits = new AtomicInteger();
            skull.onCollideWithEntity(InterfaceProxy.create(Damageable.class, (proxy, method, arguments) -> {
                assertEquals("damage", method.getName());
                assertEquals(5.0F, arguments[0]);
                DamageSource source = (DamageSource) arguments[1];
                assertSame(DamageTypes.MAGIC, source.getDamageType());
                assertSame(skull, source.getDirectEntity());
                assertNull(source.getCausingEntity());
                hits.incrementAndGet();
                return false;
            }));
            assertEquals(1, hits.get());
        }
    }

    private static List<EntityWitherSkull> skulls() {
        EntityWitherSkull normal = assertInstanceOf(EntityWitherSkull.class,
                PersistenceFixtures.entity(EntityTypes.WITHER_SKULL));
        EntityWitherSkullDangerous dangerous = assertInstanceOf(EntityWitherSkullDangerous.class,
                PersistenceFixtures.entity(EntityTypes.WITHER_SKULL_DANGEROUS));
        assertSame(EntityTypes.WITHER_SKULL, normal.getType());
        assertSame(EntityTypes.WITHER_SKULL_DANGEROUS, dangerous.getType());
        return List.of(normal, dangerous);
    }
}
