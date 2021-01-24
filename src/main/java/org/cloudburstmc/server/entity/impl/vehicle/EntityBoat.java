package org.cloudburstmc.server.entity.impl.vehicle;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.packet.AnimatePacket;
import lombok.val;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.behavior.BlockBehaviorWater;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.EntityLiving;
import org.cloudburstmc.server.entity.impl.passive.EntityWaterAnimal;
import org.cloudburstmc.server.entity.vehicle.Boat;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.event.vehicle.VehicleMoveEvent;
import org.cloudburstmc.server.event.vehicle.VehicleUpdateEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.world.gamerule.GameRules;
import org.cloudburstmc.server.world.particle.SmokeParticle;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.player.Player;

import java.util.ArrayList;

import static com.nukkitx.protocol.bedrock.data.entity.EntityData.*;

/**
 * Created by yescallop on 2016/2/13.
 */
public class EntityBoat extends EntityVehicle implements Boat {

    public static final Vector3f RIDER_PLAYER_OFFSET = Vector3f.from(0, 1.02001f, 0);
    public static final Vector3f RIDER_OFFSET = Vector3f.from(0, -0.2, 0);
    public static final Vector3f PASSENGER_OFFSET = Vector3f.from(-0.6, 0, 0);
    public static final Vector3f RIDER_PASSENGER_OFFSET = Vector3f.from(0.2, 0, 0);
    public static final int RIDER_INDEX = 0;
    public static final int PASSENGER_INDEX = 1;
    public static final double SINKING_DEPTH = 0.07;
    public static final double SINKING_SPEED = 0.0005;
    public static final double SINKING_MAX_SPEED = 0.005;
    private static final String INTERACT_TAG = "action.interact.ride.boat";
    protected boolean sinking = true;

    public EntityBoat(EntityType<Boat> type, Location location) {
        super(type, location);

        this.setMaxHealth(40);
        this.setHealth(40);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
    }

    public int getWoodType() {
        return this.data.getInt(VARIANT);
    }

    @Override
    public void setWoodType(int woodType) {
        this.data.setInt(VARIANT, woodType);
    }

    @Override
    public float getHeight() {
        return 0.455f;
    }

    @Override
    public float getWidth() {
        return 1.4f;
    }

    @Override
    public float getDrag() {
        return 0.1f;
    }

    @Override
    public float getGravity() {
        return 0.04F;
    }

    @Override
    public float getBaseOffset() {
        return 0.375F;
    }

    @Override
    public String getInteractButtonText() {
        return INTERACT_TAG;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (invulnerable) {
            return false;
        } else {
            source.setDamage(source.getDamage() * 2);

            boolean attack = super.attack(source);

            if (isAlive()) {
                performHurtAnimation();
            }

            return attack;
        }
    }

