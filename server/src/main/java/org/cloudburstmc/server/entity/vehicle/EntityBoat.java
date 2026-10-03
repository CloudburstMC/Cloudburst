package org.cloudburstmc.server.entity.vehicle;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.block.LiquidTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityComponents;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Pose;
import org.cloudburstmc.api.entity.component.Buoyancy;
import org.cloudburstmc.api.entity.vehicle.Boat;
import org.cloudburstmc.api.entity.vehicle.BoatStatus;
import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.api.event.vehicle.*;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.*;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.PredictionType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.server.config.ServerConfig;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.EntityHuman;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.entity.HumanPoses;
import org.cloudburstmc.server.entity.data.SyncedEntityData;
import org.cloudburstmc.server.entity.passive.Animal;
import org.cloudburstmc.server.entity.passive.EntityWaterAnimal;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.level.collision.BlockBoxTraversal;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.ArrayList;
import java.util.Objects;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

public class EntityBoat extends EntityVehicle implements Boat {

    private static final int CORRECTION_INTERVAL_TICKS = 5;

    private BoatType boatType = BoatType.OAK;
    private BoatStatus status = BoatStatus.IN_AIR;
    private Buoyancy buoyancy;
    private float waterLevel;
    private float landFriction;
    private float deltaRotation;
    private float lastVerticalMovement;
    private float damageTaken;
    private int underwaterTicks;
    private boolean paddlingLeft;
    private boolean paddlingRight;
    private float paddleTimeLeft;
    private float paddleTimeRight;
    private int bubbleTime;
    private boolean bubbleContact;
    private boolean bubbleDown;
    private boolean dropsBoat = true;
    private @Nullable CloudPlayer predictingDriver;
    private long lastInputTick = -1;
    private long nextCorrectionTick;
    private int inputClockTick = -1;
    private int availableInputTicks;
    private @Nullable Location lastEventLocation;

    public EntityBoat(EntityType<? extends Boat> type, Location location) {
        super(type, location);
        this.data.set(STRUCTURAL_INTEGRITY, 40);
        this.data.set(VARIANT, 0);
        this.data.set(ROW_TIME_LEFT, 0f);
        this.data.set(ROW_TIME_RIGHT, 0f);
        this.data.set(BOAT_BUBBLE_TIME, 0);
        this.data.setFlag(EntityFlag.COLLIDABLE, true);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.buoyancy = CloudEntityRegistry.get().requireComponent(this.getType(), EntityComponents.BUOYANCY);
        this.setDamage(0);
        this.lastEventLocation = this.getLocation();
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        this.setBoatType(VanillaBoats.fromVariant(tag.getInt("WoodType", 0)).type());
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putInt("WoodType", VanillaBoats.definition(this.boatType).variant());
    }

    @Override
    public BoatType getBoatType() {
        return this.boatType;
    }

    @Override
    public void setBoatType(BoatType type) {
        this.boatType = Objects.requireNonNull(type, "type");
        this.data.set(VARIANT, VanillaBoats.definition(type).variant());
        this.updatePassengers();
    }

    public ItemType getBoatItem() {
        return VanillaBoats.definition(this.boatType).boatItem();
    }

    @Override
    public BoatStatus getStatus() {
        return this.status;
    }

    @Override
    public float getWidth() {
        return 1.4f;
    }

    @Override
    public float getHeight() {
        return 0.455f;
    }

    @Override
    public float getBaseOffset() {
        return 0.375f;
    }

    @Override
    public float getGravity() {
        return 0.04f;
    }

    @Override
    public float getDrag() {
        return 0.1f;
    }

    @Override
    public int getDamage() {
        return 40 - this.data.require(STRUCTURAL_INTEGRITY);
    }

    @Override
    public void setDamage(int damage) {
        this.data.set(STRUCTURAL_INTEGRITY, 40 - damage);
    }

    @Override
    public boolean isPaddlingLeft() {
        return this.paddlingLeft;
    }

