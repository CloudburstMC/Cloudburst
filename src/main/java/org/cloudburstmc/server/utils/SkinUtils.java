package org.cloudburstmc.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.api.player.skin.data.ImageData;
import org.cloudburstmc.api.player.skin.data.PersonaPiece;
import org.cloudburstmc.api.player.skin.data.PersonaPieceTint;
import org.cloudburstmc.api.player.skin.data.SkinAnimation;
import org.cloudburstmc.protocol.bedrock.data.skin.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@UtilityClass
public class SkinUtils {

    public static Skin fromSerialized(SerializedSkin skin) {
        Skin newSkin = new Skin(skin.getFullSkinId());
        newSkin.setSkinId(skin.getSkinId());
        newSkin.setPlayFabId(skin.getPlayFabId());
        newSkin.setSkinResourcePatch(skin.getSkinResourcePatch());
        newSkin.setSkinData(new ImageData(skin.getSkinData().getWidth(), skin.getSkinData().getWidth(), skin.getSkinData().getImage()));
        List<SkinAnimation> animations = new ArrayList<>();
        for (AnimationData data : skin.getAnimations()) {
            animations.add(new SkinAnimation(
                    new ImageData(data.getImage().getWidth(), data.getImage().getHeight(), data.getImage().getImage()),
                    data.getTextureType().ordinal(),
                    data.getFrames(),
                    data.getExpressionType().ordinal()));
        }
        skin.getPersonaPieces().forEach((piece) -> newSkin.getPersonaPieces().add(new PersonaPiece(piece.getId(), piece.getType(), piece.getPackId(), piece.isDefault(), piece.getProductId())));
        skin.getTintColors().forEach(color -> newSkin.getTintColors().add(new PersonaPieceTint(color.getType(), color.getColors())));
        newSkin.setCapeData(new ImageData(skin.getCapeData().getWidth(), skin.getCapeData().getWidth(), skin.getCapeData().getImage()));
        newSkin.setGeometryData(skin.getGeometryData());
        newSkin.setAnimationData(skin.getAnimationData());
        newSkin.setPremium(skin.isPremium());
        newSkin.setPersona(skin.isPersona());
        newSkin.setCapeOnClassic(skin.isCapeOnClassic());
        newSkin.setCapeId(skin.getCapeId());
        newSkin.setSkinColor(skin.getSkinColor());
        newSkin.setArmSize(skin.getArmSize());
        newSkin.setTrusted(false);
        return newSkin;
    }

    public static SerializedSkin toSerialized(Skin skin) {
        SerializedSkin.Builder builder = SerializedSkin.builder();
        builder.skinId(skin.getSkinId())
                .fullSkinId(skin.getFullSkinId())
                .playFabId(skin.getPlayFabId())
                .skinResourcePatch(skin.getSkinResourcePatch())
                .skinData(org.cloudburstmc.protocol.bedrock.data.skin.ImageData.of(skin.getSkinData().getWidth(), skin.getSkinData().getHeight(), skin.getSkinData().getImage()))
                .capeData(org.cloudburstmc.protocol.bedrock.data.skin.ImageData.of(skin.getCapeData().getWidth(), skin.getCapeData().getHeight(), skin.getCapeData().getImage()))
                .geometryData(skin.getGeometryData())
                .animationData(skin.getAnimationData())
                .premium(skin.isPremium())
                .persona(skin.isPersona())
                .capeOnClassic(skin.isCapeOnClassic())
                .capeId(skin.getCapeId())
                .skinColor(skin.getSkinColor())
                .armSize(skin.getArmSize());

        List<AnimationData> animations = new ArrayList<>();
        List<PersonaPieceData> personas = new ArrayList<>();
        List<PersonaPieceTintData> tints = new ArrayList<>();

        skin.getAnimations().forEach(animation -> animations.add(new AnimationData(org.cloudburstmc.protocol.bedrock.data.skin.ImageData.of(animation.getImage().getWidth(), animation.getImage().getHeight(), animation.getImage().getImage()), AnimatedTextureType.values()[animation.getType()], animation.getFrames(), AnimationExpressionType.values()[animation.getExpression()])));
        skin.getPersonaPieces().forEach(piece -> personas.add(new PersonaPieceData(piece.getId(), piece.getType(), piece.getPackId(), piece.isDefault(), piece.getProductId())));
        skin.getTintColors().forEach(color -> tints.add(new PersonaPieceTintData(color.getPieceType(), color.getColors())));

        builder.animations(animations).personaPieces(personas).tintColors(tints);

        return builder.build();
    }

