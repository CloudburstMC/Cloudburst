package org.cloudburstmc.server.nbt;

import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.skin.AnimatedTextureType;
import org.cloudburstmc.protocol.bedrock.data.skin.AnimationData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.server.utils.SkinUtils;

import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Converts the runtime skin data objects hold to today's {@code Skin} compound and
 * back, quirks included: the resource patch is read as a string but written back as
 * bytes, loaded skins always carry zero animations, and dimensions fall back to the
 * imageless form when the width or height tags are missing.
 */
public final class SkinNbt {

    private SkinNbt() {
    }

    /**
     * Reads a stored skin compound as today's load did.
     *
     * @param skinTag the stored skin compound
     * @return the skin
     */
    public static Skin fromNbt(NbtMap skinTag) {
        SerializedSkin.Builder skin = SerializedSkin.builder();

        skinTag.listenForString("ModelId", skin::skinId);
        if (skinTag.containsKey("Data")) {
            byte[] image = skinTag.getByteArray("Data");
            if (skinTag.containsKey("SkinImageWidth") && skinTag.containsKey("SkinImageHeight")) {
                int width = skinTag.getInt("SkinImageWidth");
                int height = skinTag.getInt("SkinImageHeight");
                skin.skinData(org.cloudburstmc.protocol.bedrock.data.skin.ImageData.of(width, height, image));
            } else {
                skin.skinData(org.cloudburstmc.protocol.bedrock.data.skin.ImageData.of(image));
            }
        }

        skinTag.listenForString("CapeId", skin::capeId);
        if (skinTag.containsKey("CapeData")) {
            byte[] capeImage = skinTag.getByteArray("CapeData");
            if (skinTag.containsKey("CapeImageWidth") && skinTag.containsKey("CapeImageHeight")) {
                int width = skinTag.getInt("CapeImageWidth");
                int height = skinTag.getInt("CapeImageHeight");
                skin.capeData(org.cloudburstmc.protocol.bedrock.data.skin.ImageData.of(width, height, capeImage));
            } else {
                skin.capeData(org.cloudburstmc.protocol.bedrock.data.skin.ImageData.of(capeImage));
            }
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
            List<NbtMap> list = skinTag.getList("AnimatedImageData", NbtType.COMPOUND);
            List<AnimationData> animations = new ArrayList<>();
            for (NbtMap animationTag : list) {
                float frames = animationTag.getFloat("Frames");
                AnimatedTextureType type = AnimatedTextureType.values()[animationTag.getInt("Type")];
                byte[] image = animationTag.getByteArray("Image");
                int width = animationTag.getInt("ImageWidth");
                int height = animationTag.getInt("ImageHeight");
                animations.add(new AnimationData(
                        org.cloudburstmc.protocol.bedrock.data.skin.ImageData.of(width, height, image), type, frames));
            }
            skin.animations(animations);
        }
        return SkinUtils.fromSerialized(skin.build());
    }

    /**
     * Writes a live skin as today's save did.
     *
     * @param currentSkin the live skin, never {@code null}
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
            List<NbtMap> animationsTag = new ArrayList<>();
            for (AnimationData animation : animations) {
                animationsTag.add(NbtMap.builder()
                        .putFloat("Frames", animation.getFrames())
                        .putInt("Type", animation.getTextureType().ordinal())
                        .putInt("ImageWidth", animation.getImage().getWidth())
                        .putInt("ImageHeight", animation.getImage().getHeight())
                        .putByteArray("Image", animation.getImage().getImage())
                        .build());
            }
            skinTag.putList("AnimatedImageData", NbtType.COMPOUND, animationsTag);
        }
        return skinTag.build();
    }
}
