package org.cloudburstmc.api.item.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;

import java.util.List;

/**
 * A banner's variant, base color and ordered pattern layers.
 */
@EqualsAndHashCode
public class BannerData {

    private final int type;
    private final DyeColor base;
    private final ImmutableList<BannerPattern> patterns;

    private BannerData(int type, DyeColor base, ImmutableList<BannerPattern> patterns) {
        this.type = type;
        this.base = base;
        this.patterns = patterns;
    }

    /**
     * @param type     banner variant
     * @param base     base color
     * @param patterns pattern layers from bottom to top
     * @return immutable banner data
     */
    public static BannerData of(int type, DyeColor base, List<BannerPattern> patterns) {
        Preconditions.checkNotNull(base, "base");
        Preconditions.checkNotNull(patterns, "patterns");
        return new BannerData(type, base, ImmutableList.copyOf(patterns));
    }

    /**
     * @return the banner variant
     */
    public int getType() {
        return this.type;
    }

    /**
     * @return the base color
     */
    public DyeColor getBase() {
        return this.base;
    }

    /**
     * @return immutable pattern layers from bottom to top
     */
    public ImmutableList<BannerPattern> getPatterns() {
        return this.patterns;
    }
}
