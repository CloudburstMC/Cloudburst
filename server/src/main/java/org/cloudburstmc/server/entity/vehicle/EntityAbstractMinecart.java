package org.cloudburstmc.server.entity.vehicle;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.vehicle.Minecart;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.event.vehicle.VehicleMoveEvent;
import org.cloudburstmc.api.event.vehicle.VehicleUpdateEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.api.util.data.RailDirection;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.block.CloudBlockDefinition;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.block.util.RailConnector;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.CloudHumanEntity;
import org.cloudburstmc.server.entity.CloudLivingEntity;
import org.cloudburstmc.server.entity.data.CloudAbstractMinecartData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.math.MathHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

@Log4j2
public abstract class EntityAbstractMinecart extends EntityVehicle implements Minecart {

    private static final int DEFAULT_DISPLAY_OFFSET = 6;
    private static final double RIDER_START_IMPULSE = 0.001;
    private static final double RIDER_START_SPEED_SQUARED = 0.01;

    @Setter
    @Getter
    private boolean slowWhenEmpty = true;
    private Vector3f derailed = Vector3f.from(0.5, 0.5, 0.5);
    private Vector3f flying = Vector3f.from(0.95, 0.95, 0.95);
    private float maxSpeed = 0.4f;
    private boolean wasOnRail = false;
    private boolean flipped = false;
    private Vector2f riderInput = Vector2f.ZERO;

    private final CloudAbstractMinecartData persistenceState;

    public EntityAbstractMinecart(EntityType<?> type, Location location) {
        this(type, location, new CloudAbstractMinecartData());
    }

    protected EntityAbstractMinecart(EntityType<?> type, Location location, CloudAbstractMinecartData state) {
        super(type, location, state);
        this.persistenceState = state;
        this.data.set(CUSTOM_DISPLAY, (byte) 0);
        this.data.set(DISPLAY_OFFSET, DEFAULT_DISPLAY_OFFSET);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(40);
        this.setHealth(40);

        setHurtTime(0);
        setHurtDirection(1);
    }

    @Override
    protected CloudAbstractMinecartData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        this.setDisplayOffset(tag.containsKey("DisplayOffset")
                ? getInt(tag, "DisplayOffset", DEFAULT_DISPLAY_OFFSET) : DEFAULT_DISPLAY_OFFSET);

        boolean customDisplay = getBoolean(tag, "CustomDisplayTile", false);
        if (tag.containsKey("DisplayBlock") || customDisplay) {
            BlockState displayBlock;
            try {
                displayBlock = BlockStateCodec.read(tag.getCompound("DisplayBlock"));
            } catch (UnsupportedOperationException failure) {
                throw new IllegalArgumentException(failure.getMessage(), failure);
            }

            if (customDisplay) {
                this.setDisplay(true);
                this.setDisplayBlock(displayBlock);
            }
        }
        super.loadCustom(tag);
        if (tag.containsKey("DisplayTile") || tag.containsKey("DisplayData")) {
            throw new UnsupportedOperationException("Numeric minecart display data is no longer supported");
        }
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        if (this.getDisplayOffset() != DEFAULT_DISPLAY_OFFSET) {
            tag.putInt("DisplayOffset", this.getDisplayOffset());
        }

