package org.cloudburstmc.server.entity.vehicle;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.vehicle.VehicleMoveEvent;
import org.cloudburstmc.api.event.vehicle.VehicleUpdateEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.api.util.data.RailDirection;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.CloudBlockDefinition;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.block.util.RailConnector;
import org.cloudburstmc.server.entity.EntityHuman;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.math.MathHelper;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

@Log4j2
public abstract class EntityAbstractMinecart extends EntityVehicle {
    private String entityName;
    private boolean slowWhenEmpty = true;
    private Vector3f derailed = Vector3f.from(0.5, 0.5, 0.5);
    private Vector3f flying = Vector3f.from(0.95, 0.95, 0.95);
    private float maxSpeed = 0.4f;
    private boolean wasOnRail = false;
    private boolean flipped = false;
    private float inputMotionY = 0;

    public EntityAbstractMinecart(EntityType<?> type, Location location) {
        super(type, location);

        setMaxHealth(40);
        setHealth(40);
    }

    public abstract boolean isRideable();

    public abstract MinecartType getMinecartType();

    @Override
    public float getHeight() {
        return 0.7F;
    }

    @Override
    public Vector3f getPassengerAttachmentPoint(Entity passenger) {
        return Vector3f.from(0f, getBaseOffset() + getMountedOffset(passenger).getY(), 0f);
    }

    @Override
    public float getWidth() {
        return 0.98F;
    }

    @Override
    public float getDrag() {
        return 0.1F;
    }

    @Override
    public String getName() {
        return entityName == null ? "Minecart" : entityName;
    }

    public void setName(String name) {
        entityName = name;
    }

    @Override
    public float getBaseOffset() {
        return isOnRail() ? 0f : 0.35f;
    }

    @Override
    public Vector3f getMountedOffset(Entity passenger) {
        return Vector3f.from(0f, 1.02001f, 0f);
    }

    @Override
    public boolean hasNameTag() {
        return entityName != null;
    }

    @Override
    public boolean canDoInteraction() {
        return isRideable() && passengers.isEmpty();
    }

    @Override
    public void initEntity() {
        super.initEntity();

        setRollingAmplitude(0);
        setRollingDirection(1);

        this.data.set(CUSTOM_DISPLAY, (byte) 0);
        this.data.set(DISPLAY_OFFSET, 6);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        if (tag.getBoolean("CustomDisplayTile")) {
            this.setDisplay(true);

            int id;
            int meta;
            CloudBlockRegistry registry = CloudBlockRegistry.REGISTRY;
            if (tag.containsKey("DisplayTile") && tag.containsKey("DisplayData")) {
                id = tag.getInt("DisplayTile");
                meta = tag.getInt("DisplayData");
            } else {
                NbtMap plantTag = tag.getCompound("DisplayBlock");
                id = registry.getLegacyId(plantTag.getString("name"));
                meta = plantTag.getShort("val");
            }
            this.setDisplayBlock(registry.getBlock(id, meta));

//            this.setDisplayBlockOffset(tag.getInt("DisplayOffset"));
        }
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        if (this.hasDisplay()) {
            tag.putBoolean("CustomDisplayTile", true);

            BlockState blockState = this.getDisplayBlock();
            tag.putCompound("DisplayBlock", NbtMap.builder()
                    .putString("name", blockState.getType().toString())
                    .putShort("val", (short) BlockStateMetaMappings.getMetaFromState(blockState)) //TODO: check
                    .build());

//            tag.putInt("DisplayOffset", this.getDisplayOffset());
        }
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        if (!this.isAlive()) {
            ++this.deadTicks;
            if (this.deadTicks >= 10) {
                this.despawnFromAll();
                this.close();
            }
            return this.deadTicks < 10;
        }

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0) {
            return false;
        }

        this.lastUpdate = currentTick;

