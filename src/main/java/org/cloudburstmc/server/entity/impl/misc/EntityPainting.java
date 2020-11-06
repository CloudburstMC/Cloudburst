package org.cloudburstmc.server.entity.impl.misc;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.packet.AddPaintingPacket;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.HangingEntity;
import org.cloudburstmc.server.entity.misc.Painting;
import org.cloudburstmc.server.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.player.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityPainting extends HangingEntity implements Painting {

    public final static Motive[] motives = Motive.values();
    private Motive motive = Motive.KEBAB;

    public EntityPainting(EntityType<Painting> type, Location location) {
        super(type, location);
    }

    public static Motive getMotive(String name) {
        return Motive.from(name, Motive.KEBAB);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForString("Motive", this::setMotive);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putString("Motive", this.motive.title);
    }

    @Override
    public BedrockPacket createAddEntityPacket() {
        AddPaintingPacket addPainting = new AddPaintingPacket();
        addPainting.setUniqueEntityId(this.getUniqueId());
        addPainting.setRuntimeEntityId(this.getRuntimeId());
        addPainting.setPosition(this.getPosition());
        addPainting.setDirection(this.getDirection().getHorizontalIndex());
        addPainting.setMotive(this.motive.title);
        return addPainting;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (super.attack(source)) {
            if (source instanceof EntityDamageByEntityEvent) {
                Entity damager = ((EntityDamageByEntityEvent) source).getDamager();
                if (damager instanceof Player && ((Player) damager).isSurvival() && this.level.getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
                    this.level.dropItem(this.getPosition(), ItemStack.get(ItemTypes.PAINTING));
                }
            }
            this.close();
            return true;
        } else {
            return false;
        }
    }

    public Motive getMotive() {
        return motive;
    }

    private void setMotive(String motive) {
        this.motive = Motive.from(motive);
    }

    public void setMotive(Motive motive) {
        this.motive = motive;
    }
}
