package org.cloudburstmc.server.item.data.serializer;

import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import lombok.val;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.item.data.Banner;

import java.util.ArrayList;
import java.util.List;

public class BannerSerializer implements ItemDataSerializer<Banner> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder rootTag, NbtMapBuilder dataTag, Banner value) {
        dataTag.putInt("Base", value.getBase().getDyeData());
        dataTag.putInt("Type", value.getType());

        if (!value.getPatterns().isEmpty()) {
            List<NbtMap> patternsTag = new ArrayList<>();
            for (BannerPattern pattern : value.getPatterns()) {
                patternsTag.add(NbtMap.builder().
                        putInt("Color", pattern.getColor().getDyeData() & 0x0f).
                        putString("Pattern", pattern.getType().getName())
                        .build());
            }
            dataTag.putList("Patterns", NbtType.COMPOUND, patternsTag);
        }
    }

    @Override
    public Banner deserialize(Identifier id, NbtMap rootTag, NbtMap dataTag) {
        val base = DyeColor.getByDyeData(dataTag.getInt("Base", 0));
        val bannerType = dataTag.getInt("Type", 0);

        val patternTags = dataTag.getList("Patterns", NbtType.COMPOUND);
        List<BannerPattern> patterns;

        if (patternTags != null) {
            patterns = new ArrayList<>(patternTags.size());

            for (NbtMap patternTag : patternTags) {
                String pattern = patternTag.getString("Pattern");
                DyeColor color = DyeColor.getByDyeData(patternTag.getInt("Color"));
                patterns.add(new BannerPattern(BannerPattern.Type.getByName(pattern), color));
            }
        } else {
            patterns = ImmutableList.of();
        }

        return Banner.of(bannerType, base, patterns);
    }
}
