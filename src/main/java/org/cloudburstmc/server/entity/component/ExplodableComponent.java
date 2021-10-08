package org.cloudburstmc.server.entity.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.component.Explodable;

import java.util.function.ObjIntConsumer;

public class ExplodableComponent implements Explodable {
    private final Entity entity;
    private final ObjIntConsumer<Entity> explosion;
    private int fuse;
    private boolean primed;
    private int radius;

    public ExplodableComponent(Entity entity, ObjIntConsumer<Entity> explosion, int fuse, int radius) {
        this.entity = entity;
        this.explosion = explosion;
        this.fuse = fuse;
        this.radius = radius;
    }

    public int getFuse() {
        return fuse;
    }

    @Override
    public void setFuse(int fuse) {
        this.fuse = fuse;
    }

    @Override
    public boolean isPrimed() {
        return primed;
    }

    public void prime() {
        this.primed = true;
    }

    @Override
    public int getRadius() {
        return radius;
    }

    @Override
    public void setRadius(int radius) {
        this.radius = radius;
    }

    @Override
    public void explode() {
        this.explosion.accept(this.entity, this.radius);
    }
}
