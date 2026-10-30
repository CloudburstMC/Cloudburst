package org.cloudburstmc.server.blockentity;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.blockentity.Banner;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.data.BannerData;
import org.cloudburstmc.api.blockentity.data.BannerPatternData;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class BannerBlockEntity extends BaseBlockEntity implements Banner {

    private final List<BannerPattern> patterns = new ArrayList<>();
    private boolean patternsPresent;
    private DyeColor base = DyeColor.WHITE;
    private int type;

    public BannerBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return BannerData.class;
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        BannerData banner = (BannerData) data;
        this.base = DyeColor.getByDyeData(banner.getBase());
        this.type = banner.getType();
        this.patterns.clear();
        this.patternsPresent = banner.getPatterns() != null;
        if (banner.getPatterns() != null) {
            for (BannerPatternData patternTag : banner.getPatterns()) {
                BannerPattern.Type patternType = BannerPattern.Type.getByName(patternTag.getPattern());
                if (patternType == null) {
                    throw new IllegalArgumentException("Unknown banner pattern: " + patternTag.getPattern());
                }
                this.patterns.add(new BannerPattern(patternType, DyeColor.getByDyeData(patternTag.getColor())));
            }
        }
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        BannerData banner = (BannerData) data;
        banner.setBase(this.base.getDyeData());
        banner.setType(this.type);
        if (!this.patterns.isEmpty()) {
            List<BannerPatternData> patternTags = new ArrayList<>();
            for (BannerPattern pattern : this.patterns) {
                BannerPatternData patternTag = new BannerPatternData();
                patternTag.setColor(pattern.getColor().getDyeData() & 0x0f);
                patternTag.setPattern(pattern.getType().getName());
                patternTags.add(patternTag);
            }
            banner.setPatterns(patternTags);
        }
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        // An explicitly loaded empty list round-trips as an empty list. The DTO
        // only carries non-empty layers, so the client tag stays as today.
        if (this.patternsPresent && this.patterns.isEmpty()) {
            tag.putList("Patterns", NbtType.COMPOUND, List.of());
        }
    }

    @Override
    public DyeColor getBase() {
        return this.base;
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