    public static Skin fromToken(JsonNode skinToken) {
        Skin newSkin = new Skin();

        if (skinToken.has("SkinId")) {
            newSkin.setSkinId(skinToken.get("SkinId").textValue());
        }
        if (skinToken.has("PlayFabId")) {
            newSkin.setPlayFabId(skinToken.get("PlayFabId").textValue());
        }
        if (skinToken.has("SkinResourcePatch")) {
            newSkin.setSkinResourcePatch(new String(Base64.getDecoder().decode(skinToken.get("SkinResourcePatch").textValue()), StandardCharsets.UTF_8));
        }

        newSkin.setSkinData(getImage(skinToken, "Skin"));

        if (skinToken.has("AnimatedImageData")) {
            List<SkinAnimation> animations = new ArrayList<>();
            JsonNode array = skinToken.get("AnimatedImageData");
            for (JsonNode element : array) {
                animations.add(getAnimation(element));
            }
            newSkin.setAnimations(animations);
        }

        if (skinToken.has("PersonaSkin")) {
            newSkin.setPersona(skinToken.get("PersonaSkin").booleanValue());
            if (skinToken.has("PersonaPieces")) {
                for (JsonNode piece : skinToken.get("PersonaPieces")) {
                    newSkin.getPersonaPieces().add(new PersonaPiece(
                            piece.get("PieceId").textValue(),
                            piece.get("PieceType").textValue(),
                            piece.get("PackId").textValue(),
                            piece.get("IsDefault").booleanValue(),
                            piece.get("ProductId").textValue()
                    ));
                }
            }
            if (skinToken.has("PieceTintColors")) {
                for (JsonNode node : skinToken.get("PieceTintColors")) {
                    List<String> colors = new ArrayList<>();
                    for (JsonNode color : node.get("Colors")) {
                        colors.add(color.textValue());
                    }
                    newSkin.getTintColors().add(new PersonaPieceTint(
                            node.get("PieceType").textValue(),
                            colors
                    ));
                }
            }
        }

        newSkin.setCapeData(getImage(skinToken, "Cape"));

        if (skinToken.has("SkinGeometryData")) {
            newSkin.setGeometryData(new String(Base64.getDecoder().decode(skinToken.get("SkinGeometryData").textValue()), StandardCharsets.UTF_8));
        }
        if (skinToken.has("SkinAnimationData")) {
            newSkin.setAnimationData(new String(Base64.getDecoder().decode(skinToken.get("SkinAnimationData").textValue()), StandardCharsets.UTF_8));
        }
        if (skinToken.has("PremiumSkin")) {
            newSkin.setPremium(skinToken.get("PremiumSkin").booleanValue());
        }
        if (skinToken.has("CapeOnClassicSkin")) {
            newSkin.setCapeOnClassic(skinToken.get("CapeOnClassicSkin").booleanValue());
        }
        if (skinToken.has("CapeId")) {
            newSkin.setCapeId(skinToken.get("CapeId").textValue());
        }
        if (skinToken.has("SkinColor")) {
            newSkin.setSkinColor(skinToken.get("SkinColor").textValue());
        }
        if (skinToken.has("ArmSize")) {
            newSkin.setArmSize(skinToken.get("ArmSize").textValue());
        }

        return newSkin;
    }

    private static SkinAnimation getAnimation(JsonNode element) {
        float frames = element.get("Frames").floatValue();
        int type = element.get("Type").intValue();
        byte[] data = Base64.getDecoder().decode(element.get("Image").textValue());
        int width = element.get("ImageWidth").intValue();
        int height = element.get("ImageHeight").intValue();
        int expression = 0;
        if (element.hasNonNull("ExpressionType")) {
            expression = element.get("ExpressionType").intValue();
        }
        return new SkinAnimation(new ImageData(width, height, data), type, frames, expression);
    }

    private static ImageData getImage(JsonNode token, String name) {
        if (token.has(name + "Data")) {
            byte[] skinImage = Base64.getDecoder().decode(token.get(name + "Data").textValue());
            if (token.has(name + "ImageHeight") && token.has(name + "ImageWidth")) {
                int width = token.get(name + "ImageWidth").intValue();
                int height = token.get(name + "ImageHeight").intValue();
                return new ImageData(width, height, skinImage);
            } else {
                switch (skinImage.length / 4) {
                    case 2048:
                        return new ImageData(64, 32, skinImage);
                    case 4096:
                        return new ImageData(64, 64, skinImage);
                    case 8192:
                        return new ImageData(128, 64, skinImage);
                    case 16384:
                        return new ImageData(128, 128, skinImage);
                    default:
                        return ImageData.EMPTY;
                }
            }
        }
        return ImageData.EMPTY;
    }
}
