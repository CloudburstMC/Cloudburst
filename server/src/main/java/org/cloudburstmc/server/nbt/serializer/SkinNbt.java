package org.cloudburstmc.server.nbt.serializer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.skin.AnimatedTextureType;
import org.cloudburstmc.protocol.bedrock.data.skin.AnimationData;
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.server.utils.SkinUtils;

import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Reads and writes the player skin compound.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SkinNbt {

    /**
     * Reads a stored skin.
     *
     * @param skinTag the stored skin compound
     * @return the skin
     */
    public static Skin fromNbt(NbtMap skinTag) {
        SerializedSkin.Builder skin = SerializedSkin.builder();

        skinTag.listenForString("ModelId", skin::skinId);
        if (skinTag.containsKey("Data")) {
            skin.skinData(readImage(skinTag, "Data", "SkinImageWidth", "SkinImageHeight"));
        }

        skinTag.listenForString("CapeId", skin::capeId);
        if (skinTag.containsKey("CapeData")) {
            skin.capeData(readImage(skinTag, "CapeData", "CapeImageWidth", "CapeImageHeight"));
        }

        skinTag.listenForString("GeometryName", skin::geometryName);
        skinTag.listenForString("PlayFabId", skin::playFabId);
        skinTag.listenForString("SkinResourcePatch", skin::skinResourcePatch);
        skinTag.listenForByteArray("GeometryData", bytes -> skin.geometryData(new String(bytes, UTF_8)));
        skinTag.listenForString("GeometryDataEngineVersion", skin::geometryDataEngineVersion);
        skinTag.listenForByteArray("SkinAnimationData", bytes -> skin.animationData(new String(bytes, UTF_8)));
        skinTag.listenForBoolean("PremiumSkin", skin::premium);
        skinTag.listenForBoolean("PersonaSkin", skin::persona);
        skinTag.listenForBoolean("CapeOnClassicSkin", skin::capeOnClassic);
        skinTag.listenForBoolean("TrustedSkin", skin::trusted);
        skinTag.listenForBoolean("OverrideSkin", skin::overridingPlayerAppearance);
        skinTag.listenForString("ProfileHash", skin::profileHash);

        if (skinTag.containsKey("AnimatedImageData")) {
            skin.animations(readAnimations(skinTag.getList("AnimatedImageData", NbtType.COMPOUND)));
        }

        return SkinUtils.fromSerialized(skin.build());
    }

    /**
     * Writes a skin for storage.
     *
     * @param currentSkin the skin to store
     * @return the skin compound
     */
    public static NbtMap toNbt(Skin currentSkin) {
        SerializedSkin nbtSkin = SkinUtils.toSerialized(currentSkin);
        NbtMapBuilder skinTag = NbtMap.builder()
                .putByteArray("Data", nbtSkin.getSkinData().getImage())
                .putInt("SkinImageWidth", nbtSkin.getSkinData().getWidth())
                .putInt("SkinImageHeight", nbtSkin.getSkinData().getHeight())
                .putString("ModelId", nbtSkin.getSkinId())
                .putString("PlayFabId", nbtSkin.getPlayFabId())
                .putString("CapeId", nbtSkin.getCapeId())
                .putByteArray("CapeData", nbtSkin.getCapeData().getImage())
                .putInt("CapeImageWidth", nbtSkin.getCapeData().getWidth())
                .putInt("CapeImageHeight", nbtSkin.getCapeData().getHeight())
                .putByteArray("SkinResourcePatch", nbtSkin.getSkinResourcePatch().getBytes(UTF_8))
                .putByteArray("GeometryData", nbtSkin.getGeometryData().getBytes(UTF_8))
                .putString("GeometryDataEngineVersion", nbtSkin.getGeometryDataEngineVersion())
                .putByteArray("SkinAnimationData", nbtSkin.getAnimationData().getBytes(UTF_8))
                .putBoolean("PremiumSkin", nbtSkin.isPremium())
                .putBoolean("PersonaSkin", nbtSkin.isPersona())
                .putBoolean("CapeOnClassicSkin", nbtSkin.isCapeOnClassic())
                .putBoolean("TrustedSkin", nbtSkin.isTrusted())
                .putBoolean("OverrideSkin", nbtSkin.isOverridingPlayerAppearance())
                .putString("ProfileHash", nbtSkin.getProfileHash());

        List<AnimationData> animations = nbtSkin.getAnimations();
        if (!animations.isEmpty()) {
            skinTag.putList("AnimatedImageData", NbtType.COMPOUND, writeAnimations(animations));
        }

        return skinTag.build();
    }

    private static ImageData readImage(NbtMap tag, String dataKey, String widthKey, String heightKey) {
        byte[] image = tag.getByteArray(dataKey);
        if (!tag.containsKey(widthKey) || !tag.containsKey(heightKey)) {
            return ImageData.of(image);
        }

        return ImageData.of(tag.getInt(widthKey), tag.getInt(heightKey), image);
    }

    private static List<AnimationData> readAnimations(List<NbtMap> tags) {
        List<AnimationData> animations = new ArrayList<>(tags.size());
        for (NbtMap tag : tags) {
            float frames = tag.getFloat("Frames");
            AnimatedTextureType type = AnimatedTextureType.values()[tag.getInt("Type")];
            ImageData image = ImageData.of(tag.getInt("ImageWidth"), tag.getInt("ImageHeight"), tag.getByteArray("Image"));
            animations.add(new AnimationData(image, type, frames));
        }

        return animations;
    }

    private static List<NbtMap> writeAnimations(List<AnimationData> animations) {
        List<NbtMap> tags = new ArrayList<>(animations.size());
        for (AnimationData animation : animations) {
            ImageData image = animation.getImage();
            tags.add(NbtMap.builder()
                    .putFloat("Frames", animation.getFrames())
                    .putInt("Type", animation.getTextureType().ordinal())
                    .putInt("ImageWidth", image.getWidth())
                    .putInt("ImageHeight", image.getHeight())
                    .putByteArray("Image", image.getImage())
                    .build());
        }

        return tags;
    }
}
