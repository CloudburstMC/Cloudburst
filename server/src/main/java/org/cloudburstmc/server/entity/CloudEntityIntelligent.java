package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.ai.behaviorgroup.BehaviorGroup;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;

public abstract class CloudEntityIntelligent extends EntityCreature implements EntityIntelligent {

    private BehaviorGroup behaviorGroup;

    public CloudEntityIntelligent(EntityType<?> type, Location location) {
        super(type, location);
    }

    protected abstract BehaviorGroup createBehaviorGroup();

    @Override
    public BehaviorGroup getBehaviorGroup() {
        if (this.behaviorGroup == null) {
            this.behaviorGroup = createBehaviorGroup();
            this.behaviorGroup.setEntity(this);
        }
        return this.behaviorGroup;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean update = super.entityBaseTick(tickDiff);
        if (!this.isAlive() || this.isClosed()) return update;

        if (!this.isImmobile()) {
            var group = getBehaviorGroup();
            group.tick();
            group.processSyncedActions();
        }
        this.tickPhysics();
        return true;
    }

    private void tickPhysics() {
        Vector3f m = this.motion;
        if (!this.onGround) {
            m = m.sub(0, this.getGravity(), 0);
        }
        this.motion = m;
        this.move(this.motion);

        float ground = this.onGround ? 0.6f : 1f;
        float h = 0.91f * ground;
        this.motion = Vector3f.from(this.motion.getX() * h, this.motion.getY() * 0.98f, this.motion.getZ() * h);
    }
}
