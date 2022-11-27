package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.entity.BaseEntity;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.AIR;
import static org.cloudburstmc.api.block.BlockTypes.TALL_GRASS;

/**
 * Created by boybook on 2016/2/27.
 */
public class EntityLightningBolt extends BaseEntity implements LightningBolt {

    public int state;
    public int liveTime;
    protected boolean isEffect = true;

    public EntityLightningBolt(EntityType<LightningBolt> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setHealth(4);
        this.setMaxHealth(4);

        this.state = 2;
        this.liveTime = ThreadLocalRandom.current().nextInt(3) + 1;

        if (isEffect && this.level.getGameRules().get(GameRules.DO_FIRE_TICK) && (this.server.getDifficulty().ordinal() >= 2)) {
            Block block = this.getLevel().getBlock(this.getPosition().toInt());
            var state = block.getState();

            if (state.getType() == AIR || state.getType() == TALL_GRASS) {

//                TODO
//                if (BlockBehaviorFire.isBlockTopFacingSurfaceSolid(block.getSideState(Direction.DOWN, 1)) || BlockBehaviorFire.canNeighborBurn(block)) {
//
//                    BlockIgniteEvent e = new BlockIgniteEvent(block, null, this, BlockIgniteEvent.BlockIgniteCause.LIGHTNING);
//                    getServer().getEventManager().fire(e);
//
//                    if (!e.isCancelled()) {
//                        var fire = CloudBlockRegistry.get().getBlock(BlockTypes.FIRE);
//                        block.set(fire);
//
//                        level.scheduleUpdate(block.getPosition(), fire.getBehavior().tickRate() + ThreadLocalRandom.current().nextInt(10));
//                    }
//                }
            }
        }
    }

    @Override
    public boolean isEffect() {
        return this.isEffect;
    }

    @Override
    public void setEffect(boolean effect) {
        this.isEffect = effect;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        //false?
        source.setDamage(0);
        return super.attack(source);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0 && !this.justCreated) {
            return true;
        }

        this.lastUpdate = currentTick;

        this.entityBaseTick(tickDiff);

        if (this.state == 2) {
            ((CloudLevel) this.level).addLevelSoundEvent(this.getPosition(), SoundEvent.THUNDER, -1, EntityTypes.LIGHTNING_BOLT);
            ((CloudLevel) this.level).addLevelSoundEvent(this.getPosition(), SoundEvent.EXPLODE, -1, EntityTypes.LIGHTNING_BOLT);
        }

        this.state--;

        if (this.state < 0) {
            if (this.liveTime == 0) {
                this.close();
                return false;
            } else if (this.state < -ThreadLocalRandom.current().nextInt(10)) {
                this.liveTime--;
                this.state = 1;

                if (this.isEffect && this.level.getGameRules().get(GameRules.DO_FIRE_TICK)) {
                    Block block = this.getLevel().getBlock(this.getPosition().toInt());
                    var state = block.getState();

//                    TODO
//                    if (state.getType() == AIR || state.getType() == TALL_GRASS) {
//                        BlockIgniteEvent e = new BlockIgniteEvent(block, null, this, BlockIgniteEvent.BlockIgniteCause.LIGHTNING);
//                        getServer().getEventManager().fire(e);
//
//                        if (!e.isCancelled()) {
//                            BlockState fire = CloudBlockRegistry.get().getBlock(BlockTypes.FIRE);
//                            block.set(fire);
//
//                            this.getLevel().scheduleUpdate(block.getPosition(), fire.getBehavior().tickRate());
//                        }
//                    }
                }
            }
        }

        if (this.state >= 0) {
            if (this.isEffect) {
                AxisAlignedBB bb = getBoundingBox().grow(3, 3, 3);
                bb.setMaxX(bb.getMaxX() + 6);

                for (Entity entity : this.level.getCollidingEntities(bb, this)) {
                    entity.onStruckByLightning(this);
                }
            }
        }

        return true;
    }


}
