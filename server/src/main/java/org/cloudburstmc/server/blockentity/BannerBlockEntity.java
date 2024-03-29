package org.cloudburstmc.server.blockentity;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.Banner;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class BannerBlockEntity extends BaseBlockEntity implements Banner {

    private final List<BannerPattern> patterns = new ArrayList<>();
    private DyeColor base = DyeColor.WHITE;
    private int type;

    public BannerBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean isValid() {
        var b = getBlockState().getType();
        return b == BlockTypes.WALL_BANNER || b == BlockTypes.STANDING_BANNER;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForInt("Base", this::setBase);
        tag.listenForInt("Type", this::setBannerType);

        tag.listenForList("Patterns", NbtType.COMPOUND, patternTags -> {
            for (NbtMap patternTag : patternTags) {
                String pattern = patternTag.getString("Pattern");
                DyeColor color = DyeColor.getByDyeData(patternTag.getInt("Color"));
                this.patterns.add(new BannerPattern(BannerPattern.Type.getByName(pattern), color));
            }
        });
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);

        tag.putInt("Base", this.base.getDyeData());
        tag.putInt("Type", this.type);

        if (!patterns.isEmpty()) {
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

    @Override
    public DyeColor getBase() {
        return this.base;
    }

    private void setBase(int base) {
        this.base = DyeColor.getByDyeData(base);
    }

    @Override
    public void setBase(DyeColor color) {
        this.base = checkNotNull(color, "color");
    }

    @Override
    public int getBannerType() {
        return type;
    }

    @Override
    public void setBannerType(int type) {
        this.type = type;
    }

    @Override
    public void addPattern(BannerPattern pattern) {
        this.patterns.add(pattern);
    }

    @Override
    public BannerPattern getPattern(int index) {
        return this.patterns.get(index);
    }

    @Override
    public ImmutableList<BannerPattern> getPatterns() {
        return ImmutableList.copyOf(this.patterns);
    }

    @Override
    public void removePattern(int index) {
        this.patterns.remove(index);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
