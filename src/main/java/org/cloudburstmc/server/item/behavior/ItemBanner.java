package org.cloudburstmc.server.item.behavior;

import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.BannerPattern;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by PetteriM1
 */
public class ItemBanner extends Item {

    public ItemBanner(Identifier id) {
        super(id);
    }

    private final List<BannerPattern> patterns = new ArrayList<>();
    private DyeColor base = DyeColor.WHITE;
    private int type;

    @Override
    public BlockState getBlock() {
        return BlockState.get(BlockIds.STANDING_BANNER);
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForInt("Base", v -> this.base = DyeColor.getByDyeData(v));
        tag.listenForInt("Type", v -> this.type = v);

        tag.listenForList("Patterns", NbtType.COMPOUND, patternTags -> {
            for (NbtMap patternTag : patternTags) {
                String pattern = patternTag.getString("Pattern");
                DyeColor color = DyeColor.getByDyeData(patternTag.getInt("Color"));
                this.patterns.add(new BannerPattern(BannerPattern.Type.getByName(pattern), color));
            }
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putInt("Base", this.base.getDyeData());
        tag.putInt("Type", this.type);

        if (!this.patterns.isEmpty()) {
            List<NbtMap> patternsTag = new ArrayList<>();
            for (BannerPattern pattern : this.patterns) {
                patternsTag.add(NbtMap.builder().
                        putInt("Color", pattern.getColor().getDyeData() & 0x0f).
                        putString("Pattern", pattern.getType().getName())
                        .build());
            }
            tag.putList("Patterns", NbtType.COMPOUND, patternsTag);
        }
    }

    public DyeColor getBaseColor() {
        return this.base;
    }

    public void setBaseColor(DyeColor color) {
        this.base = color;
        this.setMeta(color.getDyeData() & 0x0f);
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public DyeColor getBase() {
        return this.base;
    }

    public void setBase(DyeColor color) {
        this.setMeta(color.getDyeData());
    }

    public int getBannerType() {
        return type;
    }

    public void setBannerType(int type) {
        this.type = type;
    }

    public void addPattern(BannerPattern pattern) {
        this.patterns.add(pattern);
    }

    public BannerPattern getPattern(int index) {
        return this.patterns.get(index);
    }

    public ImmutableList<BannerPattern> getPatterns() {
        return ImmutableList.copyOf(this.patterns);
    }

    public void removePattern(int index) {
        this.patterns.remove(index);
    }

    @Override
    protected void onMetaChange(int newMeta) {
        this.base = DyeColor.getByDyeData(newMeta & 0xf);
    }
}
