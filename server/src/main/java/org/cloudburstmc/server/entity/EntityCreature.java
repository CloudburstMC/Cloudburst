package org.cloudburstmc.server.entity;

import lombok.Getter;
import org.cloudburstmc.api.entity.Creature;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.view.CloudArmorView;
import org.cloudburstmc.server.container.view.CloudOffhandView;
import org.cloudburstmc.server.item.ItemUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class EntityCreature extends EntityLiving implements Creature {

    @Getter
    protected final CloudArmorView armor = new CloudArmorView(this, new CloudContainer(4));
    @Getter
    protected final CloudOffhandView offhand = new CloudOffhandView(this, new CloudContainer(1));

    public EntityCreature(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

//        tag.listenForList("Inventory", NbtType.COMPOUND, items -> {
//            for (NbtMap itemTag : items) {
//                getContainer().setItem(itemTag.getByte("Slot"), ItemUtils.deserializeItem(itemTag));
//            }
//        });

        tag.listenForList("Offhand", NbtType.COMPOUND, items -> {
            this.offhand.setOffhandItem(ItemUtils.deserializeItem(items.get(0)));
        });

        tag.listenForList("Armor", NbtType.COMPOUND, items -> {
            CloudContainer container = this.armor.getContainer();
            for (int slot = 0; slot < 4; ++slot) {
                container.setItem(slot, ItemUtils.deserializeItem(items.get(slot)));
            }
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putList("Offhand", NbtType.COMPOUND, ItemUtils.serializeItem(this.offhand.getOffhandItem(), 0));

        List<NbtMap> armor = new ArrayList<>(this.armor.size());
        for (int i = 0; i < this.armor.size(); i++) {
            armor.add(ItemUtils.serializeItem(this.armor.getItem(i), i));
        }
        tag.putList("Armor", NbtType.COMPOUND, armor);
    }
}
