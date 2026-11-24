package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.DeprecatedVillager;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.CloudMob;
import org.cloudburstmc.server.entity.component.MobNameTagInteraction;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

public class EntityDeprecatedVillager extends CloudMob implements DeprecatedVillager {

    public EntityDeprecatedVillager(EntityType<DeprecatedVillager> type, Location location) {
        super(type, location);
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        return MobNameTagInteraction.tryApply(this, player, item);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.8f;
    }

    @Override
    protected String getDefaultName() {
        return "Villager";
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(20);
    }

    public void setBaby(boolean baby) {
        this.data.setFlag(BABY, baby);
        this.setScale(baby ? 0.5f : 1);
    }
}