    @Override
    public boolean isPaddlingRight() {
        return this.paddlingRight;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        int elapsed = currentTick - this.lastUpdate;
        if (elapsed <= 0) {
            return true;
        }

        this.lastUpdate = currentTick;
        Location from = this.lastEventLocation == null ? this.getLocation() : this.lastEventLocation;
        for (int tick = 0; tick < elapsed && !this.closed; tick++) {
            this.tickBoat();
        }

        if (this.closed) {
            return false;
        }

        CloudEntityRegistry.get().requireComponent(this.getType(), EntityComponents.ON_TICK).execute(this, currentTick);
        if (this.closed) {
            return false;
        }

        this.updatePassengers();
        this.updateMovement();
        this.flushEntityData();
        this.server.getEventManager().fire(new VehicleUpdateEvent(this));

        Location to = this.getLocation();
        if (!from.equals(to)) {
            this.server.getEventManager().fire(new VehicleMoveEvent(this, from, to));
        }

        this.lastEventLocation = this.getLocation();
        return true;
    }

    private void tickBoat() {
        this.entityBaseTick(1);
        if (this.closed) {
            return;
        }

        BoatStatus previousStatus = this.status;
        if (this.getControllingPassenger() != null) {
            this.status = this.findStatus();
        } else {
            this.simulateUnoccupiedMovement();
        }

        if (this.closed) {
            return;
        }

        if ((previousStatus == BoatStatus.IN_AIR || previousStatus == BoatStatus.ON_LAND)
                && this.status != BoatStatus.IN_AIR && this.status != BoatStatus.ON_LAND) {
            this.sendSound(SoundEvent.SPLASH, 0);
        }

        boolean underwater = this.status == BoatStatus.UNDER_WATER || this.status == BoatStatus.UNDER_FLOWING_WATER;
        this.underwaterTicks = underwater ? this.underwaterTicks + 1 : 0;
        if (this.underwaterTicks >= 60) {
            this.ejectPassengers();
        }

        if (this.damageTaken > 0) {
            this.damageTaken = Math.max(0, this.damageTaken - 1);
            this.setDamage((int) this.damageTaken);
        }

        this.tickBubbleColumn();
        this.updatePaddles();
        this.handleNearbyEntities();
        this.resetFallDistance();
    }

    public void handleInput(CloudPlayer driver, Input input) {
        if (this.closed || !this.isControlling(driver) || !driver.isAlive() || driver.getTeleportPosition() != null
                || driver.getLevel() != this.level
                || !Float.isFinite(input.position().getX()) || !Float.isFinite(input.position().getY())
                || !Float.isFinite(input.position().getZ()) || !Float.isFinite(input.yaw())) {
            return;
        }

        if (input.tick() <= this.lastInputTick) {
            return;
        }

        int currentTick = this.server.getTick();
        int capacity = Math.max(1, this.server.getConfig().getMovement().getRewindHistorySize());
        this.availableInputTicks = this.inputClockTick < 0 ? capacity
                : (int) Math.min(capacity, this.availableInputTicks + Math.max(0L, (long) currentTick - this.inputClockTick));
        this.inputClockTick = currentTick;

        if (this.availableInputTicks == 0) {
            return;
        }

        this.availableInputTicks--;
        this.predictingDriver = driver;

        Vector3f previousPosition = this.position;
        this.lastInputTick = input.tick();

        Vector3f target = input.position().sub(0, this.getBaseOffset(), 0);
        Vector3f requested = target.sub(this.position);

        ServerConfig.Movement movementConfig = this.server.getConfig().getMovement();
        float speedLimit = movementConfig.getMaxSpeedThreshold();
        if (movementConfig.isStrictMovement()) {
            speedLimit *= 0.5f;
        }

        float distanceSquared = requested.lengthSquared();
        float maximumDelta = movementConfig.getMaxPositionDelta();
        if (!Float.isFinite(distanceSquared) || distanceSquared > maximumDelta * maximumDelta || distanceSquared > Math.max(speedLimit, this.motion.lengthSquared())) {
            this.sendPredictionCorrection(driver, input.tick());
            return;
        }

        this.deltaRotation = (float) Math.IEEEremainder(input.yaw() - this.yaw, 360);
        this.setRotation(input.yaw() % 360, 0);
        this.motion = requested;

        BoundingBox before = this.boundingBox;
        this.move(MovementType.PLAYER, requested);
        this.lastVerticalMovement = this.getY() - previousPosition.getY();
        this.status = this.findStatus();

        this.setPaddling(input.right() && !input.left() || input.forward(), input.left() && !input.right() || input.forward());
        this.notifyBlockCollisions(before, requested, this.position.sub(previousPosition));

        if (this.closed || !this.isControlling(driver)) {
            return;
        }

        this.updatePassengers();
        this.reconcilePrediction(driver, input);
        this.scheduleUpdate();
    }