    @Override
    public void close() {
        super.close();

        for (Entity passenger : this.passengers) {
            passenger.dismount(this);
        }

        SmokeParticle particle = new SmokeParticle(this.getPosition());
        this.getWorld().addParticle(particle);
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

        boolean hasUpdate = this.entityBaseTick(tickDiff);

        if (this.isAlive()) {
            super.onUpdate(currentTick);

            double waterDiff = getWaterLevel();
            if (!hasControllingPassenger()) {

                if (waterDiff > SINKING_DEPTH && !sinking) {
                    sinking = true;
                } else if (waterDiff < -SINKING_DEPTH && sinking) {
                    sinking = false;
                }

                if (waterDiff < -SINKING_DEPTH) {
                    this.motion = Vector3f.from(this.motion.getX(), Math.min(0.05, this.motion.getY() + 0.005), this.motion.getZ());
                } else if (waterDiff < 0 || !sinking) {
                    this.motion = Vector3f.from(this.motion.getX(),
                            this.motion.getY() > SINKING_MAX_SPEED ?
                                    Math.max(this.motion.getY() - 0.02, SINKING_MAX_SPEED) :
                                    this.motion.getY() + SINKING_SPEED,
                            this.motion.getZ());
//                    this.motionY = this.motionY + SINKING_SPEED > SINKING_MAX_SPEED ? this.motionY - SINKING_SPEED : this.motionY + SINKING_SPEED;
                }
            }

            if (this.checkObstruction(this.position)) {
                hasUpdate = true;
            }

            this.move(this.motion);

            double friction = 1 - this.getDrag();

            if (this.onGround && (Math.abs(this.motion.getX()) > 0.00001 || Math.abs(this.motion.getZ()) > 0.00001)) {
                friction *= this.getWorld().getBlockAt(this.getPosition().down().toInt()).getBehavior().getFrictionFactor();
            }

            this.motion = motion.mul(friction, 1, friction);

            if (!hasControllingPassenger() && (waterDiff > SINKING_DEPTH || sinking)) {
                this.motion = Vector3f.from(motion.getX(),
                        waterDiff > 0.5 ?
                                this.motion.getY() - this.getGravity() :
                                (this.motion.getY() - SINKING_SPEED < -SINKING_MAX_SPEED ? this.motion.getY() : this.motion.getY() - SINKING_SPEED),
                        motion.getZ());
            }

            Location from = Location.from(this.lastPosition, lastYaw, lastPitch, this.getWorld());
            Location to = Location.from(this.position, this.yaw, this.pitch, this.getWorld());

            this.getServer().getEventManager().fire(new VehicleUpdateEvent(this));

            if (!from.equals(to)) {
                this.getServer().getEventManager().fire(new VehicleMoveEvent(this, from, to));
            }

            //TODO: lily pad collision
            this.updateMovement();

            if (this.passengers.size() < 2) {
                for (Entity entity : this.getWorld().getCollidingEntities(this.boundingBox.grow(0.2f, 0, 0.2f), this)) {
                    if (entity.getVehicle() != null || !(entity instanceof EntityLiving) || entity instanceof Player || entity instanceof EntityWaterAnimal || isPassenger(entity)) {
                        continue;
                    }

                    entity.mount(this);

                    if (this.passengers.size() >= 2) {
                        break;
                    }
                }
            }
        }

        return hasUpdate || !this.onGround || motion.abs().length() > 0.00001;
    }

    public void updatePassengers() {
        updatePassengers(false);
    }

    public void updatePassengers(boolean sendLinks) {
        if (this.passengers.isEmpty()) {
            return;
        }

        for (Entity passenger : new ArrayList<>(passengers)) {
            if (!passenger.isAlive()) {
                this.dismount(passenger);
            }
        }

        Entity ent;

        if (passengers.size() == 1) {
            (ent = this.passengers.get(0)).setSeatPosition(getMountedOffset(ent));
            super.updatePassengerPosition(ent);

            if (sendLinks) {
                broadcastLinkPacket(ent, EntityLinkData.Type.RIDER);
            }
        } else if (passengers.size() == 2) {
            if (!((ent = passengers.get(0)) instanceof Player)) { //swap
                Entity passenger2 = passengers.get(1);

                if (passenger2 instanceof Player) {
                    this.passengers.set(0, passenger2);
                    this.passengers.set(1, ent);

                    ent = passenger2;
                }
            }

            ent.setSeatPosition(getMountedOffset(ent).add(RIDER_PASSENGER_OFFSET));
            super.updatePassengerPosition(ent);
            if (sendLinks) {
                broadcastLinkPacket(ent, EntityLinkData.Type.RIDER);
            }

            (ent = this.passengers.get(1)).setSeatPosition(getMountedOffset(ent).add(PASSENGER_OFFSET));

            super.updatePassengerPosition(ent);

            if (sendLinks) {
                broadcastLinkPacket(this, EntityLinkData.Type.PASSENGER);
            }

            float yawDiff = ent.getUniqueId() % 2 == 0 ? 90 : 270;
            ent.setRotation(this.yaw + yawDiff, ent.getPitch());
            //ent.updateMovement();
        } else {
            for (Entity passenger : passengers) {
                super.updatePassengerPosition(passenger);
            }
        }
    }

