package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.Ageable;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.entity.data.CloudAgeableMobData;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

public abstract class EntityAgeableAnimal extends Animal implements Ageable {

    private final boolean savesZeroAge;

    protected EntityAgeableAnimal(EntityType<?> type, Location location, CloudAgeableMobData state, boolean savesZeroAge) {
        super(type, location, state);
        this.savesZeroAge = savesZeroAge;
        if (!savesZeroAge) {
            state.setAge(null);
        }
    }

    @Override
    protected CloudAgeableMobData data() {
        return (CloudAgeableMobData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        this.updateAge(getInt(tag, "Age", 0));
        super.loadCustom(tag);
    }

    @Override
    public int getAge() {
        Integer age = this.data().getAge();
        return age == null ? 0 : age;
    }

    @Override
    public void setAge(int age) {
        boolean changed = this.getAge() != age;
        this.updateAge(age);
        if (changed && this.isValid()) {
            this.markChunkDirty();
        }
    }

    private void updateAge(int age) {
        boolean wasBaby = this.isBaby();
        boolean baby = age < 0;
        this.data().setAge(age == 0 && !this.savesZeroAge ? null : age);
        if (wasBaby != baby) {
            this.data.setFlag(BABY, baby);
            this.onBabyStatusChanged(baby);
        }
    }

    protected void onBabyStatusChanged(boolean baby) {
        this.recalculateBoundingBox();
    }

    @Override
    public boolean entityBaseTick(int elapsedTicks) {
        boolean updated = super.entityBaseTick(elapsedTicks);
        return this.tickAge(elapsedTicks) || updated;
    }

    private boolean tickAge(int elapsedTicks) {
        int age = this.getAge();
        if (!this.isAlive() || elapsedTicks <= 0 || age == 0) {
            return false;
        }

        int nextAge = age < 0 ? (int) Math.min(0L, (long) age + elapsedTicks) : Math.max(0, age - elapsedTicks);
        this.setAge(nextAge);
        if (age < 0 && nextAge == 0) {
            this.onMature();
        }

        return true;
    }

    protected void onMature() {
    }
}