    private void resetInput() {
        this.predictingDriver = null;
        this.lastInputTick = -1;
        this.nextCorrectionTick = 0;
        this.inputClockTick = -1;
        this.availableInputTicks = 0;
        this.deltaRotation = 0;
        this.setPaddling(false, false);
    }

    private void simulateUnoccupiedMovement() {
        BoatStatus previous = this.status;
        this.status = this.findStatus();
        this.floatBoat(previous);
        this.setPaddling(false, false);

        BoundingBox before = this.boundingBox;
        Vector3f velocity = this.motion;
        Vector3f positionBefore = this.position;
        this.move(velocity);
        this.lastVerticalMovement = this.getY() - positionBefore.getY();
        this.notifyBlockCollisions(before, velocity, this.position.sub(positionBefore));
    }

    private BoatStatus findStatus() {
        WaterContact contact = new WaterContact();
        BlockBoxTraversal.forEach(this.boundingBox.inflate(0, 0.001f, 0), contact);
        this.waterLevel = contact.surface;
        if (contact.flowingAbove) {
            return BoatStatus.UNDER_FLOWING_WATER;
        }

        if (contact.waterAbove) {
            return BoatStatus.UNDER_WATER;
        }

        if (contact.waterBelow) {
            return BoatStatus.IN_WATER;
        }

        this.landFriction = this.findLandFriction();
        return this.landFriction > 0 ? BoatStatus.ON_LAND : BoatStatus.IN_AIR;
    }

    private float findLandFriction() {
        BoundingBox floor = new BoundingBox(this.boundingBox.getMinX(), this.boundingBox.getMinY() - 0.001f,
                this.boundingBox.getMinZ(), this.boundingBox.getMaxX(), this.boundingBox.getMinY(), this.boundingBox.getMaxZ());
        float[] result = new float[2];

        BlockBoxTraversal.forEach(floor, (x, y, z) -> {
            Block block = this.level.getBlock(x, y, z);
            if (block.getState().getType() != BlockTypes.WATERLILY
                    && block.getCollisionShape(CollisionContext.of(this)).overlaps(floor, x, y, z)) {
                result[0] += block.getState().getFriction();
                result[1]++;
            }
        });

        return result[1] == 0 ? 0 : result[0] / result[1];
    }

    private void floatBoat(BoatStatus previous) {
        if (previous == BoatStatus.IN_AIR && this.status != BoatStatus.IN_AIR && this.status != BoatStatus.ON_LAND) {
            this.waterLevel = this.boundingBox.getMaxY();
            float surface = this.findWaterLevelAbove();
            Vector3f surfacePosition = this.position.add(0, surface - this.getHeight() + 0.101f - this.getY(), 0);
            BoundingBox surfaceBounds = this.boundingBox.move(surfacePosition.sub(this.position));

            if (!this.level.hasCollision(this, surfaceBounds)) {
                this.move(surfacePosition.sub(this.position));
                this.motion = Vector3f.from(this.motion.getX(), 0, this.motion.getZ());
                this.lastVerticalMovement = 0;
            }

            this.status = BoatStatus.IN_WATER;
            return;
        }

        float drag = 0.9f;
        float gravity = this.buoyancy.applyGravity() ? -this.getGravity() : 0;
        float buoyancy = 0;

        switch (this.status) {
            case IN_WATER -> buoyancy = (this.waterLevel - this.getY()) / this.getHeight() * this.buoyancy.baseBuoyancy();
            case UNDER_FLOWING_WATER -> gravity = this.buoyancy.applyGravity() ? -0.0007f : 0;
            case UNDER_WATER -> {
                buoyancy = 0.01f * this.buoyancy.baseBuoyancy();
                drag = 0.45f;
            }
            case ON_LAND -> drag = this.landFriction;
            case IN_AIR -> { }
        }

        float vertical = this.motion.getY() + gravity;
        if (buoyancy > 0) {
            vertical = (vertical + buoyancy * (0.04f / 0.65f)) * 0.75f;
        }

        this.motion = Vector3f.from(this.motion.getX() * drag, vertical, this.motion.getZ() * drag);
        this.deltaRotation *= drag;
    }