        if (isAlive()) {
            super.onUpdate(currentTick);

            // Regenerate health slowly after taking a hit.
            if (getHealth() < 20) {
                setHealth(getHealth() + 1);
            }

            this.lastPosition = this.position;
            this.motion = this.motion.sub(0, 0.04, 0);
            int dx = this.position.getFloorX();
            int dy = this.position.getFloorY();
            int dz = this.position.getFloorZ();

            // Check one block below in case the rail is flush with the floor.
            if (RailConnector.isRail(this.getLevel().getBlockState(dx, dy - 1, dz))) {
                --dy;
            }

            Block block = this.getLevel().getBlock(dx, dy, dz);
            BlockState state = block.getState();

            if (RailConnector.isRail(state)) {
                processMovement(dx, dy, dz, block);
                if (state.getType() == BlockTypes.ACTIVATOR_RAIL && state.ensureTrait(BlockTraits.IS_POWERED)) {
                    activate(dx, dy, dz, true);
                }
            } else {
                setFalling();
            }

            boolean nowOnRail = isOnRail();
            if (nowOnRail != wasOnRail) {
                // The seat offset differs on and off rail, so push it to all passengers
                // whenever the cart crosses that boundary.
                for (Entity linked : passengers) {
                    linked.setSeatPosition(getMountedOffset(linked));
                    updatePassengerPosition(linked);
                }
                wasOnRail = nowOnRail;
            }

            checkBlockCollision();

            // Update yaw to face the direction of travel.
            pitch = 0;
            float diffX = this.lastPosition.getX() - this.getX();
            float diffZ = this.lastPosition.getZ() - this.getZ();
            float yawToChange = yaw;
            if (diffX * diffX + diffZ * diffZ > 0.001D) {
                yawToChange = (float) (Math.atan2(diffZ, diffX) * 180 / Math.PI);
            }

            // Wrap the yaw difference into a signed range so we can detect a near-180 degree
            // reversal. When the cart crosses that boundary, flip orientation instead of
            // spinning 360 degrees.
            double rotDiff = ((yawToChange - this.yaw) % 360.0 + 540.0) % 360.0 - 180.0;
            if (rotDiff < -170.0 || rotDiff >= 170.0) {
                yawToChange += 180.0f;
                flipped = !flipped;
            }

            setRotation(yawToChange % 360.0f, 0);

            Location from = Location.from(this.lastPosition, lastYaw, lastPitch, this.getLevel());
            Location to = Location.from(this.position, this.yaw, this.pitch, this.getLevel());

            this.getServer().getEventManager().fire(new VehicleUpdateEvent(this));

            if (!from.equals(to)) {
                this.getServer().getEventManager().fire(new VehicleMoveEvent(this, from, to));
            }

            // Push nearby entities and let them push back.
            for (Entity entity : this.getLevel().getNearbyEntities(this, boundingBox.inflate(0.2f, 0, 0.2f))) {
                if (passengers.contains(entity)) {
                    continue;
                }
                entity.onEntityCollision(this);
                onEntityCollision(entity);
            }

            Iterator<Entity> linkedIterator = this.passengers.iterator();

            while (linkedIterator.hasNext()) {
                Entity linked = linkedIterator.next();

                if (!linked.isAlive()) {
                    if (linked.getVehicle() == this) {
                        linked.dismount(this);
                    }

                    linkedIterator.remove();
                }
            }

            // Always return true so the caller keeps ticking this entity.
            return true;
        }

