package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Turtle;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.data.CloudTurtleData;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public class EntityTurtle extends EntityAgeableAnimal implements Turtle {

    public EntityTurtle(EntityType<Turtle> type, Location location) {
        super(type, location, new CloudTurtleData(), true);
    }

    @Override
    protected String getDefaultName() {
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
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(30);
        this.data().setHomePosition(this.position.toInt());
    }

    @Override
    protected CloudTurtleData data() {
        return (CloudTurtleData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        Vector3i loaded = this.position.toInt();
        this.data().setHomePosition(Vector3i.from(
                tag.containsKey("HomePosX") ? getInt(tag, "HomePosX", loaded.getX()) : loaded.getX(),
                tag.containsKey("HomePosY") ? getInt(tag, "HomePosY", loaded.getY()) : loaded.getY(),
                tag.containsKey("HomePosZ") ? getInt(tag, "HomePosZ", loaded.getZ()) : loaded.getZ()));
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
    public Location getHome() {
        return Location.from(this.data().getHomePosition().toFloat(), 0, 0, this.level);
    }

    @Override
    public void setHome(Location home) {
        checkArgument(home.getLevel() == this.level, "Home must be in the turtle's level");
        this.data().setHomePosition(home.getPosition().toInt());
    }

    @Override
    protected void onMature() {
        if (this.level.getGameRules().get(GameRules.DO_MOB_LOOT)) {
            this.level.dropItem(this.position, ItemStack.from(ItemTypes.TURTLE_SCUTE));
        }
    }
}
