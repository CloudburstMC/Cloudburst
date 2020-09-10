package org.cloudburstmc.server.item.serializer;

import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import lombok.val;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.data.Banner;
import org.cloudburstmc.server.utils.BannerPattern;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

import java.util.ArrayList;
import java.util.List;

public class BannerSerializer implements ItemDataSerializer<Banner> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, Banner value) {
        tag.putInt("Base", value.getBase().getDyeData());
        tag.putInt("Type", value.getType());

        if (!value.getPatterns().isEmpty()) {
            List<NbtMap> patternsTag = new ArrayList<>();
            for (BannerPattern pattern : value.getPatterns()) {
                patternsTag.add(NbtMap.builder().
                        putInt("Color", pattern.getColor().getDyeData() & 0x0f).
                        putString("Pattern", pattern.getType().getName())
                        .build());
            }
            tag.putList("Patterns", NbtType.COMPOUND, patternsTag);
        }
    }

    @Override
    public Banner deserialize(Identifier type, Integer meta, NbtMap tag) {
        val base = DyeColor.getByDyeData(tag.getInt("Base", 0));
        val bannerType = tag.getInt("Type", 0);

        val patternTags = tag.getList("Patterns", NbtType.COMPOUND);
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
