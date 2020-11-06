package org.cloudburstmc.server.blockentity;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.server.utils.data.BannerPattern;
import org.cloudburstmc.server.utils.data.DyeColor;

public interface Banner extends BlockEntity {

    DyeColor getBase();

    void setBase(DyeColor color);

    int getBannerType();

    void setBannerType(int type);

    void addPattern(BannerPattern pattern);

    BannerPattern getPattern(int index);

    ImmutableList<BannerPattern> getPatterns();

    void removePattern(int index);
}
