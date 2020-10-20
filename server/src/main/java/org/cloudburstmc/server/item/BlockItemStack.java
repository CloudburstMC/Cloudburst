package org.cloudburstmc.server.item;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentType;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BlockItemStack extends CloudItemStack {

    protected final BlockState blockState;

    public BlockItemStack(BlockState state, int amount) {
        super(state.getId(), state.getType(), amount);
        this.blockState = state;
    }

    public BlockItemStack(BlockState state, int amount, String itemName, List<String> itemLore, Map<EnchantmentType, EnchantmentInstance> enchantments, Collection<Identifier> canDestroy, Collection<Identifier> canPlaceOn, Map<Class<?>, Object> data, NbtMap nbt, NbtMap dataTag, ItemData networkData) {
        super(state.getId(), state.getType(), amount, itemName, itemLore, enchantments, canDestroy, canPlaceOn, data, nbt, dataTag, networkData);
        this.blockState = state;
    }

    @Override
    public BlockState getBlockState() {
        return this.blockState;
    }

    @Override
    public boolean equals(@Nullable ItemStack other, boolean checkAmount, boolean checkData) {
        if (this == other) return true;
        if (!(other instanceof BlockItemStack)) return false;
        BlockItemStack that = (BlockItemStack) other;

        return this.blockState == that.blockState && (!checkAmount || this.amount == that.amount) &&
                (!checkData || Objects.equals(this.getDataTag(), that.getDataTag()));
    }
}
