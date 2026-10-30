package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.data.TurtleData;
import org.cloudburstmc.api.entity.passive.Turtle;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.math.vector.Vector3i;

import static com.google.common.base.Preconditions.checkArgument;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

public class EntityTurtle extends Animal implements Turtle {

    private int growingAge;
    private Vector3i homePosition = Vector3i.ZERO;

    public EntityTurtle(EntityType<Turtle> type, Location location) {
        super(type, location);
    }

    @Override
    public String getName() {
        return "Turtle";
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.36f : 1.2f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.12f : 0.4f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(30);
        this.homePosition = this.position.toInt();
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return TurtleData.class;
    }

    @Override
    protected void applyData(EntityData data) {
        super.applyData(data);
        TurtleData turtleData = (TurtleData) data;
        this.setAge(turtleData.getAge());
        this.homePosition = Vector3i.from(
                turtleData.getHomePosX() != null ? turtleData.getHomePosX() : this.getPosition().getFloorX(),
                turtleData.getHomePosY() != null ? turtleData.getHomePosY() : this.getPosition().getFloorY(),
                turtleData.getHomePosZ() != null ? turtleData.getHomePosZ() : this.getPosition().getFloorZ()
        );
    }

    @Override
    protected void fillData(EntityData data) {
        super.fillData(data);
        TurtleData turtleData = (TurtleData) data;
        turtleData.setAge(this.growingAge);
        Vector3i home = this.homePosition;
        turtleData.setHomePosX(home.getX());
        turtleData.setHomePosY(home.getY());
        turtleData.setHomePosZ(home.getZ());
    }

    @Override
    public int getAge() {
        return this.growingAge;
    }

    @Override
    public void setAge(int age) {
        boolean baby = this.isBaby();
        this.growingAge = age;
        this.data.setFlag(BABY, age < 0);
        if (baby != this.isBaby()) {
            this.recalculateBoundingBox();
        }
    }

    @Override
    public Location getHome() {
        return Location.from(this.homePosition.toFloat(), 0, 0, this.level);
    }

    @Override
    public void setHome(Location home) {
        checkArgument(home.getLevel() == this.level, "Home must be in the turtle's level");
        this.homePosition = home.getPosition().toInt();
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean updated = super.entityBaseTick(tickDiff);
        if (!this.isAlive() || tickDiff <= 0 || this.growingAge == 0) {
            return updated;
        }

        boolean baby = this.growingAge < 0;
        this.setAge(baby ? (int) Math.min(0L, (long) this.growingAge + tickDiff) : (int) Math.max(0L, (long) this.growingAge - tickDiff));
        if (baby && this.growingAge == 0 && this.level.getGameRules().get(GameRules.DO_MOB_LOOT)) {
            this.level.dropItem(this.position, ItemStack.from(ItemTypes.TURTLE_SCUTE));
        }

        return true;
    }
}
