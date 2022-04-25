package org.cloudburstmc.server.item;

import com.nukkitx.nbt.NbtMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Data;
import org.cloudburstmc.api.item.ItemStack;

import java.util.List;

@Data
public class SerializedItem {

    private final ItemStack itemStackReference;
    //TODO Decide how this is going to work
    private int netId;
    private NbtMap nbtData;

    private List<String> canBreak = new ObjectArrayList<>();
    private List<String> canPlaceOn = new ObjectArrayList<>();

    public SerializedItem(ItemStack itemStackReference) {
        this.itemStackReference = itemStackReference;
    }
}