        return false;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (invulnerable) {
            return false;
        } else {
            source.setDamage(source.getDamage() * 15);

            boolean attack = super.attack(source);

            if (isAlive()) {
                performHurtAnimation();
            }

            return attack;
        }
    }

    public void dropItem() {
        this.getLevel().dropItem(this.getPosition(), ItemStack.builder().itemType(ItemTypes.MINECART).build());
    }

    @Override
    public void kill() {
        super.kill();

        if (this.getLevel().getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            dropItem();
        }
    }

    @Override
    public void close() {
        super.close();

        for (Entity entity : passengers) {
            entity.dismount(this);
        }
    }

    @Override
    public boolean onInteract(Player player, ItemStack item, Vector3f clickedPos) {
        if (isRideable() && passengers.isEmpty()) {
            player.mount(this);
        }
        return true;
    }

    @Override
    public void onEntityCollision(Entity entity) {
        if (entity != vehicle) {
            if (entity instanceof EntityLiving
                    && !(entity instanceof EntityHuman)
                    && this.motion.getX() * this.motion.getX() + this.motion.getZ() * this.motion.getZ() > 0.01D
                    && passengers.isEmpty()
                    && entity.getVehicle() == null
                    && !this.hasDisplay()) {
                if (vehicle == null) {
                    entity.mount(this);// TODO: rewrite (weird riding)
                }
            }

            double motiveX = entity.getX() - this.getX();
            double motiveZ = entity.getZ() - this.getZ();
            double square = motiveX * motiveX + motiveZ * motiveZ;

            if (square >= 9.999999747378752E-5D) {
                square = Math.sqrt(square);
                motiveX /= square;
                motiveZ /= square;
                double next = 1 / square;

                if (next > 1) {
                    next = 1;
                }

                motiveX *= next;
                motiveZ *= next;
                motiveX *= 0.1f;
                motiveZ *= 0.1f;
                motiveX *= 1 + entityCollisionReduction;
                motiveZ *= 1 + entityCollisionReduction;
                motiveX *= 0.5D;
                motiveZ *= 0.5D;

                if (entity instanceof EntityAbstractMinecart mine) {
                    float desinityX = mine.getX() - this.getX();
                    float desinityZ = mine.getZ() - this.getZ();
                    Vector3f vector = Vector3f.from(desinityX, 0, desinityZ).normalize();
                    Vector3f vec = Vector3f.from(MathHelper.cos(yaw * 0.017453292F), 0, MathHelper.sin(yaw * 0.017453292F)).normalize();
                    float desinityXZ = Math.abs(vector.dot(vec));

                    if (desinityXZ < 0.8f) {
                        return;
                    }

                    double motX = mine.getMotion().getX() + this.motion.getX();
                    double motZ = mine.getMotion().getZ() + this.motion.getZ();

                    MinecartType myType = getMinecartType();
                    MinecartType theirType = mine.getMinecartType();

                    if (theirType == MinecartType.MINECART_FURNACE && myType != MinecartType.MINECART_FURNACE) {
                        this.motion = this.motion.mul(0.2, 1, 0.2).add(mine.getMotion().getX() - motiveX, 0, mine.getMotion().getZ() - motiveZ);
                        mine.motion = mine.motion.mul(0.95, 1, 0.95);
                    } else if (theirType != MinecartType.MINECART_FURNACE && myType == MinecartType.MINECART_FURNACE) {
                        mine.motion = mine.motion.mul(0.2, 1, 0.2).add(this.motion.getX() + motiveX, 0, this.motion.getZ() + motiveZ);
                        this.motion = this.motion.mul(0.95, 1, 0.95);
                    } else {
                        double avgX = motX / 2;
                        double avgZ = motZ / 2;
                        this.motion = this.motion.mul(0.2, 1, 0.2).add(avgX - motiveX, 0, avgZ - motiveZ);
                        mine.motion = mine.motion.mul(0.2, 1, 0.2).add(avgX + motiveX, 0, avgZ + motiveZ);
                    }
                } else {
                    this.motion = this.motion.add(-(float) motiveX, 0, -(float) motiveZ);
                }
            }
        }
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    protected void activate(int x, int y, int z, boolean flag) {
    }

    private void setFalling() {
        this.motion = Vector3f.from(
                GenericMath.clamp(this.motion.getX(), -getMaxSpeed(), getMaxSpeed()),
                this.motion.getY(),
                GenericMath.clamp(this.motion.getZ(), -getMaxSpeed(), getMaxSpeed())
        );

        if (onGround) {
            this.motion = this.motion.mul(derailed);
        }

        this.move(this.motion);
        if (!onGround) {
            this.motion = this.motion.mul(flying);
        }
    }

    /**
     * Returns the two endpoint offsets for a rail direction as a 2x3 array.
     * Each entry is an {dx, dy, dz} offset from the rail block center to one end of the segment.
     */
    private static int[][] railEndpoints(RailDirection direction) {
        return switch (direction) {
            case NORTH_SOUTH -> new int[][]{{0, 0, -1}, {0, 0, 1}};
            case EAST_WEST -> new int[][]{{-1, 0, 0}, {1, 0, 0}};
            case ASCENDING_EAST -> new int[][]{{-1, -1, 0}, {1, 0, 0}};
            case ASCENDING_WEST -> new int[][]{{-1, 0, 0}, {1, -1, 0}};
            case ASCENDING_NORTH -> new int[][]{{0, 0, -1}, {0, -1, 1}};
            case ASCENDING_SOUTH -> new int[][]{{0, -1, -1}, {0, 0, 1}};
            case SOUTH_EAST -> new int[][]{{0, 0, 1}, {1, 0, 0}};
            case SOUTH_WEST -> new int[][]{{0, 0, 1}, {-1, 0, 0}};
            case NORTH_WEST -> new int[][]{{0, 0, -1}, {-1, 0, 0}};
            case NORTH_EAST -> new int[][]{{0, 0, -1}, {1, 0, 0}};
        };
    }

    private void processMovement(int dx, int dy, int dz, Block block) {
        fallDistance = 0.0F;

        BlockState state = block.getState();
        Map<BlockTrait<?>, Comparable<?>> traits = state.getTraits();

        RailDirection railDirection;
        if (traits.containsKey(BlockTraits.RAIL_DIRECTION)) {
            railDirection = state.ensureTrait(BlockTraits.RAIL_DIRECTION);
        } else if (traits.containsKey(BlockTraits.SIMPLE_RAIL_DIRECTION)) {
            railDirection = state.ensureTrait(BlockTraits.SIMPLE_RAIL_DIRECTION);
        } else {
            return;
        }

        // A powered rail that is not currently powered acts as a brake.
        boolean isPoweredRail = traits.containsKey(BlockTraits.IS_POWERED);
        boolean isPowered = isPoweredRail && state.ensureTrait(BlockTraits.IS_POWERED);
        boolean isSlowed = isPoweredRail && !isPowered;

        // Record the Y position on the rail before moving so we can compute
        // the hill speed adjustment after the cart has advanced.
        Vector3f oldRailPos = getNextRail(this.getPosition());

        float x = this.getX();
        float z = this.getZ();
        // Ascending rails raise the target Y by one relative to the block Y.
        int y = dy;

        // The track constrains vertical motion on rail, so gravity does not accumulate.
        float motionX = this.motion.getX();
        float motionZ = this.motion.getZ();

        // Each ascending rail direction applies a small opposing force to simulate the climb.
        switch (railDirection) {
            case ASCENDING_EAST:
                motionX -= 0.0078125f;
                y++;
                break;
            case ASCENDING_WEST:
                motionX += 0.0078125f;
                y++;
                break;
            case ASCENDING_NORTH:
                motionZ += 0.0078125f;
                y++;
                break;
            case ASCENDING_SOUTH:
                motionZ -= 0.0078125f;
                y++;
                break;
            default:
                break;
        }

        // Direction vectors along the two ends of the rail segment.
        int[][] exits = railEndpoints(railDirection);
        float exitDX = exits[1][0] - exits[0][0];
        float exitDZ = exits[1][2] - exits[0][2];
        float segLen = (float) Math.sqrt(exitDX * exitDX + exitDZ * exitDZ);

        // Flip direction so the cart always moves in the same sense as its velocity.
        if (motionX * exitDX + motionZ * exitDZ < 0) {
            exitDX = -exitDX;
            exitDZ = -exitDZ;
        }

        // Project speed onto the rail axis (cap at 2 m/t).
        float speed = (float) Math.min(2.0, Math.sqrt(motionX * motionX + motionZ * motionZ));
        motionX = speed * exitDX / segLen;
        motionZ = speed * exitDZ / segLen;

        // If a player is riding and pressing forward or backward while the cart is nearly
        // stopped, add a small impulse in the direction the player is facing so the cart
        // can get moving again.
        Entity passenger = getPassenger();
        if (passenger instanceof Player && inputMotionY != 0) {
            float sinYaw = (float) -Math.sin(passenger.getLocation().getYaw() * Math.PI / 180.0);
            float cosYaw = (float) Math.cos(passenger.getLocation().getYaw() * Math.PI / 180.0);
            if (motionX * motionX + motionZ * motionZ < 0.01) {
                motionX += sinYaw * 0.001;
                motionZ += cosYaw * 0.001;
                isSlowed = false;
            }
        }

        // Unpowered powered-rail braking.
        if (isSlowed) {
            float hSpeed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (hSpeed < 0.03) {
                motionX = 0;
                motionZ = 0;
            } else {
                motionX *= 0.5;
                motionZ *= 0.5;
            }
        }

        // Compute the parametric progress of the cart along the current rail segment
        // and snap it to the rail center line.
        float x0 = dx + 0.5f + exits[0][0] * 0.5f;
        float z0 = dz + 0.5f + exits[0][2] * 0.5f;
        float x1 = dx + 0.5f + exits[1][0] * 0.5f;
        float z1 = dz + 0.5f + exits[1][2] * 0.5f;
        float segX = x1 - x0;
        float segZ = z1 - z0;

        float progress;
        if (segX == 0) {
            progress = z - dz;
        } else if (segZ == 0) {
            progress = x - dx;
        } else {
            progress = ((x - x0) * segX + (z - z0) * segZ) * 2;
        }

        // Cart center snapped to the rail line.
        float snapX = x0 + segX * progress;
        float snapZ = z0 + segZ * progress;

        // Scale velocity down when carrying a passenger.
        float scale = passengers.isEmpty() ? 1.0f : 0.75f;
        float advX = (float) GenericMath.clamp(scale * motionX, -getMaxSpeed(), getMaxSpeed());
        float advZ = (float) GenericMath.clamp(scale * motionZ, -getMaxSpeed(), getMaxSpeed());

        // Advance the cart along the rail. On-rail movement bypasses block collision
        // because the rail geometry fully constrains the trajectory.
        float posX = snapX + advX;
        float posZ = snapZ + advZ;
        setPosition(Vector3f.from(posX, y, posZ));

        // On an ascending rail, shift Y by the ramp height once the cart has crossed
        // into one of the raised ends of the segment.
        if (exits[0][1] != 0 && MathHelper.floor(posX) - dx == exits[0][0] && MathHelper.floor(posZ) - dz == exits[0][2]) {
            setPosition(Vector3f.from(posX, y + exits[0][1], posZ));
        } else if (exits[1][1] != 0 && MathHelper.floor(posX) - dx == exits[1][0] && MathHelper.floor(posZ) - dz == exits[1][2]) {
            setPosition(Vector3f.from(posX, y + exits[1][1], posZ));
        }

        // Natural drag applied after position is set.
        float drag = (!passengers.isEmpty() || !slowWhenEmpty) ? 0.997f : 0.96f;
        motionX *= drag;
        motionZ *= drag;

        // Water slows the cart further.
        if (level.getBlock(dx, dy, dz).getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
            motionX *= 0.95f;
            motionZ *= 0.95f;
        }

        // Adjust speed based on slope: downhill gains speed, uphill loses it.
        Vector3f newRailPos = getNextRail(posX, y, posZ);
        if (newRailPos != null && oldRailPos != null) {
            float hillSpeed = (oldRailPos.getY() - newRailPos.getY()) * 0.05f;
            float hSpeed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (hSpeed > 0) {
                motionX = motionX / hSpeed * (hSpeed + hillSpeed);
                motionZ = motionZ / hSpeed * (hSpeed + hillSpeed);
            }
            // Snap Y to the rail surface at the new position.
            setPosition(Vector3f.from(posX, newRailPos.getY(), posZ));
        }

        // If the cart has moved into a new block, realign the velocity direction
        // to the offset between the old and new block centres.
        int newFloorX = MathHelper.floor(posX);
        int newFloorZ = MathHelper.floor(posZ);
        if (newFloorX != dx || newFloorZ != dz) {
            float hSpeed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
            motionX = hSpeed * (newFloorX - dx);
            motionZ = hSpeed * (newFloorZ - dz);
        }

        // A powered rail accelerates the cart, or gives it a small starting push if it is stopped.
        if (isPowered) {
            float hSpeed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (hSpeed > 0.01) {
                motionX += motionX / hSpeed * 0.06f;
                motionZ += motionZ / hSpeed * 0.06f;
            } else if (railDirection == RailDirection.EAST_WEST) {
                if (isNormalBlock(level.getBlock(dx - 1, dy, dz))) motionX = 0.02f;
                else if (isNormalBlock(level.getBlock(dx + 1, dy, dz))) motionX = -0.02f;
            } else if (railDirection == RailDirection.NORTH_SOUTH) {
                if (isNormalBlock(level.getBlock(dx, dy, dz - 1))) motionZ = 0.02f;
                else if (isNormalBlock(level.getBlock(dx, dy, dz + 1))) motionZ = -0.02f;
            }
        }

        this.motion = Vector3f.from(motionX, 0, motionZ);
    }

    private Vector3f getNextRail(Vector3f d) {
        return getNextRail(d.getX(), d.getY(), d.getZ());
    }

    private Vector3f getNextRail(float dx, float dy, float dz) {
        int checkX = MathHelper.floor(dx);
        int checkY = MathHelper.floor(dy);
        int checkZ = MathHelper.floor(dz);

        if (RailConnector.isRail(level.getBlockState(checkX, checkY - 1, checkZ))) {
            --checkY;
        }

        BlockState blockState = level.getBlockState(checkX, checkY, checkZ);

        if (RailConnector.isRail(blockState)) {
            RailDirection dir = RailConnector.getDirection(blockState);
            int[][] facing = railEndpoints(dir);
            float rail;
            float nextOne = checkX + 0.5f + facing[0][0] * 0.5f;
            float nextTwo = checkY + 0.5f + facing[0][1] * 0.5f;
            float nextThree = checkZ + 0.5f + facing[0][2] * 0.5f;
            float nextFour = checkX + 0.5f + facing[1][0] * 0.5f;
            float nextFive = checkY + 0.5f + facing[1][1] * 0.5f;
            float nextSix = checkZ + 0.5f + facing[1][2] * 0.5f;
            float nextSeven = nextFour - nextOne;
            float nextEight = (nextFive - nextTwo) * 2;
            float nextMax = nextSix - nextThree;

            if (nextSeven == 0) {
                rail = dz - checkZ;
            } else if (nextMax == 0) {
                rail = dx - checkX;
            } else {
                float whatOne = dx - nextOne;
                float whatTwo = dz - nextThree;

                rail = (whatOne * nextSeven + whatTwo * nextMax) * 2;
            }

            dx = nextOne + nextSeven * rail;
            dy = nextTwo + nextEight * rail;
            dz = nextThree + nextMax * rail;
            if (nextEight < 0) {
                ++dy;
            }

            if (nextEight > 0) {
                dy += 0.5D;
            }

            return Vector3f.from(dx, dy, dz);
        } else {
            return null;
        }
    }

    public void setInputMotionY(float motionY) {
        inputMotionY = motionY;
    }

    public int getDisplayOffset() {
        return this.data.get(DISPLAY_OFFSET);
    }

    public void setDisplayBlockOffset(int offset) {
        this.data.set(DISPLAY_OFFSET, offset);
    }

    public BlockState getDisplayBlock() {
        CloudBlockDefinition definition = (CloudBlockDefinition) this.data.get(DISPLAY_BLOCK_STATE);
        return definition != null ? definition.getCloudState() : null;
    }

    public void setDisplayBlock(BlockState blockState) {
        CloudBlockDefinition definition = CloudBlockRegistry.REGISTRY.getDefinition(blockState);
        this.data.set(DISPLAY_BLOCK_STATE, definition);
    }

    public boolean hasDisplay() {
        return this.data.get(CUSTOM_DISPLAY) != 0;
    }

    public void setDisplay(boolean display) {
        this.data.set(CUSTOM_DISPLAY, (byte) (display ? 1 : 0));
    }

    public boolean isSlowWhenEmpty() {
        return slowWhenEmpty;
    }

    public void setSlowWhenEmpty(boolean slow) {
        slowWhenEmpty = slow;
    }

    public Vector3f getFlyingVelocityMod() {
        return flying;
    }

    public void setFlyingVelocityMod(Vector3f flying) {
        Objects.requireNonNull(flying, "Flying velocity modifiers cannot be null");
        this.flying = flying;
    }

    public Vector3f getDerailedVelocityMod() {
        return derailed;
    }

    public void setDerailedVelocityMod(Vector3f derailed) {
        Objects.requireNonNull(derailed, "Derailed velocity modifiers cannot be null");
        this.derailed = derailed;
    }

    public void setMaximumSpeed(float speed) {
        maxSpeed = speed;
    }

    protected boolean isOnRail() {
        int dx = this.position.getFloorX();
        int dy = this.position.getFloorY();
        int dz = this.position.getFloorZ();
        if (RailConnector.isRail(level.getBlockState(dx, dy, dz))) {
            return true;
        }
        return RailConnector.isRail(level.getBlockState(dx, dy - 1, dz));
    }

    private boolean isNormalBlock(Block block) {
        return BlockSupport.isCollisionShapeFullBlock(block.getState());
    }
}