        if (this.hasDisplay()) {
            tag.putBoolean("CustomDisplayTile", true);
            tag.putCompound("DisplayBlock", BlockStateCodec.write(this.getDisplayBlock()));
        }
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("DisplayOffset");
        keys.add("CustomDisplayTile");
        keys.add("DisplayBlock");
        return Set.copyOf(keys);
    }

    public abstract MinecartType getMinecartType();

    @Override
    protected String getDefaultName() {
        return "Minecart";
    }

    @Override
    public float getHeight() {
        return 0.7F;
    }

    @Override
    public float getWidth() {
        return 0.98F;
    }

    @Override
    public float getBaseOffset() {
        return isOnRail() ? 0f : 0.35f;
    }

    @Override
    public float getDrag() {
        return 0.1F;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float speed) {
        if (!Float.isFinite(speed) || speed < 0f) {
            throw new IllegalArgumentException("Maximum speed must be finite and non-negative");
        }
        this.maxSpeed = speed;
    }

    public Vector3f getDerailedVelocityMod() {
        return derailed;
    }

    public void setDerailedVelocityMod(Vector3f derailed) {
        Objects.requireNonNull(derailed, "Derailed velocity modifiers cannot be null");
        this.derailed = derailed;
    }

    public Vector3f getFlyingVelocityMod() {
        return flying;
    }

    public void setFlyingVelocityMod(Vector3f flying) {
        Objects.requireNonNull(flying, "Flying velocity modifiers cannot be null");
        this.flying = flying;
    }

    @Override
    public int getDisplayOffset() {
        return this.data.require(DISPLAY_OFFSET);
    }

    @Override
    public void setDisplayOffset(int offset) {
        this.data.set(DISPLAY_OFFSET, offset);
    }

    public BlockState getDisplayBlock() {
        CloudBlockDefinition definition = (CloudBlockDefinition) this.data.get(DISPLAY_BLOCK_STATE);
        return definition != null ? definition.getCloudState() : null;
    }

    public void setDisplayBlock(BlockState blockState) {
        CloudBlockDefinition definition = BlockPalette.INSTANCE.getDefinition(blockState);
        this.data.set(DISPLAY_BLOCK_STATE, definition);
    }

    public boolean hasDisplay() {
        return this.data.require(CUSTOM_DISPLAY) != 0;
    }

    public void setDisplay(boolean display) {
        this.data.set(CUSTOM_DISPLAY, (byte) (display ? 1 : 0));
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

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.isClosed()) {
            return false;
        }

        if (!this.isAlive()) {
            ++this.deadTicks;
            if (this.deadTicks >= 10) {
                this.despawnFromAll();
                this.remove(EntityRemoveCause.DEATH);
            }

            return this.deadTicks < 10;
        }

        if (isAlive()) {
            this.entityBaseTick(elapsedTicks);
            if (this.isClosed() || !this.isAlive()) {
                return false;
            }

            if (getHealth() < 20) {
                setHealth(getHealth() + 1);
            }

            Location from = this.getLocation();
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
                if (state.getType() == BlockTypes.ACTIVATOR_RAIL) {
                    activate(dx, dy, dz, state.ensureTrait(BlockTraits.IS_POWERED));
                }
            } else {
                setFalling();
            }

            boolean nowOnRail = isOnRail();
            if (nowOnRail != wasOnRail) {
                // The seat offset differs on and off rail, so push it to all passengers
                // whenever the cart crosses that boundary.
                for (Entity linked : this.getPassengers()) {
                    ((CloudEntity) linked).setSeatOffset(this.getMountedOffset(linked));
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

            this.updatePassengers();
            this.updateMovement();
            this.flushEntityData();

            Location to = this.getLocation();

            this.getServer().getEventManager().fire(new VehicleUpdateEvent(this));

            if (!from.equals(to)) {
                this.getServer().getEventManager().fire(new VehicleMoveEvent(this, from, to));
            }

            // Push nearby entities and let them push back.
            for (Entity entity : this.getLevel().getNearbyEntities(this, boundingBox.inflate(0.2f, 0, 0.2f))) {
                if (this.hasPassenger(entity)) {
                    continue;
                }

                if (entity instanceof CloudEntity target) {
                    target.onEntityCollision(this);
                }
                onEntityCollision(entity);
            }

            for (Entity linked : this.getPassengers()) {
                if (!linked.isAlive()) {
                    ((CloudEntity) linked).dismount(this, false);
                }
            }

            return true;
        }

        return false;
    }

    private void processMovement(int dx, int dy, int dz, Block block) {
        this.resetFallDistance();

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
        boolean isPoweredRail = state.getType() == BlockTypes.GOLDEN_RAIL;
        boolean isPowered = isPoweredRail && state.ensureTrait(BlockTraits.IS_POWERED);
        boolean isSlowed = isPoweredRail && !isPowered;

        // Record the Y position on the rail before moving so we can compute
        // the hill speed adjustment after the cart has advanced.
        Vector3f oldRailPos = this.projectOntoRail(this.getPosition());

        double x = this.getX();
        double z = this.getZ();
        // Ascending rails raise the target Y by one relative to the block Y.
        int y = dy;

        // The track constrains vertical motion on rail, so gravity does not accumulate.
        double motionX = this.motion.getX();
        double motionZ = this.motion.getZ();

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
        double exitDX = exits[1][0] - exits[0][0];
        double exitDZ = exits[1][2] - exits[0][2];
        double segLen = Math.sqrt(exitDX * exitDX + exitDZ * exitDZ);

        // Flip direction so the cart always moves in the same sense as its velocity.
        if (motionX * exitDX + motionZ * exitDZ < 0) {
            exitDX = -exitDX;
            exitDZ = -exitDZ;
        }

        // Project speed onto the rail axis (cap at 2 m/t).
        double speed = Math.min(2.0, Math.sqrt(motionX * motionX + motionZ * motionZ));
        motionX = speed * exitDX / segLen;
        motionZ = speed * exitDZ / segLen;

        if (motionX * motionX + motionZ * motionZ < RIDER_START_SPEED_SQUARED) {
            Vector3f impulse = this.getRiderStartImpulse();
            if (impulse.lengthSquared() > 0) {
                motionX += impulse.getX();
                motionZ += impulse.getZ();
                isSlowed = false;
            }
        }

        // Unpowered powered-rail braking.
        if (isSlowed) {
            double hSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
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
        double x0 = dx + 0.5 + exits[0][0] * 0.5;
        double z0 = dz + 0.5 + exits[0][2] * 0.5;
        double x1 = dx + 0.5 + exits[1][0] * 0.5;
        double z1 = dz + 0.5 + exits[1][2] * 0.5;
        double segX = x1 - x0;
        double segZ = z1 - z0;

        double progress;
        if (segX == 0) {
            progress = z - dz;
        } else if (segZ == 0) {
            progress = x - dx;
        } else {
            progress = ((x - x0) * segX + (z - z0) * segZ) * 2;
        }

        // Cart center snapped to the rail line.
        double snapX = x0 + segX * progress;
        double snapZ = z0 + segZ * progress;

        // Scale velocity down when carrying a passenger.
        double scale = this.getPassengers().isEmpty() ? 1.0 : 0.75;
        double advX = GenericMath.clamp(scale * motionX, -getMaxSpeed(), getMaxSpeed());
        double advZ = GenericMath.clamp(scale * motionZ, -getMaxSpeed(), getMaxSpeed());

        this.setPosition(Vector3f.from(snapX, y, snapZ));
        this.motion = Vector3f.from(motionX, 0, motionZ);
        this.move((float) advX, 0, (float) advZ);
        double posX = this.getX();
        double posZ = this.getZ();
        motionX = this.motion.getX();
        motionZ = this.motion.getZ();

        // On an ascending rail, shift Y by the ramp height once the cart has crossed
        // into one of the raised ends of the segment.
        if (exits[0][1] != 0 && MathHelper.floor(posX) - dx == exits[0][0] && MathHelper.floor(posZ) - dz == exits[0][2]) {
            setPosition(Vector3f.from(posX, y + exits[0][1], posZ));
        } else if (exits[1][1] != 0 && MathHelper.floor(posX) - dx == exits[1][0] && MathHelper.floor(posZ) - dz == exits[1][2]) {
            setPosition(Vector3f.from(posX, y + exits[1][1], posZ));
        }

        // Natural drag applied after position is set.
        float drag = (!this.getPassengers().isEmpty() || !slowWhenEmpty) ? 0.997f : 0.96f;
        motionX *= drag;
        motionZ *= drag;

        // Water slows the cart further.
        if (level.getBlock(dx, dy, dz).getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
            motionX *= 0.95f;
            motionZ *= 0.95f;
        }

        // Adjust speed based on slope: downhill gains speed, uphill loses it.
        Vector3f newRailPos = this.projectOntoRail(posX, y, posZ);
        if (newRailPos != null && oldRailPos != null) {
            double hillSpeed = ((double) oldRailPos.getY() - newRailPos.getY()) * 0.05;
            double hSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
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
            double hSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
            motionX = hSpeed * (newFloorX - dx);
            motionZ = hSpeed * (newFloorZ - dz);
        }

        // A powered rail accelerates the cart, or gives it a small starting push if it is stopped.
        if (isPowered) {
            double hSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
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

    private Vector3f projectOntoRail(Vector3f position) {
        return this.projectOntoRail(position.getX(), position.getY(), position.getZ());
    }

    private Vector3f projectOntoRail(double x, double y, double z) {
        int railX = MathHelper.floor(x);
        int railY = MathHelper.floor(y);
        int railZ = MathHelper.floor(z);
        if (RailConnector.isRail(this.level.getBlockState(railX, railY - 1, railZ))) {
            --railY;
        }

        BlockState railState = this.level.getBlockState(railX, railY, railZ);
        if (!RailConnector.isRail(railState)) {
            return null;
        }

        int[][] endpoints = railEndpoints(RailConnector.getDirection(railState));
        double startX = railX + 0.5 + endpoints[0][0] * 0.5;
        double startY = railY + 0.5 + endpoints[0][1] * 0.5;
        double startZ = railZ + 0.5 + endpoints[0][2] * 0.5;
        double endX = railX + 0.5 + endpoints[1][0] * 0.5;
        double endY = railY + 0.5 + endpoints[1][1] * 0.5;
        double endZ = railZ + 0.5 + endpoints[1][2] * 0.5;
        double deltaX = endX - startX;
        double deltaY = (endY - startY) * 2;
        double deltaZ = endZ - startZ;

        double progress;
        if (deltaX == 0) {
            progress = z - railZ;
        } else if (deltaZ == 0) {
            progress = x - railX;
        } else {
            progress = ((x - startX) * deltaX + (z - startZ) * deltaZ) * 2;
        }

        double projectedY = startY + deltaY * progress;
        if (deltaY < 0) {
            projectedY++;
        } else if (deltaY > 0) {
            projectedY += 0.5;
        }

        return Vector3f.from(startX + deltaX * progress, projectedY, startZ + deltaZ * progress);
    }

    private boolean isNormalBlock(Block block) {
        return BlockSupport.isCollisionShapeFullBlock(block.getState());
    }

    private void setFalling() {
        this.motion = Vector3f.from(
                GenericMath.clamp(this.motion.getX(), -getMaxSpeed(), getMaxSpeed()),
                this.motion.getY(),
                GenericMath.clamp(this.motion.getZ(), -getMaxSpeed(), getMaxSpeed())
        );

        if (this.isOnGround()) {
            this.motion = this.motion.mul(derailed);
        }

        this.move(this.motion);
        if (!this.isOnGround()) {
            this.motion = this.motion.mul(flying);
        }
    }

    protected void activate(int x, int y, int z, boolean powered) {
    }

    public abstract boolean isRideable();

    public void handleRiderInput(Player driver, Vector2f input) {
        if (!this.isRideable() || this.isClosed() || !driver.isAlive() || !this.isControlling(driver)) {
            return;
        }

        float strafe = input.getX();
        float forward = input.getY();
        this.riderInput = Float.isFinite(strafe) && Float.isFinite(forward)
                ? Vector2f.from(Math.clamp(strafe, -1f, 1f), Math.clamp(forward, -1f, 1f))
                : Vector2f.ZERO;
    }

    private Vector3f getRiderStartImpulse() {
        float strafe = this.riderInput.getX();
        float forward = this.riderInput.getY();
        if ((strafe == 0 && forward == 0) || !(this.getControllingPassenger() instanceof Player driver)) {
            return Vector3f.ZERO;
        }

        double yaw = Math.toRadians(driver.getYaw());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double strength = RIDER_START_IMPULSE / Math.max(1, Math.hypot(strafe, forward));
        return Vector3f.from((strafe * cos - forward * sin) * strength, 0,
                (forward * cos + strafe * sin) * strength);
    }

    @Override
    protected void onDismount(Entity passenger, @Nullable Vector3f preferredPosition, boolean wasControlling) {
        if (wasControlling) {
            this.riderInput = Vector2f.ZERO;
        }
        super.onDismount(passenger, preferredPosition, wasControlling);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.isRideable() && this.getPassengers().isEmpty() && super.canAddPassenger(passenger);
    }

    @Override
    protected Vector3f getPassengerAttachmentPoint(Entity passenger) {
        return Vector3f.from(0f, this.getBaseOffset() + this.getMountedOffset(passenger).getY(), 0f);
    }

    @Override
    public Vector3f getMountedOffset(Entity passenger) {
        return Vector3f.from(0f, 1.02001f, 0f);
    }

    @Override
    public String getInteractionPrompt() {
        return this.isRideable() ? super.getInteractionPrompt() : "";
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        if (isRideable() && this.getPassengers().isEmpty()) {
            ((CloudEntity) player).tryMount(this);
        }

        return true;
    }

    @Override
    public void onEntityCollision(Entity entity) {
        Entity currentVehicle = this.getVehicle();
        if (entity != currentVehicle) {
            if (currentVehicle == null
                    && entity instanceof CloudLivingEntity living
                    && !(entity instanceof CloudHumanEntity)
                    && this.motion.getX() * this.motion.getX() + this.motion.getZ() * this.motion.getZ() > 0.01D
                    && this.getPassengers().isEmpty()
                    && entity.getVehicle() == null
                    && !this.hasDisplay()) {
                living.tryMount(this);
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
                    float densityX = mine.getX() - this.getX();
                    float densityZ = mine.getZ() - this.getZ();

                    Vector3f vector = Vector3f.from(densityX, 0, densityZ).normalize();
                    Vector3f vec = Vector3f.from(MathHelper.cos(yaw * 0.017453292F), 0, MathHelper.sin(yaw * 0.017453292F)).normalize();

                    float densityXZ = Math.abs(vector.dot(vec));
                    if (densityXZ < 0.8f) {
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

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        if (this.isInvulnerable() && !event.getDamageType().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        event.setDamage((float) Math.min((double) event.getDamage() * 15, Float.MAX_VALUE));
        boolean applied = super.applyDamage(event);
        if (applied && this.isAlive()) {
            this.performHurtAnimation();
        }
        return applied;
    }

    @Override
    public void kill() {
        if (this.isClosed() || !this.isAlive()) {
            return;
        }

        super.kill();

        if (this.getLevel().getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            this.getLevel().dropItem(this.getPosition(), ItemStack.from(this.getDropItemType()));
            this.dropContents();
        }
    }

    protected void dropContents() {
    }

    protected ItemType getDropItemType() {
        return ItemTypes.MINECART;
    }
}