    private float findWaterLevelAbove() {
        int minX = (int) Math.floor(this.boundingBox.getMinX());
        int maxX = (int) Math.ceil(this.boundingBox.getMaxX());
        int minZ = (int) Math.floor(this.boundingBox.getMinZ());
        int maxZ = (int) Math.ceil(this.boundingBox.getMaxZ());
        int maxY = (int) Math.ceil(this.boundingBox.getMaxY() - this.lastVerticalMovement);

        for (int y = (int) Math.floor(this.boundingBox.getMaxY()); y < maxY; y++) {
            float height = 0;

            for (int x = minX; x < maxX; x++) {
                for (int z = minZ; z < maxZ; z++) {
                    Block block = this.level.getBlock(x, y, z);
                    LiquidState liquid = block.getLiquid();
                    if (this.buoyancy.liquids().contains(liquid.getType())) {
                        height = Math.max(height, liquid.isSameFamily(block.up().getLiquid()) ? 1 : liquid.getOwnHeight());
                    }
                }
            }

            if (height < 1) {
                return y + height;
            }
        }

        return maxY + 1;
    }

    private void setPaddling(boolean left, boolean right) {
        this.paddlingLeft = left;
        this.paddlingRight = right;

        if (!left) {
            this.paddleTimeLeft = 0;
        }

        if (!right) {
            this.paddleTimeRight = 0;
        }

        this.data.set(ROW_TIME_LEFT, this.paddleTimeLeft);
        this.data.set(ROW_TIME_RIGHT, this.paddleTimeRight);
    }

    private void updatePaddles() {
        if ((this.age & 1) == 0) {
            if (this.paddlingLeft) {
                this.data.set(ROW_TIME_LEFT, this.paddleTimeLeft += 0.04f);
            }

            if (this.paddlingRight) {
                this.data.set(ROW_TIME_RIGHT, this.paddleTimeRight += 0.04f);
            }
        }
    }

    public void onAboveBubbleColumn(boolean dragDown) {
        this.bubbleContact = true;
        this.bubbleDown = dragDown;
        if (this.bubbleTime == 0) {
            this.bubbleTime = 60;
        }
    }

