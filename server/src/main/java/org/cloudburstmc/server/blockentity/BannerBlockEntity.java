package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.Banner;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.blockentity.data.BannerData;
import org.cloudburstmc.server.blockentity.data.BlockEntityData;
import org.cloudburstmc.server.item.data.serializer.BannerDataSerializer;

import java.util.List;
import java.util.Objects;

public class BannerBlockEntity extends BaseBlockEntity implements Banner {

    public BannerBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return BannerData.class;
    }

    @Override
    protected BannerData data() {
        return (BannerData) super.data();
    }

    @Override
    public org.cloudburstmc.api.item.data.BannerData getBannerData() {
        List<org.cloudburstmc.api.util.data.BannerPattern> patterns = this.data().getPatterns();
        return org.cloudburstmc.api.item.data.BannerData.of(this.data().getType(), this.data().getBase(), patterns == null ? List.of() : patterns);
    }

    @Override
    public void setBannerData(org.cloudburstmc.api.item.data.BannerData data) {
        Objects.requireNonNull(data, "data");
        if (!this.getBannerData().equals(data)) {
            this.storeDesign(data);
            this.setDirty();
            if (this.getLevel() != null) {
                this.spawnToAll();
            }
        }
    }

    @Override
    public void applyPlacementItem(ItemStack item) {
        org.cloudburstmc.api.item.data.BannerData supplied = item.get(ItemDataComponents.BANNER_DATA);
        this.storeDesign(supplied == null ? org.cloudburstmc.api.item.data.BannerData.of(0, item.getOrDefault(ItemDataComponents.COLOR, DyeColor.BLACK), List.of()) : supplied);
        this.setCustomName(item.get(ItemDataComponents.CUSTOM_NAME));
        this.setDirty();
        this.spawnToAll();
    }

    private void storeDesign(org.cloudburstmc.api.item.data.BannerData design) {
        this.data().setBase(design.getBase());
        this.data().setType(design.getType());
        this.data().setPatterns(design.getPatterns().isEmpty() ? null : design.getPatterns());
    }

    public ItemStack asItem(boolean includeDesign) {
        org.cloudburstmc.api.item.data.BannerData design = this.getBannerData();
        ItemStackBuilder builder = ItemStack.from(ItemTypes.BANNER).toBuilder()
                .setData(ItemDataComponents.COLOR, design.getBase());
        if (includeDesign) {
            if (design.getType() != 0 || !design.getPatterns().isEmpty()) {
                builder.setData(ItemDataComponents.BANNER_DATA, design);
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
    protected void saveCustomClient(NbtMapBuilder tag) {
        BannerDataSerializer.write(tag, this.getBannerData());
        if (this.hasCustomName()) {
            tag.putString("CustomName", this.getCustomName());
        } else {
            tag.remove("CustomName");
        }
    }
}
