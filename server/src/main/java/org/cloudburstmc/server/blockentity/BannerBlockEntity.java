package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.Banner;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.BannerData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.data.serializer.BannerDataSerializer;

import java.util.List;
import java.util.Objects;

public class BannerBlockEntity extends BaseBlockEntity implements Banner {

    private static final BannerData DEFAULT = BannerData.of(0, DyeColor.WHITE, List.of());

    private BannerData data = DEFAULT;

    public BannerBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public BannerData getBannerData() {
        return this.data;
    }

    @Override
    public void setBannerData(BannerData data) {
        Objects.requireNonNull(data, "data");
        if (!this.data.equals(data)) {
            this.data = data;
            this.setDirty();
            this.spawnToAll();
        }
    }

    @Override
    public void applyPlacementItem(ItemStack item) {
        BannerData supplied = item.get(ItemDataComponents.BANNER_DATA);
        this.data = supplied == null ? BannerData.of(0, item.getOrDefault(ItemDataComponents.COLOR, DyeColor.BLACK), List.of()) : supplied;
        this.setCustomName(item.get(ItemDataComponents.CUSTOM_NAME));
        this.setDirty();
        this.spawnToAll();
    }

    public ItemStack asItem(boolean includeDesign) {
        ItemStackBuilder builder = ItemStack.from(ItemTypes.BANNER).toBuilder().setData(ItemDataComponents.COLOR, this.data.getBase());
        if (includeDesign) {
            if (this.data.getType() != 0 || !this.data.getPatterns().isEmpty()) {
                builder.setData(ItemDataComponents.BANNER_DATA, this.data);
            }

            if (this.hasCustomName()) {
                builder.setData(ItemDataComponents.CUSTOM_NAME, this.getCustomName());
            }
        }

        return builder.build();
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        this.data = Objects.requireNonNullElse(BannerDataSerializer.read(tag), DEFAULT);
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        BannerDataSerializer.write(tag, this.data);
        if (this.hasCustomName()) {
            tag.putString("CustomName", this.getCustomName());
        } else {
            tag.remove("CustomName");
        }
    }
}
