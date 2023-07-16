package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.container.ContainerViewTypes;
import org.cloudburstmc.api.container.view.OffhandView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.entity.CloudEntity;

public class CloudOffhandView extends CloudEntityContainerView implements OffhandView {


    public CloudOffhandView(CloudEntity holder, CloudContainer container) {
        super(ContainerViewTypes.OFFHAND, holder, container);
    }

    @Override
    public ItemStack getOffhandItem() {
        return getItem(0);
    }

    @Override
    public void setOffhandItem(ItemStack item) {
        setItem(0, item);
    }
}
