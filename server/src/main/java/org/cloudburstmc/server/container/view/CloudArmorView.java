package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.container.ContainerViewTypes;
import org.cloudburstmc.api.container.view.ArmorView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.entity.CloudEntity;

public class CloudArmorView extends CloudEntityContainerView implements ArmorView {

    public CloudArmorView(CloudEntity entity, CloudContainer container) {
        super(ContainerViewTypes.ARMOR, entity, container);
    }

    @Override
    public ItemStack getHelmet() {
        return getItem(0);
    }

    @Override
    public void setHelmet(ItemStack helmet) {
        setItem(0, helmet);
    }

    @Override
    public ItemStack getChestplate() {
        return getItem(1);
    }

    @Override
    public void setChestplate(ItemStack chestplate) {
        setItem(1, chestplate);
    }

    @Override
    public ItemStack getLeggings() {
        return getItem(2);
    }

    @Override
    public void setLeggings(ItemStack leggings) {
        setItem(2, leggings);
    }

    @Override
    public ItemStack getBoots() {
        return getItem(3);
    }

    @Override
    public void setBoots(ItemStack boots) {
        setItem(3, boots);
    }
}
