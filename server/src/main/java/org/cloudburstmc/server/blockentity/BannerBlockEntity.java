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
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

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
    protected void loadCustom(NbtMap tag) {
        // The presence flag follows the merged DTO, exactly like the previous
        // applyData: a tag that carries Patterns (even an empty one) marks the
        // list present, so the server tag keeps an explicitly loaded empty list.
        // An empty list is stored as absent so the client tag stays as today.
        data().setPatternsPresent(data().getPatterns() != null);
        if (data().getPatterns() != null && data().getPatterns().isEmpty()) {
            data().setPatterns(null);
        }
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        // An explicitly loaded empty list round-trips as an empty list. The DTO
        // only carries non-empty layers, so the client tag stays as today.
        if (data().isPatternsPresent()
                && (data().getPatterns() == null || data().getPatterns().isEmpty())) {
            tag.putList("Patterns", NbtType.COMPOUND, List.of());
        }
    }

    @Override
    public DyeColor getBase() {
        return data().getBase();
    }

    @Override
    public void setBase(DyeColor color) {
        data().setBase(checkNotNull(color, "color"));
    }

    @Override
    public int getBannerType() {
        return data().getType();
    }

    @Override
    public void setBannerType(int type) {
        data().setType(type);
    }

    @Override
    public void addPattern(BannerPattern pattern) {
        List<BannerPatternData> patterns = data().getPatterns() == null
                ? new ArrayList<>()
                : new ArrayList<>(data().getPatterns());
        BannerPatternData layer = new BannerPatternData();
        layer.setColor(pattern.getColor());
        layer.setPattern(pattern.getType());
        patterns.add(layer);
        data().setPatterns(patterns);
    }

    @Override
    public BannerPattern getPattern(int index) {
        return runtimePatterns().get(index);
    }

    @Override
    public ImmutableList<BannerPattern> getPatterns() {
        return ImmutableList.copyOf(runtimePatterns());
    }

    @Override
    public void removePattern(int index) {
        List<BannerPatternData> current = data().getPatterns() == null ? List.of() : data().getPatterns();
        List<BannerPatternData> patterns = new ArrayList<>(current);
        patterns.remove(index);
        // An emptied list is stored as absent so the client tag stays as today;
        // the sticky presence flag still marks it explicitly loaded.
        data().setPatterns(patterns.isEmpty() ? null : patterns);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    private List<BannerPattern> runtimePatterns() {
        List<BannerPatternData> patterns = data().getPatterns();
        if (patterns == null) {
            return List.of();
        }
        List<BannerPattern> runtime = new ArrayList<>(patterns.size());
        for (BannerPatternData layer : patterns) {
            runtime.add(new BannerPattern(layer.getPattern(), layer.getColor()));
        }
        return runtime;
    }
}
