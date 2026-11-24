package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.projectile.ProjectileSource;
import org.cloudburstmc.api.entity.vehicle.Boat;
import org.cloudburstmc.api.entity.vehicle.Minecart;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.CloudLivingEntity;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectileTargetTest {

    private static final Location LOCATION = Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class));

    @Test
    void airborneProjectileCanCollideWithVehicles() {
        EntityProjectile projectile = newProjectile();

        assertTrue(projectile.canCollideWith(InterfaceProxy.create(Boat.class)));
        assertTrue(projectile.canCollideWith(InterfaceProxy.create(Minecart.class)));
    }

    @Test
    void closedEntityShooterIsNotReturned() {
        EntityProjectile projectile = newProjectile();
        CloudLivingEntity shooter = new CloudLivingEntity(EntityTypes.PIG, LOCATION) {};
        projectile.setShooter(shooter);
        assertSame(shooter, projectile.getShooter());

        shooter.close();

        assertNull(projectile.getShooter());
    }

    @Test
    void nonEntitySourceIsKeptUntilTheOwnerChanges() {
        EntityProjectile projectile = newProjectile();
        ProjectileSource source = InterfaceProxy.create(ProjectileSource.class);

        projectile.setShooter(source);
        assertSame(source, projectile.getShooter());

        projectile.setOwner(null);
        assertNull(projectile.getShooter());
    }

    private static EntityProjectile newProjectile() {
        return new EntityProjectile(EntityTypes.ARROW, LOCATION) {};
    }
}
