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
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

public class EntityTurtle extends Animal implements Turtle {

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
        this.data().setHomePosition(this.position.toInt());
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return TurtleData.class;
    }

    @Override
    protected TurtleData data() {
        return (TurtleData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        // Age is a DTO int, but setting it syncs the baby metadata and the
        // collision box, exactly as the old applyData did. An absent key resets
        // to today's default of 0.
        this.setAge(getInt(tag, "Age", 0));
        // Home spans three keys with a per-axis fallback to the loaded
        // position, matching the previous applyData plus transition hook.
        Vector3i loaded = this.position.toInt();
        Vector3i home = this.data().getHomePosition();
        int fallbackX = home == null ? loaded.getX() : home.getX();
        int fallbackY = home == null ? loaded.getY() : home.getY();
        int fallbackZ = home == null ? loaded.getZ() : home.getZ();
        this.data().setHomePosition(Vector3i.from(
                tag.containsKey("HomePosX") ? getInt(tag, "HomePosX", fallbackX) : loaded.getX(),
                tag.containsKey("HomePosY") ? getInt(tag, "HomePosY", fallbackY) : loaded.getY(),
                tag.containsKey("HomePosZ") ? getInt(tag, "HomePosZ", fallbackZ) : loaded.getZ()));
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        Vector3i home = this.data().getHomePosition();
        tag.putInt("HomePosX", home.getX());
        tag.putInt("HomePosY", home.getY());
        tag.putInt("HomePosZ", home.getZ());
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("HomePosX");
        keys.add("HomePosY");
        keys.add("HomePosZ");
        return Set.copyOf(keys);
    }

    @Override
    public int getAge() {
        return this.data().getAge();
    }

    @Override
    public void setAge(int age) {
        boolean baby = this.isBaby();
        this.data().setAge(age);
        this.data.setFlag(BABY, age < 0);
        if (baby != this.isBaby()) {
            this.recalculateBoundingBox();
        }
    }

    @Override
    public Location getHome() {
        return Location.from(this.data().getHomePosition().toFloat(), 0, 0, this.level);
    }

    @Override
    public void setHome(Location home) {
        checkArgument(home.getLevel() == this.level, "Home must be in the turtle's level");
        this.data().setHomePosition(home.getPosition().toInt());
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean updated = super.entityBaseTick(tickDiff);
        if (!this.isAlive() || tickDiff <= 0 || this.getAge() == 0) {
            return updated;
        }

        boolean baby = this.getAge() < 0;
        this.setAge(baby ? (int) Math.min(0L, (long) this.getAge() + tickDiff) : (int) Math.max(0L, (long) this.getAge() - tickDiff));
        if (baby && this.getAge() == 0 && this.level.getGameRules().get(GameRules.DO_MOB_LOOT)) {
            this.level.dropItem(this.position, ItemStack.from(ItemTypes.TURTLE_SCUTE));
        }

        return true;
    }
}