    public double getWaterLevel() {
        double maxY = this.boundingBox.getMinY() + getBaseOffset();
        AxisAlignedBB.BBConsumer<Double> consumer = new AxisAlignedBB.BBConsumer<Double>() {

            private double diffY = Double.MAX_VALUE;

            @Override
            public void accept(int x, int y, int z) {
                val block = getWorld().getBlock(x, y, z);
                BlockState state = block.getState();

                if (state.getType() == BlockIds.WATER || state.getType() == BlockIds.FLOWING_WATER) {
                    double level = ((BlockBehaviorWater) state.getBehavior()).getMaxY(block);

                    diffY = Math.min(maxY - level, diffY);
                }
            }

            @Override
            public Double get() {
                return diffY;
            }
        };

        this.boundingBox.forEach(consumer);

        return consumer.get();
    }

    @Override
    public boolean mount(Entity entity) {
        boolean player = this.passengers.size() >= 1 && this.passengers.get(0) instanceof Player;
        EntityLinkData.Type mode = EntityLinkData.Type.PASSENGER;

        if (!player && (entity instanceof Player || this.passengers.size() == 0)) {
            mode = EntityLinkData.Type.RIDER;
        }

        return super.mount(entity, mode);
    }

    @Override
    public void onMount(Entity passenger) {
        super.onMount(passenger);

        passenger.getData().setByte(RIDER_ROTATION_LOCKED, 1);
        passenger.getData().setFloat(RIDER_MAX_ROTATION, 90);
        passenger.getData().setFloat(RIDER_MIN_ROTATION, -90);

        //            if(entity instanceof Player && mode == SetEntityLinkPacket.TYPE_RIDE){ //TODO: controlling?
//                entity.setDataProperty(new ByteEntityData(DATA_FLAG_WASD_CONTROLLED))
//            }
    }

    @Override
    public void onDismount(Entity passenger) {
        super.onDismount(passenger);
    }

    @Override
    protected void updatePassengerPosition(Entity passenger) {
        updatePassengers();
    }

    @Override
    public boolean isControlling(Entity entity) {
        return entity instanceof Player && this.passengers.indexOf(entity) == 0;
    }

    @Override
    public boolean onInteract(Player player, Item item, Vector3f clickedPos) {
        if (this.passengers.size() >= 2) {
            return false;
        }

        player.mount(this);
        return super.onInteract(player, item, clickedPos);
    }

    @Override
    public Vector3f getMountedOffset(Entity entity) {
        return entity instanceof Player ? RIDER_PLAYER_OFFSET : RIDER_OFFSET;
    }

    public void onPaddle(AnimatePacket.Action animation, float value) {
        EntityData data = animation == AnimatePacket.Action.ROW_RIGHT ? ROW_TIME_RIGHT : ROW_TIME_LEFT;

        if (this.data.getFloat(data) != value) {
            this.data.setFloat(data, value);
        }
    }

    @Override
    public void onEntityCollision(Entity entity) {
        if (this.vehicle == null && entity.getVehicle() != this && !entity.getPassengers().contains(this)) {
            if (!entity.getBoundingBox().intersectsWith(this.boundingBox.grow(0.2f, -0.1f, 0.2f))) {
                return;
            }

            double diffX = entity.getX() - this.getX();
            double diffZ = entity.getZ() - this.getZ();

            double direction = NukkitMath.getDirection(diffX, diffZ);

            if (direction >= 0.009999999776482582D) {
                direction = Math.sqrt(direction);
                diffX /= direction;
                diffZ /= direction;

                double d3 = Math.min(1 / direction, 1);

                diffX *= d3;
                diffZ *= d3;
                diffX *= 0.05000000074505806;
                diffZ *= 0.05000000074505806;
                diffX *= 1 + entityCollisionReduction;

                if (this.vehicle == null) {
                    this.motion = motion.sub(diffX, 0, diffZ);
                }
            }
        }
    }

    @Override
    public boolean canPassThrough() {
        return false;
    }

    @Override
    public void kill() {
        super.kill();

        if (this.getWorld().getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            this.getWorld().dropItem(this.getPosition(), Item.get(ItemIds.BOAT));
        }
    }

    @Override
    public boolean mount(Entity entity, EntityLinkData.Type mode) {
        return this.mount(entity);
    }
}