    private void tickBubbleColumn() {
        if (!this.bubbleContact) {
            this.bubbleTime = 0;
        } else if (this.bubbleTime > 0 && --this.bubbleTime == 0) {
            if (this.bubbleDown) {
                this.ejectPassengers();
            }

            float vertical = this.bubbleDown ? this.motion.getY() - 0.7f : 0.6f;
            this.setMotion(Vector3f.from(this.motion.getX(), vertical, this.motion.getZ()));
        }

        this.data.set(BOAT_BUBBLE_TIME, this.bubbleTime);
        this.bubbleContact = false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return !this.sharesRootVehicle(entity) && (entity.canBeCollidedWith(this) || entity.isPushable());
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    private void handleNearbyEntities() {
        for (Entity entity : this.level.getCollidingEntities(this, this.boundingBox.inflate(0.2f, -0.01f, 0.2f))) {
            if (this.sharesRootVehicle(entity)) {
                continue;
            }

            if (this.getControllingPassenger() == null && this.canAddPassenger(entity) && entity.getVehicle() == null
                    && entity.getWidth() * entity.getScale() < this.getWidth() * this.getScale()
                    && entity instanceof EntityLiving && !(entity instanceof Player) && !(entity instanceof EntityWaterAnimal)) {
                ((CloudEntity) entity).tryMount(this);
            } else {
                this.onEntityCollision(entity);
            }
        }
    }

    private void notifyBlockCollisions(BoundingBox before, Vector3f requested, Vector3f resolved) {
        boolean collidedX = Math.abs(requested.getX() - resolved.getX()) > 0.0001f;
        boolean collidedZ = Math.abs(requested.getZ() - resolved.getZ()) > 0.0001f;
        if (!collidedX && !collidedZ) {
            return;
        }

        BlockBoxTraversal.forEach(before.expandTowards(requested).inflate(0.001f, 0, 0.001f), (x, y, z) -> {
            Block block = this.level.getBlock(x, y, z);
            VoxelShape shape = block.getCollisionShape(CollisionContext.of(this));
            if (shape.isEmpty() || this.closed) {
                return;
            }

            boolean blockedX = collidedX && Math.abs(shape.collide(Direction.Axis.X, before, requested.getX(), x, y, z) - resolved.getX()) < 0.0001f;
            boolean blockedZ = collidedZ && Math.abs(shape.collide(Direction.Axis.Z, before, requested.getZ(), x, y, z) - resolved.getZ()) < 0.0001f;
            if (blockedX || blockedZ) {
                this.server.getEventManager().fire(new VehicleBlockCollisionEvent(this, block, requested));
            }
        });
    }

    @Override
    public void onEntityCollision(Entity entity) {
        if (this.sharesRootVehicle(entity) || !entity.isPushable()
                || (entity instanceof EntityBoat ? entity.getBoundingBox().getMinY() >= this.boundingBox.getMaxY()
                : entity.getBoundingBox().getMinY() > this.boundingBox.getMinY())) {
            return;
        }

        VehicleEntityCollisionEvent event = new VehicleEntityCollisionEvent(this, entity);
        this.server.getEventManager().fire(event);
        if (event.isCancelled() || this.closed || entity.isClosed() || this.sharesRootVehicle(entity)) {
            return;
        }

        double x = entity.getX() - this.getX();
        double z = entity.getZ() - this.getZ();
        double distance = Math.max(Math.abs(x), Math.abs(z));
        if (distance < 0.01) {
            return;
        }

        distance = Math.sqrt(distance);
        double strength = Math.min(1 / distance, 1) * 0.05;
        Vector3f push = Vector3f.from(x / distance * strength, 0, z / distance * strength);
        if (this.getVehicle() == null) {
            this.setMotion(this.motion.sub(push));
        }

        if (entity.getVehicle() == null) {
            entity.setMotion(entity.getMotion().add(push));
        }
    }

    @Override
    public int getMaxPassengers() {
        return 2;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return super.canAddPassenger(passenger) && this.passengers.size() < this.getMaxPassengers()
                && this.status != BoatStatus.UNDER_WATER && this.status != BoatStatus.UNDER_FLOWING_WATER;
    }

    @Override
    public @Nullable Player getControllingPassenger() {
        return !this.passengers.isEmpty() && this.passengers.getFirst() instanceof Player player ? player : null;
    }

    @Override
    protected void onMount(Entity passenger) {
        super.onMount(passenger);
        if (this.isControlling(passenger)) {
            this.resetInput();
        }

        SyncedEntityData data = ((CloudEntity) passenger).getData();
        data.set(SEAT_LOCK_RIDER_ROTATION, true);
        data.set(SEAT_LOCK_RIDER_ROTATION_DEGREES, 90f);
        data.set(SEAT_HAS_ROTATION, true);
        data.set(SEAT_ROTATION_OFFSET_DEGREES, -90f);

        this.updatePassengers();
        this.sendSound(SoundEvent.MOUNT, -1);
    }

    @Override
    protected void onDismount(Entity passenger, @Nullable Vector3f preferredPosition) {
        boolean controlling = this.isControlling(passenger);
        super.onDismount(passenger, preferredPosition);

        SyncedEntityData data = ((CloudEntity) passenger).getData();
        data.set(SEAT_LOCK_RIDER_ROTATION, false);
        data.set(SEAT_HAS_ROTATION, false);
        data.set(SEAT_ROTATION_OFFSET_DEGREES, 0f);

        if (controlling) {
            this.resetInput();
        }

        this.updatePassengers();
        this.placeDismountedPassenger(passenger, preferredPosition);
    }

    protected float getSinglePassengerOffset() {
        return 0;
    }

    @Override
    public Vector3f getMountedOffset(Entity passenger) {
        float longitudinal = this.passengers.size() > 1 ? this.passengers.indexOf(passenger) == 0 ? 0.2f : -0.6f : this.getSinglePassengerOffset();
        if (this.passengers.size() > 1 && passenger instanceof Animal) {
            longitudinal += 0.2f;
        }

        float vertical = this.getPassengerVerticalOffset(passenger) + passenger.getBaseOffset() - this.getBaseOffset();
        return Vector3f.from(longitudinal, vertical, 0);
    }

    @Override
    public float getMountedHeightOffset() {
        return this.boatType == BoatType.BAMBOO ? 0.1f : -0.2f;
    }

    private float getPassengerVerticalOffset(Entity passenger) {
        float offset = passenger instanceof Player ? -0.4f : passenger instanceof Animal ? 0.14f : passenger.getPassengerHeightOffset();
        return this.getBaseOffset() + this.getMountedHeightOffset() + offset;
    }

    @Override
    public Vector3f getPassengerAttachmentPoint(Entity passenger) {
        Vector3f seat = this.getMountedOffset(passenger);
        float longitudinal = seat.getX();
        double angle = Math.toRadians(this.yaw);
        return Vector3f.from(-Math.sin(angle) * longitudinal,
                this.getPassengerVerticalOffset(passenger), Math.cos(angle) * longitudinal);
    }

    @Override
    protected void updatePassengerPosition(Entity passenger) {
        passenger.setSeatPosition(this.getMountedOffset(passenger));
        super.updatePassengerPosition(passenger);
    }

    private void placeDismountedPassenger(Entity passenger, @Nullable Vector3f preferredPosition) {
        if (passenger.isClosed()) {
            return;
        }

        if (preferredPosition != null && this.isReachableDismountPosition(passenger, preferredPosition)
                && (this.canDismountAt(passenger, preferredPosition, false)
                || passenger instanceof EntityHuman && this.canDismountAt(passenger, preferredPosition, true))) {
            passenger.setPosition(preferredPosition);
            ((CloudEntity) passenger).sendAuthoritativeDisplacement();
            return;
        }

        double angle = Math.toRadians(passenger.getYaw());
        double directionX = -Math.sin(angle);
        double directionZ = Math.cos(angle);
        double distance = (this.getWidth() * Math.sqrt(2) + passenger.getWidth() + 0.00001) * 0.5;
        double scale = Math.max(Math.abs(directionX), Math.abs(directionZ));

        Vector3f beside = this.position.add(directionX * distance / scale, 0, directionZ * distance / scale);
        int y = (int) Math.floor(this.boundingBox.getMaxY());
        Block targetBlock = this.level.getLoadedBlock(Vector3i.from((int) Math.floor(beside.getX()), y, (int) Math.floor(beside.getZ())));

        Vector3f target = null;
        Block belowTarget = targetBlock == null ? null : this.level.getLoadedBlock(targetBlock.getPosition().add(0, -1, 0));

        if (targetBlock != null && belowTarget != null && !belowTarget.getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
            ArrayList<Vector3f> candidates = new ArrayList<Vector3f>(2);
            for (int offset = 0; offset >= -1; offset--) {
                Block candidate = offset == 0 ? targetBlock : belowTarget;
                float floor = this.getDismountFloor(candidate, passenger);
                if (Float.isFinite(floor) && floor < 1) {
                    candidates.add(Vector3f.from(beside.getX(), candidate.getY() + floor, beside.getZ()));
                }
            }

            int poses = passenger instanceof EntityHuman ? 2 : 1;
            for (int pose = 0; pose < poses && target == null; pose++) {
                for (Vector3f location : candidates) {
                    if (this.canDismountAt(passenger, location, pose == 1)) {
                        target = location;
                        break;
                    }
                }
            }
        }

        passenger.setPosition(target == null ? Vector3f.from(this.getX(), this.boundingBox.getMaxY(), this.getZ()) : target);
        ((CloudEntity) passenger).sendAuthoritativeDisplacement();
    }

    private boolean isReachableDismountPosition(Entity passenger, Vector3f position) {
        if (!Float.isFinite(position.getX()) || !Float.isFinite(position.getY()) || !Float.isFinite(position.getZ())) {
            return false;
        }

        float reach = this.getWidth() + passenger.getWidth();
        float dx = position.getX() - this.getX();
        float dz = position.getZ() - this.getZ();
        return dx * dx + dz * dz <= reach * reach && Math.abs(position.getY() - this.getY()) <= this.getHeight() + passenger.getHeight();
    }

    private boolean canDismountAt(Entity passenger, Vector3f position, boolean crouching) {
        BoundingBox bounds = passenger instanceof EntityHuman
                ? HumanPoses.dimensions(crouching ? Pose.CROUCHING : Pose.STANDING).boundingBox(position, passenger.getScale())
                : passenger.getBoundingBox().move(position.sub(passenger.getPosition()));
        if (bounds.getMinY() < this.level.getMinHeight() || bounds.getMaxY() > this.level.getMaxHeight()) {
            return false;
        }

        boolean[] loaded = {true};
        BlockBoxTraversal.forEach(bounds.deflate(1.0E-5f, 1.0E-5f, 1.0E-5f), (x, y, z) -> {
            if (this.level.getLoadedBlock(Vector3i.from(x, y, z)) == null) {
                loaded[0] = false;
            }
        });

        return loaded[0] && !this.level.hasCollision(passenger, bounds);
    }

    private float getDismountFloor(Block block, Entity passenger) {
        CollisionContext context = CollisionContext.of(passenger);
        VoxelShape shape = block.getCollisionShape(context);
        if (!shape.isEmpty()) {
            return shape.bounds().getMaxY();
        }

        Block belowBlock = this.level.getLoadedBlock(block.getPosition().add(0, -1, 0));
        if (belowBlock == null) {
            return Float.NEGATIVE_INFINITY;
        }

        VoxelShape below = belowBlock.getCollisionShape(context);
        if (!below.isEmpty() && below.bounds().getMaxY() >= 1) {
            return below.bounds().getMaxY() - 1;
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public String getInteractButtonText() {
        return "action.interact.ride.boat";
    }

    @Override
    public boolean canDoInteraction() {
        return this.passengers.size() < this.getMaxPassengers();
    }

    @Override
    public boolean onInteract(Player player, ItemStack item, Vector3f clickedPos) {
        return !player.isSpectator() && ((CloudEntity) player).tryMount(this);
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent source) {
        if (this.isDamageImmune(source.getDamageSource())) {
            return false;
        }

        this.server.getEventManager().fire(source);
        if (source.isCancelled() || source.getDamage() <= 0 || this.closed || !this.isAlive()) {
            return false;
        }

        VehicleDamageEvent event = new VehicleDamageEvent(this, source.getDamageSource(), source.getDamage());
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            source.setCancelled(true);
            return false;
        }

        if (event.getDamage() <= 0 || this.closed || !this.isAlive()) {
            return false;
        }

        source.setDamage(event.getDamage());
        float damage = (float) Math.min(Float.MAX_VALUE, this.damageTaken + (double) source.getFinalDamage() * 10);
        boolean creative = source.getDamageSource().getCausingEntity() instanceof CloudPlayer player && player.isCreative();
        boolean destroys = creative || damage > 40;
        if (destroys) {
            VehicleDestroyEvent destroy = new VehicleDestroyEvent(this, source.getDamageSource());
            this.server.getEventManager().fire(destroy);
            if (this.closed || !this.isAlive()) {
                return false;
            }

            if (destroy.isCancelled()) {
                damage = 40;
                destroys = false;
            }
        }

        this.setLastDamageCause(source);
        this.damageTaken = damage;
        this.setDamage((int) damage);
        this.performHurtAnimation();
        if (destroys) {
            this.dropsBoat = !creative;
            this.kill();
        }

        return true;
    }

    @Override
    public void kill() {
        if (this.closed || !this.isAlive()) {
            return;
        }

        if (this.level.getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            if (this.dropsBoat) {
                ItemStack drop = ItemStack.from(this.getBoatItem());
                if (this.hasNameTag()) {
                    drop = drop.toBuilder().setData(ItemDataComponents.CUSTOM_NAME, this.getNameTag()).build();
                }
                this.level.dropItem(this.position, drop);
            }
        }

        this.dropContents();
        this.health = 0;
        this.close();
    }

    protected void dropContents() {
    }

    @Override
    protected void addAdditionalSpawnData(AddEntityPacket packet) {
        super.addAdditionalSpawnData(packet);
        packet.setRotation(Vector2f.from(0, this.yaw + 90));
        packet.setHeadRotation(this.yaw + 90);
        packet.setBodyRotation(this.yaw + 90);
    }

    private void sendSound(SoundEvent sound, int extraData) {
        LevelSoundEventPacket packet = new LevelSoundEventPacket();
        packet.setSound(sound);
        Vector3f soundPosition = this.position.add(0, this.getBaseOffset(), 0);
        packet.setPosition(soundPosition);
        packet.setExtraData(extraData);
        packet.setIdentifier(this.getType().getId().toString());
        packet.setEntityUniqueId(this.getUniqueId());
        this.level.addChunkPacket(soundPosition, packet);
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        MoveEntityAbsolutePacket packet = new MoveEntityAbsolutePacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setPosition(Vector3f.from(x, y, z));
        packet.setRotation(Vector3f.from(0, yaw + 90, headYaw + 90));
        packet.setOnGround(this.onGround);
        for (CloudPlayer viewer : this.getViewers()) {
            if (viewer != this.predictingDriver) {
                viewer.sendPacket(packet);
            }
        }
    }

    @Override
    public void addMotion(Vector3f motion) {
        SetEntityMotionPacket packet = new SetEntityMotionPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setMotion(motion);
        for (CloudPlayer viewer : this.getViewers()) {
            if (viewer != this.predictingDriver) {
                viewer.sendPacket(packet);
            }
        }
    }

    @Override
    protected void sendDataToViewers(EntityDataMap metadata) {
        SetEntityDataPacket observers = new SetEntityDataPacket();
        observers.setRuntimeEntityId(this.getRuntimeId());
        observers.getMetadata().putAll(metadata);
        SetEntityDataPacket driver = new SetEntityDataPacket();
        driver.setRuntimeEntityId(this.getRuntimeId());
        driver.setTick(this.lastInputTick < 0 ? 0 : this.lastInputTick);
        driver.getMetadata().putAll(metadata);
        // The driver animates paddles from input, observers need the replicated phase.
        driver.getMetadata().remove(ROW_TIME_LEFT);
        driver.getMetadata().remove(ROW_TIME_RIGHT);
        for (CloudPlayer viewer : this.getViewers()) {
            if (viewer != this.predictingDriver) {
                viewer.sendPacket(observers);
            } else if (!driver.getMetadata().isEmpty()) {
                viewer.sendPacket(driver);
            }
        }
    }

    @Override
    protected MoveEntityAbsolutePacket createAuthoritativeDisplacementPacket() {
        MoveEntityAbsolutePacket packet = super.createAuthoritativeDisplacementPacket();
        packet.setRotation(Vector3f.from(0, this.yaw + 90, this.yaw + 90));
        return packet;
    }

    @Override
    protected void onMotionChanged() {
        super.onMotionChanged();
        if (this.predictingDriver != null && this.isControlling(this.predictingDriver)) {
            this.sendPredictionCorrection(this.predictingDriver, this.lastInputTick);
        }
    }

    private void reconcilePrediction(CloudPlayer driver, Input input) {
        if (input.tick() < this.nextCorrectionTick) {
            return;
        }

        Vector3f position = this.position.add(0, this.getBaseOffset(), 0);
        float threshold = this.server.getConfig().getMovement().getPositionAcceptanceThreshold();
        if (position.distanceSquared(input.position()) <= threshold * threshold) {
            return;
        }
        this.sendPredictionCorrection(driver, input.tick());
        this.nextCorrectionTick = input.tick() + CORRECTION_INTERVAL_TICKS;
    }

    private void sendPredictionCorrection(CloudPlayer driver, long tick) {
        CorrectPlayerMovePredictionPacket correction = new CorrectPlayerMovePredictionPacket();
        correction.setPredictionType(PredictionType.VEHICLE);
        correction.setPosition(this.position.add(0, this.getBaseOffset(), 0));
        correction.setDelta(this.motion);
        correction.setVehicleRotation(Vector2f.from(0, this.yaw + 90));
        correction.setVehicleAngularVelocity(this.deltaRotation);
        correction.setOnGround(this.onGround);
        correction.setTick(tick);
        driver.sendPacket(correction);
    }

    public record Input(long tick, Vector3f position, float yaw, boolean left, boolean right,
                        boolean forward) {
    }

    private final class WaterContact implements BlockBoxTraversal.BlockPositionConsumer {
        private float surface = Float.NEGATIVE_INFINITY;
        private boolean waterBelow;
        private boolean waterAbove;
        private boolean flowingAbove;

        @Override
        public void accept(int x, int y, int z) {
            Block block = EntityBoat.this.level.getBlock(x, y, z);
            LiquidState liquid = block.getLiquid();
            if (!EntityBoat.this.buoyancy.liquids().contains(liquid.getType())) {
                return;
            }

            float top = y + (liquid.isSameFamily(block.up().getLiquid()) ? 1 : liquid.getOwnHeight());
            this.surface = Math.max(this.surface, top);
            if (y <= EntityBoat.this.boundingBox.getMinY() && top > EntityBoat.this.boundingBox.getMinY()) {
                this.waterBelow = true;
            }

            if (y <= EntityBoat.this.boundingBox.getMaxY() && top > EntityBoat.this.boundingBox.getMaxY() + 0.001f) {
                this.waterAbove = true;
                this.flowingAbove |= !liquid.isSource();
            }
        }
    }
}
