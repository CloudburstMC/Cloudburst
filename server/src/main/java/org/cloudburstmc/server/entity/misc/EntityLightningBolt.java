package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.event.block.BlockIgniteCause;
import org.cloudburstmc.api.event.block.BlockIgniteEvent;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.component.FireBlockHandlers;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.AIR;

public class EntityLightningBolt extends CloudEntity implements LightningBolt {

    private int flashTicks;
    private int flashesRemaining;
    private boolean visualOnly;

    public EntityLightningBolt(EntityType<LightningBolt> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setHealth(4);
        this.setDefaultMaxHealth(4);

        this.flashTicks = 2;
        this.flashesRemaining = ThreadLocalRandom.current().nextInt(3) + 1;
    }

    @Override
    public boolean isVisualOnly() {
        return this.visualOnly;
    }

    @Override
    public void setVisualOnly(boolean visualOnly) {
        this.visualOnly = visualOnly;
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        return false;
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.isClosed()) {
            return false;
        }

        this.entityBaseTick(elapsedTicks);
        if (this.isClosed()) {
            return false;
        }

        if (this.flashTicks == 2) {
            this.level.addLevelSoundEvent(this.getPosition(), SoundEvent.THUNDER, -1, EntityTypes.LIGHTNING_BOLT);
            this.level.addLevelSoundEvent(this.getPosition(), SoundEvent.EXPLODE, -1, EntityTypes.LIGHTNING_BOLT);
            if (this.server.getDifficulty().getValue() >= 2) {
                this.igniteStrikeBlock();
            }
        }

        this.flashTicks--;

        if (this.flashTicks < 0) {
            if (this.flashesRemaining == 0) {
                this.remove(EntityRemoveCause.DESPAWN);
                return false;
            } else if (this.flashTicks < -ThreadLocalRandom.current().nextInt(10)) {
                this.flashesRemaining--;
                this.flashTicks = 1;

                this.igniteStrikeBlock();
            }
        }

        if (this.isClosed()) {
            return false;
        }

        if (this.flashTicks >= 0 && !this.visualOnly) {
            BoundingBox bounds = this.getBoundingBox();
            BoundingBox strikeBounds = bounds.inflate(3, 3, 3).setMaxY(bounds.getMaxY() + 9);

            for (Entity entity : this.level.getNearbyEntities(this, strikeBounds)) {
                if (entity instanceof CloudEntity target && target.isAlive() && !target.isClosed()) {
                    target.onStruckByLightning(this);
                }
            }
        }

        return true;
    }

    private void igniteStrikeBlock() {
        if (this.visualOnly || !this.level.getGameRules().get(GameRules.DO_FIRE_TICK)) {
            return;
        }

        Block block = this.level.getLoadedBlock(this.getPosition().toInt());
        if (block == null || block.getState().getType() != AIR) {
            return;
        }

        if (!this.canPlaceFire(block, FireBlockHandlers.placementState(block))) {
            return;
        }

        BlockIgniteEvent event = new BlockIgniteEvent(block, BlockIgniteCause.LIGHTNING, this, null);
        this.server.getEventManager().fire(event);
        if (event.isCancelled() || this.isClosed() || this.visualOnly || this.level != block.getLevel()) {
            return;
        }

        Block current = this.level.getLoadedBlock(block.getPosition());
        if (current != null && current.getState().getType() == AIR) {
            BlockState fire = FireBlockHandlers.placementState(current);
            if (this.canPlaceFire(current, fire)) {
                this.level.setBlockState(current.getPosition(), fire, false, true);
            }
        }
    }

    private boolean canPlaceFire(Block block, BlockState fire) {
        return this.server.getBlockRegistry()
                .requireComponent(fire.getType(), BlockComponents.CAN_SURVIVE).execute(block);
    }
}
