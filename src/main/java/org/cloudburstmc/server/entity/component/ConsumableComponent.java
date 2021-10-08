package org.cloudburstmc.server.entity.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.component.Consumable;
import org.cloudburstmc.api.util.AxisAlignedBB;

import java.util.function.BiPredicate;

public class ConsumableComponent implements Consumable {
    private final Entity entity;
    private final BiPredicate<Entity, Entity> onConsume;
    private boolean infiniteDelay;
    private AxisAlignedBB boundingBox;
    private int delay;
    
    public ConsumableComponent(Entity entity, BiPredicate<Entity, Entity> onConsume, int delayTicks) {
        this.entity = entity;
        this.onConsume = onConsume;
        this.delay = delayTicks;
    }

    @Override
    public boolean hasInfiniteDelay() {
        return infiniteDelay;
    }

    @Override
    public void setInfiniteDelay(boolean infiniteDelay) {
        this.infiniteDelay = infiniteDelay;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public void setDelay(int ticks) {
        this.delay = ticks;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return boundingBox;
    }

    @Override
    public void setBoundingBox(AxisAlignedBB boundingBox) {
        this.boundingBox = boundingBox;
    }

    @Override
    public boolean consume(Entity entity) {
        return this.onConsume.test(this.entity, entity);
    }
}
